package sdfrpe.github.io.ptc.Utils;

import org.bukkit.Bukkit;
import java.util.EnumSet;
import java.util.Set;

public class LogSystem {

    private static final String PREFIX = "PTC";
    private static boolean DEBUG_ENABLED = false;

    private static final Set<LogCategory> ENABLED_DEBUG_CATEGORIES = EnumSet.noneOf(LogCategory.class);

    public enum LogLevel {
        ERROR("&4ERROR", 1),
        WARN("&6WARN", 2),
        INFO("&9INFO", 3),
        DEBUG("&eDEBUG", 4);

        private final String prefix;
        private final int priority;

        LogLevel(String prefix, int priority) {
            this.prefix = prefix;
            this.priority = priority;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getPriority() {
            return priority;
        }
    }

    public enum LogCategory {
        CORE("CORE"),
        GAME("GAME"),
        PLAYER("PLAYER"),
        TEAM("TEAM"),
        DATABASE("DATABASE"),
        PERFORMANCE("PERF"),
        NETWORK("NET"),
        API("API");

        private final String tag;

        LogCategory(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }

    public static void setDebugMode(boolean enabled) {
        DEBUG_ENABLED = enabled;
        if (enabled) {
            log(LogLevel.WARN, LogCategory.CORE, "DEBUG MODE ENABLED - Logs verbose activados");
        } else {
            log(LogLevel.INFO, LogCategory.CORE, "DEBUG MODE DISABLED - Solo logs importantes");
        }
    }

    public static void enableDebugCategory(LogCategory category) {
        ENABLED_DEBUG_CATEGORIES.add(category);
        log(LogLevel.INFO, LogCategory.CORE, "Debug habilitado para categoría: " + category.getTag());
    }

    public static void disableDebugCategory(LogCategory category) {
        ENABLED_DEBUG_CATEGORIES.remove(category);
        log(LogLevel.INFO, LogCategory.CORE, "Debug deshabilitado para categoría: " + category.getTag());
    }

    public static void log(LogLevel level, LogCategory category, String... messages) {
        if (level == LogLevel.DEBUG) {
            if (!DEBUG_ENABLED && !ENABLED_DEBUG_CATEGORIES.contains(category)) {
                return;
            }
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String msg : messages) {
            messageBuilder.append(msg).append(" ");
        }

        String finalMessage = messageBuilder.toString().trim();
        String formattedMessage = Statics.c(String.format("&8[&a%s&8] [%s&8] [&7%s&8]&f %s",
                PREFIX,
                level.getPrefix(),
                category.getTag(),
                finalMessage
        ));

        Bukkit.getConsoleSender().sendMessage(formattedMessage);
    }

    public static void log(LogLevel level, String... messages) {
        log(level, LogCategory.CORE, messages);
    }

    public static void error(LogCategory category, String... messages) {
        log(LogLevel.ERROR, category, messages);
    }

    public static void warn(LogCategory category, String... messages) {
        log(LogLevel.WARN, category, messages);
    }

    public static void info(LogCategory category, String... messages) {
        log(LogLevel.INFO, category, messages);
    }

    public static void debug(LogCategory category, String... messages) {
        log(LogLevel.DEBUG, category, messages);
    }

    public static void logPluginStart() {
        log(LogLevel.INFO, LogCategory.CORE, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.CORE, "PTC PLUGIN INICIANDO...");
        log(LogLevel.INFO, LogCategory.CORE, "═══════════════════════════════════");
    }

    public static void logPluginStop() {
        log(LogLevel.INFO, LogCategory.CORE, "PTC Plugin detenido correctamente");
    }

    public static void logPlayerJoin(String playerName) {
        info(LogCategory.PLAYER, "Jugador conectado:", playerName);
    }

    public static void logPlayerQuit(String playerName) {
        info(LogCategory.PLAYER, "Jugador desconectado:", playerName);
    }

    public static void logGameStart(String arenaName, int players) {
        log(LogLevel.INFO, LogCategory.GAME, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.GAME, "PARTIDA INICIADA");
        log(LogLevel.INFO, LogCategory.GAME, "Arena:", arenaName);
        log(LogLevel.INFO, LogCategory.GAME, "Jugadores:", String.valueOf(players));
        log(LogLevel.INFO, LogCategory.GAME, "═══════════════════════════════════");
    }

    public static void logGameEnd(String winnerTeam) {
        log(LogLevel.INFO, LogCategory.GAME, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.GAME, "PARTIDA FINALIZADA");
        log(LogLevel.INFO, LogCategory.GAME, "Equipo ganador:", winnerTeam);
        log(LogLevel.INFO, LogCategory.GAME, "═══════════════════════════════════");
    }

    public static void logCoreDestroyed(String teamColor, int coresRemaining) {
        warn(LogCategory.GAME, "Core destruido -", teamColor, "- Restantes:", String.valueOf(coresRemaining));
    }

    public static void logDatabaseError(String operation, String errorMessage) {
        error(LogCategory.DATABASE, "Error en operación:", operation, "-", errorMessage);
    }

    public static void logDatabaseSuccess(String operation) {
        debug(LogCategory.DATABASE, "Operación exitosa:", operation);
    }

    public static boolean isDebugEnabled() {
        return DEBUG_ENABLED;
    }
}