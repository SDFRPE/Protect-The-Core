package sdfrpe.github.io.ptc.Commands.cmds;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SalirCommand implements CommandExecutor {

    private final PTC plugin;
    private final Map<UUID, Long> pendingConfirmations = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 10000;

    public SalirCommand(PTC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser usado por jugadores."));
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (Statics.gameStatus == GameStatus.LOBBY || Statics.gameStatus == GameStatus.STARTING) {
            sendToLobby(player);
            return true;
        }

        Long lastRequest = pendingConfirmations.get(playerId);
        long currentTime = System.currentTimeMillis();

        if (lastRequest != null && (currentTime - lastRequest) < CONFIRMATION_TIMEOUT) {
            pendingConfirmations.remove(playerId);
            sendToLobby(player);
        } else {
            pendingConfirmations.put(playerId, currentTime);
            player.sendMessage(c("&e¿Realmente deseas regresar al Lobby?"));
            player.sendMessage(c("&7Vuelve a ejecutar &f/salir &7para confirmar"));
            player.sendMessage(c("&7Esta confirmación expira en 10 segundos"));
        }

        cleanupExpiredConfirmations();

        return true;
    }

    private void sendToLobby(Player player) {
        try {
            String lobbyServerName = plugin.getGameManager().getGlobalSettings().getLobbyServerName();

            if (lobbyServerName == null || lobbyServerName.isEmpty()) {
                player.sendMessage(c("&cError: El servidor lobby no está configurado."));
                Console.error("Lobby server name is not configured for /salir command!");
                return;
            }

            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(player);
                player.showPlayer(online);
            }

            LogSystem.debug(LogCategory.PLAYER, "Visibilidad restaurada para /salir:", player.getName());

            player.sendMessage(c("&e¡Regresando al lobby..."));
            player.sendMessage(c("&7Conectando a: &b" + lobbyServerName));

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(lobbyServerName);

            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

            Console.log(String.format("Player %s used /salir - sending to lobby server: %s",
                    player.getName(), lobbyServerName));

        } catch (Exception ex) {
            player.sendMessage(c("&cError al intentar conectar con el lobby."));
            player.sendMessage(c("&7Por favor, contacta con un administrador."));
            Console.error("Failed to send player " + player.getName() + " to lobby via /salir: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void cleanupExpiredConfirmations() {
        long currentTime = System.currentTimeMillis();
        pendingConfirmations.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > CONFIRMATION_TIMEOUT
        );
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}