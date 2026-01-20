package sdfrpe.github.io.ptc.Utils;

import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.util.UUID;

public class ClanChatUtils {

    private static final String PTC_CLANS_INTEGRATION_CLASS = "sdfrpe.github.io.ptcclans.Integration.PTCIntegration";
    private static boolean ptcClansAvailable = false;
    private static Class<?> ptcIntegrationClass = null;
    private static java.lang.reflect.Method getClanDisplayNameMethod = null;
    private static java.lang.reflect.Method getClanTagMethod = null;
    private static java.lang.reflect.Method hasClanMethod = null;

    static {
        initializePTCClansIntegration();
    }

    private static void initializePTCClansIntegration() {
        try {
            ptcIntegrationClass = Class.forName(PTC_CLANS_INTEGRATION_CLASS);

            getClanDisplayNameMethod = ptcIntegrationClass.getMethod("getClanDisplayName", UUID.class);
            getClanTagMethod = ptcIntegrationClass.getMethod("getClanTag", UUID.class);
            hasClanMethod = ptcIntegrationClass.getMethod("hasClan", UUID.class);

            ptcClansAvailable = true;
            LogSystem.info(LogCategory.PLAYER, "ClanChatUtils: Integración con PTCClans activada");
        } catch (ClassNotFoundException e) {
            ptcClansAvailable = false;
            LogSystem.info(LogCategory.PLAYER, "ClanChatUtils: PTCClans no detectado - funcionalidad de clanes deshabilitada");
        } catch (Exception e) {
            ptcClansAvailable = false;
            LogSystem.warn(LogCategory.PLAYER, "ClanChatUtils: Error al inicializar integración con PTCClans:", e.getMessage());
        }
    }

    public static boolean isPTCClansAvailable() {
        return ptcClansAvailable;
    }

    public static String getClanDisplayForChat(Player player) {
        if (player == null) {
            return "";
        }
        return getClanDisplayForChat(player.getUniqueId());
    }

    public static String getClanDisplayForChat(UUID playerUuid) {
        String displayName = getClanDisplayName(playerUuid);

        if (displayName != null && !displayName.isEmpty()) {
            int clanLevel = getClanLevel(playerUuid);

            if (clanLevel > 0) {
                return "§7[" + displayName + " §7<§e" + clanLevel + "§7>§7] ";
            } else {
                return "§7[" + displayName + "§7] ";
            }
        }

        return "";
    }

    public static int getClanLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return getClanLevel(player.getUniqueId());
    }

    public static int getClanLevel(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }

        try {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(playerUuid);

            if (gamePlayer != null && gamePlayer.getPlayerStats() != null) {
                return gamePlayer.getPlayerStats().getClanLevel();
            }
        } catch (Exception e) {
            LogSystem.debug(LogCategory.PLAYER, "Error obteniendo clan level:", e.getMessage());
        }

        return 0;
    }

    public static String getClanDisplayName(Player player) {
        if (player == null) {
            return null;
        }
        return getClanDisplayName(player.getUniqueId());
    }

    public static String getClanDisplayName(UUID playerUuid) {
        if (!ptcClansAvailable || playerUuid == null) {
            return null;
        }

        try {
            Object result = getClanDisplayNameMethod.invoke(null, playerUuid);

            if (result instanceof String) {
                return (String) result;
            }
        } catch (Exception e) {
            LogSystem.debug(LogCategory.PLAYER, "Error obteniendo clan display name:", e.getMessage());
        }

        return null;
    }

    public static String getClanTag(Player player) {
        if (player == null) {
            return null;
        }
        return getClanTag(player.getUniqueId());
    }

    public static String getClanTag(UUID playerUuid) {
        if (!ptcClansAvailable || playerUuid == null) {
            return null;
        }

        try {
            Object result = getClanTagMethod.invoke(null, playerUuid);

            if (result instanceof String) {
                return (String) result;
            }
        } catch (Exception e) {
            LogSystem.debug(LogCategory.PLAYER, "Error obteniendo clan tag:", e.getMessage());
        }

        return null;
    }

    public static boolean hasClan(Player player) {
        if (player == null) {
            return false;
        }
        return hasClan(player.getUniqueId());
    }

    public static boolean hasClan(UUID playerUuid) {
        if (!ptcClansAvailable || playerUuid == null) {
            return false;
        }

        try {
            Object result = hasClanMethod.invoke(null, playerUuid);

            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (Exception e) {
            LogSystem.debug(LogCategory.PLAYER, "Error verificando clan membership:", e.getMessage());
        }

        return false;
    }

    public static String getClanTagForChat(Player player) {
        String tag = getClanTag(player);

        if (tag != null && !tag.isEmpty()) {
            return tag + " ";
        }

        return "";
    }

    public static String getClanTagForChat(UUID playerUuid) {
        String tag = getClanTag(playerUuid);

        if (tag != null && !tag.isEmpty()) {
            return tag + " ";
        }

        return "";
    }

    public static void reinitialize() {
        LogSystem.info(LogCategory.PLAYER, "ClanChatUtils: Reinicializando integración con PTCClans...");
        initializePTCClansIntegration();
    }
}