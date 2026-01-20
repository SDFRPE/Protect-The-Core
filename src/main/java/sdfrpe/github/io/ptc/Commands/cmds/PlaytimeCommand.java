package sdfrpe.github.io.ptc.Commands.cmds;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Database.Engines.PlaytimeAPI;

import java.util.UUID;

public class PlaytimeCommand implements CommandExecutor {

    private final PTC plugin;
    private final PlaytimeAPI playtimeAPI;

    public PlaytimeCommand(PTC plugin) {
        this.plugin = plugin;
        this.playtimeAPI = new PlaytimeAPI();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID targetUuid = null;
        String targetName = null;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(c("&cUso: /playtime <jugador>"));
                return true;
            }
            Player player = (Player) sender;
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        } else {
            Player onlineTarget = Bukkit.getPlayer(args[0]);
            if (onlineTarget != null) {
                targetUuid = onlineTarget.getUniqueId();
                targetName = onlineTarget.getName();
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);
                if (offlineTarget != null && offlineTarget.hasPlayedBefore()) {
                    targetUuid = offlineTarget.getUniqueId();
                    targetName = offlineTarget.getName() != null ? offlineTarget.getName() : args[0];
                } else {
                    targetName = args[0];
                }
            }
        }

        final UUID uuid = targetUuid;
        final String name = targetName;

        sender.sendMessage(c("&7Consultando tiempo de juego..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            JsonObject response = null;

            if (uuid != null) {
                response = playtimeAPI.getPlaytime(uuid);
            }

            if (response == null || (response.has("error") && response.get("error").getAsBoolean())) {
                if (uuid == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(c("&cJugador '&f" + name + "&c' no encontrado."));
                        sender.sendMessage(c("&7El jugador nunca ha entrado al servidor."));
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(c("&cNo se encontraron datos de tiempo de juego para &f" + name + "&c."));
                    });
                }
                return;
            }

            JsonObject data = response.getAsJsonObject("data");
            if (data == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(c("&cNo hay datos de tiempo de juego para este jugador."));
                });
                return;
            }

            String totalFormatted = data.has("totalPlaytimeFormatted") ?
                    data.get("totalPlaytimeFormatted").getAsString() : "0s";
            String weeklyFormatted = data.has("weeklyPlaytimeFormatted") ?
                    data.get("weeklyPlaytimeFormatted").getAsString() : "0s";
            boolean isOnline = data.has("isOnline") && data.get("isOnline").getAsBoolean();

            String playerName = data.has("playerName") && !data.get("playerName").isJsonNull() ?
                    data.get("playerName").getAsString() : name;

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(c("&6&l━━━ Tiempo de Juego: " + playerName + " ━━━"));
                sender.sendMessage(c("&eTiempo total: &f" + totalFormatted));
                sender.sendMessage(c("&eTiempo semanal: &f" + weeklyFormatted));
                sender.sendMessage(c("&eEstado: " + (isOnline ? "&a● Online" : "&7● Offline")));

                if (isOnline && data.has("currentSession") && !data.get("currentSession").isJsonNull()) {
                    JsonObject session = data.getAsJsonObject("currentSession");
                    if (session.has("currentDurationFormatted")) {
                        String sessionTime = session.get("currentDurationFormatted").getAsString();
                        sender.sendMessage(c("&eSesión actual: &f" + sessionTime));
                    }
                    if (session.has("serverName") && !session.get("serverName").isJsonNull()) {
                        String serverName = session.get("serverName").getAsString();
                        sender.sendMessage(c("&eServidor: &f" + serverName));
                    }
                }

                if (!isOnline && data.has("lastSessionEnd") && !data.get("lastSessionEnd").isJsonNull()) {
                    long lastSession = data.get("lastSessionEnd").getAsLong();
                    if (lastSession > 0) {
                        String lastSeenFormatted = formatTimeAgo(lastSession);
                        sender.sendMessage(c("&eÚltima vez: &f" + lastSeenFormatted));
                    }
                }

                sender.sendMessage(c("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            });
        });

        return true;
    }

    private String formatTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return "hace " + days + " día" + (days != 1 ? "s" : "");
        } else if (hours > 0) {
            return "hace " + hours + " hora" + (hours != 1 ? "s" : "");
        } else if (minutes > 0) {
            return "hace " + minutes + " minuto" + (minutes != 1 ? "s" : "");
        } else {
            return "hace unos segundos";
        }
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}