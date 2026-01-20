package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Statics;

public class TiendaCommand implements CommandExecutor {

    private final PTC plugin;

    public TiendaCommand(PTC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser usado por jugadores."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("ptc.shop")) {
            player.sendMessage(c("&cNecesitas el permiso &6ptc.shop &cpara usar este comando."));
            return true;
        }

        if (plugin.getGameManager().getGlobalSettings().isLobbyMode()) {
            player.sendMessage(c("&c✖ La tienda no está disponible en el servidor de lobby."));
            player.sendMessage(c("&7Únete a una partida para usar la tienda."));
            return true;
        }

        if (Statics.gameStatus != GameStatus.IN_GAME) {
            player.sendMessage(c("&c✖ La tienda solo está disponible durante una partida."));
            player.sendMessage(c("&7Estado actual: &e" + Statics.gameStatus.name()));
            return true;
        }

        if (plugin.getGameManager().getGlobalSettings().isModeCW()) {
            player.sendMessage(c("&c✖ La tienda está deshabilitada en modo Clan War."));
            return true;
        }

        GamePlayer gamePlayer = plugin.getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

        if (gamePlayer == null) {
            player.sendMessage(c("&cError: No se pudo encontrar tu información de jugador."));
            return true;
        }

        plugin.getMenuManager().openInventory("tienda.yml", gamePlayer);

        return true;
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}