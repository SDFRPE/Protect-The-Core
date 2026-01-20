package sdfrpe.github.io.ptc.Utils.Inventories;

import com.google.common.collect.Lists;
import sdfrpe.github.io.ptc.PTC;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GInventory {
   private String name;
   private List<GItem> items;

   public GInventory(String name) {
      this(name, Lists.newArrayList());
   }

   public GInventory(String name, List<GItem> items) {
      this.name = name;
      this.items = items;
   }

   public void build(Player player) {
      for (GItem gItem : this.items) {
         boolean isModeCW = PTC.getInstance().getGameManager().getGlobalSettings().isModeCW();
         boolean isTeamSelector = gItem.getAction() != null && gItem.getAction().contains("teamsMenu");
         boolean isExtrasMenu = gItem.getAction() != null && gItem.getAction().contains("extrasMenu");

         if (isModeCW && (isTeamSelector || isExtrasMenu)) {
            continue;
         }

         if (gItem.getNeed().equalsIgnoreCase("none") || player.hasPermission(gItem.getNeed())) {
            player.getInventory().setItem(gItem.getSlot(), gItem.getItemStack());
         }
      }
      player.updateInventory();
   }

   public GItem findItem(ItemStack item) {
      if (item == null) {
         return null;
      }

      for (GItem gItem : this.items) {
         if (gItem.getItemStack().equals(item)) {
            return gItem;
         }

         if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                 gItem.getItemStack().hasItemMeta() && gItem.getItemStack().getItemMeta().hasDisplayName()) {

            String itemName = item.getItemMeta().getDisplayName();
            String gItemName = gItem.getItemStack().getItemMeta().getDisplayName();

            if (itemName.equals(gItemName) &&
                    item.getType() == gItem.getItemStack().getType()) {
               return gItem;
            }
         }
      }

      return null;
   }

   public String getName() {
      return this.name;
   }

   public List<GItem> getItems() {
      return this.items;
   }
}