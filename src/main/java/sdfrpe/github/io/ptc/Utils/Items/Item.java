package sdfrpe.github.io.ptc.Utils.Items;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Item {
   private String name;
   private int amount;
   private List<String> lore;
   private List<Enchant> enchants = Lists.newArrayList();
   private String material;
   private short damage;

   public Item(ItemStack stack) {
      if (stack.hasItemMeta()) {
         if (stack.getItemMeta().hasDisplayName()) {
            this.name = stack.getItemMeta().getDisplayName();
         } else {
            this.name = null;
         }

         if (stack.getItemMeta().hasLore()) {
            this.lore = stack.getItemMeta().getLore();
         } else {
            this.lore = null;
         }

         if (stack.getItemMeta().hasEnchants()) {
            stack.getItemMeta().getEnchants().forEach((enchantment, integer) -> {
               this.enchants.add(new Enchant(enchantment.getName().toUpperCase(), integer));
            });
         }
      } else {
         this.name = null;
         this.lore = null;
      }

      this.amount = stack.getAmount();
      this.material = stack.getType().name().toUpperCase();
      this.damage = stack.getDurability();
   }

   public ItemStack build() {
      ItemStack itemStack = new ItemStack(Material.matchMaterial(this.material), this.amount, this.damage);
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (this.name != null) {
         itemMeta.setDisplayName(this.c(this.name));
      }

      if (this.lore != null && !this.lore.isEmpty()) {
         itemMeta.setLore(this.c(this.lore));
      }

      itemStack.setItemMeta(itemMeta);
      if (this.enchants != null && !this.enchants.isEmpty()) {
         Iterator var3 = this.enchants.iterator();

         while(var3.hasNext()) {
            Enchant enchant = (Enchant)var3.next();
            itemStack.addEnchantment(Enchantment.getByName(enchant.getName()), enchant.getLevel());
         }
      }

      return itemStack;
   }

   private String c(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }

   public List<String> c(List<String> ls) {
      return (List)ls.stream().map(this::c).collect(Collectors.toList());
   }
}
