package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Statics;

public class FeedCommand implements CommandExecutor {

    private final PTC plugin;

    public FeedCommand(PTC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser usado por jugadores."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("ptc.feed")) {
            player.sendMessage(c("&cNecesitas el permiso &6ptc.feed &cpara usar este comando."));
            return true;
        }

        if (plugin.getGameManager().getGlobalSettings().isLobbyMode()) {
            player.sendMessage(c("&c✖ Este comando no está disponible en el servidor de lobby."));
            player.sendMessage(c("&7Únete a una partida para usar este comando."));
            return true;
        }

        if (Statics.gameStatus != GameStatus.IN_GAME) {
            player.sendMessage(c("&c✖ Este comando solo está disponible durante una partida."));
            player.sendMessage(c("&7Estado actual: &e" + Statics.gameStatus.name()));
            return true;
        }

        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.sendMessage(c("&a✓ ¡Has sido alimentado!"));

        return true;
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}