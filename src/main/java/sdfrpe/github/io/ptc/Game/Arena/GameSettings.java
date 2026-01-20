package sdfrpe.github.io.ptc.Game.Arena;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.Chunk;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.Game.Settings.ArenaLocations;
import sdfrpe.github.io.ptc.Game.Settings.ArenaSettings;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerUtils;
import sdfrpe.github.io.ptc.Tasks.InGame.StartingTask;
import sdfrpe.github.io.ptc.Utils.Location;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Managers.GlobalTabManager;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Enums.TimeOfDay;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GameSettings {
    private final GameManager gameManager;
    private final Map<TeamColor, ArenaTeam> teamList;
    private final ArenaTeam spectatorTeam;
    private static final Object START_LOCK = new Object();
    private volatile boolean isStarting = false;

    public GameSettings(GameManager gameManager) {
        this.gameManager = gameManager;
        this.teamList = Maps.newConcurrentMap();
        this.spectatorTeam = new ArenaTeam(TeamColor.SPECTATOR, 0);
    }

    public void createTeams() {
        this.teamList.put(TeamColor.BLUE, new ArenaTeam(TeamColor.BLUE, null, null, this.gameManager.getGlobalSettings().getArenaTeams().getCoresPerTeam()));
        this.teamList.put(TeamColor.GREEN, new ArenaTeam(TeamColor.GREEN, null, null, this.gameManager.getGlobalSettings().getArenaTeams().getCoresPerTeam()));
        this.teamList.put(TeamColor.RED, new ArenaTeam(TeamColor.RED, null, null, this.gameManager.getGlobalSettings().getArenaTeams().getCoresPerTeam()));
        this.teamList.put(TeamColor.YELLOW, new ArenaTeam(TeamColor.YELLOW, null, null, this.gameManager.getGlobalSettings().getArenaTeams().getCoresPerTeam()));
    }

    public ArenaTeam checkWinnerTeam() {
        return this.getTeamList().values().stream().filter(arenaTeam -> !arenaTeam.isDeathTeam()).findFirst().orElse(null);
    }

    public long countValidTeams() {
        return this.getTeamList().values().stream().filter(arenaTeam -> !arenaTeam.isDeathTeam()).count();
    }

    public void removeEmptyTeams() {
        boolean isModeCW = PTC.getInstance().getGameManager().getGlobalSettings().isModeCW();

        if (isModeCW) {
            ArenaTeam blueTeam = this.teamList.get(TeamColor.BLUE);
            ArenaTeam redTeam = this.teamList.get(TeamColor.RED);

            if (blueTeam != null && blueTeam.countPlayers() == 0) {
                this.teamList.remove(TeamColor.BLUE);
                LogSystem.info(LogCategory.TEAM, "Equipo AZUL vacío removido");
            }

            if (redTeam != null && redTeam.countPlayers() == 0) {
                this.teamList.remove(TeamColor.RED);
                LogSystem.info(LogCategory.TEAM, "Equipo ROJO vacío removido");
            }

            this.teamList.remove(TeamColor.GREEN);
            this.teamList.remove(TeamColor.YELLOW);

            LogSystem.info(LogCategory.TEAM, "Equipos activos en modo CW:", String.valueOf(this.teamList.size()));
            return;
        }

        int totalPlayersInGame = (int) this.teamList.values().stream()
                .mapToInt(ArenaTeam::countPlayers)
                .sum();

        if (totalPlayersInGame == 1) {
            LogSystem.info(LogCategory.GAME, "Modo force-start: Manteniendo todos los equipos (1 jugador total)");
            return;
        }

        List<TeamColor> emptyTeams = Lists.newArrayList();

        for (Map.Entry<TeamColor, ArenaTeam> entry : this.teamList.entrySet()) {
            if (entry.getValue().countPlayers() == 0) {
                emptyTeams.add(entry.getKey());
            }
        }

        for (TeamColor color : emptyTeams) {
            this.teamList.remove(color);
            LogSystem.debug(LogCategory.TEAM, "Equipo vacío removido:", color.getName());
        }

        LogSystem.info(LogCategory.TEAM, "Equipos activos después de limpieza:", String.valueOf(this.teamList.size()));
    }

    public int countTeamsWithPlayers() {
        return (int) this.teamList.values().stream().filter(team -> team.countPlayers() > 0).count();
    }

    public void insertPlayersInTeams(Collection<GamePlayer> players) {
        boolean isModeCW = PTC.getInstance().getGameManager().getGlobalSettings().isModeCW();

        if (isModeCW) {
            insertPlayersByClans(players);
        } else {
            insertPlayersNormally(players);
        }
    }

    private void insertPlayersByClans(Collection<GamePlayer> players) {
        LogSystem.info(LogCategory.TEAM, "═══════════════════════════════════════");
        LogSystem.info(LogCategory.TEAM, "MODO CLAN WAR - ASIGNACIÓN POR CLANES");
        LogSystem.info(LogCategory.TEAM, "═══════════════════════════════════════");

        try {
            Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
            if (adapter == null) {
                LogSystem.error(LogCategory.TEAM, "ClanWarAdapter no disponible - fallback a asignación normal");
                insertPlayersNormally(players);
                return;
            }

            Method isPlayerInBlueMethod = adapter.getClass().getMethod("isPlayerInBlueClan", UUID.class);
            Method isPlayerInRedMethod = adapter.getClass().getMethod("isPlayerInRedClan", UUID.class);
            Method getBlueTagMethod = adapter.getClass().getMethod("getBlueClanTag");
            Method getRedTagMethod = adapter.getClass().getMethod("getRedClanTag");

            String blueTag = (String) getBlueTagMethod.invoke(adapter);
            String redTag = (String) getRedTagMethod.invoke(adapter);

            LogSystem.info(LogCategory.TEAM, "Clan AZUL:", blueTag);
            LogSystem.info(LogCategory.TEAM, "Clan ROJO:", redTag);

            ArenaTeam blueTeam = this.teamList.get(TeamColor.BLUE);
            ArenaTeam redTeam = this.teamList.get(TeamColor.RED);

            int blueCount = 0;
            int redCount = 0;
            int kickedCount = 0;

            for (GamePlayer player : players) {
                if (player == null) continue;

                try {
                    Boolean isBlue = (Boolean) isPlayerInBlueMethod.invoke(adapter, player.getUuid());
                    Boolean isRed = (Boolean) isPlayerInRedMethod.invoke(adapter, player.getUuid());

                    if (isBlue != null && isBlue) {
                        blueTeam.addPlayer(player, false);
                        blueCount++;
                        LogSystem.debug(LogCategory.TEAM, player.getName(), "→ AZUL (Clan " + blueTag + ")");
                    } else if (isRed != null && isRed) {
                        redTeam.addPlayer(player, false);
                        redCount++;
                        LogSystem.debug(LogCategory.TEAM, player.getName(), "→ ROJO (Clan " + redTag + ")");
                    } else {
                        LogSystem.warn(LogCategory.TEAM, "Jugador expulsado (no pertenece a ningún clan):", player.getName());
                        if (player.getPlayer() != null) {
                            player.getPlayer().kickPlayer(org.bukkit.ChatColor.RED + "Solo miembros de clanes en guerra pueden jugar.");
                        }
                        kickedCount++;
                    }
                } catch (Exception e) {
                    LogSystem.error(LogCategory.TEAM, "Error asignando jugador:", player.getName(), e.getMessage());
                }
            }

            LogSystem.info(LogCategory.TEAM, "═══════════════════════════════════════");
            LogSystem.info(LogCategory.TEAM, "ASIGNACIÓN COMPLETADA:");
            LogSystem.info(LogCategory.TEAM, "  - AZUL (" + blueTag + "):", blueCount + " miembros");
            LogSystem.info(LogCategory.TEAM, "  - ROJO (" + redTag + "):", redCount + " miembros");
            if (kickedCount > 0) {
                LogSystem.info(LogCategory.TEAM, "  - Expulsados:", kickedCount + " jugadores");
            }
            LogSystem.info(LogCategory.TEAM, "═══════════════════════════════════════");

        } catch (Exception e) {
            LogSystem.error(LogCategory.TEAM, "Error en asignación por clanes:", e.getMessage());
            e.printStackTrace();
            insertPlayersNormally(players);
            return;
        }

        GlobalTabManager.getInstance().updateAllPlayerTabs();
        this.removeEmptyTeams();
        this.finalizeTeamSetup();
    }

    private void insertPlayersNormally(Collection<GamePlayer> players) {
        LogSystem.debug(LogCategory.TEAM, "Iniciando asignación de equipos para", players.size() + " jugadores");

        List<GamePlayer> playersWithTeam = new ArrayList<GamePlayer>();
        List<GamePlayer> playersWithoutTeam = new ArrayList<GamePlayer>();

        for (GamePlayer player : players) {
            if (player == null) {
                LogSystem.error(LogCategory.TEAM, "Jugador null encontrado en colección");
                continue;
            }

            if (player.getArenaTeam() != null && player.getArenaTeam().getColor() != TeamColor.SPECTATOR && player.getArenaTeam().getColor() != TeamColor.LOBBY) {
                playersWithTeam.add(player);
                LogSystem.debug(LogCategory.TEAM, player.getName(), "ya tiene equipo:", player.getArenaTeam().getColor().getName());
            } else {
                playersWithoutTeam.add(player);
            }
        }

        List<ArenaTeam> teams = this.teamList.values().stream().sorted(Comparator.comparingInt(ArenaTeam::countPlayers)).collect(Collectors.toList());

        if (teams.isEmpty()) {
            LogSystem.error(LogCategory.TEAM, "No hay equipos disponibles para asignación");
            return;
        }

        LogSystem.debug(LogCategory.TEAM, "Distribuyendo", playersWithoutTeam.size() + " jugadores en", teams.size() + " equipos");

        for (GamePlayer player : playersWithoutTeam) {
            ArenaTeam selectedTeam = teams.stream().min(Comparator.comparingInt(ArenaTeam::countPlayers)).orElse(teams.get(0));

            if (player != null && selectedTeam != null) {
                selectedTeam.addPlayer(player, false);
                LogSystem.debug(LogCategory.TEAM, player.getName(), "asignado a", selectedTeam.getColor().getName());
            }
        }

        LogSystem.info(LogCategory.TEAM, "═══════════════════════════════════════");
        LogSystem.info(LogCategory.TEAM, "ASIGNACIÓN DE EQUIPOS COMPLETADA:");
        LogSystem.info(LogCategory.TEAM, "  - Con equipo pre-seleccionado:", String.valueOf(playersWithTeam.size()));
        LogSystem.info(LogCategory.TEAM, "  - Auto-asignados:", String.valueOf(playersWithoutTeam.size()));
        for (ArenaTeam team : teams) {
            LogSystem.info(LogCategory.TEAM, "  -", team.getColor().getName() + ":", team.countPlayers() + " jugadores");
        }
        LogSystem.info(LogCategory.TEAM, "═══════════════════════════════════════");

        GlobalTabManager.getInstance().updateAllPlayerTabs();
        this.removeEmptyTeams();
        this.finalizeTeamSetup();
    }

    private void finalizeTeamSetup() {
        boolean isModeCW = PTC.getInstance().getGameManager().getGlobalSettings().isModeCW();
        int teamsWithPlayers = this.countTeamsWithPlayers();

        if (teamsWithPlayers < 1) {
            LogSystem.error(LogCategory.GAME, "Sin equipos con jugadores - Cancelando partida");

            Statics.gameStatus = GameStatus.LOBBY;
            BossBarAPI.setEnabled(false);

            for (ArenaTeam team : this.teamList.values()) {
                team.getTeamPlayers().clear();
            }

            Bukkit.broadcastMessage(Statics.c("&cLa partida fue cancelada: No hay suficientes equipos con jugadores."));
            Bukkit.broadcastMessage(Statics.c("&eUsa &6/ptc start &epara forzar el inicio con menos jugadores."));
            return;
        }

        if (teamsWithPlayers == 1) {
            if (isModeCW) {
                ArenaTeam winnerTeam = this.teamList.values().stream()
                        .filter(team -> team.countPlayers() > 0)
                        .findFirst()
                        .orElse(null);

                if (winnerTeam != null) {
                    LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
                    LogSystem.info(LogCategory.GAME, "VICTORIA AUTOMÁTICA - Solo 1 equipo con jugadores");
                    LogSystem.info(LogCategory.GAME, "Equipo ganador:", winnerTeam.getColor().getName());
                    LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

                    Statics.gameStatus = GameStatus.ENDED;
                    BossBarAPI.setEnabled(false);

                    String winnerClanTag = null;
                    try {
                        Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
                        if (adapter != null) {
                            if (winnerTeam.getColor() == TeamColor.BLUE) {
                                Method getBlueTagMethod = adapter.getClass().getMethod("getBlueClanTag");
                                winnerClanTag = (String) getBlueTagMethod.invoke(adapter);
                            } else if (winnerTeam.getColor() == TeamColor.RED) {
                                Method getRedTagMethod = adapter.getClass().getMethod("getRedClanTag");
                                winnerClanTag = (String) getRedTagMethod.invoke(adapter);
                            }

                            Method onWarEndedMethod = adapter.getClass().getMethod("onWarEnded", String.class);
                            onWarEndedMethod.invoke(adapter, winnerClanTag);
                        }
                    } catch (Exception e) {
                        LogSystem.error(LogCategory.GAME, "Error notificando victoria:", e.getMessage());
                    }

                    Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));
                    Bukkit.broadcastMessage(Statics.c("&a&l✔ VICTORIA AUTOMÁTICA"));
                    Bukkit.broadcastMessage(Statics.c("&7Solo el equipo " + winnerTeam.getColor().getChatColor() + winnerTeam.getColor().getName() + " &7tiene jugadores"));
                    if (winnerClanTag != null) {
                        Bukkit.broadcastMessage(Statics.c("&7Clan ganador: &e" + winnerClanTag));
                    }
                    Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));

                    for (ArenaTeam team : this.teamList.values()) {
                        team.getTeamPlayers().clear();
                    }
                    return;
                }
            } else {
                LogSystem.warn(LogCategory.GAME, "Iniciando con solo 1 equipo (modo prueba)");
                Bukkit.broadcastMessage(Statics.c("&e&l⚠ &ePartida iniciada con solo 1 equipo &7(modo prueba)"));
            }
        }

        LogSystem.info(LogCategory.GAME, "Equipos finalizados - Preparado para iniciar");
    }

    private void applyArenaSettings(ArenaSettings settings) {
        if (settings == null) {
            LogSystem.error(LogCategory.GAME, "ArenaSettings es null");
            return;
        }

        World world = Bukkit.getWorld(settings.getName());
        if (world == null) {
            LogSystem.error(LogCategory.GAME, "Mundo no encontrado:", settings.getName());
            return;
        }

        TimeOfDay timeOfDay = settings.getTimeOfDay();
        if (timeOfDay != null) {
            world.setTime(timeOfDay.getTicks());
            LogSystem.info(LogCategory.GAME, "Tiempo aplicado:", timeOfDay.getDisplayName());
        }

        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(0);
        world.setGameRuleValue("doWeatherCycle", "false");
        LogSystem.debug(LogCategory.GAME, "Clima desactivado en arena:", settings.getName());

        applyHealthToAllPlayers(settings.getExtraHearts());
    }

    public void applyHealthToAllPlayers(int extraHearts) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyHealthToPlayer(player, extraHearts);
        }

        if (extraHearts > 0) {
            LogSystem.info(LogCategory.GAME, "Vida extra aplicada:", "+" + extraHearts + " corazones");
        } else {
            LogSystem.info(LogCategory.GAME, "Vida restablecida a default (10 corazones)");
        }
    }

    public void applyHealthToPlayer(Player player, int extraHearts) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            double baseHealth = 20.0;
            double newMaxHealth = baseHealth + (extraHearts * 2.0);
            player.setMaxHealth(newMaxHealth);
            player.setHealth(newMaxHealth);
            LogSystem.debug(LogCategory.PLAYER, "Vida aplicada a", player.getName() + ":", newMaxHealth / 2.0 + " corazones");
        } catch (Exception e) {
            LogSystem.error(LogCategory.PLAYER, "Error aplicando vida a:", player.getName(), e.getMessage());
        }
    }

    public int getCurrentArenaExtraHearts() {
        Arena arena = this.gameManager.getArena();
        if (arena != null && arena.getArenaSettings() != null) {
            return arena.getArenaSettings().getExtraHearts();
        }
        return 0;
    }

    public List<GamePlayer> removeExistent(Collection<GamePlayer> players) {
        List<GamePlayer> newPlayers = Lists.newArrayList();

        for (GamePlayer player : players) {
            long count = this.teamList.values().stream().filter(arenaTeam -> arenaTeam.getTeamPlayers().contains(player)).count();

            if (count < 1L) {
                newPlayers.add(player);
            }
        }

        return newPlayers;
    }

    public void setupTeams(Arena arena) {
        ArenaSettings arenaSettings = arena.getArenaSettings();
        ArenaLocations arenaLocations = arenaSettings.getArenaLocations();

        Location centerSpawn = arenaLocations.getCenterSpawn();

        if (centerSpawn == null) {
            LogSystem.error(LogCategory.GAME, "Center spawn no configurado para arena:", arenaSettings.getName());
        }

        this.teamList.get(TeamColor.BLUE).setLocations(PlayerUtils.fixTeleport(arenaLocations.getBlueSpawn(), centerSpawn), arenaLocations.getBlueCore());
        this.teamList.get(TeamColor.GREEN).setLocations(PlayerUtils.fixTeleport(arenaLocations.getGreenSpawn(), centerSpawn), arenaLocations.getGreenCore());
        this.teamList.get(TeamColor.RED).setLocations(PlayerUtils.fixTeleport(arenaLocations.getRedSpawn(), centerSpawn), arenaLocations.getRedCore());
        this.teamList.get(TeamColor.YELLOW).setLocations(PlayerUtils.fixTeleport(arenaLocations.getYellowSpawn(), centerSpawn), arenaLocations.getYellowCore());
    }

    public void startGame() {
        synchronized (START_LOCK) {
            if (isStarting) {
                LogSystem.warn(LogCategory.GAME, "startGame() llamado pero ya está iniciando - ignorando");
                return;
            }

            if (Statics.gameStatus == GameStatus.STARTING) {
                LogSystem.warn(LogCategory.GAME, "startGame() llamado pero gameStatus ya es STARTING");
                return;
            }

            if (!this.checkStart()) {
                LogSystem.debug(LogCategory.GAME, "checkStart() falló - no se puede iniciar");
                return;
            }

            isStarting = true;

            try {
                new StartingTask().run();
                Statics.gameStatus = GameStatus.STARTING;
                BossBarAPI.setEnabled(true);
                LogSystem.info(LogCategory.GAME, "Partida iniciando correctamente");
            } catch (Exception e) {
                isStarting = false;
                LogSystem.error(LogCategory.GAME, "Error iniciando partida:", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void resetStartingFlag() {
        synchronized (START_LOCK) {
            isStarting = false;
            LogSystem.debug(LogCategory.GAME, "Starting flag reseteado");
        }
    }

    public boolean checkStart() {
        boolean isModeCW = PTC.getInstance().getGameManager().getGlobalSettings().isModeCW();
        int minPlayers = isModeCW ? 2 : 4;
        int currentPlayers = this.countPlayers();

        if (currentPlayers < 1) {
            return false;
        }

        if (currentPlayers < minPlayers) {
            LogSystem.debug(LogCategory.GAME, "No se puede iniciar:", currentPlayers + "/" + minPlayers + " jugadores");
            return false;
        }

        LogSystem.debug(LogCategory.GAME, "Puede iniciar:", currentPlayers + " jugadores");
        return true;
    }

    public void teleportPlayersToSpawns() {
        LogSystem.info(LogCategory.GAME, "Teleportando jugadores a spawns finales...");

        ArenaSettings arenaSettings = this.gameManager.getArena().getArenaSettings();
        this.applyArenaSettings(arenaSettings);

        Set<Chunk> chunksToLoad = new HashSet<>();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(onlinePlayer.getUniqueId());
            if (gamePlayer != null && gamePlayer.getArenaTeam() != null) {
                Location spawnLoc = gamePlayer.getArenaTeam().getSpawn();
                if (spawnLoc != null && spawnLoc.getLocation() != null) {
                    chunksToLoad.add(spawnLoc.getLocation().getChunk());
                }
            }
        }

        if (!chunksToLoad.isEmpty()) {
            LogSystem.info(LogCategory.GAME, "Pre-cargando", chunksToLoad.size() + " chunks de spawns...");
            for (Chunk chunk : chunksToLoad) {
                chunk.load(true);
            }
        }

        int delay = 0;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(onlinePlayer.getUniqueId());
            if (gamePlayer != null && gamePlayer.getArenaTeam() != null) {
                Player p = gamePlayer.getPlayer();
                if (p == null || !p.isOnline()) {
                    LogSystem.warn(LogCategory.GAME, "Jugador offline durante teleport:", gamePlayer.getName());
                    continue;
                }

                final GamePlayer finalGamePlayer = gamePlayer;
                final Player finalPlayer = p;

                Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
                    try {
                        Location spawnLoc = finalGamePlayer.getArenaTeam().handleRespawn(finalGamePlayer);
                        if (spawnLoc != null) {
                            finalPlayer.teleport(spawnLoc.getLocation());

                            Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
                                try {
                                    finalPlayer.playSound(finalPlayer.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0F, 1.0F);
                                } catch (Exception soundEx) {
                                    LogSystem.debug(LogCategory.GAME, "Error reproduciendo sonido para:", finalGamePlayer.getName());
                                }

                                finalGamePlayer.createPBoard();

                                LogSystem.debug(LogCategory.GAME, "Jugador", finalGamePlayer.getName(), "teleportado y configurado");
                            }, 5L);

                        } else {
                            LogSystem.error(LogCategory.GAME, "Spawn location null para:", finalGamePlayer.getName());
                        }

                    } catch (Exception e) {
                        LogSystem.error(LogCategory.PLAYER, "Error teleportando:", finalGamePlayer.getName(), e.getMessage());
                    }
                }, delay);

                delay += 2L;
            }
        }

        LogSystem.info(LogCategory.GAME, "Teleports programados -", delay / 2 + " jugadores");
    }

    public int countPlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    public GameManager getGameManager() {
        return this.gameManager;
    }

    public Map<TeamColor, ArenaTeam> getTeamList() {
        return this.teamList;
    }

    public ArenaTeam getSpectatorTeam() {
        return this.spectatorTeam;
    }
}