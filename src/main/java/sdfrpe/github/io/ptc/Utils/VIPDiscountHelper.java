package sdfrpe.github.io.ptc.Utils;

import org.bukkit.entity.Player;

public class VIPDiscountHelper {

    public static int getDiscountPercentage(Player player) {
        if (player == null) {
            return 0;
        }
        if (player.hasPermission("ptc.shop.vip+")) {
            return 35;
        }
        if (player.hasPermission("ptc.shop.vip")) {
            return 15;
        }
        return 0;
    }

    public static int calculateDiscountedPrice(int originalPrice, int discountPercentage) {
        if (discountPercentage <= 0 || discountPercentage > 100) {
            return originalPrice;
        }
        double discount = originalPrice * (discountPercentage / 100.0);
        return (int) Math.floor(originalPrice - discount);
    }

    public static String formatDiscountedPrice(int originalPrice, int discountPercentage) {
        if (discountPercentage <= 0) {
            return String.valueOf(originalPrice);
        }
        int discountedPrice = calculateDiscountedPrice(originalPrice, discountPercentage);
        return "§c§m" + originalPrice + "§r §e" + discountedPrice + " §a(-" + discountPercentage + "%)";
    }
}