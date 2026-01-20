package sdfrpe.github.io.ptc.Utils;

import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogLevel;

public class Console {

   public static void log(String... strings) {
      LogSystem.log(LogLevel.INFO, LogCategory.CORE, strings);
   }

   public static void info(String... strings) {
      LogSystem.log(LogLevel.INFO, LogCategory.CORE, strings);
   }

   public static void warning(String... strings) {
      LogSystem.log(LogLevel.WARN, LogCategory.CORE, strings);
   }

   public static void error(String... strings) {
      LogSystem.log(LogLevel.ERROR, LogCategory.CORE, strings);
   }

   public static void debug(String... strings) {
      LogSystem.log(LogLevel.DEBUG, LogCategory.CORE, strings);
   }
}