package sdfrpe.github.io.ptc.Commands.cmds;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Utils.Console;
import org.bukkit.command.CommandSender;

public class SetLobbyServer extends SubCommand {
    public SetLobbyServer(PTC plugin) {
        super(plugin, "Set Lobby Server", "setLobbyServer", "Configura el nombre del servidor de lobby para BungeeCord.", "/ptc setLobbyServer <nombre>");
    }

    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.c("&cUso: /ptc setLobbyServer <nombre>"));
            sender.sendMessage(this.c("&7Ejemplo: /ptc setLobbyServer PTC-Lobby"));
            return false;
        }

        String lobbyServerName = args[1];

        this.plugin.getGameManager().getGlobalSettings().setLobbyServerName(lobbyServerName);
        this.plugin.getGameManager().saveGlobalSettings();

        Console.log("Lobby server name set to: " + lobbyServerName);
        Console.debug("Saving GlobalSettings to: " + this.plugin.getGameManager().getSettingsFile().getAbsolutePath());

        sender.sendMessage(this.c("&aServidor de lobby configurado a: &f" + lobbyServerName));
        sender.sendMessage(this.c("&7Este es el servidor al que se enviarán los jugadores cuando salgan o sean eliminados."));

        return true;
    }
}