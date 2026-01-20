package sdfrpe.github.io.ptc.Utils.Items;

import sdfrpe.github.io.ptc.Player.PlayerUtils;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KitController {
   public static void setTeamKit(TeamColor color, Player p) {
      PlayerUtils.clean(p);

      p.setGameMode(GameMode.SURVIVAL);

      if (color.equals(TeamColor.RED)) {
         p.getInventory().setHelmet(ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.RED));
         p.getInventory().setChestplate(ItemBuilder.createArmor(Material.LEATHER_CHESTPLATE, Color.RED));
         p.getInventory().setLeggings(ItemBuilder.createArmor(Material.LEATHER_LEGGINGS, Color.RED));
         p.getInventory().setBoots(ItemBuilder.createArmor(Material.LEATHER_BOOTS, Color.RED));
      } else if (color.equals(TeamColor.BLUE)) {
         p.getInventory().setHelmet(ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.BLUE));
         p.getInventory().setChestplate(ItemBuilder.createArmor(Material.LEATHER_CHESTPLATE, Color.BLUE));
         p.getInventory().setLeggings(ItemBuilder.createArmor(Material.LEATHER_LEGGINGS, Color.BLUE));
         p.getInventory().setBoots(ItemBuilder.createArmor(Material.LEATHER_BOOTS, Color.BLUE));
      } else if (color.equals(TeamColor.GREEN)) {
         p.getInventory().setHelmet(ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.GREEN));
         p.getInventory().setChestplate(ItemBuilder.createArmor(Material.LEATHER_CHESTPLATE, Color.GREEN));
         p.getInventory().setLeggings(ItemBuilder.createArmor(Material.LEATHER_LEGGINGS, Color.GREEN));
         p.getInventory().setBoots(ItemBuilder.createArmor(Material.LEATHER_BOOTS, Color.GREEN));
      } else if (color.equals(TeamColor.YELLOW)) {
         p.getInventory().setHelmet(ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.YELLOW));
         p.getInventory().setChestplate(ItemBuilder.createArmor(Material.LEATHER_CHESTPLATE, Color.YELLOW));
         p.getInventory().setLeggings(ItemBuilder.createArmor(Material.LEATHER_LEGGINGS, Color.YELLOW));
         p.getInventory().setBoots(ItemBuilder.createArmor(Material.LEATHER_BOOTS, Color.YELLOW));
      }

      p.getInventory().addItem(new ItemStack[]{ItemBuilder.createItemUnbreakable(XMaterial.WOODEN_SWORD, 1)});
      p.getInventory().addItem(new ItemStack[]{ItemBuilder.createItemUnbreakable(XMaterial.WOODEN_PICKAXE, 1)});
      p.getInventory().addItem(new ItemStack[]{ItemBuilder.createItemUnbreakable(XMaterial.WOODEN_AXE, 1)});
      p.getInventory().addItem(new ItemStack[]{ItemBuilder.createItemUnbreakable(XMaterial.WOODEN_SHOVEL, 1)});
   }
}