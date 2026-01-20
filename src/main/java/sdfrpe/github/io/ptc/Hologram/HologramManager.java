package sdfrpe.github.io.ptc.Hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramManager {
    private final Plugin plugin;
    private final Object holoEasy;
    private final Object pool;
    private final File configFile;
    private FileConfiguration config;
    private final Map<String, Object> hologramMap;
    private final TopDataFetcher dataFetcher;
    private final TopDesignConfig designConfig;
    private TopUpdaterTask updaterTask;
    private int updateIntervalMinutes;

    private final Class<?> holoEasyClass;
    private final Class<?> hologramClass;
    private final Method startPoolMethod;
    private final Method showMethod;
    private final Method hideMethod;
    private final Method getLocationMethod;

    public HologramManager(Plugin plugin, String apiBaseUrl, Object holoEasy) {
        this.plugin = plugin;
        this.holoEasy = holoEasy;
        this.configFile = new File(plugin.getDataFolder(), "hologram-config.yml");
        this.hologramMap = new HashMap<>();
        this.dataFetcher = new TopDataFetcher(apiBaseUrl);
        this.designConfig = new TopDesignConfig(plugin);
        this.updateIntervalMinutes = 5;

        try {
            this.holoEasyClass = Class.forName("org.holoeasy.HoloEasy");
            this.hologramClass = Class.forName("org.holoeasy.hologram.Hologram");

            this.startPoolMethod = holoEasyClass.getMethod("startPool", double.class, boolean.class, boolean.class);
            this.pool = startPoolMethod.invoke(holoEasy, 60.0D, true, false);

            this.showMethod = hologramClass.getMethod("show", Class.forName("org.holoeasy.pool.IHologramPool"));
            this.hideMethod = hologramClass.getMethod("hide", Class.forName("org.holoeasy.pool.IHologramPool"));
            this.getLocationMethod = hologramClass.getMethod("getLocation");

            LogSystem.info(LogCategory.CORE, "HoloEasy pool inicializado correctamente");
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error inicializando HoloEasy pool:", e.getMessage());
            throw new RuntimeException("No se pudo inicializar HoloEasy", e);
        }

        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadHolograms();
        startUpdater();
    }

    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            config = YamlConfiguration.loadConfiguration(configFile);
            config.set("update-interval-minutes", 5);
            config.set("holograms", new ArrayList<>());

            config.save(configFile);
            LogSystem.info(LogCategory.CORE, "Archivo hologram-config.yml creado");
        } catch (IOException e) {
            LogSystem.error(LogCategory.CORE, "Error creando hologram-config.yml:", e.getMessage());
        }
    }

    private void loadHolograms() {
        hologramMap.clear();
        updateIntervalMinutes = config.getInt("update-interval-minutes", 5);

        List<?> hologramList = config.getList("holograms");
        if (hologramList == null || hologramList.isEmpty()) {
            LogSystem.debug(LogCategory.CORE, "No hay hologramas configurados");
            return;
        }

        for (Object obj : hologramList) {
            if (obj instanceof ConfigurationSection) {
                ConfigurationSection section = (ConfigurationSection) obj;
                loadHologram(section);
            } else if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                loadHologramFromMap(map);
            }
        }

        LogSystem.info(LogCategory.CORE, "Hologramas cargados:", String.valueOf(hologramMap.size()));

        if (!hologramMap.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    updateAllHolograms();
                    LogSystem.info(LogCategory.CORE, "Hologramas actualizados con datos de API");
                }
            }, 100L);
        }
    }

    private void loadHologramFromMap(Map<String, Object> map) {
        try {
            String id = (String) map.get("id");
            String typeStr = (String) map.get("type");
            TopType type = TopType.fromString(typeStr);

            if (type == null) {
                LogSystem.warn(LogCategory.CORE, "Tipo de holograma inválido:", typeStr);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> locMap = (Map<String, Object>) map.get("location");
            String worldName = (String) locMap.get("world");
            double x = ((Number) locMap.get("x")).doubleValue();
            double y = ((Number) locMap.get("y")).doubleValue();
            double z = ((Number) locMap.get("z")).doubleValue();

            Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
            List<TopEntry> initialEntries = dataFetcher.fetchTop(type, 10);

            Object hologram = createTopHologram(id, type, location, initialEntries);
            showMethod.invoke(hologram, pool);

            hologramMap.put(id, hologram);
            LogSystem.debug(LogCategory.CORE, "Holograma cargado desde Map:", id, "-", type.name());
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error cargando holograma desde Map:", e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadHologram(ConfigurationSection section) {
        try {
            String id = section.getString("id");
            String typeStr = section.getString("type");
            TopType type = TopType.fromString(typeStr);

            if (type == null) {
                LogSystem.warn(LogCategory.CORE, "Tipo de holograma inválido:", typeStr);
                return;
            }

            ConfigurationSection locSection = section.getConfigurationSection("location");
            String worldName = locSection.getString("world");
            double x = locSection.getDouble("x");
            double y = locSection.getDouble("y");
            double z = locSection.getDouble("z");

            Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
            List<TopEntry> initialEntries = dataFetcher.fetchTop(type, 10);

            Object hologram = createTopHologram(id, type, location, initialEntries);
            showMethod.invoke(hologram, pool);

            hologramMap.put(id, hologram);
            LogSystem.debug(LogCategory.CORE, "Holograma cargado:", id, "-", type.name());
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error cargando holograma:", e.getMessage());
            e.printStackTrace();
        }
    }

    private Object createTopHologram(String id, TopType type, Location location, List<TopEntry> entries) throws Exception {
        Class<?> topHologramClass = Class.forName("sdfrpe.github.io.ptc.Hologram.TopHologram");
        Constructor<?> constructor = topHologramClass.getConstructor(
                holoEasyClass,
                String.class,
                TopType.class,
                Location.class,
                TopDesignConfig.class,
                List.class
        );
        return constructor.newInstance(holoEasy, id, type, location, designConfig, entries);
    }

    public void createHologram(String id, TopType type, Location location) {
        try {
            List<TopEntry> initialEntries = dataFetcher.fetchTop(type, 10);

            Object hologram = createTopHologram(id, type, location, initialEntries);
            showMethod.invoke(hologram, pool);

            hologramMap.put(id, hologram);
            saveHologram(id, type, location);

            LogSystem.info(LogCategory.CORE, "Holograma creado:", id, "-", type.name());
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error creando holograma:", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveHologram(String id, TopType type, Location location) {
        List<?> hologramList = config.getList("holograms");
        List<Map<String, Object>> mutableList = new ArrayList<>();

        if (hologramList != null) {
            for (Object obj : hologramList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    mutableList.add(new HashMap<>(map));
                } else if (obj instanceof ConfigurationSection) {
                    ConfigurationSection section = (ConfigurationSection) obj;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", section.getString("id"));
                    map.put("type", section.getString("type"));

                    Map<String, Object> locMap = new HashMap<>();
                    ConfigurationSection locSection = section.getConfigurationSection("location");
                    if (locSection != null) {
                        locMap.put("world", locSection.getString("world"));
                        locMap.put("x", locSection.getDouble("x"));
                        locMap.put("y", locSection.getDouble("y"));
                        locMap.put("z", locSection.getDouble("z"));
                    }
                    map.put("location", locMap);
                    mutableList.add(map);
                }
            }
        }

        Map<String, Object> newHologram = new HashMap<>();
        newHologram.put("id", id);
        newHologram.put("type", type.name());

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("world", location.getWorld().getName());
        locationMap.put("x", location.getX());
        locationMap.put("y", location.getY());
        locationMap.put("z", location.getZ());
        newHologram.put("location", locationMap);

        mutableList.add(newHologram);
        config.set("holograms", mutableList);

        try {
            config.save(configFile);
            LogSystem.debug(LogCategory.CORE, "Holograma guardado en config:", id);
        } catch (IOException e) {
            LogSystem.error(LogCategory.CORE, "Error guardando holograma:", e.getMessage());
        }
    }

    public boolean removeNearestHologram(Location location, double radius) {
        try {
            Object nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (Object hologram : hologramMap.values()) {
                Location hologramLocation = (Location) getLocationMethod.invoke(hologram);
                double distance = hologramLocation.distance(location);
                if (distance < radius && distance < nearestDistance) {
                    nearest = hologram;
                    nearestDistance = distance;
                }
            }

            if (nearest != null) {
                removeHologram(nearest);
                return true;
            }

            return false;
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error buscando holograma cercano:", e.getMessage());
            return false;
        }
    }

    private void removeHologram(Object hologram) {
        try {
            hideMethod.invoke(hologram, pool);

            Method getHologramIdMethod = hologram.getClass().getMethod("getHologramId");
            String id = (String) getHologramIdMethod.invoke(hologram);

            hologramMap.remove(id);
            removeFromConfig(id);
            LogSystem.info(LogCategory.CORE, "Holograma eliminado:", id);
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error eliminando holograma:", e.getMessage());
            e.printStackTrace();
        }
    }

    private void removeFromConfig(String id) {
        List<?> hologramList = config.getList("holograms");
        if (hologramList == null) return;

        List<Map<String, Object>> mutableList = new ArrayList<>();

        for (Object obj : hologramList) {
            String hologramId = null;

            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                hologramId = (String) map.get("id");
                if (!id.equals(hologramId)) {
                    mutableList.add(new HashMap<>(map));
                }
            } else if (obj instanceof ConfigurationSection) {
                ConfigurationSection section = (ConfigurationSection) obj;
                hologramId = section.getString("id");
                if (!id.equals(hologramId)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", section.getString("id"));
                    map.put("type", section.getString("type"));

                    Map<String, Object> locMap = new HashMap<>();
                    ConfigurationSection locSection = section.getConfigurationSection("location");
                    if (locSection != null) {
                        locMap.put("world", locSection.getString("world"));
                        locMap.put("x", locSection.getDouble("x"));
                        locMap.put("y", locSection.getDouble("y"));
                        locMap.put("z", locSection.getDouble("z"));
                    }
                    map.put("location", locMap);
                    mutableList.add(map);
                }
            }
        }

        config.set("holograms", mutableList);

        try {
            config.save(configFile);
            LogSystem.debug(LogCategory.CORE, "Holograma removido de config:", id);
        } catch (IOException e) {
            LogSystem.error(LogCategory.CORE, "Error guardando config:", e.getMessage());
        }
    }

    private void updateHologram(String id) {
        try {
            Object oldHologram = hologramMap.get(id);
            if (oldHologram == null) return;

            Method getTypeMethod = oldHologram.getClass().getMethod("getType");
            Method getHologramIdMethod = oldHologram.getClass().getMethod("getHologramId");

            final TopType type = (TopType) getTypeMethod.invoke(oldHologram);
            final String hologramId = (String) getHologramIdMethod.invoke(oldHologram);
            final Location location = (Location) getLocationMethod.invoke(oldHologram);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<TopEntry> entries = dataFetcher.fetchTop(type, 10);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        hideMethod.invoke(oldHologram, pool);
                        hologramMap.remove(id);

                        Object newHologram = createTopHologram(hologramId, type, location, entries);
                        showMethod.invoke(newHologram, pool);
                        hologramMap.put(id, newHologram);

                        LogSystem.debug(LogCategory.CORE, "Hologram recreated:", id);
                    } catch (Exception e) {
                        LogSystem.error(LogCategory.CORE, "Error recreando holograma:", e.getMessage());
                        e.printStackTrace();
                    }
                });
            });
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error actualizando holograma:", e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateAllHolograms() {
        for (String id : new ArrayList<>(hologramMap.keySet())) {
            updateHologram(id);
        }
        LogSystem.info(LogCategory.CORE, "Todos los hologramas actualizados");
    }

    private void startUpdater() {
        if (updaterTask != null) {
            updaterTask.cancel();
        }

        updaterTask = new TopUpdaterTask(this, dataFetcher);
        long intervalTicks = updateIntervalMinutes * 60 * 20L;
        updaterTask.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);

        LogSystem.info(LogCategory.CORE, "Actualizador de hologramas iniciado - Intervalo:", updateIntervalMinutes + " minutos");
    }

    public void reload() {
        try {
            for (Object hologram : hologramMap.values()) {
                hideMethod.invoke(hologram, pool);
            }
            hologramMap.clear();

            if (updaterTask != null) {
                updaterTask.cancel();
            }

            designConfig.reload();
            config = YamlConfiguration.loadConfiguration(configFile);
            loadHolograms();
            startUpdater();

            LogSystem.info(LogCategory.CORE, "HologramManager recargado");
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error recargando hologramas:", e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        try {
            if (updaterTask != null) {
                updaterTask.cancel();
            }

            for (Object hologram : hologramMap.values()) {
                hideMethod.invoke(hologram, pool);
            }

            hologramMap.clear();
            LogSystem.info(LogCategory.CORE, "HologramManager detenido");
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error deteniendo hologramas:", e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Object> getHolograms() {
        return new ArrayList<>(hologramMap.values());
    }
}