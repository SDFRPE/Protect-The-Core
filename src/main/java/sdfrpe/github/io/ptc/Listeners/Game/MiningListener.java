package sdfrpe.github.io.ptc.Listeners.Game;

import sdfrpe.github.io.ptc.Events.Arena.OnMiningBlockPlaceEvent;
import sdfrpe.github.io.ptc.Events.Arena.OnMiningEvent;
import sdfrpe.github.io.ptc.Utils.Statics;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

public class MiningListener implements Listener {
   @EventHandler
   public void onMining(OnMiningEvent e) {
      Block block = e.getBlock();
      if (Statics.placedMinerals.containsKey(block)) {
         Statics.placedMinerals.remove(e.getBlock());
      } else {
         if (block.getType() != Material.OBSIDIAN) {
            e.getPlayer().getInventory().addItem((ItemStack[])block.getDrops().toArray(new ItemStack[0]));
            Statics.mineralsMined.put(block, block.getType());

            int expToDrop = getExperienceFromOre(block.getType());
            if (expToDrop > 0) {
               ExperienceOrb orb = block.getWorld().spawn(block.getLocation().add(0.5, 0.5, 0.5), ExperienceOrb.class);
               orb.setExperience(expToDrop);
            }

            block.setType(Material.BEDROCK);
            e.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onPlace(OnMiningBlockPlaceEvent e) {
      Block block = e.getBlock();
      Statics.placedMinerals.put(block, block.getType());
   }

   @EventHandler
   public void onItemSpawn(ItemSpawnEvent e) {
      Item item = e.getEntity();
      if (item.getItemStack().getType() == Material.OBSIDIAN) {
         e.setCancelled(true);
         item.remove();
      }
   }

   private int getExperienceFromOre(Material material) {
      switch (material) {
         case COAL_ORE:
            return randomRange(0, 2);
         case DIAMOND_ORE:
            return randomRange(3, 7);
         case EMERALD_ORE:
            return randomRange(3, 7);
         case LAPIS_ORE:
            return randomRange(2, 5);
         case REDSTONE_ORE:
         case GLOWING_REDSTONE_ORE:
            return randomRange(1, 5);
         case QUARTZ_ORE:
            return randomRange(2, 5);
         case IRON_ORE:
         case GOLD_ORE:
         case STONE:
         default:
            return 0;
      }
   }

   private int randomRange(int min, int max) {
      return (int) (Math.random() * (max - min + 1)) + min;
   }
}