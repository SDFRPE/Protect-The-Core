package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;

public class OpenArenaSelector extends SubCommand {
    public OpenArenaSelector(PTC plugin) {
        super(plugin, "Open Arena Selector", "arenas", "Abre el selector de arenas multi-servidor.", "/ptc arenas");
    }

    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            this.plugin.getGameManager().getArenaSelectorMenu().open(player);

            player.sendMessage(this.c("&aAbriendo selector de arenas..."));
        } else {
            sender.sendMessage("Necesitas ser un jugador para usar este comando.");
        }

        return false;
    }
}