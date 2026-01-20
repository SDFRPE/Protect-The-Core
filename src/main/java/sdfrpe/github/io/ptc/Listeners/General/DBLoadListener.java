package sdfrpe.github.io.ptc.Listeners.General;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import sdfrpe.github.io.ptc.Database.Structures.GameLoader;
import sdfrpe.github.io.ptc.Events.Player.PlayerLoadEvent;
import sdfrpe.github.io.ptc.Events.Player.PlayerUnloadEvent;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerStats;
import sdfrpe.github.io.ptc.Player.PlayerUtils;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Location;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Managers.GlobalTabManager;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import sdfrpe.github.io.ptc.Utils.Statics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DBLoadListener implements Listener {
    private static final Set<UUID> playersInCurrentGame = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> playerParticipations = new ConcurrentHashMap<>();
    private static final int LOAD_TIMEOUT_TICKS = 60;

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerLoad(PlayerLoadEvent e) {
        boolean isLobbyMode = PTC.getInstance().getGameManager().getGlobalSettings().isLobbyMode();

        if (isLobbyMode) {
            GameLoader loader = (GameLoader) PTC.getInstance().getGameManager().getDatabase();
            PlayerStats loadedStats = loader.loadPlayerDataSync(e.getUniqueId());

            GamePlayer gamePlayer = new GamePlayer(e.getUniqueId(), null);
            gamePlayer.setPlayerStats(loadedStats);

            PTC.getInstance().getGameManager().getPlayerManager().addPlayer(e.getUniqueId(), gamePlayer);

            LogSystem.debug(LogCategory.PLAYER, "Datos cargados:", e.getUniqueId().toString(),
                    "(Modo Lobby, XP:", String.valueOf(loadedStats.getPlayerLevels().getTotalExp()) + ")");
            return;
        }

        if (Statics.gameStatus != GameStatus.IN_GAME && Statics.gameStatus != GameStatus.ENDED) {
            GameLoader loader = (GameLoader) PTC.getInstance().getGameManager().getDatabase();
            PlayerStats loadedStats = loader.loadPlayerDataSync(e.getUniqueId());

            GamePlayer gamePlayer = new GamePlayer(e.getUniqueId(), null);
            gamePlayer.setPlayerStats(loadedStats);

            PTC.getInstance().getGameManager().getPlayerManager().addPlayer(e.getUniqueId(), gamePlayer);

            LogSystem.debug(LogCategory.PLAYER, "Datos cargados:", e.getUniqueId().toString(),
                    "(XP:", String.valueOf(loadedStats.getPlayerLevels().getTotalExp()) + ")");
        } else {
            LogSystem.debug(LogCategory.PLAYER, "Jugador conectando durante IN_GAME:", e.getUniqueId().toString());

            GameLoader loader = (GameLoader) PTC.getInstance().getGameManager().getDatabase();
            PlayerStats loadedStats = loader.loadPlayerDataSync(e.getUniqueId());

            GamePlayer gamePlayer = new GamePlayer(e.getUniqueId(), null);
            gamePlayer.setPlayerStats(loadedStats);

            PTC.getInstance().getGameManager().getPlayerManager().addPlayer(e.getUniqueId(), gamePlayer);

            LogSystem.debug(LogCategory.PLAYER, "Datos cargados durante partida:", e.getUniqueId().toString(),
                    "(XP:", String.valueOf(loadedStats.getPlayerLevels().getTotalExp()) + ")");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void PlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        final UUID playerUUID = player.getUniqueId();

        e.setJoinMessage(null);

        if (Statics.gameStatus == GameStatus.IN_GAME || Statics.gameStatus == GameStatus.ENDED) {
            boolean hasPlayedThisGame = playersInCurrentGame.contains(playerUUID);

            if (hasPlayedThisGame) {
                if (!player.hasPermission("ptc.rejoin")) {
                    LogSystem.debug(LogCategory.PLAYER, "Jugador reconectando sin permiso rejoin expulsado:", player.getName());
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                            "&c&l⚠ RECONEXIÓN NO PERMITIDA ⚠\n\n" +
                                    "&7Necesitas el permiso &eptc.rejoin\n" +
                                    "&7para volver a unirte a la partida.\n\n" +
                                    "&6discord.me/ElDaysuu"));
                    return;
                }
            } else {
                boolean canJoinAsPlayer = player.hasPermission("ptc.joiningame");
                boolean canJoinAsSpectator = player.hasPermission("ptc.joinspectator");

                if (!canJoinAsPlayer && !canJoinAsSpectator) {
                    LogSystem.debug(LogCategory.PLAYER, "Jugador sin permisos VIP bloqueado:", player.getName());
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                            "&c&l⚠ PARTIDA EN CURSO ⚠\n\n" +
                                    "&7La partida ya está en progreso.\n" +
                                    "&e¡Adquiere el rango VIP para\n" +
                                    "&eacceder a partidas en curso!\n\n" +
                                    "&6discord.me/ElDaysuu"));
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), new Runnable() {
            @Override
            public void run() {
                GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(playerUUID);

                if (gamePlayer == null) {
                    Player p = Bukkit.getPlayer(playerUUID);
                    if (p != null && p.isOnline()) {
                        LogSystem.error(LogCategory.PLAYER, "GamePlayer no cargado después de timeout:", playerUUID.toString());
                        p.kickPlayer(ChatColor.RED + "Tus datos no se cargaron correctamente, por favor reconéctate.");
                    }
                    return;
                }

                completePlayerJoin(player, gamePlayer);
            }
        }, LOAD_TIMEOUT_TICKS);
    }

    private void completePlayerJoin(Player player, GamePlayer gamePlayer) {
        if (player == null || !player.isOnline()) {
            return;
        }

        boolean isLobbyMode = PTC.getInstance().getGameManager().getGlobalSettings().isLobbyMode();

        if (isLobbyMode) {
            gamePlayer.setName(player.getName());
            gamePlayer.getPlayerStats().applyExp(player);

            if (gamePlayer.getArenaTeam() == null) {
                ArenaTeam lobbyTeam = PTC.getInstance().getGameManager().getGameSettings().getTeamList().get(TeamColor.LOBBY);
                if (lobbyTeam != null) {
                    gamePlayer.setArenaTeam(lobbyTeam);
                    lobbyTeam.getTeamPlayers().add(gamePlayer);
                }
            }

            gamePlayer.startPlayerRunnable();
            LogSystem.debug(LogCategory.DATABASE, "XP aplicada al entrar:",
                    String.valueOf(gamePlayer.getPlayerStats().getPlayerLevels().getTotalExp()));
            LogSystem.logPlayerJoin(player.getName());
            return;
        }

        if (Statics.gameStatus == GameStatus.IN_GAME || Statics.gameStatus == GameStatus.ENDED) {
            boolean hasPlayedThisGame = playersInCurrentGame.contains(player.getUniqueId());

            if (hasPlayedThisGame) {
                handleReconnectingPlayer(player, gamePlayer);
            } else {
                handleNewVIPPlayer(player, gamePlayer);
            }
            return;
        }

        gamePlayer.setName(player.getName());
        gamePlayer.getPlayerStats().applyExp(player);

        if (gamePlayer.getArenaTeam() == null) {
            ArenaTeam lobbyTeam = PTC.getInstance().getGameManager().getGameSettings().getTeamList().get(TeamColor.LOBBY);
            if (lobbyTeam != null) {
                gamePlayer.setArenaTeam(lobbyTeam);
                lobbyTeam.getTeamPlayers().add(gamePlayer);
            }
        }

        if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
            assignClanWarTeamOnJoin(player, gamePlayer);
        }

        if (Statics.gameStatus == GameStatus.LOBBY || Statics.gameStatus == GameStatus.STARTING) {
            GlobalTabManager.getInstance().applyToPlayer(player);
            GlobalTabManager.getInstance().updatePlayerTab(gamePlayer);
            PTC.getInstance().getGameManager().getScoreManager().addLobby(player);
        } else if (Statics.gameStatus == GameStatus.IN_GAME || Statics.gameStatus == GameStatus.ENDED) {
            gamePlayer.createPBoard();
        }

        gamePlayer.startPlayerRunnable();

        LogSystem.debug(LogCategory.DATABASE, "XP aplicada al entrar:",
                String.valueOf(gamePlayer.getPlayerStats().getPlayerLevels().getTotalExp()));

        LogSystem.logPlayerJoin(player.getName());
    }

    private void handleReconnectingPlayer(Player player, GamePlayer gamePlayer) {
        LogSystem.info(LogCategory.PLAYER, "Jugador reconectando (espectador):", player.getName());

        ArenaTeam spectatorTeam = PTC.getInstance().getGameManager().getGameSettings().getSpectatorTeam();

        if (gamePlayer.getArenaTeam() != null && gamePlayer.getArenaTeam() != spectatorTeam) {
            gamePlayer.getArenaTeam().removePlayer(gamePlayer);
        }

        gamePlayer.setName(player.getName());
        gamePlayer.getPlayerStats().applyExp(player);

        Integer savedParticipations = playerParticipations.get(player.getUniqueId());
        if (savedParticipations != null) {
            for (int i = 0; i < savedParticipations; i++) {
                gamePlayer.incrementParticipation();
            }
            LogSystem.info(LogCategory.PLAYER, "Participaciones restauradas:",
                    player.getName(), String.valueOf(savedParticipations) + "/2");
        }

        gamePlayer.setArenaTeam(spectatorTeam);
        spectatorTeam.getTeamPlayers().add(gamePlayer);

        player.setGameMode(GameMode.CREATIVE);
        PlayerUtils.fakeSpectator(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
                online.hidePlayer(player);
            }
        }

        gamePlayer.createPBoard();

        try {
            Location center = PTC.getInstance().getGameManager().getArena().getArenaSettings().getArenaLocations().getCenterSpawn();
            if (center != null) {
                final Player finalPlayer = player;
                Bukkit.getScheduler().runTaskLater(PTC.getInstance(), new Runnable() {
                    public void run() {
                        finalPlayer.teleport(center.getLocation());
                    }
                }, 5L);
            }
        } catch (Exception ex) {
            LogSystem.error(LogCategory.PLAYER, "Error teleportando espectador:", ex.getMessage());
        }

        gamePlayer.startPlayerRunnable();

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            int participations = gamePlayer.getParticipationCount();
            int remaining = gamePlayer.getRemainingParticipations();

            if (remaining > 0) {
                player.sendMessage(Statics.c("&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                player.sendMessage(Statics.c("&e&l¡RECONECTADO COMO ESPECTADOR!"));
                player.sendMessage(Statics.c(""));
                player.sendMessage(Statics.c("&7Usa el comando &e/jugar &7para"));
                player.sendMessage(Statics.c("&7unirte a un equipo activo"));
                player.sendMessage(Statics.c(""));
                player.sendMessage(Statics.c("&7Participaciones: &e" + participations + "&7/&c2"));
                player.sendMessage(Statics.c("&7Puedes jugar &c" + remaining + " vez" + (remaining == 1 ? "" : "es") + " &7más"));
                player.sendMessage(Statics.c("&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            } else {
                player.sendMessage(Statics.c("&c&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                player.sendMessage(Statics.c("&e&l¡RECONECTADO COMO ESPECTADOR!"));
                player.sendMessage(Statics.c(""));
                player.sendMessage(Statics.c("&c&lLÍMITE ALCANZADO"));
                player.sendMessage(Statics.c("&7Ya jugaste &c2 veces &7en esta partida"));
                player.sendMessage(Statics.c("&7Continuarás como espectador"));
                player.sendMessage(Statics.c(""));
                player.sendMessage(Statics.c("&7Participaciones: &c2&7/&c2"));
                player.sendMessage(Statics.c("&c&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            }
        }, 20L);

        LogSystem.debug(LogCategory.DATABASE, "XP aplicada al reconectar:",
                String.valueOf(gamePlayer.getPlayerStats().getPlayerLevels().getTotalExp()));
    }

    private void handleNewVIPPlayer(Player player, GamePlayer gamePlayer) {
        boolean canJoinAsPlayer = player.hasPermission("ptc.joiningame");
        boolean canJoinAsSpectator = player.hasPermission("ptc.joinspectator");

        gamePlayer.setName(player.getName());
        gamePlayer.getPlayerStats().applyExp(player);
        gamePlayer.resetParticipation();

        if (canJoinAsPlayer || canJoinAsSpectator) {
            LogSystem.info(LogCategory.PLAYER, "VIP ingresando como espectador (usará /jugar):", player.getName());
            handleVIPSpectatorEntry(player, gamePlayer);
        } else {
            player.kickPlayer(Statics.c("&cNo tienes permisos VIP"));
        }

        LogSystem.debug(LogCategory.DATABASE, "XP aplicada a VIP:",
                String.valueOf(gamePlayer.getPlayerStats().getPlayerLevels().getTotalExp()));
    }

    private void handleVIPSpectatorEntry(Player player, GamePlayer gamePlayer) {
        ArenaTeam spectatorTeam = PTC.getInstance().getGameManager().getGameSettings().getSpectatorTeam();

        gamePlayer.setArenaTeam(spectatorTeam);
        spectatorTeam.getTeamPlayers().add(gamePlayer);
        gamePlayer.resetParticipation();

        player.setGameMode(GameMode.CREATIVE);
        PlayerUtils.fakeSpectator(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
                online.hidePlayer(player);
            }
        }

        gamePlayer.createPBoard();
        gamePlayer.startPlayerRunnable();

        try {
            sdfrpe.github.io.ptc.Utils.Location center = PTC.getInstance().getGameManager()
                    .getArena().getArenaSettings().getArenaLocations().getCenterSpawn();
            if (center != null) {
                final Player finalPlayer = player;
                Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
                    finalPlayer.teleport(center.getLocation());
                }, 5L);
            }
        } catch (Exception ex) {
            LogSystem.error(LogCategory.PLAYER, "Error teleportando espectador VIP:", ex.getMessage());
        }

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            try {
                TitleAPI titleAPI = new TitleAPI();
                titleAPI.title(Statics.c("&e&lUSA /JUGAR"));
                titleAPI.subTitle(Statics.c("&7Para unirte a un equipo"));
                titleAPI.fadeInTime(10);
                titleAPI.showTime(100);
                titleAPI.fadeOutTime(10);
                titleAPI.send(player);
            } catch (Exception e) {
                LogSystem.error(LogCategory.PLAYER, "Error enviando título:", e.getMessage());
            }
        }, 10L);

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            player.sendMessage(Statics.c("&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.sendMessage(Statics.c("&e&lBIENVENIDO A LA PARTIDA"));
            player.sendMessage(Statics.c(""));
            player.sendMessage(Statics.c("&7Usa el comando &e/jugar &7para"));
            player.sendMessage(Statics.c("&7unirte a un equipo activo"));
            player.sendMessage(Statics.c(""));
            player.sendMessage(Statics.c("&7Participaciones: &a0&7/&c2"));
            player.sendMessage(Statics.c("&7Puedes jugar hasta &c2 veces &7en esta partida"));
            player.sendMessage(Statics.c("&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        }, 20L);

        LogSystem.info(LogCategory.PLAYER, "VIP ingresado como espectador:",
                player.getName(), "(Participaciones: 0/2)");
    }

    private void handleNewVIPSpectator(Player player, GamePlayer gamePlayer) {
        ArenaTeam spectatorTeam = PTC.getInstance().getGameManager().getGameSettings().getSpectatorTeam();

        if (gamePlayer.getArenaTeam() != null) {
            gamePlayer.getArenaTeam().removePlayer(gamePlayer);
        }

        gamePlayer.setArenaTeam(spectatorTeam);
        spectatorTeam.getTeamPlayers().add(gamePlayer);

        player.setGameMode(GameMode.CREATIVE);
        PlayerUtils.fakeSpectator(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
                online.hidePlayer(player);
            }
        }

        gamePlayer.createPBoard();

        try {
            Location center = PTC.getInstance().getGameManager().getArena().getArenaSettings().getArenaLocations().getCenterSpawn();
            if (center != null) {
                final Player finalPlayer = player;
                Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
                    finalPlayer.teleport(center.getLocation());
                }, 5L);
            }
        } catch (Exception ex) {
            LogSystem.error(LogCategory.PLAYER, "Error teleportando espectador VIP:", ex.getMessage());
        }

        gamePlayer.startPlayerRunnable();

        player.sendMessage(ChatColor.GOLD + "¡Has ingresado como espectador!");
        player.sendMessage(ChatColor.GRAY + "Eres invisible para los demás jugadores.");
        player.sendMessage(ChatColor.GRAY + "Apareces en el tabulador pero no pueden verte.");
    }

    private ArenaTeam findBestTeamForNewPlayer() {
        ArenaTeam smallestTeam = null;
        int smallestSize = Integer.MAX_VALUE;

        for (ArenaTeam team : PTC.getInstance().getGameManager().getGameSettings().getTeamList().values()) {
            if (team.getColor() == TeamColor.SPECTATOR || team.getColor() == TeamColor.LOBBY) {
                continue;
            }

            if (team.isDeathTeam()) {
                continue;
            }

            int teamSize = team.countPlayers();

            if (teamSize < smallestSize) {
                smallestSize = teamSize;
                smallestTeam = team;
            }
        }

        if (smallestTeam != null) {
            LogSystem.debug(LogCategory.TEAM, "Mejor equipo para nuevo VIP:",
                    smallestTeam.getColor().getName(), "con", String.valueOf(smallestSize), "jugadores");
        }

        return smallestTeam;
    }

    private void assignClanWarTeamOnJoin(Player player, GamePlayer gamePlayer) {
        try {
            Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
            if (adapter == null) {
                LogSystem.warn(LogCategory.TEAM, "No se pudo asignar equipo CW - Adapter null:", player.getName());
                return;
            }

            Method isPlayerInBlueMethod = adapter.getClass().getMethod("isPlayerInBlueClan", UUID.class);
            Method isPlayerInRedMethod = adapter.getClass().getMethod("isPlayerInRedClan", UUID.class);
            Method getBlueDisplayNameMethod = adapter.getClass().getMethod("getBlueClanDisplayName");
            Method getRedDisplayNameMethod = adapter.getClass().getMethod("getRedClanDisplayName");
            Method getBlueColorMethod = adapter.getClass().getMethod("getBlueClanColor");
            Method getRedColorMethod = adapter.getClass().getMethod("getRedClanColor");

            Boolean isBlue = (Boolean) isPlayerInBlueMethod.invoke(adapter, player.getUniqueId());
            Boolean isRed = (Boolean) isPlayerInRedMethod.invoke(adapter, player.getUniqueId());

            if (isBlue != null && isBlue) {
                ArenaTeam blueTeam = PTC.getInstance().getGameManager().getGameSettings().getTeamList().get(TeamColor.BLUE);
                if (blueTeam != null) {
                    blueTeam.addPlayer(gamePlayer, false);

                    String displayName = (String) getBlueDisplayNameMethod.invoke(adapter);
                    String clanColor = (String) getBlueColorMethod.invoke(adapter);
                    int teammates = blueTeam.countPlayers();

                    LogSystem.info(LogCategory.TEAM, "Jugador asignado al EQUIPO AZUL:", player.getName(), "- Clan:", displayName);

                    new TitleAPI()
                            .title(clanColor.replace("&", "§") + displayName)
                            .subTitle(ChatColor.WHITE + "Compañeros: " + ChatColor.YELLOW + teammates)
                            .send(player);

                    player.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
                    player.sendMessage(ChatColor.AQUA + "  ✦ EQUIPO ASIGNADO ✦");
                    player.sendMessage(clanColor.replace("&", "§") + "  " + displayName);
                    player.sendMessage(ChatColor.WHITE + "  Compañeros: " + ChatColor.YELLOW + teammates);
                    player.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
                }
            } else if (isRed != null && isRed) {
                ArenaTeam redTeam = PTC.getInstance().getGameManager().getGameSettings().getTeamList().get(TeamColor.RED);
                if (redTeam != null) {
                    redTeam.addPlayer(gamePlayer, false);

                    String displayName = (String) getRedDisplayNameMethod.invoke(adapter);
                    String clanColor = (String) getRedColorMethod.invoke(adapter);
                    int teammates = redTeam.countPlayers();

                    LogSystem.info(LogCategory.TEAM, "Jugador asignado al EQUIPO ROJO:", player.getName(), "- Clan:", displayName);

                    new TitleAPI()
                            .title(clanColor.replace("&", "§") + displayName)
                            .subTitle(ChatColor.WHITE + "Compañeros: " + ChatColor.YELLOW + teammates)
                            .send(player);

                    player.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
                    player.sendMessage(ChatColor.AQUA + "  ✦ EQUIPO ASIGNADO ✦");
                    player.sendMessage(clanColor.replace("&", "§") + "  " + displayName);
                    player.sendMessage(ChatColor.WHITE + "  Compañeros: " + ChatColor.YELLOW + teammates);
                    player.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
                }
            } else {
                LogSystem.warn(LogCategory.TEAM, "Jugador no pertenece a ningún clan en guerra:", player.getName());
            }

        } catch (Exception e) {
            LogSystem.error(LogCategory.TEAM, "Error asignando equipo CW:", player.getName(), e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerUnload(PlayerUnloadEvent e) {
        Player player = Bukkit.getPlayer(e.getUniqueId());
        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(e.getUniqueId());

        if (gamePlayer != null) {
            if (player != null) {
                gamePlayer.getPlayerStats().setExp(player);
                LogSystem.debug(LogCategory.DATABASE, "XP sincronizada antes de guardar:",
                        String.valueOf(gamePlayer.getPlayerStats().getPlayerLevels().getTotalExp()));
            }

            if (Statics.gameStatus == GameStatus.IN_GAME || Statics.gameStatus == GameStatus.ENDED) {
                int participations = gamePlayer.getParticipationCount();
                if (participations > 0) {
                    playerParticipations.put(e.getUniqueId(), participations);
                    LogSystem.info(LogCategory.PLAYER, "Participaciones guardadas para reconexión:",
                            gamePlayer.getName(), String.valueOf(participations) + "/2");
                }
            }

            gamePlayer.cleanup();

            PTC.getInstance().getGameManager().getArenaManager().removePlayerVotes(gamePlayer);
            PTC.getInstance().getGameManager().getGameVoteManager().removePlayerVotes(gamePlayer);

            if (gamePlayer.getArenaTeam() != null) {
                try {
                    gamePlayer.getArenaTeam().removePlayer(gamePlayer);
                } catch (Exception ex) {
                    LogSystem.error(LogCategory.TEAM, "Error removiendo jugador de equipo:", ex.getMessage());
                }
            }

            LogSystem.logPlayerQuit(gamePlayer.getName());
        }

        try {
            PTC.getInstance().getGameManager().getDatabase().savePlayerSync(e.getUniqueId());
            LogSystem.info(LogCategory.DATABASE, "Datos guardados síncronamente para:",
                    player != null ? player.getName() : e.getUniqueId().toString());
        } catch (Exception ex) {
            LogSystem.error(LogCategory.DATABASE, "Error guardando jugador:", ex.getMessage());
        }

        PTC.getInstance().getGameManager().getPlayerManager().removePlayer(e.getUniqueId());

        if (gamePlayer != null) {
            GameLoader loader = (GameLoader) PTC.getInstance().getGameManager().getDatabase();
            loader.clearRetries(e.getUniqueId());
        }
    }

    public static void registerGameStart() {
        playersInCurrentGame.clear();
        playerParticipations.clear();

        for (GamePlayer gp : PTC.getInstance().getGameManager().getPlayerManager().getPlayerMap().values()) {
            if (gp.getArenaTeam() != null && gp.getArenaTeam().getColor() != TeamColor.SPECTATOR) {
                playersInCurrentGame.add(gp.getUuid());
            }
        }

        LogSystem.info(LogCategory.GAME, "Jugadores registrados para partida:", playersInCurrentGame.size() + " jugadores");
        LogSystem.info(LogCategory.GAME, "Contador de participaciones reseteado");
    }

    public static void savePlayerParticipation(UUID playerUUID, int participations) {
        playerParticipations.put(playerUUID, participations);
        LogSystem.debug(LogCategory.PLAYER, "Participación guardada:",
                playerUUID.toString(), String.valueOf(participations) + "/2");
    }

    public static void addPlayerToCurrentGame(UUID playerUUID) {
        playersInCurrentGame.add(playerUUID);
        LogSystem.info(LogCategory.PLAYER, "Jugador añadido a partida actual:",
                playerUUID.toString());
    }

    public static void clearGamePlayers() {
        int count = playersInCurrentGame.size();
        int participationsCount = playerParticipations.size();
        playersInCurrentGame.clear();
        playerParticipations.clear();
        LogSystem.info(LogCategory.GAME, "Registro de jugadores limpiado:", count + " jugadores");
        LogSystem.info(LogCategory.GAME, "Participaciones limpiadas:", participationsCount + " registros");
    }
}