package sdfrpe.github.io.ptc.Playtime;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Database.Engines.PlaytimeAPI;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeManager {

    private final PTC plugin;
    private final PlaytimeAPI playtimeAPI;
    private final Map<UUID, Integer> heartbeatTasks;
    private final Map<UUID, Long> sessionStartTimes;
    private final Map<UUID, Boolean> activeSessions;
    private GameStatus lastKnownStatus;
    private BukkitTask statusCheckerTask;

    private static final long HEARTBEAT_INTERVAL = 600L;

    public PlaytimeManager(PTC plugin) {
        this.plugin = plugin;
        this.playtimeAPI = new PlaytimeAPI();
        this.heartbeatTasks = new ConcurrentHashMap<>();
        this.sessionStartTimes = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.lastKnownStatus = Statics.gameStatus;

        if (!plugin.getGameManager().getGlobalSettings().isLobbyMode()) {
            startStatusChecker();
        }
    }

    private void startStatusChecker() {
        statusCheckerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            GameStatus currentStatus = Statics.gameStatus;

            if (currentStatus != lastKnownStatus) {
                LogSystem.debug(LogCategory.CORE, "GameStatus cambió:", lastKnownStatus.name(), "->", currentStatus.name());

                if (currentStatus == GameStatus.IN_GAME && lastKnownStatus != GameStatus.IN_GAME) {
                    onGameStart();
                } else if (currentStatus != GameStatus.IN_GAME && lastKnownStatus == GameStatus.IN_GAME) {
                    onGameEnd();
                }

                lastKnownStatus = currentStatus;
            }
        }, 20L, 20L);
    }

    private void onGameStart() {
        LogSystem.info(LogCategory.CORE, "Partida iniciada - Iniciando sesiones de playtime para todos los jugadores");

        for (Player player : Bukkit.getOnlinePlayers()) {
            startSession(player);
        }
    }

    private void onGameEnd() {
        LogSystem.info(LogCategory.CORE, "Partida terminada - Finalizando sesiones de playtime");

        for (UUID uuid : activeSessions.keySet()) {
            endSession(uuid);
        }
    }

    public void onPlayerJoin(Player player) {
        if (plugin.getGameManager().getGlobalSettings().isLobbyMode()) {
            LogSystem.debug(LogCategory.CORE, "Modo lobby - Playtime desactivado para:", player.getName());
            return;
        }

        if (Statics.gameStatus == GameStatus.IN_GAME) {
            startSession(player);
        } else {
            LogSystem.debug(LogCategory.CORE, "Jugador entró pero no está IN_GAME - Playtime pendiente:", player.getName());
        }
    }

    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeSessions.containsKey(uuid)) {
            endSession(uuid);
        }

        cancelHeartbeat(uuid);
    }

    private void startSession(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        String serverName = plugin.getGameManager().getGlobalSettings().getServerName();

        if (activeSessions.containsKey(uuid) && activeSessions.get(uuid)) {
            LogSystem.debug(LogCategory.CORE, "Sesión ya activa para:", playerName);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = playtimeAPI.startSession(uuid, playerName, serverName);

            if (success) {
                sessionStartTimes.put(uuid, System.currentTimeMillis());
                activeSessions.put(uuid, true);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    scheduleHeartbeat(uuid);
                });

                LogSystem.info(LogCategory.CORE, "Sesión de playtime iniciada:", playerName);
            } else {
                LogSystem.warn(LogCategory.CORE, "No se pudo iniciar sesión de playtime:", playerName);
            }
        });
    }

    private void endSession(UUID uuid) {
        cancelHeartbeat(uuid);

        if (!activeSessions.containsKey(uuid) || !activeSessions.get(uuid)) {
            return;
        }

        activeSessions.put(uuid, false);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = playtimeAPI.endSession(uuid);

            if (success) {
                sessionStartTimes.remove(uuid);
                activeSessions.remove(uuid);
                LogSystem.debug(LogCategory.CORE, "Sesión de playtime terminada:", uuid.toString());
            } else {
                LogSystem.warn(LogCategory.CORE, "Error terminando sesión de playtime:", uuid.toString());
            }
        });
    }

    private void scheduleHeartbeat(UUID uuid) {
        cancelHeartbeat(uuid);

        int taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (Statics.gameStatus != GameStatus.IN_GAME) {
                return;
            }

            if (!activeSessions.containsKey(uuid) || !activeSessions.get(uuid)) {
                return;
            }

            sendHeartbeat(uuid);
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL).getTaskId();

        heartbeatTasks.put(uuid, taskId);
    }

    private void sendHeartbeat(UUID uuid) {
        PlaytimeAPI.HeartbeatResponse response = playtimeAPI.sendHeartbeat(uuid);

        if (response == null) {
            LogSystem.warn(LogCategory.CORE, "Heartbeat fallido para:", uuid.toString());
            return;
        }

        if (response.isNeedsRestart()) {
            LogSystem.info(LogCategory.CORE, "Sesión expirada, reiniciando:", uuid.toString());

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && Statics.gameStatus == GameStatus.IN_GAME) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    activeSessions.put(uuid, false);
                    startSession(player);
                });
            }
        }
    }

    private void cancelHeartbeat(UUID uuid) {
        Integer taskId = heartbeatTasks.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void shutdown() {
        LogSystem.info(LogCategory.CORE, "Cerrando PlaytimeManager...");

        if (statusCheckerTask != null) {
            statusCheckerTask.cancel();
        }

        for (Integer taskId : heartbeatTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        heartbeatTasks.clear();

        for (UUID uuid : activeSessions.keySet()) {
            if (activeSessions.get(uuid)) {
                playtimeAPI.endSession(uuid);
                LogSystem.debug(LogCategory.CORE, "Sesión cerrada en shutdown:", uuid.toString());
            }
        }

        sessionStartTimes.clear();
        activeSessions.clear();

        LogSystem.info(LogCategory.CORE, "PlaytimeManager cerrado correctamente");
    }

    public long getCurrentSessionTime(UUID uuid) {
        Long startTime = sessionStartTimes.get(uuid);
        if (startTime == null) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    public boolean hasActiveSession(UUID uuid) {
        return activeSessions.containsKey(uuid) && activeSessions.get(uuid);
    }

    public static String formatPlaytime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}