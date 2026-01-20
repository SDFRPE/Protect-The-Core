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
import sdfrpe.github.io.ptc.Utils.Console;

import java.util.UUID;

public class CoinsCommand implements CommandExecutor {

    private final PTC plugin;
    private final GameAPI gameAPI;
    private final Gson gson;

    public CoinsCommand(PTC plugin) {
        this.plugin = plugin;
        this.gameAPI = new GameAPI();
        this.gson = new Gson();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
                return true;
            }

            Player player = (Player) sender;
            GamePlayer gamePlayer = plugin.getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

            int coins = 0;
            if (gamePlayer != null) {
                coins = gamePlayer.getCoins();
            } else {
                try {
                    JsonObject response = gameAPI.GET(player.getUniqueId());
                    if (response != null && !response.get("error").getAsBoolean()) {
                        JsonObject data = response.getAsJsonObject("data");
                        coins = data.has("coins") ? data.get("coins").getAsInt() : 0;
                        Console.debug("Fetched coins from API for " + player.getName() + ": " + coins);
                    } else {
                        sender.sendMessage(ChatColor.RED + "No se pudo cargar tu información desde la base de datos.");
                        return true;
                    }
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Error al cargar tus coins. Intenta de nuevo.");
                    Console.error("Error fetching coins from API: " + e.getMessage());
                    return true;
                }
            }

            sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
            sender.sendMessage(ChatColor.YELLOW + "Tus Coins: " + ChatColor.GREEN + coins);
            sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
            return true;
        }

        if (args.length == 1) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "El jugador '" + args[0] + "' nunca ha jugado en el servidor.");
                return true;
            }

            GamePlayer targetGamePlayer = plugin.getGameManager().getPlayerManager().getPlayer(target.getUniqueId());

            int coins = 0;
            if (targetGamePlayer != null) {
                coins = targetGamePlayer.getCoins();
            } else {
                try {
                    JsonObject response = gameAPI.GET(target.getUniqueId());
                    if (response != null && !response.get("error").getAsBoolean()) {
                        JsonObject data = response.getAsJsonObject("data");
                        coins = data.has("coins") ? data.get("coins").getAsInt() : 0;
                        Console.debug("Fetched coins from API for " + target.getName() + ": " + coins);
                    } else {
                        sender.sendMessage(ChatColor.RED + "No se pudo cargar la información de " + target.getName() + " desde la base de datos.");
                        return true;
                    }
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Error al cargar las coins de " + target.getName() + ".");
                    Console.error("Error fetching coins from API: " + e.getMessage());
                    return true;
                }
            }

            sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
            sender.sendMessage(ChatColor.YELLOW + "Coins de " + ChatColor.WHITE + target.getName() + ChatColor.YELLOW + ": " + ChatColor.GREEN + coins);
            sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
            return true;
        }

        if (args.length < 3) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (!hasPermissionForSubCommand(sender, subCommand)) {
            sender.sendMessage(ChatColor.RED + "No tienes el permiso &6ptc.coins." + subCommand + " &cpara usar este comando.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "El jugador '" + args[1] + "' nunca ha jugado en el servidor.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        String targetName = target.getName();
        GamePlayer targetGamePlayer = plugin.getGameManager().getPlayerManager().getPlayer(targetUUID);

        boolean wasTemporary = false;
        if (targetGamePlayer == null) {
            try {
                JsonObject response = gameAPI.GET(targetUUID);
                if (response != null && !response.get("error").getAsBoolean()) {
                    JsonObject data = response.getAsJsonObject("data");
                    PlayerStats stats = gson.fromJson(data.toString(), PlayerStats.class);

                    targetGamePlayer = new GamePlayer(targetUUID, targetName);
                    targetGamePlayer.setPlayerStats(stats);
                    plugin.getGameManager().getPlayerManager().addPlayer(targetUUID, targetGamePlayer);
                    wasTemporary = true;
                    Console.debug("Created temporary GamePlayer for " + targetName + " with API data");
                } else {
                    sender.sendMessage(ChatColor.RED + "No se pudo cargar la información de " + targetName + " desde la base de datos.");
                    return true;
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Error al cargar la información de " + targetName + ".");
                Console.error("Error loading player from API: " + e.getMessage());
                return true;
            }
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "'" + args[2] + "' no es un número válido.");
            return true;
        }

        switch (subCommand) {
            case "set":
                if (amount < 0) {
                    sender.sendMessage(ChatColor.RED + "La cantidad no puede ser negativa.");
                    return true;
                }
                targetGamePlayer.setCoins(amount);

                plugin.getGameManager().getDatabase().savePlayer(targetUUID);

                sender.sendMessage(ChatColor.GREEN + "Has establecido las coins de " + ChatColor.WHITE + targetName + ChatColor.GREEN + " a " + ChatColor.YELLOW + amount);

                if (target.isOnline()) {
                    ((Player) target).sendMessage(ChatColor.GREEN + "Tus coins han sido establecidas a " + ChatColor.YELLOW + amount);
                }

                Console.log(sender.getName() + " set coins of " + targetName + " to " + amount);
                break;

            case "add":
                if (amount <= 0) {
                    sender.sendMessage(ChatColor.RED + "La cantidad debe ser mayor a 0.");
                    return true;
                }
                int oldCoinsAdd = targetGamePlayer.getCoins();
                targetGamePlayer.addCoinsRaw(amount);

                plugin.getGameManager().getDatabase().savePlayer(targetUUID);

                sender.sendMessage(ChatColor.GREEN + "Has añadido " + ChatColor.YELLOW + amount + ChatColor.GREEN + " coins a " + ChatColor.WHITE + targetName);
                sender.sendMessage(ChatColor.GRAY + "Antes: " + oldCoinsAdd + " → Después: " + targetGamePlayer.getCoins());

                if (target.isOnline()) {
                    ((Player) target).sendMessage(ChatColor.GREEN + "Has recibido " + ChatColor.YELLOW + "+" + amount + ChatColor.GREEN + " coins!");
                }

                Console.log(sender.getName() + " added " + amount + " coins to " + targetName);
                break;

            case "remove":
            case "take":
                if (amount <= 0) {
                    sender.sendMessage(ChatColor.RED + "La cantidad debe ser mayor a 0.");
                    return true;
                }
                int oldCoinsRemove = targetGamePlayer.getCoins();
                targetGamePlayer.removeCoins(amount);

                plugin.getGameManager().getDatabase().savePlayer(targetUUID);

                sender.sendMessage(ChatColor.GREEN + "Has quitado " + ChatColor.YELLOW + amount + ChatColor.GREEN + " coins de " + ChatColor.WHITE + targetName);
                sender.sendMessage(ChatColor.GRAY + "Antes: " + oldCoinsRemove + " → Después: " + targetGamePlayer.getCoins());

                if (target.isOnline()) {
                    ((Player) target).sendMessage(ChatColor.RED + "Se te han quitado " + ChatColor.YELLOW + amount + ChatColor.RED + " coins.");
                }

                Console.log(sender.getName() + " removed " + amount + " coins from " + targetName);
                break;

            case "reset":
                int oldCoinsReset = targetGamePlayer.getCoins();
                targetGamePlayer.setCoins(0);

                plugin.getGameManager().getDatabase().savePlayer(targetUUID);

                sender.sendMessage(ChatColor.GREEN + "Has reseteado las coins de " + ChatColor.WHITE + targetName);
                sender.sendMessage(ChatColor.GRAY + "Antes: " + oldCoinsReset + " → Después: 0");

                if (target.isOnline()) {
                    ((Player) target).sendMessage(ChatColor.RED + "Tus coins han sido reseteadas a 0.");
                }

                Console.log(sender.getName() + " reset coins of " + targetName + " (was: " + oldCoinsReset + ")");
                break;

            default:
                sendHelpMessage(sender);
                return true;
        }

        if (wasTemporary) {
            plugin.getGameManager().getPlayerManager().removePlayer(targetUUID);
            Console.debug("Removed temporary GamePlayer for " + targetName);
        }

        return true;
    }

    private boolean hasPermissionForSubCommand(CommandSender sender, String subCommand) {
        switch (subCommand) {
            case "set":
                return sender.hasPermission("ptc.coins.set");
            case "add":
                return sender.hasPermission("ptc.coins.add");
            case "remove":
            case "take":
                return sender.hasPermission("ptc.coins.remove");
            case "reset":
                return sender.hasPermission("ptc.coins.reset");
            default:
                return false;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
        sender.sendMessage(ChatColor.YELLOW + "Comandos de Coins:");
        sender.sendMessage(ChatColor.WHITE + "/coins" + ChatColor.GRAY + " - Ver tus coins");
        sender.sendMessage(ChatColor.WHITE + "/coins <jugador>" + ChatColor.GRAY + " - Ver coins de otro jugador");

        sender.sendMessage(ChatColor.GOLD + "Admin:");
        if (sender.hasPermission("ptc.coins.set")) {
            sender.sendMessage(ChatColor.WHITE + "/coins set <jugador> <cantidad>" + ChatColor.GRAY + " - Establecer coins");
        }
        if (sender.hasPermission("ptc.coins.add")) {
            sender.sendMessage(ChatColor.WHITE + "/coins add <jugador> <cantidad>" + ChatColor.GRAY + " - Añadir coins");
        }
        if (sender.hasPermission("ptc.coins.remove")) {
            sender.sendMessage(ChatColor.WHITE + "/coins remove <jugador> <cantidad>" + ChatColor.GRAY + " - Quitar coins");
        }
        if (sender.hasPermission("ptc.coins.reset")) {
            sender.sendMessage(ChatColor.WHITE + "/coins reset <jugador>" + ChatColor.GRAY + " - Resetear a 0");
        }
        sender.sendMessage(ChatColor.GOLD + "════════════════════════════");
    }
}