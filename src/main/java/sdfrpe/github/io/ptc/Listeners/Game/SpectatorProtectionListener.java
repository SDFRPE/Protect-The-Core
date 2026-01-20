package sdfrpe.github.io.ptc.Listeners.Game;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

public class SpectatorProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();

        if (isSpectator(player)) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Los espectadores no pueden romper bloques.");
            LogSystem.debug(LogCategory.GAME, "Espectador bloqueado (romper):", player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();

        if (isSpectator(player)) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Los espectadores no pueden colocar bloques.");
            LogSystem.debug(LogCategory.GAME, "Espectador bloqueado (colocar):", player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if (isSpectator(player)) {
            e.setCancelled(true);
            LogSystem.debug(LogCategory.GAME, "Espectador bloqueado (interacción):", player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();

        if (isSpectator(player)) {
            e.setCancelled(true);
            LogSystem.debug(LogCategory.GAME, "Espectador bloqueado (drop):", player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(PlayerPickupItemEvent e) {
        Player player = e.getPlayer();

        if (isSpectator(player)) {
            e.setCancelled(true);
            LogSystem.debug(LogCategory.GAME, "Espectador bloqueado (pickup):", player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            Player player = (Player) e.getDamager();

            if (isSpectator(player)) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Los espectadores no pueden atacar.");
                LogSystem.debug(LogCategory.GAME, "Espectador bloqueado (ataque):", player.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();

            if (isSpectator(player)) {
                e.setCancelled(true);
                LogSystem.debug(LogCategory.GAME, "Espectador protegido (daño):", player.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player player = (Player) e.getWhoClicked();

            if (isSpectator(player)) {
                e.setCancelled(true);
                LogSystem.debug(LogCategory.GAME, "Espectador bloqueado (inventario):", player.getName());
            }
        }
    }

    private boolean isSpectator(Player player) {
        if (player == null) {
            return false;
        }

        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

        if (gamePlayer == null) {
            return false;
        }

        if (gamePlayer.getArenaTeam() == null) {
            return false;
        }

        return gamePlayer.getArenaTeam().getColor() == TeamColor.SPECTATOR;
    }
}