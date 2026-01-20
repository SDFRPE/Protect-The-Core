package sdfrpe.github.io.ptc.Utils.Menu.utils;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;

public class listEnchants {
   public static String getBukkitVersion() {
      String packageName = Bukkit.getServer().getClass().getPackage().getName();
      return packageName.substring(packageName.lastIndexOf(46) + 1);
   }

   public static Enchantment getEnchantmentFromString(String input) {
      if (input != null && input.length() != 0) {
         if (isValidInteger(input)) {
            return Enchantment.getByName(String.valueOf(Integer.parseInt(input)));
         } else {
            String formattedInput = input.toLowerCase().replace("_", "").replace(" ", "");
            if (formattedInput.equalsIgnoreCase("protection")) {
               return Enchantment.PROTECTION_ENVIRONMENTAL;
            } else if (formattedInput.equalsIgnoreCase("fireprotection")) {
               return Enchantment.PROTECTION_FIRE;
            } else if (formattedInput.equalsIgnoreCase("featherfalling")) {
               return Enchantment.PROTECTION_FALL;
            } else if (formattedInput.equalsIgnoreCase("blastprotection")) {
               return Enchantment.PROTECTION_EXPLOSIONS;
            } else if (formattedInput.equalsIgnoreCase("projectileprotection")) {
               return Enchantment.PROTECTION_PROJECTILE;
            } else if (formattedInput.equalsIgnoreCase("respiration")) {
               return Enchantment.OXYGEN;
            } else if (formattedInput.equalsIgnoreCase("aquaaffinity")) {
               return Enchantment.WATER_WORKER;
            } else if (formattedInput.equalsIgnoreCase("thorns")) {
               return Enchantment.THORNS;
            } else if (formattedInput.equalsIgnoreCase("sharpness")) {
               return Enchantment.DAMAGE_ALL;
            } else if (formattedInput.equalsIgnoreCase("smite")) {
               return Enchantment.DAMAGE_UNDEAD;
            } else if (formattedInput.equalsIgnoreCase("baneofarthropods")) {
               return Enchantment.DAMAGE_ARTHROPODS;
            } else if (formattedInput.equalsIgnoreCase("knockback")) {
               return Enchantment.KNOCKBACK;
            } else if (formattedInput.equalsIgnoreCase("fireaspect")) {
               return Enchantment.FIRE_ASPECT;
            } else if (formattedInput.equalsIgnoreCase("looting")) {
               return Enchantment.LOOT_BONUS_MOBS;
            } else if (formattedInput.equalsIgnoreCase("efficiency")) {
               return Enchantment.DIG_SPEED;
            } else if (formattedInput.equalsIgnoreCase("silktouch")) {
               return Enchantment.SILK_TOUCH;
            } else if (formattedInput.equalsIgnoreCase("unbreaking")) {
               return Enchantment.DURABILITY;
            } else if (formattedInput.equalsIgnoreCase("fortune")) {
               return Enchantment.LOOT_BONUS_BLOCKS;
            } else if (formattedInput.equalsIgnoreCase("power")) {
               return Enchantment.ARROW_DAMAGE;
            } else if (formattedInput.equalsIgnoreCase("punch")) {
               return Enchantment.ARROW_KNOCKBACK;
            } else if (formattedInput.equalsIgnoreCase("flame")) {
               return Enchantment.ARROW_FIRE;
            } else if (formattedInput.equalsIgnoreCase("infinity")) {
               return Enchantment.ARROW_INFINITE;
            } else if (formattedInput.equalsIgnoreCase("depthstrider")) {
               return Enchantment.DEPTH_STRIDER;
            } else if (formattedInput.equalsIgnoreCase("luck")) {
               return Enchantment.LUCK;
            } else {
               return formattedInput.equalsIgnoreCase("lure") ? Enchantment.LURE : Enchantment.getByName(input.toUpperCase().replace(" ", "_"));
            }
         }
      } else {
         return null;
      }
   }

   public static boolean isValidInteger(String input) {
      try {
         Integer.parseInt(input);
         return true;
      } catch (NumberFormatException var2) {
         return false;
      }
   }
}
