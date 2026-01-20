package sdfrpe.github.io.ptc.Utils.Configuration;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigUtils {
   private Map<String, FileConfiguration> loadedConfig = Maps.newHashMap();

   public void saveConfig(Plugin plugin, String configName) {
      File pluginDir = plugin.getDataFolder();
      File configFile = new File(pluginDir, configName + ".yml");
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

      try {
         config.save(configFile);
      } catch (IOException var7) {
         var7.printStackTrace();
      }

   }

   public FileConfiguration getConfig(Plugin plugin, String configName) {
      if (this.loadedConfig.containsKey(configName)) {
         return (FileConfiguration)this.loadedConfig.get(configName);
      } else {
         File configFile = this.getFile(plugin, configName);
         FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
         this.loadedConfig.put(configName, fileConfiguration);
         return fileConfiguration;
      }
   }

   public File getFile(Plugin plugin, String configName) {
      File pluginDir = plugin.getDataFolder();
      return new File(pluginDir, configName + ".yml");
   }

   public void reloadConfig() {
      this.loadedConfig.clear();
   }
}
