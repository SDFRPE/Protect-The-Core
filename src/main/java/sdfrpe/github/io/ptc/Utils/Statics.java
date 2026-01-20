package sdfrpe.github.io.ptc.Utils;

import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;

import java.util.LinkedHashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;

public class Statics {
    public static boolean DEBUG = true;
    public static LinkedHashMap<Block, Material> mineralsMined = Maps.newLinkedHashMap();
    public static LinkedHashMap<Block, Material> placedMinerals = Maps.newLinkedHashMap();
    public static LinkedHashMap<Furnace, UUID> placedFurnaces = Maps.newLinkedHashMap();
    public static LinkedHashMap<Location, UUID> protectedChests = Maps.newLinkedHashMap();
    public static GameStatus gameStatus;
    public static int MAX_FURNACES = 5;
    public static int MAX_CHESTS = 3;
    public static int KILL_COINS = 1;
    public static int KILL_XP = 5;
    public static int WINNER_COINS = 10;
    public static int WINNER_XP = 50;
    public static String VERSION = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];

    static {
        gameStatus = GameStatus.LOBBY;
    }

    public static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String strip(String s) {
        return ChatColor.stripColor(s);
    }

    public static void cleanupGameStructures() {
        mineralsMined.clear();
        placedMinerals.clear();
        placedFurnaces.clear();
        protectedChests.clear();

        LogSystem.info(LogSystem.LogCategory.GAME, "═══════════════════════════════════");
        LogSystem.info(LogSystem.LogCategory.GAME, "LIMPIEZA DE ESTRUCTURAS COMPLETADA");
        LogSystem.info(LogSystem.LogCategory.GAME, "  - Minerales minados: limpiados");
        LogSystem.info(LogSystem.LogCategory.GAME, "  - Minerales colocados: limpiados");
        LogSystem.info(LogSystem.LogCategory.GAME, "  - Hornos protegidos: limpiados");
        LogSystem.info(LogSystem.LogCategory.GAME, "  - Cofres protegidos: limpiados");
        LogSystem.info(LogSystem.LogCategory.GAME, "═══════════════════════════════════");
    }
}