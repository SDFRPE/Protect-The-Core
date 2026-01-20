package sdfrpe.github.io.ptc.Listeners.Vanilla;

import sdfrpe.github.io.ptc.Events.Arena.OnMiningBlockPlaceEvent;
import sdfrpe.github.io.ptc.Events.Arena.OnMiningEvent;
import sdfrpe.github.io.ptc.Events.Player.ItemInteractEvent;
import sdfrpe.github.io.ptc.Events.Player.PlayerLoadEvent;
import sdfrpe.github.io.ptc.Events.Player.PlayerUnloadEvent;
import sdfrpe.github.io.ptc.Utils.Enums.Action;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
   @EventHandler(priority = EventPriority.LOWEST)
   public void PlayerPreLogin(AsyncPlayerPreLoginEvent e) {
      PlayerLoadEvent playerLoadEvent = new PlayerLoadEvent(e.getUniqueId());
      Bukkit.getPluginManager().callEvent(playerLoadEvent);
      if (playerLoadEvent.isCancelled()) {
         e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, playerLoadEvent.getMessage());
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void PlayerLeave(PlayerQuitEvent e) {
      Bukkit.getPluginManager().callEvent(new PlayerUnloadEvent(e.getPlayer().getUniqueId()));
   }

   @EventHandler
   public void itemInteract(PlayerInteractEvent e) {
      if (e.getItem() != null) {
         if (e.getClickedBlock() != null && isInteractableBlock(e.getClickedBlock().getType())) {
            return;
         }

         ItemStack itemStack = e.getItem();
         Action action = Action.valueOf(e.getAction().name().toUpperCase());
         ItemInteractEvent ev = new ItemInteractEvent(e.getPlayer(), itemStack, action);
         Bukkit.getPluginManager().callEvent(ev);
         e.setCancelled(ev.isCancelled());
      }
   }

   private boolean isInteractableBlock(Material type) {
      switch (type) {
         case ENDER_PORTAL_FRAME:
         case CHEST:
         case TRAPPED_CHEST:
         case ENDER_CHEST:
         case FURNACE:
         case BURNING_FURNACE:
         case WORKBENCH:
         case ENCHANTMENT_TABLE:
         case ANVIL:
         case BREWING_STAND:
         case HOPPER:
         case DROPPER:
         case DISPENSER:
         case BEACON:
         case WOODEN_DOOR:
         case IRON_DOOR_BLOCK:
         case TRAP_DOOR:
         case FENCE_GATE:
         case SPRUCE_FENCE_GATE:
         case BIRCH_FENCE_GATE:
         case JUNGLE_FENCE_GATE:
         case ACACIA_FENCE_GATE:
         case DARK_OAK_FENCE_GATE:
         case LEVER:
         case STONE_BUTTON:
         case WOOD_BUTTON:
         case BED_BLOCK:
         case NOTE_BLOCK:
         case JUKEBOX:
            return true;
         default:
            return false;
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void blockBreak(BlockBreakEvent e) {
      switch(e.getBlock().getType()) {
         case STONE:
         case COAL_ORE:
         case IRON_ORE:
         case GOLD_ORE:
         case DIAMOND_ORE:
         case REDSTONE_ORE:
         case LAPIS_ORE:
            OnMiningEvent onMiningEvent = new OnMiningEvent(e.getBlock(), e.getPlayer());
            onMiningEvent.setExpToDrop(e.getExpToDrop());
            Bukkit.getPluginManager().callEvent(onMiningEvent);
            e.setCancelled(onMiningEvent.isCancelled());
         default:
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void blockPlace(BlockPlaceEvent e) {
      switch(e.getBlock().getType()) {
         case STONE:
         case COAL_ORE:
         case IRON_ORE:
         case GOLD_ORE:
         case DIAMOND_ORE:
         case REDSTONE_ORE:
         case LAPIS_ORE:
            OnMiningBlockPlaceEvent onMiningEvent = new OnMiningBlockPlaceEvent(e.getBlock(), e.getPlayer());
            Bukkit.getPluginManager().callEvent(onMiningEvent);
            e.setCancelled(onMiningEvent.isCancelled());
         default:
      }
   }
}