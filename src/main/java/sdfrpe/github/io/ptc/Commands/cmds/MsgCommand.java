package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Listeners.Game.ChatListener;
import sdfrpe.github.io.ptc.Player.GamePlayer;

public class MsgCommand implements CommandExecutor {

    private final PTC plugin;

    public MsgCommand(PTC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser usado por jugadores."));
            return false;
        }

        Player player = (Player) sender;

        GamePlayer gamePlayer = this.plugin.getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
        if (gamePlayer == null) {
            player.sendMessage(c("&cError: No se pudo encontrar tu información de jugador."));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(c("&cUso correcto: &f/msg <jugador> <mensaje>"));
            player.sendMessage(c("&7Ejemplo: &f/msg Shande Hola, ¿cómo estás?"));
            return false;
        }

        String recipientName = args[0];

        Player recipient = Bukkit.getPlayer(recipientName);

        if (recipient == null || !recipient.isOnline()) {
            player.sendMessage(c("&cEl jugador &f" + recipientName + " &cno está en línea."));
            return false;
        }

        if (recipient.equals(player)) {
            player.sendMessage(c("&cNo puedes enviarte mensajes a ti mismo."));
            return false;
        }

        GamePlayer recipientGamePlayer = this.plugin.getGameManager().getPlayerManager().getPlayer(recipient.getUniqueId());
        if (recipientGamePlayer == null) {
            player.sendMessage(c("&cEl jugador " + recipientName + " no está disponible en este momento."));
            return false;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }
        String message = messageBuilder.toString();

        if (message.trim().isEmpty()) {
            player.sendMessage(c("&cEscribe un mensaje."));
            return false;
        }

        ChatListener.sendPrivateMessage(player, recipient, message);

        return true;
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}