package sdfrpe.github.io.ptc.Utils.Configuration;

import java.io.File;
import org.bukkit.plugin.Plugin;

public class ConfigCreator {
   private static ConfigCreator instance;

   public static ConfigCreator get() {
      if (instance == null) {
         instance = new ConfigCreator();
      }

      return instance;
   }

   public void setup(Plugin p, String configName) {
      File pluginDir = p.getDataFolder();
      File configFile = new File(pluginDir, configName + ".yml");
      if (!configFile.exists()) {
         p.saveResource(configName + ".yml", false);
      }

   }
}
