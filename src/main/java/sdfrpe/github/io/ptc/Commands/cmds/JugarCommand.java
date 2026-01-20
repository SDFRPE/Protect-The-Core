package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Game.Arena.GameSettings;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerUtils;
import sdfrpe.github.io.ptc.Tasks.InGame.ArenaTask;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;

import java.util.Map;

public class JugarCommand implements CommandExecutor {

    private static String c(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser usado por jugadores"));
            return true;
        }

        Player player = (Player) sender;
        GamePlayer gamePlayer = PTC.getInstance().getGameManager()
                .getPlayerManager().getPlayer(player.getUniqueId());

        if (gamePlayer == null) {
            player.sendMessage(c("&cError: No se encontró tu información de jugador"));
            return true;
        }

        if (Statics.gameStatus != GameStatus.IN_GAME) {
            player.sendMessage(c("&c▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.sendMessage(c("&c&l⚠ COMANDO NO DISPONIBLE ⚠"));
            player.sendMessage(c(""));
            player.sendMessage(c("&7Solo puedes usar &e/jugar"));
            player.sendMessage(c("&7durante una partida activa"));
            player.sendMessage(c("&c▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            return true;
        }

        if (gamePlayer.isCurrentlyPlaying()) {
            player.sendMessage(c("&c▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.sendMessage(c("&c&l⚠ YA ESTÁS JUGANDO ⚠"));
            player.sendMessage(c(""));
            player.sendMessage(c("&7Ya estás en el equipo " +
                    gamePlayer.getArenaTeam().getColor().getColoredName()));
            player.sendMessage(c("&7No necesitas usar este comando"));
            player.sendMessage(c("&c▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            return true;
        }

        if (!gamePlayer.canUseJugarCommand()) {
            sendLimitReachedMessage(player, gamePlayer);
            return true;
        }

        ArenaTeam bestTeam = findBestAvailableTeam();

        if (bestTeam == null) {
            player.sendMessage(c("&c▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.sendMessage(c("&c&l⚠ NO HAY EQUIPOS DISPONIBLES ⚠"));
            player.sendMessage(c(""));
            player.sendMessage(c("&7Todos los equipos han sido eliminados"));
            player.sendMessage(c("&7o están completos"));
            player.sendMessage(c("&c▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            return true;
        }

        assignPlayerToTeam(player, gamePlayer, bestTeam);

        return true;
    }

    private void assignPlayerToTeam(Player player, GamePlayer gamePlayer, ArenaTeam team) {
        LogSystem.info(LogCategory.PLAYER, "Asignando jugador vía /jugar:",
                player.getName(), "→", team.getColor().getName());

        ArenaTeam spectatorTeam = PTC.getInstance().getGameManager()
                .getGameSettings().getSpectatorTeam();
        if (spectatorTeam != null) {
            spectatorTeam.removePlayer(gamePlayer);
        }

        gamePlayer.setArenaTeam(team);
        team.addPlayer(gamePlayer, false);

        gamePlayer.incrementParticipation();
        int currentCount = gamePlayer.getParticipationCount();
        int remaining = gamePlayer.getRemainingParticipations();

        sdfrpe.github.io.ptc.Listeners.General.DBLoadListener.savePlayerParticipation(
                player.getUniqueId(), currentCount
        );

        sdfrpe.github.io.ptc.Listeners.General.DBLoadListener.addPlayerToCurrentGame(player.getUniqueId());

        player.setGameMode(GameMode.SURVIVAL);
        PlayerUtils.clean(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(player);
        }

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            player.teleport(team.getSpawn().getLocation());
        }, 5L);

        PTC.getInstance().getInventories().setInGame(player);
        team.setInventory(gamePlayer);

        try {
            GameSettings gameSettings = PTC.getInstance().getGameManager().getGameSettings();
            int extraHearts = gameSettings.getCurrentArenaExtraHearts();
            gameSettings.applyHealthToPlayer(player, extraHearts);
        } catch (Exception e) {
            LogSystem.error(LogCategory.PLAYER, "Error aplicando vida:", e.getMessage());
        }

        if (gamePlayer.getPBoard() == null) {
            gamePlayer.createPBoard();
        } else {
            gamePlayer.getPBoard().update(gamePlayer, false);
        }

        gamePlayer.updateTabList();
        gamePlayer.forceUpdateNameTag();

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(player);

            GamePlayer onlineGP = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(online.getUniqueId());
            if (onlineGP != null && onlineGP.getArenaTeam() != null) {
                if (onlineGP.getArenaTeam().getColor() == TeamColor.SPECTATOR) {
                    player.hidePlayer(online);
                }

                if (online != player) {
                    onlineGP.forceUpdateNameTag();
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            sdfrpe.github.io.ptc.Utils.PlayerTabUpdater.updateAllPlayerTabs();
        }, 3L);

        sendSuccessMessage(player, gamePlayer, team, currentCount, remaining);

        Bukkit.broadcastMessage(c(team.getColor().getChatColor() + player.getName() +
                " &7se ha unido al equipo " + team.getColor().getColoredName()));

        LogSystem.info(LogCategory.PLAYER, "Jugador asignado exitosamente:",
                player.getName(), "- Participación", currentCount + "/2");

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            if (Statics.gameStatus == GameStatus.IN_GAME) {
                ArenaTask task = ArenaTask.getInstance();
                if (task != null) {
                    String timeFormatted = task.getFormattedTime();
                    int timeRemaining = task.getTimeRemainingSeconds();
                    int dTime = task.getDurationSeconds();
                    float percentage = (float) (timeRemaining * 100) / (float) dTime;
                    String message = String.format("%sTiempo restante: %s", ChatColor.GREEN, timeFormatted);

                    BossBarAPI.updatePlayer(player, message, percentage);
                    LogSystem.debug(LogCategory.PLAYER, "BossBar mostrada para jugador vía /jugar:", player.getName());
                }
            }
        }, 5L);
    }

    private void sendSuccessMessage(Player player, GamePlayer gamePlayer,
                                    ArenaTeam team, int currentCount, int remaining) {
        player.sendMessage(c("&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(c("&a&l✔ ASIGNADO AL EQUIPO"));
        player.sendMessage(c(""));
        player.sendMessage(c("&7Has sido asignado al equipo"));
        player.sendMessage(c(team.getColor().getColoredName()));
        player.sendMessage(c(""));

        if (remaining == 0) {
            player.sendMessage(c("&c&l⚠ ÚLTIMA PARTICIPACIÓN ⚠"));
            player.sendMessage(c("&7No podrás usar &e/jugar &7nuevamente"));
            player.sendMessage(c("&7Si tu equipo es eliminado, quedarás"));
            player.sendMessage(c("&7como espectador permanente"));
        } else {
            player.sendMessage(c("&7Participaciones: &e" + currentCount + "&7/&c2"));
            player.sendMessage(c("&c⚠ &7Solo puedes usar &e/jugar &c" +
                    remaining + " vez" + (remaining == 1 ? "" : "es") + " &7más"));
        }

        player.sendMessage(c(""));
        player.sendMessage(c("&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    private void sendLimitReachedMessage(Player player, GamePlayer gamePlayer) {
        player.sendMessage(c("&c&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendMessage(c("&c&l⚠ LÍMITE ALCANZADO ⚠"));
        player.sendMessage(c(""));
        player.sendMessage(c("&7Ya has jugado &c2 veces &7en esta partida"));
        player.sendMessage(c("&7No puedes unirte nuevamente para evitar abusos"));
        player.sendMessage(c(""));
        player.sendMessage(c("&7Participaciones: &c2&7/&c2 &c(MÁXIMO)"));
        player.sendMessage(c("&7Puedes seguir observando como espectador"));
        player.sendMessage(c("&c&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        LogSystem.debug(LogCategory.PLAYER, "Comando /jugar bloqueado:",
                player.getName(), "- Límite alcanzado (2/2)");
    }

    private ArenaTeam findBestAvailableTeam() {
        Map<TeamColor, ArenaTeam> teams = PTC.getInstance().getGameManager()
                .getGameSettings().getTeamList();

        ArenaTeam bestTeam = null;
        int minPlayers = Integer.MAX_VALUE;

        for (ArenaTeam team : teams.values()) {
            if (team.getColor() == TeamColor.SPECTATOR ||
                    team.getColor() == TeamColor.LOBBY) {
                continue;
            }

            if (team.isDeathTeam()) {
                continue;
            }

            int playerCount = team.countPlayers();
            if (playerCount < minPlayers) {
                minPlayers = playerCount;
                bestTeam = team;
            }
        }

        if (bestTeam != null) {
            LogSystem.debug(LogCategory.TEAM, "Mejor equipo encontrado:",
                    bestTeam.getColor().getName(), "con", minPlayers + " jugadores");
        } else {
            LogSystem.warn(LogCategory.TEAM, "No se encontraron equipos disponibles");
        }

        return bestTeam;
    }
}