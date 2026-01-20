package sdfrpe.github.io.ptc.Listeners.Lobby;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.PlayerUtils;

public class LobbyModeListener implements Listener {

    @EventHandler
    public void cancelDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void cancelBlockBreak(BlockBreakEvent e) {
        if (!e.getPlayer().isOp()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void cancelBlockPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().isOp()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void cancelItemDrop(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void cancelHunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            player.setFoodLevel(20);
            player.setExhaustion(20.0F);
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        e.setJoinMessage(null);

        PlayerUtils.clean(player);

        player.teleport(PTC.getInstance().getGameManager().getGlobalSettings().getLobbyLocation().getLocation());
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
    }
}