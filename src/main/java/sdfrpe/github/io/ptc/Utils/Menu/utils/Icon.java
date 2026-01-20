package sdfrpe.github.io.ptc.Utils.Menu.utils;

import com.google.common.collect.Maps;
import com.cryptomorin.xseries.SkullUtils;
import com.cryptomorin.xseries.XMaterial;
import sdfrpe.github.io.ptc.Utils.VIPDiscountHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class Icon {
   private final ItemStack item;
   private final ItemMeta im;
   private String skullOwner;
   private boolean iconView;
   private String permissionView;
   private ItemStack permissionViewItem;
   private Map<Enchantment, Integer> enchantments;
   private static final Pattern COINS_PATTERN = Pattern.compile("&eCoins:\\s*(\\d+)");

   public Icon(XMaterial m, int amount, short dataValue) {
      this.item = new ItemStack((Material)Objects.requireNonNull(m.parseMaterial()), amount != 0 ? amount : 1, dataValue);
      this.im = this.item.getItemMeta();
      this.permissionViewItem = null;
      this.iconView = false;
      this.enchantments = Maps.newHashMap();
   }

   public Icon setName(String name) {
      this.im.setDisplayName(this.c(name));
      this.item.setItemMeta(this.im);
      return this;
   }

   public Icon setLore(List<String> lore) {
      ArrayList<String> fullLore = (ArrayList)lore.stream().map(this::c).collect(Collectors.toCollection(ArrayList::new));
      this.im.setLore(fullLore);
      this.item.setItemMeta(this.im);
      return this;
   }

   public Icon setLore(String... lore) {
      ArrayList<String> fullLore = (ArrayList)Arrays.stream(lore).map(this::c).collect(Collectors.toCollection(ArrayList::new));
      this.im.setLore(fullLore);
      this.item.setItemMeta(this.im);
      return this;
   }

   public Icon setSkull(String skullOwner) {
      this.skullOwner = skullOwner;
      return this;
   }

   public Icon addEnchantment(Map<Enchantment, Integer> enchantments) {
      this.enchantments = enchantments;
      ItemStack var10001 = this.item;
      enchantments.forEach(var10001::addUnsafeEnchantment);
      return this;
   }

   public Icon addPermissionView(boolean iconView, String permissionView, Material material, short type, List<String> lore) {
      ItemStack itemPerms = new ItemStack((Material)Objects.requireNonNull(XMaterial.matchXMaterial(material).parseMaterial()), 1, type);
      ItemMeta meta = itemPerms.getItemMeta();
      meta.setDisplayName(this.im.getDisplayName());
      meta.setLore(lore);
      itemPerms.setItemMeta(meta);
      this.permissionViewItem = itemPerms;
      this.permissionView = permissionView;
      this.iconView = iconView;
      return this;
   }

   public Icon addDamage(short damage) {
      if (damage == 0) {
         return this;
      } else {
         short total = (short)(this.item.getType().getMaxDurability() - damage);
         this.item.setDurability(total);
         return this;
      }
   }

   private void replaceName(Player p) {
      this.im.setDisplayName(this.c(this.im.getDisplayName()).replaceAll("%player%", p.getName()));
      this.item.setItemMeta(this.im);
   }

   private void replaceLore() {
      List<String> list = new ArrayList();
      Iterator var2 = this.im.getLore().iterator();

      while(var2.hasNext()) {
         String s = (String)var2.next();
         list.add(this.c(s));
      }

      this.im.setLore(list);
      this.item.setItemMeta(this.im);
   }

   private void applyVIPDiscount(Player p) {
      if (this.im.getLore() == null) {
         return;
      }
      int discountPercent = VIPDiscountHelper.getDiscountPercentage(p);
      if (discountPercent <= 0) {
         return;
      }
      List<String> newLore = new ArrayList<>();
      for (String line : this.im.getLore()) {
         String strippedLine = ChatColor.stripColor(line);
         if (strippedLine.toLowerCase().startsWith("coins:")) {
            try {
               String priceStr = strippedLine.substring(6).trim();
               int originalPrice = Integer.parseInt(priceStr);
               String discountedDisplay = VIPDiscountHelper.formatDiscountedPrice(originalPrice, discountPercent);
               newLore.add("§eCoins: " + discountedDisplay);
            } catch (NumberFormatException e) {
               newLore.add(line);
            }
         } else {
            newLore.add(line);
         }
      }
      this.im.setLore(newLore);
      this.item.setItemMeta(this.im);
   }

   public ItemStack build(Player p) {
      if (this.im.getDisplayName() != null) {
         this.replaceName(p);
      }

      if (this.iconView && this.permissionViewItem != null && !this.hasPerm(p)) {
         return this.permissionViewItem;
      } else {
         if (this.im.getLore() != null) {
            this.replaceLore();
            this.applyVIPDiscount(p);
         }

         if (this.skullOwner != null && this.im instanceof SkullMeta) {
            this.item.setType(Material.SKULL_ITEM);
            SkullMeta skullMeta = (SkullMeta)this.im;
            if (this.skullOwner.equalsIgnoreCase("%player%")) {
               skullMeta.setOwner(p.getName());
            } else if (this.skullOwner.length() <= 16) {
               skullMeta.setOwner(this.skullOwner);
            } else {
               SkullUtils.applySkin(skullMeta, (String)this.skullOwner);
            }

            this.item.setItemMeta(this.im);
         }

         return this.item;
      }
   }

   public boolean hasPerm(Player player) {
      return this.permissionView == null || this.permissionView.length() == 0 || player.hasPermission(this.permissionView);
   }

   private String c(String c) {
      return ChatColor.translateAlternateColorCodes('&', c);
   }

   public ItemStack getItem() {
      return this.item;
   }

   public ItemMeta getIm() {
      return this.im;
   }

   public String getSkullOwner() {
      return this.skullOwner;
   }

   public boolean isIconView() {
      return this.iconView;
   }

   public String getPermissionView() {
      return this.permissionView;
   }

   public ItemStack getPermissionViewItem() {
      return this.permissionViewItem;
   }

   public Map<Enchantment, Integer> getEnchantments() {
      return this.enchantments;
   }
}