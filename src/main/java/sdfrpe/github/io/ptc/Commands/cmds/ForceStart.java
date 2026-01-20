package sdfrpe.github.io.ptc.Commands.cmds;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Tasks.InGame.StartingTask;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class ForceStart extends SubCommand {
    public ForceStart(PTC plugin) {
        super(plugin, "Force Start", "start", "Force the game to start with any number of players.", "/ptc start");
    }

    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        int playerCount = Bukkit.getOnlinePlayers().size();
        boolean isModeCW = this.plugin.getGameManager().getGlobalSettings().isModeCW();

        int minPlayers = isModeCW ? 2 : 1;

        if (playerCount < minPlayers) {
            sender.sendMessage(this.c("&cSe necesita al menos " + minPlayers + " jugador(es) para iniciar!"));
            sender.sendMessage(this.c("&7Jugadores actuales: &f" + playerCount));
            if (isModeCW) {
                sender.sendMessage(this.c("&7Modo Clan War requiere 1 jugador por equipo (2 total)"));
            }
            return false;
        }

        if (Statics.gameStatus != GameStatus.LOBBY) {
            sender.sendMessage(this.c("&cEl juego ya está en progreso o iniciando!"));
            sender.sendMessage(this.c("&7Estado actual: &f" + Statics.gameStatus.name()));
            return false;
        }

        if (this.plugin.getGameManager().getArenaManager().countArenas() < 1) {
            sender.sendMessage(this.c("&cNo hay arenas disponibles! Crea una primero con /ptc create <nombre>"));
            return false;
        }

        Console.log("Force starting game with " + playerCount + " players by " + sender.getName());
        sender.sendMessage(this.c("&a¡Forzando inicio del juego con &f" + playerCount + " &ajugador(es)!"));
        Bukkit.broadcastMessage(this.c("&e&l¡La partida está comenzando FORZOSAMENTE!"));

        new StartingTask(true).run();
        Statics.gameStatus = GameStatus.STARTING;
        BossBarAPI.setEnabled(true);

        return true;
    }
}