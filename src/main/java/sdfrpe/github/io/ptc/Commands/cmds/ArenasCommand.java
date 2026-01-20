package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;

public class ArenasCommand implements CommandExecutor {

    private final PTC plugin;

    public ArenasCommand(PTC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("ptc.arenas")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            player.sendMessage(ChatColor.GRAY + "Permiso requerido: " + ChatColor.YELLOW + "ptc.arenas");
            return true;
        }

        if (!this.plugin.getGameManager().getGlobalSettings().isLobbyMode()) {
            player.sendMessage(ChatColor.RED + "Este comando solo está disponible en el modo lobby.");
            return true;
        }

        this.plugin.getGameManager().getArenaSelectorMenu().open(player);
        return true;
    }
}