package sdfrpe.github.io.ptc.Listeners.Game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CoreProtectionListener implements Listener {

    private static final Set<Material> REDSTONE_MATERIALS = new HashSet<>(Arrays.asList(
            Material.REDSTONE,
            Material.REDSTONE_WIRE,
            Material.REDSTONE_BLOCK,
            Material.REDSTONE_TORCH_OFF,
            Material.REDSTONE_TORCH_ON,
            Material.DIODE_BLOCK_OFF,
            Material.DIODE_BLOCK_ON,
            Material.REDSTONE_COMPARATOR_OFF,
            Material.REDSTONE_COMPARATOR_ON,
            Material.PISTON_BASE,
            Material.PISTON_STICKY_BASE,
            Material.PISTON_EXTENSION,
            Material.PISTON_MOVING_PIECE,
            Material.DISPENSER,
            Material.DROPPER,
            Material.HOPPER,
            Material.REDSTONE_LAMP_OFF,
            Material.REDSTONE_LAMP_ON,
            Material.LEVER,
            Material.STONE_BUTTON,
            Material.WOOD_BUTTON,
            Material.TRIPWIRE,
            Material.TRIPWIRE_HOOK,
            Material.TRAPPED_CHEST,
            Material.DAYLIGHT_DETECTOR,
            Material.DAYLIGHT_DETECTOR_INVERTED,
            Material.TNT,
            Material.RAILS,
            Material.POWERED_RAIL,
            Material.DETECTOR_RAIL,
            Material.ACTIVATOR_RAIL
    ));

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location blockLocation = event.getBlockPlaced().getLocation();
        Material material = event.getBlockPlaced().getType();

        if (!REDSTONE_MATERIALS.contains(material)) {
            return;
        }

        double protectionRadius = PTC.getInstance().getGameManager()
                .getGlobalSettings().getCoreProtectionRadius();
        double protectionRadiusSquared = protectionRadius * protectionRadius;

        for (ArenaTeam team : PTC.getInstance().getGameManager().getGameSettings().getTeamList().values()) {
            if (team.getCoreLocation() == null) {
                continue;
            }

            sdfrpe.github.io.ptc.Utils.Location coreLocation = team.getCoreLocation();
            double distanceSquared = calculateDistanceSquared(blockLocation, coreLocation);

            if (distanceSquared <= protectionRadiusSquared) {
                event.setCancelled(true);

                player.sendMessage(String.format(
                        "§c¡No puedes colocar redstone cerca de los cores! (Radio: %.1f bloques)",
                        protectionRadius
                ));

                LogSystem.debug(LogCategory.GAME, "Bloqueado", material.name(), "cerca de core", team.getColor().getName(),
                        "por", player.getName());

                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        boolean isBoat = item.getType() == Material.BOAT;

        if (!isBoat) {
            return;
        }

        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }

        Location playerLocation = player.getLocation();
        double protectionRadius = PTC.getInstance().getGameManager()
                .getGlobalSettings().getCoreProtectionRadius();
        double protectionRadiusSquared = protectionRadius * protectionRadius;

        for (ArenaTeam team : PTC.getInstance().getGameManager().getGameSettings().getTeamList().values()) {
            if (team.getCoreLocation() == null) {
                continue;
            }

            sdfrpe.github.io.ptc.Utils.Location coreLocation = team.getCoreLocation();
            double distanceSquared = calculateDistanceSquared(playerLocation, coreLocation);

            if (distanceSquared <= protectionRadiusSquared) {
                event.setCancelled(true);

                player.sendMessage(String.format(
                        "§c¡No puedes usar botes cerca de los cores! (Radio: %.1f bloques)",
                        protectionRadius
                ));

                LogSystem.debug(LogCategory.GAME, "Bloqueado bote cerca de core", team.getColor().getName(),
                        "por", player.getName());

                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        Player player = event.getPlayer();
        Location destination = event.getTo();

        double protectionRadius = PTC.getInstance().getGameManager()
                .getGlobalSettings().getCoreProtectionRadius();
        double protectionRadiusSquared = protectionRadius * protectionRadius;

        for (ArenaTeam team : PTC.getInstance().getGameManager().getGameSettings().getTeamList().values()) {
            if (team.getCoreLocation() == null) {
                continue;
            }

            sdfrpe.github.io.ptc.Utils.Location coreLocation = team.getCoreLocation();
            double distanceSquared = calculateDistanceSquared(destination, coreLocation);

            if (distanceSquared <= protectionRadiusSquared) {
                event.setCancelled(true);

                player.sendMessage(String.format(
                        "§c¡No puedes teletransportarte cerca de los cores! (Radio: %.1f bloques)",
                        protectionRadius
                ));

                LogSystem.debug(LogCategory.GAME, "Bloqueada enderpearl cerca de core", team.getColor().getName(),
                        "por", player.getName());

                return;
            }
        }
    }

    private double calculateDistanceSquared(Location loc1, sdfrpe.github.io.ptc.Utils.Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dy = loc1.getY() - loc2.getY();
        double dz = loc1.getZ() - loc2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}