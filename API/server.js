const express = require('express');
const bodyParser = require('body-parser');
const fs = require('fs').promises;
const fsSync = require('fs');
const path = require('path');
const { MongoClient } = require('mongodb');

const app = express();
// CONFIGURACIÓN: Cambia el puerto según tus necesidades
const PORT = process.env.PORT || 3000;
const DATA_DIR = path.join(__dirname, 'data');
const CLANS_DIR = path.join(__dirname, 'clans');
const WARS_DIR = path.join(__dirname, 'wars');
const WEEKLY_TOPS_DIR = path.join(__dirname, 'weekly_tops');
const WEEKLY_HISTORY_DIR = path.join(__dirname, 'weekly_history');
const SESSIONS_DIR = path.join(__dirname, 'sessions');

// CONFIGURACIÓN: Configura tu URI de MongoDB aquí
// Formato: mongodb://usuario:contraseña@host:puerto/
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/';
const DB_NAME = 'ptc_database';
let db;
let serversCollection;
let weeklyStatsCollection;
let sessionsCollection;

const SESSION_TIMEOUT = 60 * 1000;
const HEARTBEAT_INTERVAL = 30 * 1000;
const SESSION_CLEANUP_INTERVAL = 30 * 1000;

const activeSessions = new Map();

let isResettingWeekly = false;
let lastResetAttempt = 0;
const RESET_COOLDOWN = 5 * 60 * 1000;

const validateUUID = (uuid) => {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  return uuidRegex.test(uuid);
};

const validateRequestBody = (req, res, next) => {
  if (req.method === 'POST' || req.method === 'PATCH') {
    if (!req.body || typeof req.body !== 'object') {
      return res.status(400).json({ error: true, code: 400, message: 'Invalid request body' });
    }
  }
  next();
};

app.use(bodyParser.json({ limit: '10mb' }));
app.use(validateRequestBody);

app.use((err, req, res, next) => {
  console.error('[ERROR] Unhandled error:', err);
  res.status(500).json({ error: true, code: 500, message: 'Internal server error' });
});

function getWeekNumber(date = new Date()) {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
  return `${d.getUTCFullYear()}-W${String(weekNo).padStart(2, '0')}`;
}

function getNextMonday() {
  const now = new Date();
  const dayOfWeek = now.getUTCDay();
  const daysUntilMonday = dayOfWeek === 0 ? 1 : 8 - dayOfWeek;
  const nextMonday = new Date(now);
  nextMonday.setUTCDate(now.getUTCDate() + daysUntilMonday);
  nextMonday.setUTCHours(0, 0, 0, 0);
  return nextMonday;
}

function isMondayMidnight() {
  const now = new Date();
  const dayOfWeek = now.getUTCDay();
  const hour = now.getUTCHours();
  const minute = now.getUTCMinutes();
  return dayOfWeek === 1 && hour === 0 && minute < 60;
}

function formatPlaytime(ms) {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) {
    return `${days}d ${hours % 24}h ${minutes % 60}m`;
  } else if (hours > 0) {
    return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  } else {
    return `${seconds}s`;
  }
}

async function initializeSession(uuid, playerName, serverName = null) {
  const now = Date.now();
  const session = {
    uuid: uuid,
    playerName: playerName,
    serverName: serverName,
    startTime: now,
    lastHeartbeat: now,
    isActive: true
  };

  activeSessions.set(uuid, session);

  if (sessionsCollection) {
    await sessionsCollection.updateOne(
        { uuid: uuid },
        { $set: session },
        { upsert: true }
    );
  }

  console.log(`[PLAYTIME] Sesión iniciada: ${playerName} (${uuid})`);
  return session;
}

async function updateSessionHeartbeat(uuid) {
  const session = activeSessions.get(uuid);
  if (!session) {
    return null;
  }

  const now = Date.now();
  const previousHeartbeat = session.lastHeartbeat;
  session.lastHeartbeat = now;

  const timeSinceLastHeartbeat = Math.min(now - previousHeartbeat, SESSION_TIMEOUT);

  if (sessionsCollection) {
    await sessionsCollection.updateOne(
        { uuid: uuid },
        { $set: { lastHeartbeat: now } }
    );
  }

  return { session, addedTime: timeSinceLastHeartbeat };
}

async function endSession(uuid) {
  const session = activeSessions.get(uuid);
  if (!session) {
    return null;
  }

  const now = Date.now();
  const sessionDuration = now - session.startTime;

  await updatePlayerPlaytime(uuid, sessionDuration);

  activeSessions.delete(uuid);

  if (sessionsCollection) {
    await sessionsCollection.deleteOne({ uuid: uuid });
  }

  console.log(`[PLAYTIME] Sesión terminada: ${session.playerName} - Duración: ${formatPlaytime(sessionDuration)}`);

  return { session, duration: sessionDuration };
}

async function updatePlayerPlaytime(uuid, addedTime) {
  const filePath = path.join(DATA_DIR, `${uuid}.json`);

  try {
    let playerData;
    try {
      playerData = JSON.parse(await fs.readFile(filePath, 'utf8'));
    } catch (error) {
      return null;
    }

    if (typeof playerData.playtime === 'undefined') {
      playerData.playtime = 0;
    }
    if (typeof playerData.lastSessionEnd === 'undefined') {
      playerData.lastSessionEnd = null;
    }

    playerData.playtime = Math.max(0, (playerData.playtime || 0) + addedTime);
    playerData.lastSessionEnd = Date.now();

    await fs.writeFile(filePath, JSON.stringify(playerData, null, 2));

    await updateWeeklyPlaytime(uuid, addedTime, playerData.playerName);

    return playerData.playtime;
  } catch (error) {
    console.error(`[PLAYTIME ERROR] Error actualizando playtime: ${uuid}`, error);
    return null;
  }
}

async function updateWeeklyPlaytime(uuid, addedTime, playerName) {
  try {
    const weekId = getWeekNumber();
    const weeklyStatsFile = path.join(WEEKLY_TOPS_DIR, 'current.json');

    let weeklyData;
    try {
      weeklyData = JSON.parse(await fs.readFile(weeklyStatsFile, 'utf8'));
    } catch (error) {
      weeklyData = {
        weekId: weekId,
        startDate: new Date().toISOString(),
        players: {}
      };
    }

    if (!weeklyData.players[uuid]) {
      weeklyData.players[uuid] = {
        playerName: playerName || 'Desconocido',
        baseline: { wins: 0, kills: 0, deaths: 0, cores: 0, bDomination: 0 },
        wins: 0,
        kills: 0,
        deaths: 0,
        cores: 0,
        bDomination: 0,
        playtime: 0,
        clanLevel: 1
      };
    }

    weeklyData.players[uuid].playtime = (weeklyData.players[uuid].playtime || 0) + addedTime;

    await fs.writeFile(weeklyStatsFile, JSON.stringify(weeklyData, null, 2));
  } catch (error) {
    console.error('[PLAYTIME ERROR] Error actualizando playtime semanal:', error);
  }
}

async function cleanupStaleSessions() {
  const now = Date.now();
  const staleUuids = [];

  for (const [uuid, session] of activeSessions.entries()) {
    if (now - session.lastHeartbeat > SESSION_TIMEOUT) {
      staleUuids.push(uuid);
    }
  }

  for (const uuid of staleUuids) {
    const session = activeSessions.get(uuid);
    if (session) {
      console.log(`[PLAYTIME] Sesión expirada por timeout: ${session.playerName}`);
      await endSession(uuid);
    }
  }

  if (staleUuids.length > 0) {
    console.log(`[PLAYTIME] Limpieza: ${staleUuids.length} sesiones expiradas`);
  }
}

async function restoreSessionsFromDB() {
  if (!sessionsCollection) return;

  try {
    const sessions = await sessionsCollection.find({}).toArray();
    const now = Date.now();

    for (const session of sessions) {
      if (now - session.lastHeartbeat > SESSION_TIMEOUT) {
        const duration = session.lastHeartbeat - session.startTime;
        await updatePlayerPlaytime(session.uuid, duration);
        await sessionsCollection.deleteOne({ uuid: session.uuid });
        console.log(`[PLAYTIME] Sesión restaurada y cerrada: ${session.playerName}`);
      } else {
        activeSessions.set(session.uuid, session);
        console.log(`[PLAYTIME] Sesión restaurada: ${session.playerName}`);
      }
    }
  } catch (error) {
    console.error('[PLAYTIME ERROR] Error restaurando sesiones:', error);
  }
}

async function archiveCurrentWeek() {
  const currentWeek = getWeekNumber();
  const weeklyStatsFile = path.join(WEEKLY_TOPS_DIR, 'current.json');

  try {
    const stats = await fs.readFile(weeklyStatsFile, 'utf8');
    const archiveFile = path.join(WEEKLY_HISTORY_DIR, `${currentWeek}.json`);
    const archiveData = {
      weekId: currentWeek,
      archivedAt: new Date().toISOString(),
      stats: JSON.parse(stats)
    };
    await fs.writeFile(archiveFile, JSON.stringify(archiveData, null, 2));
    console.log(`[WEEKLY] Archivada semana ${currentWeek}`);
    return true;
  } catch (error) {
    console.error('[WEEKLY ERROR] Error archivando semana:', error);
    return false;
  }
}

async function resetWeeklyStats(isManual = false) {
  if (isResettingWeekly) {
    console.log('[WEEKLY] Reinicio ya en progreso, ignorando...');
    return false;
  }

  const now = Date.now();
  if (now - lastResetAttempt < RESET_COOLDOWN && !isManual) {
    console.log('[WEEKLY] Cooldown activo, ignorando reinicio');
    return false;
  }

  if (!isManual && !isMondayMidnight()) {
    const dayNames = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
    const now = new Date();
    console.error(`[WEEKLY] REINICIO BLOQUEADO - No es lunes! Es ${dayNames[now.getUTCDay()]} a las ${now.getUTCHours()}:${now.getUTCMinutes()} UTC`);
    return false;
  }

  try {
    isResettingWeekly = true;
    lastResetAttempt = now;

    const currentWeek = getWeekNumber();
    console.log(`[WEEKLY] Iniciando reinicio para semana ${currentWeek} (${isManual ? 'MANUAL' : 'AUTOMÁTICO'})`);

    await archiveCurrentWeek();

    const newWeek = getWeekNumber();
    const weeklyStatsFile = path.join(WEEKLY_TOPS_DIR, 'current.json');
    const emptyStats = {
      weekId: newWeek,
      startDate: new Date().toISOString(),
      lastReset: new Date().toISOString(),
      resetMode: isManual ? 'manual' : 'automatic',
      players: {}
    };
    await fs.writeFile(weeklyStatsFile, JSON.stringify(emptyStats, null, 2));

    if (weeklyStatsCollection) {
      await weeklyStatsCollection.deleteMany({});
      await weeklyStatsCollection.insertOne({
        weekId: newWeek,
        resetAt: new Date(),
        resetMode: isManual ? 'manual' : 'automatic',
        stats: {}
      });
    }

    console.log(`[WEEKLY] Reiniciados tops semanales para semana ${newWeek}`);
    console.log(`[WEEKLY] Próximo reinicio: ${getNextMonday().toISOString()}`);

    isResettingWeekly = false;
    return true;
  } catch (error) {
    console.error('[WEEKLY ERROR] Error reiniciando stats semanales:', error);
    isResettingWeekly = false;
    return false;
  }
}

async function scheduleWeeklyReset() {
  const checkAndReset = async () => {
    const now = new Date();
    const dayOfWeek = now.getUTCDay();
    const hour = now.getUTCHours();
    const minute = now.getUTCMinutes();

    if (dayOfWeek === 1 && hour === 0 && minute < 60) {
      console.log(`[WEEKLY] Es lunes ${now.toISOString()} - Verificando reinicio...`);

      try {
        const weeklyStatsFile = path.join(WEEKLY_TOPS_DIR, 'current.json');
        const weeklyData = JSON.parse(await fs.readFile(weeklyStatsFile, 'utf8'));
        const currentWeek = getWeekNumber();

        if (weeklyData.weekId !== currentWeek) {
          console.log(`[WEEKLY] WeekId desactualizado (${weeklyData.weekId} vs ${currentWeek}), reiniciando...`);
          await resetWeeklyStats(false);
        } else {
          console.log(`[WEEKLY] WeekId correcto (${weeklyData.weekId}), no es necesario reiniciar`);
        }
      } catch (error) {
        console.error('[WEEKLY ERROR] Error verificando reinicio:', error);
      }
    }
  };

  setInterval(checkAndReset, 60 * 60 * 1000);
  console.log('[WEEKLY] Scheduler iniciado - Próximo reinicio: ' + getNextMonday().toISOString());
  await checkAndReset();
}

async function updateWeeklyStats(uuid, newPlayerData, previousPlayerData) {
  try {
    const weekId = getWeekNumber();
    const weeklyStatsFile = path.join(WEEKLY_TOPS_DIR, 'current.json');

    let weeklyData;
    try {
      weeklyData = JSON.parse(await fs.readFile(weeklyStatsFile, 'utf8'));

      if (weeklyData.weekId !== weekId) {
        console.warn(`[WEEKLY] ADVERTENCIA: weekId desincronizado (${weeklyData.weekId} vs ${weekId})`);
        console.warn(`[WEEKLY] Los datos continuarán con weekId ${weeklyData.weekId} hasta el próximo reinicio`);
      }
    } catch (error) {
      weeklyData = {
        weekId: weekId,
        startDate: new Date().toISOString(),
        players: {}
      };
    }

    if (!weeklyData.players[uuid]) {
      const baselineStats = previousPlayerData ? {
        wins: previousPlayerData.wins || 0,
        kills: previousPlayerData.kills || 0,
        deaths: previousPlayerData.deaths || 0,
        cores: previousPlayerData.cores || 0,
        bDomination: previousPlayerData.bDomination || 0
      } : {
        wins: 0,
        kills: 0,
        deaths: 0,
        cores: 0,
        bDomination: 0
      };

      weeklyData.players[uuid] = {
        playerName: newPlayerData.playerName || 'Desconocido',
        baseline: baselineStats,
        clanLevel: newPlayerData.clanLevel || 1,
        playtime: 0
      };
    }

    const baseline = weeklyData.players[uuid].baseline;
    const weeklyKills = Math.max(0, (newPlayerData.kills || 0) - baseline.kills);
    const weeklyWins = Math.max(0, (newPlayerData.wins || 0) - baseline.wins);
    const weeklyCores = Math.max(0, (newPlayerData.cores || 0) - baseline.cores);
    const weeklyDeaths = Math.max(0, (newPlayerData.deaths || 0) - baseline.deaths);
    const weeklyDomination = Math.max(0, (newPlayerData.bDomination || 0) - baseline.bDomination);

    weeklyData.players[uuid] = {
      playerName: newPlayerData.playerName || weeklyData.players[uuid].playerName,
      baseline: baseline,
      wins: weeklyWins,
      kills: weeklyKills,
      deaths: weeklyDeaths,
      cores: weeklyCores,
      bDomination: weeklyDomination,
      clanLevel: newPlayerData.clanLevel || 1,
      playtime: weeklyData.players[uuid].playtime || 0
    };

    await fs.writeFile(weeklyStatsFile, JSON.stringify(weeklyData, null, 2));
  } catch (error) {
    console.error('[WEEKLY ERROR] Error actualizando stats semanales:', error);
  }
}

async function initializeMongoDB() {
  try {
    const client = new MongoClient(MONGODB_URI);
    await client.connect();
    console.log('[MONGODB] Connected successfully');
    db = client.db(DB_NAME);
    serversCollection = db.collection('servers');
    weeklyStatsCollection = db.collection('weekly_stats');
    sessionsCollection = db.collection('active_sessions');

    await serversCollection.createIndex({ serverName: 1 }, { unique: true });
    await serversCollection.createIndex({ lastUpdate: 1 });
    await serversCollection.createIndex({ modeCW: 1 });
    await weeklyStatsCollection.createIndex({ weekId: 1 }, { unique: true });
    await sessionsCollection.createIndex({ uuid: 1 }, { unique: true });
    await sessionsCollection.createIndex({ lastHeartbeat: 1 });

    const currentWeek = getWeekNumber();
    const existingWeek = await weeklyStatsCollection.findOne({ weekId: currentWeek });
    if (!existingWeek) {
      await weeklyStatsCollection.insertOne({
        weekId: currentWeek,
        resetAt: new Date(),
        stats: {}
      });
    }

    await restoreSessionsFromDB();
  } catch (error) {
    console.error('[MONGODB ERROR] Failed to connect:', error);
    process.exit(1);
  }
}

const directories = [DATA_DIR, CLANS_DIR, WARS_DIR, WEEKLY_TOPS_DIR, WEEKLY_HISTORY_DIR, SESSIONS_DIR];
for (const dir of directories) {
  if (!fsSync.existsSync(dir)) {
    fsSync.mkdirSync(dir, { recursive: true });
  }
}

const currentWeekFile = path.join(WEEKLY_TOPS_DIR, 'current.json');
if (!fsSync.existsSync(currentWeekFile)) {
  const weekId = getWeekNumber();
  fsSync.writeFileSync(currentWeekFile, JSON.stringify({
    weekId: weekId,
    startDate: new Date().toISOString(),
    players: {}
  }, null, 2));
}

const validatePlayerData = (data) => {
  if (!data || typeof data !== 'object') {
    throw new Error('Invalid player data');
  }

  const playerLevels = data.playerLevels || {};
  const validatedLevels = {
    level: Math.max(0, parseInt(playerLevels.level) || 0),
    expLevel: Math.max(0, Math.min(1, parseFloat(playerLevels.expLevel) || 0)),
    totalExp: Math.max(0, parseInt(playerLevels.totalExp) || 0)
  };

  let playerName = 'Desconocido';
  if (data.playerName && typeof data.playerName === 'string' && data.playerName.trim() !== '') {
    playerName = data.playerName.trim();
  }

  return {
    playerName: playerName,
    playerLevels: validatedLevels,
    multiplier: Math.max(1, Math.min(10, parseInt(data.multiplier) || 1)),
    multiplierExpiration: Math.max(0, parseInt(data.multiplierExpiration) || 0),
    wins: Math.max(0, parseInt(data.wins) || 0),
    kills: Math.max(0, parseInt(data.kills) || 0),
    deaths: Math.max(0, parseInt(data.deaths) || 0),
    cores: Math.max(0, parseInt(data.cores) || 0),
    bDomination: Math.max(0, parseInt(data.bDomination) || 0),
    bKillStreak: Math.max(0, parseInt(data.bKillStreak) || 0),
    coins: Math.max(0, parseInt(data.coins) || 0),
    clanLevel: Math.max(1, Math.min(100, parseInt(data.clanLevel) || 1)),
    clanXP: Math.max(0, parseInt(data.clanXP) || 0),
    playtime: Math.max(0, parseInt(data.playtime) || 0),
    lastSessionEnd: data.lastSessionEnd || null
  };
};

app.post('/ptc/playtime/:uuid/start', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }

  const { playerName, serverName } = req.body;

  if (!playerName || typeof playerName !== 'string' || playerName.trim() === '') {
    return res.status(400).json({ error: true, code: 400, message: 'playerName is required' });
  }

  try {
    if (activeSessions.has(uuid)) {
      await endSession(uuid);
    }

    const session = await initializeSession(uuid, playerName.trim(), serverName || null);

    res.json({
      error: false,
      code: 200,
      message: 'Session started',
      data: {
        sessionId: uuid,
        startTime: session.startTime,
        serverName: session.serverName,
        heartbeatInterval: HEARTBEAT_INTERVAL
      }
    });
  } catch (error) {
    console.error(`[ERROR] Error starting session: ${uuid}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error starting session' });
  }
});

app.post('/ptc/playtime/:uuid/heartbeat', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }

  try {
    const result = await updateSessionHeartbeat(uuid);

    if (!result) {
      return res.status(404).json({
        error: true,
        code: 404,
        message: 'No active session found. Call /start first.',
        data: { needsRestart: true }
      });
    }

    const currentSessionTime = Date.now() - result.session.startTime;

    res.json({
      error: false,
      code: 200,
      message: 'Heartbeat received',
      data: {
        sessionTime: currentSessionTime,
        sessionTimeFormatted: formatPlaytime(currentSessionTime),
        lastHeartbeat: result.session.lastHeartbeat
      }
    });
  } catch (error) {
    console.error(`[ERROR] Error processing heartbeat: ${uuid}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error processing heartbeat' });
  }
});

app.post('/ptc/playtime/:uuid/end', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }

  try {
    const result = await endSession(uuid);

    if (!result) {
      return res.status(404).json({
        error: true,
        code: 404,
        message: 'No active session found'
      });
    }

    let totalPlaytime = 0;
    try {
      const playerData = JSON.parse(await fs.readFile(path.join(DATA_DIR, `${uuid}.json`), 'utf8'));
      totalPlaytime = playerData.playtime || 0;
    } catch (e) {}

    res.json({
      error: false,
      code: 200,
      message: 'Session ended',
      data: {
        sessionDuration: result.duration,
        sessionDurationFormatted: formatPlaytime(result.duration),
        totalPlaytime: totalPlaytime,
        totalPlaytimeFormatted: formatPlaytime(totalPlaytime)
      }
    });
  } catch (error) {
    console.error(`[ERROR] Error ending session: ${uuid}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error ending session' });
  }
});

app.get('/ptc/playtime/:uuid', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }

  try {
    const filePath = path.join(DATA_DIR, `${uuid}.json`);
    let playerData;

    try {
      playerData = JSON.parse(await fs.readFile(filePath, 'utf8'));
    } catch (error) {
      if (error.code === 'ENOENT') {
        return res.status(404).json({ error: true, code: 404, message: 'Player not found' });
      }
      throw error;
    }

    const totalPlaytime = playerData.playtime || 0;
    const activeSession = activeSessions.get(uuid);
    let currentSessionTime = 0;

    if (activeSession) {
      currentSessionTime = Date.now() - activeSession.startTime;
    }

    let weeklyPlaytime = 0;
    try {
      const weeklyData = JSON.parse(await fs.readFile(path.join(WEEKLY_TOPS_DIR, 'current.json'), 'utf8'));
      if (weeklyData.players[uuid]) {
        weeklyPlaytime = weeklyData.players[uuid].playtime || 0;
      }
    } catch (e) {}

    res.json({
      error: false,
      code: 200,
      data: {
        uuid: uuid,
        playerName: playerData.playerName,
        totalPlaytime: totalPlaytime,
        totalPlaytimeFormatted: formatPlaytime(totalPlaytime),
        weeklyPlaytime: weeklyPlaytime,
        weeklyPlaytimeFormatted: formatPlaytime(weeklyPlaytime),
        isOnline: !!activeSession,
        currentSession: activeSession ? {
          serverName: activeSession.serverName,
          startTime: activeSession.startTime,
          currentDuration: currentSessionTime,
          currentDurationFormatted: formatPlaytime(currentSessionTime)
        } : null,
        lastSessionEnd: playerData.lastSessionEnd
      }
    });
  } catch (error) {
    console.error(`[ERROR] Error reading playtime: ${uuid}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading playtime' });
  }
});

app.get('/ptc/playtime/sessions/active', async (req, res) => {
  try {
    const sessions = [];
    const now = Date.now();

    for (const [uuid, session] of activeSessions.entries()) {
      sessions.push({
        uuid: uuid,
        playerName: session.playerName,
        serverName: session.serverName,
        startTime: session.startTime,
        duration: now - session.startTime,
        durationFormatted: formatPlaytime(now - session.startTime),
        lastHeartbeat: session.lastHeartbeat
      });
    }

    sessions.sort((a, b) => b.duration - a.duration);

    res.json({
      error: false,
      code: 200,
      data: {
        count: sessions.length,
        sessions: sessions
      }
    });
  } catch (error) {
    console.error('[ERROR] Error reading active sessions:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading active sessions' });
  }
});

app.get('/ptc/top/playtime', async (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 10, 100);

  try {
    const files = await fs.readdir(DATA_DIR);
    const players = [];

    for (const file of files) {
      if (file.endsWith('.json')) {
        try {
          const uuid = file.replace('.json', '');
          const data = JSON.parse(await fs.readFile(path.join(DATA_DIR, file), 'utf8'));
          const playtime = data.playtime || 0;

          if (playtime > 0) {
            let clanDisplayName = null;
            try {
              const clanFiles = await fs.readdir(CLANS_DIR);
              for (const clanFile of clanFiles) {
                if (clanFile.endsWith('.json')) {
                  const clanData = JSON.parse(await fs.readFile(path.join(CLANS_DIR, clanFile), 'utf8'));
                  const member = clanData.members.find(m => m.uuid === uuid);
                  if (member) {
                    clanDisplayName = clanData.displayName || clanData.name;
                    break;
                  }
                }
              }
            } catch (e) {}

            const isOnline = activeSessions.has(uuid);

            players.push({
              uuid: uuid,
              playerName: data.playerName || 'Desconocido',
              playtime: playtime,
              playtimeFormatted: formatPlaytime(playtime),
              clanDisplayName: clanDisplayName,
              isOnline: isOnline
            });
          }
        } catch (e) {}
      }
    }

    players.sort((a, b) => b.playtime - a.playtime);
    const topPlayers = players.slice(0, limit);

    res.json({ error: false, code: 200, data: topPlayers });
  } catch (error) {
    console.error('[ERROR] Error calculating playtime top:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error calculating playtime top', data: [] });
  }
});

app.get('/ptc/top/weekly/playtime', async (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 10, 100);

  try {
    const weeklyStatsFile = path.join(WEEKLY_TOPS_DIR, 'current.json');
    const weeklyData = JSON.parse(await fs.readFile(weeklyStatsFile, 'utf8'));

    const players = [];
    for (const [uuid, stats] of Object.entries(weeklyData.players)) {
      const playtime = stats.playtime || 0;
      if (playtime > 0) {
        let clanDisplayName = null;
        try {
          const clanFiles = await fs.readdir(CLANS_DIR);
          for (const clanFile of clanFiles) {
            if (clanFile.endsWith('.json')) {
              const clanData = JSON.parse(await fs.readFile(path.join(CLANS_DIR, clanFile), 'utf8'));
              const member = clanData.members.find(m => m.uuid === uuid);
              if (member) {
                clanDisplayName = clanData.displayName || clanData.name;
                break;
              }
            }
          }
        } catch (e) {}

        const isOnline = activeSessions.has(uuid);

        players.push({
          uuid: uuid,
          playerName: stats.playerName,
          playtime: playtime,
          playtimeFormatted: formatPlaytime(playtime),
          clanDisplayName: clanDisplayName,
          clanLevel: stats.clanLevel,
          isOnline: isOnline
        });
      }
    }

    players.sort((a, b) => b.playtime - a.playtime);
    const topPlayers = players.slice(0, limit);

    res.json({
      error: false,
      code: 200,
      data: {
        weekId: weeklyData.weekId,
        startDate: weeklyData.startDate,
        resetIn: getNextMonday().getTime() - Date.now(),
        players: topPlayers
      }
    });
  } catch (error) {
    console.error('[ERROR] Error calculating weekly playtime top:', error);
    res.status(500).json({
      error: true, code: 500, message: 'Error calculating weekly playtime top',
      data: { weekId: getWeekNumber(), players: [] }
    });
  }
});

app.get('/ptc/player/:uuid/coins', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }
  const filePath = path.join(DATA_DIR, `${uuid}.json`);
  try {
    const data = JSON.parse(await fs.readFile(filePath, 'utf8'));
    const coins = data.coins || 0;
    res.json({ error: false, code: 200, data: { coins: coins } });
  } catch (error) {
    if (error.code === 'ENOENT') {
      res.status(403).json({ error: true, code: 403, message: 'Player not found', data: { coins: 0 } });
    } else {
      console.error(`[ERROR] Error reading coins: ${uuid}`, error);
      res.status(500).json({ error: true, code: 500, message: 'Error reading player coins' });
    }
  }
});

app.patch('/ptc/player/:uuid/coins', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }
  const filePath = path.join(DATA_DIR, `${uuid}.json`);
  const { coins } = req.body;
  if (typeof coins !== 'number' && typeof coins !== 'string') {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid coins value' });
  }
  try {
    let playerData;
    try {
      playerData = JSON.parse(await fs.readFile(filePath, 'utf8'));
    } catch (error) {
      playerData = {
        playerName: 'Desconocido',
        playerLevels: { level: 0, expLevel: 0.0, totalExp: 0 },
        multiplier: 1, multiplierExpiration: 0, wins: 0, kills: 0, deaths: 0,
        cores: 0, bDomination: 0, bKillStreak: 0, coins: 0, clanLevel: 1, clanXP: 0,
        playtime: 0, lastSessionEnd: null
      };
    }
    playerData.coins = Math.max(0, parseInt(coins) || 0);
    await fs.writeFile(filePath, JSON.stringify(playerData, null, 2));
    res.json({ error: false, code: 200, message: 'Coins updated successfully', data: { coins: playerData.coins } });
  } catch (error) {
    console.error(`[ERROR] Error updating coins: ${uuid}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error updating coins' });
  }
});

app.post('/ptc/player/:uuid/coins/update', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }
  const filePath = path.join(DATA_DIR, `${uuid}.json`);
  const { coins } = req.body;
  if (typeof coins !== 'number' && typeof coins !== 'string') {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid coins value' });
  }
  try {
    let playerData;
    try {
      playerData = JSON.parse(await fs.readFile(filePath, 'utf8'));
    } catch (error) {
      playerData = {
        playerName: 'Desconocido',
        playerLevels: { level: 0, expLevel: 0.0, totalExp: 0 },
        multiplier: 1, multiplierExpiration: 0, wins: 0, kills: 0, deaths: 0,
        cores: 0, bDomination: 0, bKillStreak: 0, coins: 0, clanLevel: 1, clanXP: 0,
        playtime: 0, lastSessionEnd: null
      };
    }
    playerData.coins = Math.max(0, parseInt(coins) || 0);
    await fs.writeFile(filePath, JSON.stringify(playerData, null, 2));
    res.json({ error: false, code: 200, message: 'Coins updated successfully', data: { coins: playerData.coins } });
  } catch (error) {
    console.error(`[ERROR] Error updating coins: ${uuid}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error updating coins' });
  }
});

app.get('/ptc/player/:uuid', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }
  const filePath = path.join(DATA_DIR, `${uuid}.json`);
  try {
    const data = JSON.parse(await fs.readFile(filePath, 'utf8'));
    if (typeof data.coins === 'undefined') data.coins = 0;
    if (typeof data.multiplierExpiration === 'undefined') data.multiplierExpiration = 0;
    if (typeof data.clanLevel === 'undefined') data.clanLevel = 1;
    if (typeof data.clanXP === 'undefined') data.clanXP = 0;
    if (typeof data.bKillStreak === 'undefined') data.bKillStreak = 0;
    if (typeof data.playtime === 'undefined') data.playtime = 0;
    if (typeof data.lastSessionEnd === 'undefined') data.lastSessionEnd = null;
    if (!data.playerName || data.playerName.trim() === '') data.playerName = 'Desconocido';

    const activeSession = activeSessions.get(uuid);
    data.isOnline = !!activeSession;
    if (activeSession) {
      data.currentSession = {
        serverName: activeSession.serverName,
        duration: Date.now() - activeSession.startTime
      };
    }

    res.json({ error: false, code: 200, data: data });
  } catch (error) {
    if (error.code === 'ENOENT') {
      res.status(403).json({ error: true, code: 403, message: 'Player not found' });
    } else {
      console.error(`[ERROR] Error reading player: ${uuid}`, error);
      res.status(500).json({ error: true, code: 500, message: 'Error reading player data' });
    }
  }
});

app.post('/ptc/player/:uuid', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }
  const filePath = path.join(DATA_DIR, `${uuid}.json`);
  const playerData = req.body;

  if (!playerData.playerName ||
      typeof playerData.playerName !== 'string' ||
      playerData.playerName.trim() === '' ||
      playerData.playerName.trim() === 'Desconocido' ||
      playerData.playerName.trim() === 'undefined' ||
      playerData.playerName.trim() === 'null') {

    console.error(`[ERROR] Intento de guardar con playerName inválido para UUID: ${uuid}`);
    console.error(`[ERROR] PlayerName recibido: "${playerData.playerName}"`);

    return res.status(400).json({
      error: true,
      code: 400,
      message: 'PlayerName inválido. No se permite vacío, null, "Desconocido", "undefined" o "null"'
    });
  }

  try {
    let previousData = null;
    try {
      previousData = JSON.parse(await fs.readFile(filePath, 'utf8'));
    } catch (error) {}

    if (previousData && previousData.playtime) {
      playerData.playtime = previousData.playtime;
    }
    if (previousData && previousData.lastSessionEnd) {
      playerData.lastSessionEnd = previousData.lastSessionEnd;
    }

    const validatedData = validatePlayerData(playerData);

    await updateWeeklyStats(uuid, validatedData, previousData);
    await fs.writeFile(filePath, JSON.stringify(validatedData, null, 2));
    res.json({ error: false, code: 200, message: 'Player data saved successfully' });
  } catch (error) {
    console.error(`[ERROR] Error saving player: ${uuid}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error saving player data' });
  }
});

app.delete('/ptc/player/:uuid', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }
  const filePath = path.join(DATA_DIR, `${uuid}.json`);
  try {
    if (activeSessions.has(uuid)) {
      await endSession(uuid);
    }
    await fs.unlink(filePath);
    res.json({ error: false, code: 200, message: 'Player deleted successfully' });
  } catch (error) {
    if (error.code === 'ENOENT') {
      res.status(404).json({ error: true, code: 404, message: 'Player not found' });
    } else {
      console.error(`[ERROR] Error deleting player: ${uuid}`, error);
      res.status(500).json({ error: true, code: 500, message: 'Error deleting player' });
    }
  }
});

app.get('/ptc/server/all', async (req, res) => {
  try {
    const now = Date.now();
    const cutoffTime = now - 30000;
    const activeServers = await serversCollection.find({ lastUpdate: { $gte: cutoffTime } }).toArray();
    const cleanedServers = activeServers.map(server => {
      const { _id, ...cleanServer } = server;
      return cleanServer;
    });
    res.json({ error: false, code: 200, data: cleanedServers });
  } catch (error) {
    console.error('[ERROR] Error reading servers:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading servers', data: [] });
  }
});

app.get('/ptc/server/normal', async (req, res) => {
  try {
    const now = Date.now();
    const cutoffTime = now - 30000;
    const normalServers = await serversCollection.find({
      lastUpdate: { $gte: cutoffTime },
      $or: [{ modeCW: { $exists: false } }, { modeCW: false }]
    }).toArray();
    const cleanedServers = normalServers.map(server => {
      const { _id, ...cleanServer } = server;
      return cleanServer;
    });
    res.json({ error: false, code: 200, data: cleanedServers });
  } catch (error) {
    console.error('[ERROR] Error reading normal servers:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading normal servers', data: [] });
  }
});

app.get('/ptc/server/cw', async (req, res) => {
  try {
    const now = Date.now();
    const cutoffTime = now - 30000;
    const cwServers = await serversCollection.find({
      lastUpdate: { $gte: cutoffTime },
      modeCW: true
    }).toArray();
    const cleanedServers = cwServers.map(server => {
      const { _id, ...cleanServer } = server;
      return cleanServer;
    });
    res.json({ error: false, code: 200, data: cleanedServers });
  } catch (error) {
    console.error('[ERROR] Error reading CW servers:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading CW servers', data: [] });
  }
});

app.get('/ptc/server/:name', async (req, res) => {
  const serverName = req.params.name;
  try {
    const server = await serversCollection.findOne({ serverName: serverName });
    if (server) {
      const { _id, ...cleanServer } = server;
      res.json({ error: false, code: 200, data: cleanServer });
    } else {
      res.status(404).json({ error: true, code: 404, message: 'Server not found' });
    }
  } catch (error) {
    console.error(`[ERROR] Error reading server ${serverName}:`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading server data' });
  }
});

app.post('/ptc/server/:name', async (req, res) => {
  const serverName = req.params.name;
  const serverData = req.body;
  try {
    serverData.lastUpdate = Date.now();
    serverData.serverName = serverName;
    if (typeof serverData.modeCW === 'undefined') {
      serverData.modeCW = false;
    }
    await serversCollection.updateOne(
        { serverName: serverName },
        { $set: serverData },
        { upsert: true }
    );
    res.json({ error: false, code: 200, message: 'Server updated successfully' });
  } catch (error) {
    console.error(`[ERROR] Error updating server ${serverName}:`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error updating server' });
  }
});

app.delete('/ptc/server/:name', async (req, res) => {
  const serverName = req.params.name;
  try {
    const result = await serversCollection.deleteOne({ serverName: serverName });
    if (result.deletedCount > 0) {
      res.json({ error: false, code: 200, message: 'Server removed successfully' });
    } else {
      res.status(404).json({ error: true, code: 404, message: 'Server not found' });
    }
  } catch (error) {
    console.error(`[ERROR] Error removing server ${serverName}:`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error removing server' });
  }
});

app.get('/ptc/clan/all', async (req, res) => {
  try {
    const files = await fs.readdir(CLANS_DIR);
    const clans = [];
    for (const file of files) {
      if (file.endsWith('.json')) {
        const data = JSON.parse(await fs.readFile(path.join(CLANS_DIR, file), 'utf8'));
        clans.push(data);
      }
    }
    res.json({ error: false, code: 200, data: clans });
  } catch (error) {
    console.error('[ERROR] Error reading clans:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading clans', data: [] });
  }
});

app.get('/ptc/clan/player/:uuid', async (req, res) => {
  const uuid = req.params.uuid;
  if (!validateUUID(uuid)) {
    return res.status(400).json({ error: true, code: 400, message: 'Invalid UUID format' });
  }
  try {
    const files = await fs.readdir(CLANS_DIR);
    for (const file of files) {
      if (file.endsWith('.json')) {
        const data = JSON.parse(await fs.readFile(path.join(CLANS_DIR, file), 'utf8'));
        const member = data.members.find(m => m.uuid === uuid);
        if (member) {
          res.json({ error: false, code: 200, data: data });
          return;
        }
      }
    }
    res.status(404).json({ error: true, code: 404, message: 'Player not in any clan' });
  } catch (error) {
    console.error(`[ERROR] Error finding player clan: ${uuid}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error finding player clan' });
  }
});

app.get('/ptc/clan/:tag', async (req, res) => {
  const tag = req.params.tag.toUpperCase();
  const filePath = path.join(CLANS_DIR, `${tag}.json`);
  try {
    const data = JSON.parse(await fs.readFile(filePath, 'utf8'));
    res.json({ error: false, code: 200, data: data });
  } catch (error) {
    if (error.code === 'ENOENT') {
      res.status(404).json({ error: true, code: 404, message: 'Clan not found' });
    } else {
      console.error(`[ERROR] Error reading clan: ${tag}`, error);
      res.status(500).json({ error: true, code: 500, message: 'Error reading clan data' });
    }
  }
});

app.post('/ptc/clan/:tag', async (req, res) => {
  const tag = req.params.tag.toUpperCase();
  const filePath = path.join(CLANS_DIR, `${tag}.json`);
  const clanData = req.body;
  try {
    await fs.writeFile(filePath, JSON.stringify(clanData, null, 2));
    res.json({ error: false, code: 200, message: 'Clan saved successfully' });
  } catch (error) {
    console.error(`[ERROR] Error saving clan: ${tag}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error saving clan' });
  }
});

app.delete('/ptc/clan/:tag', async (req, res) => {
  const tag = req.params.tag.toUpperCase();
  const filePath = path.join(CLANS_DIR, `${tag}.json`);
  try {
    await fs.unlink(filePath);
    res.json({ error: false, code: 200, message: 'Clan deleted successfully' });
  } catch (error) {
    if (error.code === 'ENOENT') {
      res.status(404).json({ error: true, code: 404, message: 'Clan not found' });
    } else {
      console.error(`[ERROR] Error deleting clan: ${tag}`, error);
      res.status(500).json({ error: true, code: 500, message: 'Error deleting clan' });
    }
  }
});

app.get('/ptc/war/all', async (req, res) => {
  try {
    const files = await fs.readdir(WARS_DIR);
    const wars = [];
    for (const file of files) {
      if (file.endsWith('.json')) {
        const data = JSON.parse(await fs.readFile(path.join(WARS_DIR, file), 'utf8'));
        wars.push(data);
      }
    }
    res.json({ error: false, code: 200, data: wars });
  } catch (error) {
    console.error('[ERROR] Error reading wars:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading wars', data: [] });
  }
});

app.get('/ptc/war/active', async (req, res) => {
  try {
    const files = await fs.readdir(WARS_DIR);
    const activeWars = [];
    for (const file of files) {
      if (file.endsWith('.json')) {
        const data = JSON.parse(await fs.readFile(path.join(WARS_DIR, file), 'utf8'));
        if (data.accepted && !data.finished && !data.arenaKey) {
          activeWars.push(data);
        }
      }
    }
    if (activeWars.length > 0) {
      res.json({ error: false, code: 200, data: activeWars });
    } else {
      res.status(404).json({ error: true, code: 404, message: 'No active wars without server', data: [] });
    }
  } catch (error) {
    console.error('[ERROR] Error finding active wars:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error finding active wars', data: [] });
  }
});

app.get('/ptc/war/active/all', async (req, res) => {
  try {
    const files = await fs.readdir(WARS_DIR);
    const activeWars = [];
    for (const file of files) {
      if (file.endsWith('.json')) {
        const data = JSON.parse(await fs.readFile(path.join(WARS_DIR, file), 'utf8'));
        if (data.accepted && !data.finished) {
          activeWars.push(data);
        }
      }
    }
    if (activeWars.length > 0) {
      res.json({ error: false, code: 200, data: activeWars });
    } else {
      res.status(404).json({ error: true, code: 404, message: 'No active wars found', data: [] });
    }
  } catch (error) {
    console.error('[ERROR] Error finding all active wars:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error finding all active wars', data: [] });
  }
});

app.get('/ptc/war/upcoming', async (req, res) => {
  try {
    const files = await fs.readdir(WARS_DIR);
    const upcomingWars = [];
    const now = Date.now();
    const UPCOMING_WINDOW = 60 * 60 * 1000;
    for (const file of files) {
      if (file.endsWith('.json')) {
        const data = JSON.parse(await fs.readFile(path.join(WARS_DIR, file), 'utf8'));
        if (data.accepted && !data.finished && data.scheduledTime) {
          const timeUntilWar = data.scheduledTime - now;
          if (timeUntilWar > 0 && timeUntilWar <= UPCOMING_WINDOW) {
            upcomingWars.push(data);
          }
        }
      }
    }
    upcomingWars.sort((a, b) => a.scheduledTime - b.scheduledTime);
    res.json({ error: false, code: 200, data: upcomingWars });
  } catch (error) {
    console.error('[ERROR] Error finding upcoming wars:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error finding upcoming wars', data: [] });
  }
});

app.get('/ptc/war/clan/:tag', async (req, res) => {
  const tag = req.params.tag.toUpperCase();
  try {
    const files = await fs.readdir(WARS_DIR);
    const clanWars = [];
    for (const file of files) {
      if (file.endsWith('.json')) {
        const data = JSON.parse(await fs.readFile(path.join(WARS_DIR, file), 'utf8'));
        if (data.challengerClanTag === tag || data.challengedClanTag === tag) {
          clanWars.push(data);
        }
      }
    }
    res.json({ error: false, code: 200, data: clanWars });
  } catch (error) {
    console.error(`[ERROR] Error finding clan wars: ${tag}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error finding clan wars' });
  }
});

app.get('/ptc/war/:warId', async (req, res) => {
  const warId = req.params.warId;
  const filePath = path.join(WARS_DIR, `${warId}.json`);
  try {
    const data = JSON.parse(await fs.readFile(filePath, 'utf8'));
    res.json({ error: false, code: 200, data: data });
  } catch (error) {
    if (error.code === 'ENOENT') {
      res.status(404).json({ error: true, code: 404, message: 'War not found' });
    } else {
      console.error(`[ERROR] Error reading war: ${warId}`, error);
      res.status(500).json({ error: true, code: 500, message: 'Error reading war data' });
    }
  }
});

app.post('/ptc/war/:warId', async (req, res) => {
  const warId = req.params.warId;
  const filePath = path.join(WARS_DIR, `${warId}.json`);
  const warData = req.body;
  try {
    await fs.writeFile(filePath, JSON.stringify(warData, null, 2));
    res.json({ error: false, code: 200, message: 'War saved successfully' });
  } catch (error) {
    console.error(`[ERROR] Error saving war: ${warId}`, error);
    res.status(500).json({ error: true, code: 500, message: 'Error saving war' });
  }
});

app.delete('/ptc/war/:warId', async (req, res) => {
  const warId = req.params.warId;
  const filePath = path.join(WARS_DIR, `${warId}.json`);
  try {
    await fs.unlink(filePath);
    res.json({ error: false, code: 200, message: 'War deleted successfully' });
  } catch (error) {
    if (error.code === 'ENOENT') {
      res.status(404).json({ error: true, code: 404, message: 'War not found' });
    } else {
      console.error(`[ERROR] Error deleting war: ${warId}`, error);
      res.status(500).json({ error: true, code: 500, message: 'Error deleting war' });
    }
  }
});

app.get('/ptc/top/:stat', async (req, res) => {
  const stat = req.params.stat;
  const limit = Math.min(parseInt(req.query.limit) || 10, 100);
  const validStats = ['wins', 'kills', 'deaths', 'bDomination', 'cores', 'clanLevel', 'bKillStreak', 'playtime'];

  if (!validStats.includes(stat)) {
    return res.status(400).json({
      error: true, code: 400,
      message: `Invalid stat. Valid stats: ${validStats.join(', ')}`
    });
  }

  if (stat === 'playtime') {
    return res.redirect('/ptc/top/playtime');
  }

  try {
    const files = await fs.readdir(DATA_DIR);
    const players = [];

    for (const file of files) {
      if (file.endsWith('.json')) {
        try {
          const uuid = file.replace('.json', '');
          const data = JSON.parse(await fs.readFile(path.join(DATA_DIR, file), 'utf8'));
          const statValue = data[stat] || 0;

          let clanDisplayName = null;
          if (stat === 'clanLevel' || stat === 'bKillStreak') {
            try {
              const clanFiles = await fs.readdir(CLANS_DIR);
              for (const clanFile of clanFiles) {
                if (clanFile.endsWith('.json')) {
                  const clanData = JSON.parse(await fs.readFile(path.join(CLANS_DIR, clanFile), 'utf8'));
                  const member = clanData.members.find(m => m.uuid === uuid);
                  if (member) {
                    clanDisplayName = clanData.displayName || clanData.name;
                    break;
                  }
                }
              }
            } catch (e) {}
          }

          players.push({
            uuid: uuid,
            playerName: data.playerName || 'Desconocido',
            [stat]: statValue,
            clanDisplayName: clanDisplayName
          });
        } catch (e) {}
      }
    }

    players.sort((a, b) => b[stat] - a[stat]);
    const topPlayers = players.slice(0, limit);
    res.json({ error: false, code: 200, data: topPlayers });
  } catch (error) {
    console.error('[ERROR] Error calculating all-time top:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error calculating all-time top', data: [] });
  }
});

app.get('/ptc/top/weekly/:stat', async (req, res) => {
  const stat = req.params.stat;
  const limit = Math.min(parseInt(req.query.limit) || 10, 100);
  const validStats = ['wins', 'kills', 'deaths', 'bDomination', 'cores', 'playtime'];

  if (!validStats.includes(stat)) {
    return res.status(400).json({
      error: true, code: 400,
      message: `Invalid weekly stat. Valid stats: ${validStats.join(', ')}`
    });
  }

  if (stat === 'playtime') {
    return res.redirect('/ptc/top/weekly/playtime');
  }

  try {
    const weeklyStatsFile = path.join(WEEKLY_TOPS_DIR, 'current.json');
    const weeklyData = JSON.parse(await fs.readFile(weeklyStatsFile, 'utf8'));

    const currentWeek = getWeekNumber();
    if (weeklyData.weekId !== currentWeek) {
      console.warn(`[WEEKLY] ADVERTENCIA: Consultando datos de semana ${weeklyData.weekId} (actual: ${currentWeek})`);
    }

    const players = [];
    for (const [uuid, stats] of Object.entries(weeklyData.players)) {
      if (stats[stat] > 0) {
        let clanDisplayName = null;
        try {
          const clanFiles = await fs.readdir(CLANS_DIR);
          for (const clanFile of clanFiles) {
            if (clanFile.endsWith('.json')) {
              const clanData = JSON.parse(await fs.readFile(path.join(CLANS_DIR, clanFile), 'utf8'));
              const member = clanData.members.find(m => m.uuid === uuid);
              if (member) {
                clanDisplayName = clanData.displayName || clanData.name;
                break;
              }
            }
          }
        } catch (e) {}

        players.push({
          uuid: uuid,
          playerName: stats.playerName,
          [stat]: stats[stat],
          clanDisplayName: clanDisplayName,
          clanLevel: stats.clanLevel
        });
      }
    }

    players.sort((a, b) => b[stat] - a[stat]);
    const topPlayers = players.slice(0, limit);

    res.json({
      error: false,
      code: 200,
      data: {
        weekId: weeklyData.weekId,
        startDate: weeklyData.startDate,
        resetIn: getNextMonday().getTime() - Date.now(),
        players: topPlayers
      }
    });
  } catch (error) {
    console.error('[ERROR] Error calculating weekly top:', error);
    res.status(500).json({
      error: true, code: 500, message: 'Error calculating weekly top',
      data: {
        weekId: getWeekNumber(),
        startDate: new Date().toISOString(),
        resetIn: getNextMonday().getTime() - Date.now(),
        players: []
      }
    });
  }
});

app.get('/ptc/top/history/:weekId/:stat', async (req, res) => {
  const weekId = req.params.weekId;
  const stat = req.params.stat;
  const limit = Math.min(parseInt(req.query.limit) || 10, 100);

  try {
    const historyFile = path.join(WEEKLY_HISTORY_DIR, `${weekId}.json`);
    const historyData = JSON.parse(await fs.readFile(historyFile, 'utf8'));

    const players = [];
    for (const [uuid, stats] of Object.entries(historyData.stats.players)) {
      if (stats[stat] > 0) {
        players.push({
          uuid: uuid,
          playerName: stats.playerName,
          [stat]: stats[stat],
          clanDisplayName: stats.clanDisplayName || null,
          clanLevel: stats.clanLevel
        });
      }
    }

    players.sort((a, b) => b[stat] - a[stat]);
    const topPlayers = players.slice(0, limit);

    res.json({
      error: false,
      code: 200,
      data: {
        weekId: weekId,
        archivedAt: historyData.archivedAt,
        players: topPlayers
      }
    });
  } catch (error) {
    if (error.code === 'ENOENT') {
      res.status(404).json({ error: true, code: 404, message: `No data found for week ${weekId}` });
    } else {
      console.error('[ERROR] Error reading historical top:', error);
      res.status(500).json({ error: true, code: 500, message: 'Error reading historical top' });
    }
  }
});

app.get('/ptc/top/history/list', async (req, res) => {
  try {
    const files = await fs.readdir(WEEKLY_HISTORY_DIR);
    const weeks = files
        .filter(f => f.endsWith('.json'))
        .map(f => f.replace('.json', ''))
        .sort()
        .reverse();
    res.json({ error: false, code: 200, data: weeks });
  } catch (error) {
    console.error('[ERROR] Error listing historical weeks:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error listing historical weeks', data: [] });
  }
});

app.get('/ptc/stats', async (req, res) => {
  try {
    const playerFiles = await fs.readdir(DATA_DIR);
    const playerCount = playerFiles.filter(f => f.endsWith('.json')).length;
    const clanFiles = await fs.readdir(CLANS_DIR);
    const clanCount = clanFiles.filter(f => f.endsWith('.json')).length;
    const warFiles = await fs.readdir(WARS_DIR);
    const warCount = warFiles.filter(f => f.endsWith('.json')).length;
    const totalServers = await serversCollection.countDocuments();
    const activeServers = await serversCollection.countDocuments({
      lastUpdate: { $gte: Date.now() - 30000 }
    });

    let totalCoins = 0;
    let totalKills = 0;
    let totalDeaths = 0;
    let bestStreak = 0;
    let totalPlaytime = 0;

    for (const file of playerFiles) {
      if (file.endsWith('.json')) {
        const data = JSON.parse(await fs.readFile(path.join(DATA_DIR, file), 'utf8'));
        totalCoins += data.coins || 0;
        totalKills += data.kills || 0;
        totalDeaths += data.deaths || 0;
        totalPlaytime += data.playtime || 0;
        if ((data.bKillStreak || 0) > bestStreak) {
          bestStreak = data.bKillStreak;
        }
      }
    }

    const currentWeek = getWeekNumber();
    const nextReset = getNextMonday();

    res.json({
      error: false,
      code: 200,
      data: {
        totalPlayers: playerCount,
        totalClans: clanCount,
        totalWars: warCount,
        totalServers: totalServers,
        activeServers: activeServers,
        activeSessions: activeSessions.size,
        totalCoins: totalCoins,
        totalKills: totalKills,
        totalDeaths: totalDeaths,
        bestKillStreak: bestStreak,
        totalPlaytime: totalPlaytime,
        totalPlaytimeFormatted: formatPlaytime(totalPlaytime),
        currentWeek: currentWeek,
        nextWeeklyReset: nextReset.toISOString(),
        resetInMs: nextReset.getTime() - Date.now()
      }
    });
  } catch (error) {
    console.error('[ERROR] Error reading stats:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error reading stats' });
  }
});

app.get('/health', async (req, res) => {
  const health = {
    status: 'ok',
    timestamp: Date.now(),
    uptime: process.uptime(),
    currentWeek: getWeekNumber(),
    nextReset: getNextMonday().toISOString(),
    activeSessions: activeSessions.size,
    checks: {}
  };

  try {
    await fs.access(DATA_DIR, fs.constants.R_OK | fs.constants.W_OK);
    health.checks.dataDir = 'ok';
  } catch (error) {
    health.checks.dataDir = 'error';
    health.status = 'degraded';
  }

  try {
    await fs.access(CLANS_DIR, fs.constants.R_OK | fs.constants.W_OK);
    health.checks.clansDir = 'ok';
  } catch (error) {
    health.checks.clansDir = 'error';
    health.status = 'degraded';
  }

  try {
    await fs.access(WARS_DIR, fs.constants.R_OK | fs.constants.W_OK);
    health.checks.warsDir = 'ok';
  } catch (error) {
    health.checks.warsDir = 'error';
    health.status = 'degraded';
  }

  try {
    await fs.access(WEEKLY_TOPS_DIR, fs.constants.R_OK | fs.constants.W_OK);
    health.checks.weeklyTopsDir = 'ok';
  } catch (error) {
    health.checks.weeklyTopsDir = 'error';
    health.status = 'degraded';
  }

  try {
    await fs.access(SESSIONS_DIR, fs.constants.R_OK | fs.constants.W_OK);
    health.checks.sessionsDir = 'ok';
  } catch (error) {
    health.checks.sessionsDir = 'error';
    health.status = 'degraded';
  }

  try {
    if (db) {
      await db.admin().ping();
      health.checks.mongodb = 'ok';
    } else {
      health.checks.mongodb = 'not_initialized';
      health.status = 'degraded';
    }
  } catch (error) {
    health.checks.mongodb = 'error';
    health.status = 'degraded';
  }

  const statusCode = health.status === 'ok' ? 200 : 500;
  res.status(statusCode).json({
    error: health.status !== 'ok',
    code: statusCode,
    message: health.status === 'ok' ? 'API is healthy' : 'API is degraded',
    ...health
  });
});

app.post('/ptc/weekly/reset', async (req, res) => {
  try {
    const success = await resetWeeklyStats(true);
    if (success) {
      res.json({
        error: false,
        code: 200,
        message: 'Weekly stats reset successfully',
        newWeek: getWeekNumber()
      });
    } else {
      res.status(500).json({
        error: true,
        code: 500,
        message: 'Reset failed (check logs)'
      });
    }
  } catch (error) {
    console.error('[ERROR] Error resetting weekly stats:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error resetting weekly stats' });
  }
});

app.get('/ptc/weekly/debug/:uuid', async (req, res) => {
  const uuid = req.params.uuid;
  try {
    const weeklyStatsFile = path.join(WEEKLY_TOPS_DIR, 'current.json');
    const weeklyData = JSON.parse(await fs.readFile(weeklyStatsFile, 'utf8'));
    const playerWeekly = weeklyData.players[uuid] || null;

    let playerPermanent = null;
    try {
      playerPermanent = JSON.parse(await fs.readFile(path.join(DATA_DIR, `${uuid}.json`), 'utf8'));
    } catch (e) {}

    const activeSession = activeSessions.get(uuid);

    res.json({
      error: false,
      code: 200,
      data: {
        currentWeek: getWeekNumber(),
        weeklyDataWeekId: weeklyData.weekId,
        weekIdMatch: weeklyData.weekId === getWeekNumber(),
        playerWeeklyData: playerWeekly,
        playerPermanentData: playerPermanent ? {
          kills: playerPermanent.kills,
          wins: playerPermanent.wins,
          cores: playerPermanent.cores,
          deaths: playerPermanent.deaths,
          bDomination: playerPermanent.bDomination,
          playtime: playerPermanent.playtime,
          playtimeFormatted: formatPlaytime(playerPermanent.playtime || 0)
        } : null,
        activeSession: activeSession ? {
          startTime: activeSession.startTime,
          lastHeartbeat: activeSession.lastHeartbeat,
          duration: Date.now() - activeSession.startTime,
          durationFormatted: formatPlaytime(Date.now() - activeSession.startTime)
        } : null
      }
    });
  } catch (error) {
    console.error('[ERROR] Error debugging weekly stats:', error);
    res.status(500).json({ error: true, code: 500, message: 'Error debugging weekly stats' });
  }
});

const startServer = async (port, maxRetries = 3, currentRetry = 0) => {
  await initializeMongoDB();
  await scheduleWeeklyReset();

  setInterval(cleanupStaleSessions, SESSION_CLEANUP_INTERVAL);
  console.log(`[PLAYTIME] Session cleanup scheduled every ${SESSION_CLEANUP_INTERVAL / 1000}s`);

  const server = app.listen(port, '0.0.0.0', () => {
    console.log(`=================================`);
    console.log(`PTC API Server v3.4 (with Playtime)`);
    console.log(`http://0.0.0.0:${port}`);
    console.log(`=================================`);
    console.log(`Current week: ${getWeekNumber()}`);
    console.log(`Next reset: ${getNextMonday().toISOString()}`);
    console.log(`Session timeout: ${SESSION_TIMEOUT / 1000}s`);
    console.log(`Heartbeat interval: ${HEARTBEAT_INTERVAL / 1000}s`);
    console.log(`=================================`);
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      if (currentRetry < maxRetries) {
        const nextPort = port + 1;
        console.log(`[WARN] Port ${port} in use, trying ${nextPort}...`);
        server.close();
        startServer(nextPort, maxRetries, currentRetry + 1);
      } else {
        console.error(`[FATAL] No available port after ${maxRetries} attempts`);
        process.exit(1);
      }
    } else {
      console.error('[FATAL] Server error:', err);
      process.exit(1);
    }
  });
};

startServer(PORT);

process.on('SIGTERM', async () => {
  console.log('[SHUTDOWN] SIGTERM received');
  for (const [uuid] of activeSessions.entries()) {
    await endSession(uuid);
  }
  process.exit(0);
});

process.on('SIGINT', async () => {
  console.log('[SHUTDOWN] SIGINT received');
  for (const [uuid] of activeSessions.entries()) {
    await endSession(uuid);
  }
  process.exit(0);
});
