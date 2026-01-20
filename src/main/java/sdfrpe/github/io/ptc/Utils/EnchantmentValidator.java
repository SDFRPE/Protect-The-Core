package sdfrpe.github.io.ptc.Utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class EnchantmentValidator {

    public static boolean canEnchant(ItemStack item, Enchantment enchantment) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        String materialName = item.getType().name();

        if (enchantment.equals(Enchantment.DAMAGE_ALL) ||
                enchantment.equals(Enchantment.DAMAGE_UNDEAD) ||
                enchantment.equals(Enchantment.DAMAGE_ARTHROPODS)) {
            return isSword(materialName) || isAxe(materialName);
        }

        if (enchantment.equals(Enchantment.KNOCKBACK) ||
                enchantment.equals(Enchantment.FIRE_ASPECT) ||
                enchantment.equals(Enchantment.LOOT_BONUS_MOBS)) {
            return isSword(materialName);
        }

        if (enchantment.equals(Enchantment.ARROW_DAMAGE) ||
                enchantment.equals(Enchantment.ARROW_KNOCKBACK) ||
                enchantment.equals(Enchantment.ARROW_FIRE) ||
                enchantment.equals(Enchantment.ARROW_INFINITE)) {
            return materialName.equals("BOW");
        }

        if (enchantment.equals(Enchantment.PROTECTION_ENVIRONMENTAL) ||
                enchantment.equals(Enchantment.PROTECTION_FIRE) ||
                enchantment.equals(Enchantment.PROTECTION_EXPLOSIONS) ||
                enchantment.equals(Enchantment.PROTECTION_PROJECTILE)) {
            return isArmor(materialName);
        }

        if (enchantment.equals(Enchantment.THORNS)) {
            return isArmor(materialName);
        }

        if (enchantment.equals(Enchantment.OXYGEN) ||
                enchantment.equals(Enchantment.WATER_WORKER)) {
            return isHelmet(materialName);
        }

        if (enchantment.equals(Enchantment.PROTECTION_FALL) ||
                enchantment.equals(Enchantment.DEPTH_STRIDER)) {
            return isBoots(materialName);
        }

        if (enchantment.equals(Enchantment.DIG_SPEED) ||
                enchantment.equals(Enchantment.SILK_TOUCH) ||
                enchantment.equals(Enchantment.LOOT_BONUS_BLOCKS)) {
            return isTool(materialName);
        }

        if (enchantment.equals(Enchantment.LUCK) ||
                enchantment.equals(Enchantment.LURE)) {
            return materialName.equals("FISHING_ROD");
        }

        if (enchantment.equals(Enchantment.DURABILITY)) {
            return hasDurability(item);
        }

        return enchantment.canEnchantItem(item);
    }

    public static String getRequiredItemType(Enchantment enchantment) {
        if (enchantment.equals(Enchantment.DAMAGE_ALL) ||
                enchantment.equals(Enchantment.DAMAGE_UNDEAD) ||
                enchantment.equals(Enchantment.DAMAGE_ARTHROPODS)) {
            return "espada o hacha";
        }

        if (enchantment.equals(Enchantment.KNOCKBACK) ||
                enchantment.equals(Enchantment.FIRE_ASPECT) ||
                enchantment.equals(Enchantment.LOOT_BONUS_MOBS)) {
            return "espada";
        }

        if (enchantment.equals(Enchantment.ARROW_DAMAGE) ||
                enchantment.equals(Enchantment.ARROW_KNOCKBACK) ||
                enchantment.equals(Enchantment.ARROW_FIRE) ||
                enchantment.equals(Enchantment.ARROW_INFINITE)) {
            return "arco";
        }

        if (enchantment.equals(Enchantment.PROTECTION_ENVIRONMENTAL) ||
                enchantment.equals(Enchantment.PROTECTION_FIRE) ||
                enchantment.equals(Enchantment.PROTECTION_EXPLOSIONS) ||
                enchantment.equals(Enchantment.PROTECTION_PROJECTILE) ||
                enchantment.equals(Enchantment.THORNS)) {
            return "armadura";
        }

        if (enchantment.equals(Enchantment.OXYGEN) ||
                enchantment.equals(Enchantment.WATER_WORKER)) {
            return "casco";
        }

        if (enchantment.equals(Enchantment.PROTECTION_FALL) ||
                enchantment.equals(Enchantment.DEPTH_STRIDER)) {
            return "botas";
        }

        if (enchantment.equals(Enchantment.DIG_SPEED) ||
                enchantment.equals(Enchantment.SILK_TOUCH) ||
                enchantment.equals(Enchantment.LOOT_BONUS_BLOCKS)) {
            return "herramienta (pico, hacha, pala)";
        }

        if (enchantment.equals(Enchantment.LUCK) ||
                enchantment.equals(Enchantment.LURE)) {
            return "caña de pescar";
        }

        if (enchantment.equals(Enchantment.DURABILITY)) {
            return "item con durabilidad";
        }

        return "item compatible";
    }

    private static boolean isSword(String materialName) {
        return materialName.endsWith("_SWORD");
    }

    private static boolean isAxe(String materialName) {
        return materialName.endsWith("_AXE");
    }

    private static boolean isTool(String materialName) {
        return materialName.endsWith("_PICKAXE") ||
                materialName.endsWith("_AXE") ||
                materialName.endsWith("_SPADE") ||
                materialName.endsWith("_HOE") ||
                materialName.equals("SHEARS");
    }

    private static boolean isArmor(String materialName) {
        return isHelmet(materialName) ||
                isChestplate(materialName) ||
                isLeggings(materialName) ||
                isBoots(materialName);
    }

    private static boolean isHelmet(String materialName) {
        return materialName.endsWith("_HELMET");
    }

    private static boolean isChestplate(String materialName) {
        return materialName.endsWith("_CHESTPLATE");
    }

    private static boolean isLeggings(String materialName) {
        return materialName.endsWith("_LEGGINGS");
    }

    private static boolean isBoots(String materialName) {
        return materialName.endsWith("_BOOTS");
    }

    private static boolean hasDurability(ItemStack item) {
        return item.getType().getMaxDurability() > 0;
    }
}