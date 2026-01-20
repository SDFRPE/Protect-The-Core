package sdfrpe.github.io.ptc.Utils.Items;

import com.cryptomorin.xseries.XMaterial;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class ItemBuilder {
   public static boolean isTheSame(ItemStack itemStack, ItemStack itemStack2) {
      return itemStack.toString().equals(itemStack2.toString());
   }

   public static ItemStack createItem(XMaterial material, int amount, String name, String... lore) {
      ItemStack itemStack = material.parseItem(true);

      assert itemStack != null;

      itemStack.setAmount(amount);
      ItemMeta itemMeta = itemStack.getItemMeta();

      assert itemMeta != null;

      itemMeta.setDisplayName(c(name));
      itemMeta.setLore(c(lore));
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   public static ItemStack createItem(Material material, int amount, String name, String... lore) {
      ItemStack itemStack = XMaterial.matchXMaterial(material).parseItem(true);

      assert itemStack != null;

      itemStack.setAmount(amount);
      ItemMeta itemMeta = itemStack.getItemMeta();

      assert itemMeta != null;

      itemMeta.setDisplayName(c(name));
      itemMeta.setLore(c(lore));
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   public static ItemStack createItem(Material material, short damage, int amount, String name, String... lore) {
      ItemStack itemStack = XMaterial.matchXMaterial(material).parseItem(true);

      assert itemStack != null;

      itemStack.setAmount(amount);
      ItemMeta itemMeta = itemStack.getItemMeta();

      assert itemMeta != null;

      itemMeta.setDisplayName(c(name));
      itemMeta.setLore(c(lore));
      itemStack.setItemMeta(itemMeta);
      if (damage > 0) {
         itemStack.setDurability(damage);
      }

      return itemStack;
   }

   public static ItemStack createItem(ItemStack itemStack, int amount, String name, String... lore) {
      if (itemStack == null) {
         return null;
      } else {
         itemStack.setAmount(amount);
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta != null) {
            itemMeta.setDisplayName(c(name));
            itemMeta.setLore(c(lore));
            itemStack.setItemMeta(itemMeta);
         }

         return itemStack;
      }
   }

   public static ItemStack createItem(XMaterial material, int amount) {
      ItemStack itemStack = material.parseItem(true);

      assert itemStack != null;

      itemStack.setAmount(amount);
      return itemStack;
   }

   public static ItemStack createItemUnbreakable(XMaterial material, int amount) {
      ItemStack itemStack = material.parseItem(true);

      assert itemStack != null;

      itemStack.setAmount(amount);
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.spigot().setUnbreakable(true);
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   private static String c(String c) {
      return ChatColor.translateAlternateColorCodes('&', c);
   }

   private static List<String> c(String[] colorize) {
      return (List)Arrays.stream(colorize).map(ItemBuilder::c).collect(Collectors.toList());
   }

   public static ItemStack hideAttributes(ItemStack item) {
      if (item == null) {
         return null;
      } else {
         ItemMeta meta = item.getItemMeta();
         if (isNullOrEmpty(meta.getItemFlags())) {
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
         }

         return item;
      }
   }

   private static boolean isNullOrEmpty(Collection<?> coll) {
      return coll == null || coll.isEmpty();
   }

   public static ItemStack createArmor(Material material, Color c) {
      ItemStack itemStack = new ItemStack(material, 1);
      LeatherArmorMeta meta = (LeatherArmorMeta)itemStack.getItemMeta();
      meta.setColor(c);
      meta.spigot().setUnbreakable(true);
      itemStack.setItemMeta(meta);
      return itemStack;
   }

   public static ItemStack createArmor(Material material, Color c, String name) {
      ItemStack itemStack = new ItemStack(material, 1);
      LeatherArmorMeta meta = (LeatherArmorMeta)itemStack.getItemMeta();
      meta.setDisplayName(c(name));
      meta.setColor(c);
      itemStack.setItemMeta(meta);
      return itemStack;
   }
}
