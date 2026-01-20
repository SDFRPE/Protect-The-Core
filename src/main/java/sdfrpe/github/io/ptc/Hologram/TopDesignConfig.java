package sdfrpe.github.io.ptc.Hologram;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TopDesignConfig {
    private final Plugin plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Map<TopType, TopDesign> designs;

    public TopDesignConfig(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "hologram-designs.yml");
        this.designs = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadDesigns();
    }

    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            config = YamlConfiguration.loadConfiguration(configFile);

            config.set("designs.WINS_ALLTIME.title", "&6&lTOP 10 &8┃ &eVictorias");
            config.set("designs.WINS_ALLTIME.entry-format", "&7#{position} &f{player} &8» &a{value}");
            config.set("designs.WINS_ALLTIME.spacing", 0.30);

            config.set("designs.KILLS_ALLTIME.title", "&6&lTOP 10 &8┃ &cAsesinatos");
            config.set("designs.KILLS_ALLTIME.entry-format", "&7#{position} &f{player} &8» &c{value}");
            config.set("designs.KILLS_ALLTIME.spacing", 0.30);

            config.set("designs.DEATHS_ALLTIME.title", "&6&lTOP 10 &8┃ &4Muertes");
            config.set("designs.DEATHS_ALLTIME.entry-format", "&7#{position} &f{player} &8» &4{value}");
            config.set("designs.DEATHS_ALLTIME.spacing", 0.30);

            config.set("designs.CORES_ALLTIME.title", "&6&lTOP 10 &8┃ &6Nucleos");
            config.set("designs.CORES_ALLTIME.entry-format", "&7#{position} &f{player} &8» &6{value}");
            config.set("designs.CORES_ALLTIME.spacing", 0.30);

            config.set("designs.DOMINATION_ALLTIME.title", "&6&lTOP 10 &8┃ &dDominacion");
            config.set("designs.DOMINATION_ALLTIME.entry-format", "&7#{position} &f{player} &8» &d{value}");
            config.set("designs.DOMINATION_ALLTIME.spacing", 0.30);

            config.set("designs.CLAN_LEVEL.title", "&6&lTOP 10 &8┃ &bNivel de Clan");
            config.set("designs.CLAN_LEVEL.entry-format", "&7#{position} &a[{clan}] &f{player} &8» &bNivel {value}");
            config.set("designs.CLAN_LEVEL.entry-format-no-clan", "&7#{position} &f{player} &8» &bNivel {value}");
            config.set("designs.CLAN_LEVEL.spacing", 0.30);

            config.set("designs.BEST_KILLSTREAK.title", "&6&lTOP 10 &8┃ &4Mejor Racha");
            config.set("designs.BEST_KILLSTREAK.entry-format", "&7#{position} &f{player} &8» &4{value}");
            config.set("designs.BEST_KILLSTREAK.spacing", 0.30);

            config.set("designs.WINS_WEEKLY.title", "&6&lTOP SEMANAL &8┃ &eVictorias");
            config.set("designs.WINS_WEEKLY.entry-format", "&7#{position} &f{player} &8» &a{value}");
            config.set("designs.WINS_WEEKLY.spacing", 0.30);

            config.set("designs.KILLS_WEEKLY.title", "&6&lTOP SEMANAL &8┃ &cAsesinatos");
            config.set("designs.KILLS_WEEKLY.entry-format", "&7#{position} &f{player} &8» &c{value}");
            config.set("designs.KILLS_WEEKLY.spacing", 0.30);

            config.set("designs.DEATHS_WEEKLY.title", "&6&lTOP SEMANAL &8┃ &4Muertes");
            config.set("designs.DEATHS_WEEKLY.entry-format", "&7#{position} &f{player} &8» &4{value}");
            config.set("designs.DEATHS_WEEKLY.spacing", 0.30);

            config.set("designs.CORES_WEEKLY.title", "&6&lTOP SEMANAL &8┃ &6Nucleos");
            config.set("designs.CORES_WEEKLY.entry-format", "&7#{position} &f{player} &8» &6{value}");
            config.set("designs.CORES_WEEKLY.spacing", 0.30);

            config.set("designs.DOMINATION_WEEKLY.title", "&6&lTOP SEMANAL &8┃ &dDominacion");
            config.set("designs.DOMINATION_WEEKLY.entry-format", "&7#{position} &f{player} &8» &d{value}");
            config.set("designs.DOMINATION_WEEKLY.spacing", 0.30);

            config.set("designs.PLAYTIME_ALLTIME.title", "&6&lTOP 10 &8┃ &aTiempo Jugado");
            config.set("designs.PLAYTIME_ALLTIME.entry-format", "&7#{position} &f{player} &8» &a{value}");
            config.set("designs.PLAYTIME_ALLTIME.spacing", 0.30);

            config.set("designs.PLAYTIME_WEEKLY.title", "&6&lTOP SEMANAL &8┃ &aTiempo Jugado");
            config.set("designs.PLAYTIME_WEEKLY.entry-format", "&7#{position} &f{player} &8» &a{value}");
            config.set("designs.PLAYTIME_WEEKLY.spacing", 0.30);

            config.save(configFile);
            LogSystem.info(LogCategory.CORE, "Archivo hologram-designs.yml creado con valores por defecto");
        } catch (IOException e) {
            LogSystem.error(LogCategory.CORE, "Error creando hologram-designs.yml:", e.getMessage());
        }
    }

    private void loadDesigns() {
        for (TopType type : TopType.values()) {
            String path = "designs." + type.name();

            String title = config.getString(path + ".title", "&6&lTOP 10");
            String entryFormat = config.getString(path + ".entry-format", "&7#{position} &f{player} &8» &e{value}");
            String entryFormatNoClan = config.getString(path + ".entry-format-no-clan", entryFormat);
            double spacing = config.getDouble(path + ".spacing", 0.30);

            designs.put(type, new TopDesign(title, entryFormat, entryFormatNoClan, spacing));
        }

        LogSystem.debug(LogCategory.CORE, "Cargados", designs.size() + " diseños de hologramas");
    }

    public TopDesign getDesign(TopType type) {
        return designs.getOrDefault(type, new TopDesign(
                "&6&lTOP 10",
                "&7#{position} &f{player} &8» &e{value}",
                "&7#{position} &f{player} &8» &e{value}",
                0.30
        ));
    }

    public void reload() {
        designs.clear();
        loadConfig();
    }

    public static class TopDesign {
        private final String title;
        private final String entryFormat;
        private final String entryFormatNoClan;
        private final double spacing;

        public TopDesign(String title, String entryFormat, String entryFormatNoClan, double spacing) {
            this.title = title;
            this.entryFormat = entryFormat;
            this.entryFormatNoClan = entryFormatNoClan;
            this.spacing = spacing;
        }

        public String getTitle() {
            return title;
        }

        public String getEntryFormat() {
            return entryFormat;
        }

        public String getEntryFormatNoClan() {
            return entryFormatNoClan;
        }

        public double getSpacing() {
            return spacing;
        }
    }
}