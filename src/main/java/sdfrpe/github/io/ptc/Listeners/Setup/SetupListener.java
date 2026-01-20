package sdfrpe.github.io.ptc.Listeners.Setup;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Location;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.TimeOfDay;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SetupListener implements Listener {
    @EventHandler
    public void cancelDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);

        e.getPlayer().setGameMode(GameMode.CREATIVE);
        PTC.getInstance().getGameManager().getPlayerManager().addPlayer(e.getPlayer().getUniqueId(), new GamePlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName()));
        PTC.getInstance().getGameManager().getScoreManager().addLobby(e.getPlayer());
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
    }

    @EventHandler
    public void playerFoodLevel(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            player.setFoodLevel(20);
            player.setExhaustion(20.0F);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent e) {
        Block block = e.getBlock();
        ItemStack itemStack = e.getPlayer().getItemInHand();
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
        if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            if (itemStack.getType() == Material.WOOL) {
                String colorName = getColorName(itemStack.getData().getData());
                player.sendMessage(String.format("§aEstableciste el spawn del equipo §f%s", colorName));
                switch (itemStack.getData().getData()) {
                    case 4:
                        gamePlayer.getArenaSettings().getArenaLocations().setYellowSpawn(new Location(block.getLocation()));
                        e.setCancelled(true);
                        break;
                    case 5:
                        gamePlayer.getArenaSettings().getArenaLocations().setGreenSpawn(new Location(block.getLocation()));
                        e.setCancelled(true);
                        break;
                    case 11:
                        gamePlayer.getArenaSettings().getArenaLocations().setBlueSpawn(new Location(block.getLocation()));
                        e.setCancelled(true);
                        break;
                    case 14:
                        gamePlayer.getArenaSettings().getArenaLocations().setRedSpawn(new Location(block.getLocation()));
                        e.setCancelled(true);
                }
            }

            if (itemStack.getType() == Material.STAINED_GLASS) {
                Location loc = new Location(block.getLocation());
                gamePlayer.getArenaSettings().getArenaLocations().getPigsSpawn().add(loc);
                player.sendMessage(String.format("§aAgregaste una localidad de spawneo de cerdos. §7(%s)", loc.toString()));
            }
        }
    }

    @EventHandler
    public void playerInteract(PlayerInteractEvent e) {
        ItemStack itemStack = e.getPlayer().getItemInHand();
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

        if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            String displayName = Statics.strip(itemStack.getItemMeta().getDisplayName());
            String[] parts = displayName.split(":");

            if (parts.length < 2) {
                return;
            }

            String arena = parts[1].trim();

            if (itemStack.getType() == Material.BEACON) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    if (e.getClickedBlock() != null) {
                        Location loc = new Location(e.getClickedBlock().getLocation());
                        gamePlayer.getArenaSettings().getArenaLocations().setCenterSpawn(loc);
                        player.sendMessage(String.format("§aEstableciste el Center Spawn en: §f%s", loc.toString()));
                        e.setCancelled(true);
                    } else {
                        Location loc = new Location(player.getLocation());
                        gamePlayer.getArenaSettings().getArenaLocations().setCenterSpawn(loc);
                        player.sendMessage(String.format("§aEstableciste el Center Spawn en tu ubicación actual: §f%s", loc.toString()));
                        e.setCancelled(true);
                    }
                }
            }

            if (itemStack.getType() == Material.WATCH) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    TimeOfDay currentTime = gamePlayer.getArenaSettings().getTimeOfDay();
                    TimeOfDay nextTime = currentTime.next();
                    gamePlayer.getArenaSettings().setTimeOfDay(nextTime);

                    player.sendMessage(String.format("§aEstableciste el tiempo a: §f%s", nextTime.getDisplayName()));

                    ItemMeta meta = itemStack.getItemMeta();
                    List<String> lore = new ArrayList<>();
                    lore.add(Statics.c("&7Click derecho para cambiar"));
                    lore.add(Statics.c("&7Tiempo actual: &e" + nextTime.getDisplayName()));
                    meta.setLore(lore);
                    itemStack.setItemMeta(meta);

                    e.setCancelled(true);
                }
            }

            if (itemStack.getType() == Material.GOLDEN_APPLE) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    int currentHearts = gamePlayer.getArenaSettings().getExtraHearts();
                    int nextHearts = (currentHearts + 1) % 4;
                    gamePlayer.getArenaSettings().setExtraHearts(nextHearts);

                    player.sendMessage(String.format("§aEstableciste los corazones extra a: §c%d", nextHearts));

                    ItemMeta meta = itemStack.getItemMeta();
                    List<String> lore = new ArrayList<>();
                    lore.add(Statics.c("&7Click derecho para cambiar"));
                    lore.add(Statics.c("&7Corazones extra: &c" + nextHearts));
                    meta.setLore(lore);
                    itemStack.setItemMeta(meta);

                    e.setCancelled(true);
                }
            }

            if (itemStack.getType() == Material.PAPER) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    PTC.getInstance().getGameManager().saveArena(gamePlayer.getArenaSettings());
                    player.sendMessage(String.format("§aGuardaste la arena §f%s", arena));
                    player.sendMessage(String.format("§7Tiempo: §e%s §7| Corazones extra: §c%d",
                            gamePlayer.getArenaSettings().getTimeOfDay().getDisplayName(),
                            gamePlayer.getArenaSettings().getExtraHearts()));
                    PTC.getInstance().getGameManager().getWorldManager().cloneWorlds();

                    gamePlayer.setArenaSettings(null);
                    player.getInventory().clear();
                    player.sendMessage("§e¡Arena guardada! Ahora puedes crear otra arena con /ptc create <nombre>");

                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        ItemStack itemStack = e.getPlayer().getItemInHand();
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
        if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() && itemStack.getType() == Material.WOOL) {
            String colorName = getColorName(itemStack.getData().getData());
            player.sendMessage(String.format("§aEstableciste el core del equipo §f%s", colorName));
            switch (itemStack.getData().getData()) {
                case 4:
                    gamePlayer.getArenaSettings().getArenaLocations().setYellowCore(new Location(block.getLocation()));
                    e.setCancelled(true);
                    break;
                case 5:
                    gamePlayer.getArenaSettings().getArenaLocations().setGreenCore(new Location(block.getLocation()));
                    e.setCancelled(true);
                    break;
                case 11:
                    gamePlayer.getArenaSettings().getArenaLocations().setBlueCore(new Location(block.getLocation()));
                    e.setCancelled(true);
                    break;
                case 14:
                    gamePlayer.getArenaSettings().getArenaLocations().setRedCore(new Location(block.getLocation()));
                    e.setCancelled(true);
            }
        }
    }

    private String getColorName(byte data) {
        switch (data) {
            case 4:
                return "§eAmarillo";
            case 5:
                return "§aVerde";
            case 11:
                return "§9Azul";
            case 14:
                return "§cRojo";
            default:
                return "§fDesconocido";
        }
    }
}