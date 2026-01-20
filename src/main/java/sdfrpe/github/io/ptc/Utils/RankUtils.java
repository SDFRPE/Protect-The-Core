package sdfrpe.github.io.ptc.Utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class RankUtils {

    public static String getPrefix(Player player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());

            if (user == null) {
                return "";
            }

            CachedMetaData metaData = user.getCachedData().getMetaData();
            String prefix = metaData.getPrefix();

            if (prefix == null || prefix.isEmpty()) {
                return "";
            }

            return ChatColor.translateAlternateColorCodes('&', prefix);
        } catch (Exception e) {
            Console.debug("Error getting prefix for " + player.getName() + ": " + e.getMessage());
            return "";
        }
    }

    public static String truncatePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }

        String withoutColors = ChatColor.stripColor(prefix);

        if (prefix.length() <= 16) {
            return prefix;
        }

        if (withoutColors.length() <= 14) {
            return prefix;
        }

        if (withoutColors.startsWith("[") && withoutColors.contains("]")) {
            String rankName = withoutColors.substring(1, withoutColors.indexOf("]"));

            if (rankName.length() > 8) {
                rankName = rankName.substring(0, 6);
            }

            String color = "";
            for (int i = 0; i < prefix.length() - 1; i++) {
                if (prefix.charAt(i) == '§' || prefix.charAt(i) == '&') {
                    color = prefix.substring(i, i + 2);
                    break;
                }
            }

            return ChatColor.translateAlternateColorCodes('&', color + "[" + rankName + "]");
        }

        String truncated = withoutColors.substring(0, Math.min(12, withoutColors.length()));
        return ChatColor.translateAlternateColorCodes('&', "&7[" + truncated + "]");
    }

    public static String getChatPrefix(Player player) {
        String prefix = getPrefix(player);
        if (prefix.isEmpty()) {
            return "";
        }
        return prefix + " ";
    }

    public static String getNametagPrefix(Player player) {
        String prefix = getPrefix(player);
        if (prefix.isEmpty()) {
            return "";
        }
        return truncatePrefix(prefix) + " ";
    }

    public static String cleanPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        return prefix.trim();
    }
}