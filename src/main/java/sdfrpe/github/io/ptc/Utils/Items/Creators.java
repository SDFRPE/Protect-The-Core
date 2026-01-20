package sdfrpe.github.io.ptc.Utils.Items;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.inventory.ItemStack;

public class Creators {
   public static ItemStack spawnItem;

   static {
      spawnItem = ItemBuilder.createItem((XMaterial)XMaterial.BLUE_WOOL, 1, "Spawn Item: ");
   }
}
