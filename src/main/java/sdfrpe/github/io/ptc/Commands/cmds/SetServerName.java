package sdfrpe.github.io.ptc.Commands.cmds;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Utils.Console;
import org.bukkit.command.CommandSender;

public class SetServerName extends SubCommand {
    public SetServerName(PTC plugin) {
        super(plugin, "Set Server Name", "setServerName", "Configura el nombre de este servidor en el sistema.", "/ptc setServerName <nombre>");
    }

    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.c("&cUso: /ptc setServerName <nombre>"));
            sender.sendMessage(this.c("&7Ejemplo: /ptc setServerName PTC-1"));
            return false;
        }

        String serverName = args[1];

        this.plugin.getGameManager().getGlobalSettings().setServerName(serverName);
        this.plugin.getGameManager().saveGlobalSettings();

        Console.log("Server name set to: " + serverName);
        Console.debug("Saving GlobalSettings to: " + this.plugin.getGameManager().getSettingsFile().getAbsolutePath());

        sender.sendMessage(this.c("&aNombre del servidor configurado a: &f" + serverName));
        sender.sendMessage(this.c("&7Este nombre se usa para identificar el servidor en el API."));

        return true;
    }
}