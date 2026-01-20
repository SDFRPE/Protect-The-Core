package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.PlayerTabUpdater;
import sdfrpe.github.io.ptc.Utils.Statics;

public class TeamCommand implements CommandExecutor {

    private final PTC plugin;

    public TeamCommand(PTC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ptc.team.set")) {
            sender.sendMessage(c("&cNo tienes el permiso &6ptc.team.set &cpara usar este comando."));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (!subCommand.equals("set")) {
            sendHelpMessage(sender);
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(c("&cUso: /team set <jugador> <equipo>"));
            sender.sendMessage(c("&7Equipos disponibles: &bBLUE&7, &aGREEN&7, &cRED&7, &eYELLOW"));
            return true;
        }

        if (Statics.gameStatus != GameStatus.IN_GAME) {
            sender.sendMessage(c("&cEste comando solo puede usarse durante una partida activa."));
            sender.sendMessage(c("&7Estado actual: &e" + Statics.gameStatus.name()));
            return true;
        }

        String playerName = args[1];
        String teamName = args[2].toUpperCase();

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(c("&cEl jugador &f" + playerName + " &cno está conectado."));
            return true;
        }

        TeamColor teamColor;
        try {
            teamColor = TeamColor.valueOf(teamName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(c("&cEquipo no válido: &f" + teamName));
            sender.sendMessage(c("&7Equipos disponibles: &bBLUE&7, &aGREEN&7, &cRED&7, &eYELLOW"));
            return true;
        }

        if (teamColor == TeamColor.SPECTATOR || teamColor == TeamColor.LOBBY) {
            sender.sendMessage(c("&cNo puedes asignar jugadores a los equipos SPECTATOR o LOBBY."));
            sender.sendMessage(c("&7Equipos válidos: &bBLUE&7, &aGREEN&7, &cRED&7, &eYELLOW"));
            return true;
        }

        ArenaTeam targetTeam = plugin.getGameManager().getGameSettings().getTeamList().get(teamColor);
        if (targetTeam == null) {
            sender.sendMessage(c("&cEl equipo &f" + teamColor.getName() + " &cno existe en esta partida."));
            return true;
        }

        if (targetTeam.isDeathTeam()) {
            sender.sendMessage(c("&cEl equipo &f" + teamColor.getName() + " &cya ha sido eliminado."));
            return true;
        }

        GamePlayer gamePlayer = plugin.getGameManager().getPlayerManager().getPlayer(target.getUniqueId());
        if (gamePlayer == null) {
            sender.sendMessage(c("&cNo se pudo encontrar la información del jugador."));
            return true;
        }

        ArenaTeam oldTeam = gamePlayer.getArenaTeam();
        boolean wasSpectator = (oldTeam != null && oldTeam.getColor() == TeamColor.SPECTATOR);

        if (oldTeam != null) {
            if (oldTeam.getColor() == teamColor) {
                sender.sendMessage(c("&e" + target.getName() + " &cya está en el equipo &f" + teamColor.getName() + "&c."));
                return true;
            }

            oldTeam.removePlayer(gamePlayer);
            Console.debug("Removed " + gamePlayer.getName() + " from team " + oldTeam.getColor().getName());
        }

        if (wasSpectator) {
            target.setGameMode(GameMode.SURVIVAL);
            Console.debug("Restaurando GameMode SURVIVAL para: " + target.getName());
        }

        targetTeam.addPlayer(gamePlayer, false);
        Console.log(sender.getName() + " moved player " + target.getName() + " to team " + teamColor.getName());

        if (gamePlayer.getPBoard() == null || wasSpectator) {
            gamePlayer.createPBoard();
            Console.debug("Scoreboard creado/actualizado para: " + target.getName());
        } else {
            gamePlayer.getPBoard().update(gamePlayer, false);
            Console.debug("Scoreboard actualizado para: " + target.getName());
        }

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            PlayerTabUpdater.updateAllPlayerTabs();
        }, 3L);

        try {
            sdfrpe.github.io.ptc.Utils.Location spawnLoc = targetTeam.handleRespawn(gamePlayer);
            if (spawnLoc != null) {
                target.teleport(spawnLoc.getLocation());
                Console.debug("Teleported " + target.getName() + " to new team spawn");
            }
        } catch (Exception e) {
            Console.error("Error teleporting player to new team: " + e.getMessage());
        }

        sender.sendMessage(c("&a✓ &f" + target.getName() + " &aha sido asignado al equipo " + teamColor.getChatColor() + teamColor.getName()));
        target.sendMessage(c("&eHas sido movido al equipo " + teamColor.getChatColor() + "&l" + teamColor.getName()));

        String announcement = String.format("&e%s &7fue movido al equipo %s%s",
                target.getName(),
                teamColor.getChatColor(),
                teamColor.getName());
        Bukkit.broadcastMessage(Statics.c(announcement));

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(c("&6&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(c("&e&lComando de Equipos"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&f/team set <jugador> <equipo>"));
        sender.sendMessage(c("&7  Asigna un jugador a un equipo específico"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Equipos disponibles:"));
        sender.sendMessage(c("  &9● &bBLUE   &7(Azul)"));
        sender.sendMessage(c("  &2● &aGREEN  &7(Verde)"));
        sender.sendMessage(c("  &4● &cRED    &7(Rojo)"));
        sender.sendMessage(c("  &6● &eYELLOW &7(Amarillo)"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Solo funciona durante partidas activas &8(IN_GAME)"));
        sender.sendMessage(c("&6&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}