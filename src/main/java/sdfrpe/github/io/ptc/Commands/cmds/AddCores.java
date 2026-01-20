package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Game.Arena.GameSettings;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;

public class AddCores extends SubCommand {

    public AddCores(PTC plugin) {
        super(plugin, "Add Cores", "addCores", "Añade destrucciones a un equipo específico.", "/ptc addCores <team> <cantidad>");
    }

    @Override
    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        if (Statics.gameStatus != GameStatus.IN_GAME) {
            sender.sendMessage(this.c("&c✖ Este comando solo funciona durante una partida activa."));
            return false;
        }

        if (args.length < 3) {
            this.sendHelp(sender);
            sender.sendMessage(this.c("&7Equipos disponibles: &eRED, BLUE, GREEN, YELLOW, ALL"));
            return false;
        }

        String teamInput = args[1].toUpperCase();
        int coresToAdd;

        try {
            coresToAdd = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(this.c("&c✖ La cantidad debe ser un número válido."));
            sender.sendMessage(this.c("&7Ejemplo: &e/ptc addCores RED 3"));
            return false;
        }

        if (coresToAdd <= 0) {
            sender.sendMessage(this.c("&c✖ La cantidad debe ser mayor a 0."));
            return false;
        }

        GameSettings gameSettings = this.plugin.getGameManager().getGameSettings();

        if (teamInput.equals("ALL")) {
            int teamsAffected = 0;
            for (ArenaTeam team : gameSettings.getTeamList().values()) {
                if (team.getColor() != TeamColor.SPECTATOR && team.getColor() != TeamColor.LOBBY) {
                    team.setCores(team.getCores() + coresToAdd);
                    teamsAffected++;
                }
            }

            Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));
            Bukkit.broadcastMessage(Statics.c("&a&l✓ CORES AÑADIDOS A TODOS LOS EQUIPOS"));
            Bukkit.broadcastMessage(Statics.c("&7Cantidad añadida: &e+" + coresToAdd + " destrucciones"));
            Bukkit.broadcastMessage(Statics.c("&7Equipos afectados: &e" + teamsAffected));
            Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));

            LogSystem.info(LogCategory.GAME, "Admin", sender.getName(), "añadió", coresToAdd + " cores", "a todos los equipos");

            updateAllScoreboards();
            return true;
        }

        TeamColor teamColor;
        try {
            teamColor = TeamColor.valueOf(teamInput);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(this.c("&c✖ Equipo no válido: &4" + teamInput));
            sender.sendMessage(this.c("&7Equipos disponibles: &eRED, BLUE, GREEN, YELLOW, ALL"));
            return false;
        }

        if (teamColor == TeamColor.SPECTATOR || teamColor == TeamColor.LOBBY) {
            sender.sendMessage(this.c("&c✖ No puedes añadir cores a equipos especiales."));
            return false;
        }

        ArenaTeam arenaTeam = gameSettings.getTeamList().get(teamColor);

        if (arenaTeam == null) {
            sender.sendMessage(this.c("&c✖ El equipo &4" + teamColor.getName() + " &cno existe en esta partida."));
            return false;
        }

        int oldCores = arenaTeam.getCores();
        int newCores = oldCores + coresToAdd;
        arenaTeam.setCores(newCores);

        if (arenaTeam.isDeathTeam() && newCores > 0) {
            arenaTeam.setDeathTeam(false);
        }

        Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));
        Bukkit.broadcastMessage(Statics.c("&e&l⚡ CORES AÑADIDOS"));
        Bukkit.broadcastMessage(Statics.c("&7Equipo: " + teamColor.getChatColor() + teamColor.getName()));
        Bukkit.broadcastMessage(Statics.c("&7Cores anteriores: &e" + oldCores));
        Bukkit.broadcastMessage(Statics.c("&7Cores actuales: &a" + newCores + " &7(+" + coresToAdd + ")"));
        Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));

        LogSystem.info(LogCategory.GAME, "Admin", sender.getName(), "añadió", coresToAdd + " cores", "al equipo", teamColor.getName(), "(" + oldCores, "→", newCores + ")");

        updateAllScoreboards();
        return true;
    }

    private void updateAllScoreboards() {
        for (GamePlayer gamePlayer : this.plugin.getGameManager().getPlayerManager().getPlayerMap().values()) {
            if (gamePlayer.getPlayer() != null && gamePlayer.getPlayer().isOnline()) {
                gamePlayer.createPBoard();
            }
        }
    }
}