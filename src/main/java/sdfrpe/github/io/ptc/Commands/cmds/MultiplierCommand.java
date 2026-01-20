package sdfrpe.github.io.ptc.Commands.cmds;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Database.Engines.GameAPI;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerStats;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MultiplierCommand implements CommandExecutor {

    private final PTC plugin;
    private final GameAPI gameAPI;
    private final Gson gson;

    private static final long MULTIPLIER_DURATION = TimeUnit.HOURS.toMillis(24);

    public MultiplierCommand(PTC plugin) {
        this.plugin = plugin;
        this.gameAPI = new GameAPI();
        this.gson = new Gson();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                return handleSet(sender, args);
            case "remove":
            case "reset":
                return handleRemove(sender, args);
            case "check":
            case "info":
            case "ver":
                return handleCheck(sender, args);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ptc.multiplier.set")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /multiplier set <jugador> <valor>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "El jugador '" + args[1] + "' nunca ha jugado en el servidor.");
            return true;
        }

        int multiplierValue;
        try {
            multiplierValue = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "'" + args[2] + "' no es un número válido.");
            return true;
        }

        if (multiplierValue < 1 || multiplierValue > 10) {
            sender.sendMessage(ChatColor.RED + "El multiplicador debe estar entre 1 y 10.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        String targetName = target.getName();
        GamePlayer targetGamePlayer = getOrLoadGamePlayer(targetUUID, targetName);

        if (targetGamePlayer == null) {
            sender.sendMessage(ChatColor.RED + "No se pudo cargar la información del jugador.");
            return true;
        }

        boolean wasTemporary = !plugin.getGameManager().getPlayerManager().containsPlayer(targetUUID);

        targetGamePlayer.getPlayerStats().setMultiplierWithExpiration(multiplierValue, MULTIPLIER_DURATION);
        plugin.getGameManager().getDatabase().savePlayer(targetUUID);

        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
        sender.sendMessage(ChatColor.GREEN + "Multiplicador asignado:");
        sender.sendMessage(ChatColor.YELLOW + "  Jugador: " + ChatColor.WHITE + targetName);
        sender.sendMessage(ChatColor.YELLOW + "  Valor: " + ChatColor.GOLD + "x" + multiplierValue);
        sender.sendMessage(ChatColor.YELLOW + "  Duración: " + ChatColor.AQUA + "24 horas");
        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");

        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            onlineTarget.sendMessage(ChatColor.GOLD + "════════════════════════════");
            onlineTarget.sendMessage(ChatColor.GREEN + "¡Has recibido un multiplicador!");
            onlineTarget.sendMessage(ChatColor.YELLOW + "  Valor: " + ChatColor.GOLD + "x" + multiplierValue);
            onlineTarget.sendMessage(ChatColor.YELLOW + "  Duración: " + ChatColor.AQUA + "24 horas");
            onlineTarget.sendMessage(ChatColor.GRAY + "Todas tus ganancias de coins se multiplicarán por " + multiplierValue);
            onlineTarget.sendMessage(ChatColor.GOLD + "════════════════════════════");
        }

        LogSystem.info(LogCategory.DATABASE, sender.getName(), "asignó multiplicador x" + multiplierValue, "a", targetName);

        if (wasTemporary) {
            plugin.getGameManager().getPlayerManager().removePlayer(targetUUID);
            LogSystem.debug(LogCategory.DATABASE, "Removed temporary GamePlayer for", targetName);
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ptc.multiplier.remove")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /multiplier remove <jugador>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "El jugador '" + args[1] + "' nunca ha jugado en el servidor.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        String targetName = target.getName();
        GamePlayer targetGamePlayer = getOrLoadGamePlayer(targetUUID, targetName);

        if (targetGamePlayer == null) {
            sender.sendMessage(ChatColor.RED + "No se pudo cargar la información del jugador.");
            return true;
        }

        boolean wasTemporary = !plugin.getGameManager().getPlayerManager().containsPlayer(targetUUID);

        targetGamePlayer.getPlayerStats().removeMultiplier();
        plugin.getGameManager().getDatabase().savePlayer(targetUUID);

        sender.sendMessage(ChatColor.GREEN + "Multiplicador removido de " + ChatColor.WHITE + targetName);

        if (target.isOnline()) {
            ((Player) target).sendMessage(ChatColor.YELLOW + "Tu multiplicador ha sido removido.");
        }

        LogSystem.info(LogCategory.DATABASE, sender.getName(), "removió multiplicador de", targetName);

        if (wasTemporary) {
            plugin.getGameManager().getPlayerManager().removePlayer(targetUUID);
            LogSystem.debug(LogCategory.DATABASE, "Removed temporary GamePlayer for", targetName);
        }

        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        OfflinePlayer target;

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Debes especificar un jugador desde la consola.");
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "El jugador '" + args[1] + "' nunca ha jugado en el servidor.");
                return true;
            }
        }

        UUID targetUUID = target.getUniqueId();
        String targetName = target.getName();
        GamePlayer targetGamePlayer = getOrLoadGamePlayer(targetUUID, targetName);

        if (targetGamePlayer == null) {
            sender.sendMessage(ChatColor.RED + "No se pudo cargar la información del jugador.");
            return true;
        }

        boolean wasTemporary = !plugin.getGameManager().getPlayerManager().containsPlayer(targetUUID);

        PlayerStats stats = targetGamePlayer.getPlayerStats();
        int multiplier = stats.getMultiplier();
        long timeRemaining = stats.getTimeRemaining();

        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
        sender.sendMessage(ChatColor.YELLOW + "Información de " + ChatColor.WHITE + targetName);
        sender.sendMessage(ChatColor.YELLOW + "  Multiplicador: " + ChatColor.GOLD + "x" + multiplier);

        if (timeRemaining > 0) {
            String timeFormatted = formatTime(timeRemaining);
            sender.sendMessage(ChatColor.YELLOW + "  Tiempo restante: " + ChatColor.AQUA + timeFormatted);
        } else if (multiplier > 1) {
            sender.sendMessage(ChatColor.YELLOW + "  Estado: " + ChatColor.RED + "EXPIRADO");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "  Estado: " + ChatColor.GRAY + "Sin multiplicador activo");
        }

        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");

        if (wasTemporary) {
            plugin.getGameManager().getPlayerManager().removePlayer(targetUUID);
            LogSystem.debug(LogCategory.DATABASE, "Removed temporary GamePlayer for", targetName);
        }

        return true;
    }

    private GamePlayer getOrLoadGamePlayer(UUID uuid, String name) {
        GamePlayer gamePlayer = plugin.getGameManager().getPlayerManager().getPlayer(uuid);

        if (gamePlayer == null) {
            try {
                JsonObject response = gameAPI.GET(uuid);
                if (response != null && !response.get("error").getAsBoolean()) {
                    JsonObject data = response.getAsJsonObject("data");
                    PlayerStats stats = gson.fromJson(data.toString(), PlayerStats.class);

                    gamePlayer = new GamePlayer(uuid, name);
                    gamePlayer.setPlayerStats(stats);
                    plugin.getGameManager().getPlayerManager().addPlayer(uuid, gamePlayer);

                    LogSystem.debug(LogCategory.DATABASE, "Cargado GamePlayer temporal para", name);
                }
            } catch (Exception e) {
                LogSystem.error(LogCategory.DATABASE, "Error cargando jugador desde API:", e.getMessage());
            }
        }

        return gamePlayer;
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
        sender.sendMessage(ChatColor.YELLOW + "Comandos de Multiplicador:");

        if (sender.hasPermission("ptc.multiplier.set")) {
            sender.sendMessage(ChatColor.WHITE + "/multiplier set <jugador> <valor>" + ChatColor.GRAY + " - Asignar multiplicador (24h)");
        }
        if (sender.hasPermission("ptc.multiplier.remove")) {
            sender.sendMessage(ChatColor.WHITE + "/multiplier remove <jugador>" + ChatColor.GRAY + " - Remover multiplicador");
        }

        sender.sendMessage(ChatColor.WHITE + "/multiplier check [jugador]" + ChatColor.GRAY + " - Ver multiplicador y tiempo");
        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
    }
}