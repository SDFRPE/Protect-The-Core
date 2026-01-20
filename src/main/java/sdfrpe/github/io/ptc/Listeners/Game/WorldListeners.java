package sdfrpe.github.io.ptc.Listeners.Game;

import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Pig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WorldListeners implements Listener {
   @EventHandler
   public void ExplosionBlocks(EntityExplodeEvent e) {
      Set<Block> blocks = Sets.newConcurrentHashSet(e.blockList());
      Iterator var3 = blocks.iterator();

      while(var3.hasNext()) {
         Block b = (Block)var3.next();
         Material type = b.getType();
         switch(type) {
            case DIAMOND_ORE:
            case IRON_ORE:
            case COAL_ORE:
            case REDSTONE_ORE:
            case GLOWING_REDSTONE_ORE:
            case LAPIS_ORE:
            case DIAMOND_BLOCK:
            case GLOWSTONE:
               blocks.remove(b);
         }
      }

      e.blockList().clear();
      e.blockList().addAll(blocks);
   }

   @EventHandler
   public void BreakObsidian(BlockBreakEvent e) {
      if (e.getBlock().getType() == Material.OBSIDIAN) {
         e.getBlock().getDrops().clear();
      } else if (e.getBlock().getType().equals(Material.ENDER_CHEST)) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void CreatureSpawnEvent(CreatureSpawnEvent e) {
      if (!(e.getEntity() instanceof Pig) && !e.getSpawnReason().equals(SpawnReason.CUSTOM) && !e.getSpawnReason().equals(SpawnReason.SPAWNER_EGG)) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void WeatherChange(WeatherChangeEvent e) {
      e.setCancelled(!e.getWorld().hasStorm());
   }
}