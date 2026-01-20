package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;

public class OpenCWArenaSelector extends SubCommand {
    public OpenCWArenaSelector(PTC plugin) {
        super(plugin, "Open CW Arena Selector", "cwarenas", "Abre el selector de arenas de Clan War.", "/ptc cwarenas");
    }

    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Necesitas ser un jugador para usar este comando.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("ptc.cwarenas")) {
            player.sendMessage(this.c("&cNo tienes permiso para usar este comando."));
            player.sendMessage(this.c("&7Permiso requerido: &eptc.cwarenas"));
            return false;
        }

        this.plugin.getGameManager().getCWArenaSelectorMenu().open(player);
        player.sendMessage(this.c("&6Abriendo selector de guerras de clanes..."));

        return false;
    }
}