package sdfrpe.github.io.ptc;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.Game.Extra.GlobalSettings;
import sdfrpe.github.io.ptc.Hologram.HologramManager;
import sdfrpe.github.io.ptc.Listeners.General.BossBarCleanupListener;
import sdfrpe.github.io.ptc.Listeners.ListenerManager;
import sdfrpe.github.io.ptc.Playtime.PlaytimeManager;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Tasks.BossTask;
import sdfrpe.github.io.ptc.Tasks.ServerStatusUpdater;
import sdfrpe.github.io.ptc.Tasks.Lobby.LobbyWaitingTask;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Configuration.ConfigUtils;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Interfaces.Tick;
import sdfrpe.github.io.ptc.Utils.Inventories.Inventories;
import sdfrpe.github.io.ptc.Utils.Managers.ThreadManager;
import sdfrpe.github.io.ptc.Utils.Menu.IMenuManager;
import sdfrpe.github.io.ptc.Utils.PlaceholderAPI.PTCPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicBoolean;

public final class PTC extends JavaPlugin {
    private static PTC instance;
    private final ListenerManager listenerManager;
    private final ConfigUtils configUtils;
    private final GameManager gameManager;
    private final Inventories inventories;
    private final IMenuManager menuManager;
    private final ThreadManager threadManager;
    private HologramManager hologramManager;
    private ServerStatusUpdater serverStatusUpdater;
    private PlaytimeManager playtimeManager;
    private PTCRunnable autoSaveTask;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private Object holoEasy;
    private static LobbyWaitingTask lobbyWaitingTask;

    public PTC() {
        instance = this;
        this.listenerManager = new ListenerManager(this);
        this.inventories = new Inventories(this);
        this.gameManager = new GameManager(this);
        this.configUtils = new ConfigUtils();
        this.menuManager = new IMenuManager(this);
        this.threadManager = new ThreadManager();
    }

    public static PTC getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false)
                .bStats(false);
        PacketEvents.getAPI().load();

        this.getGameManager().getWorldManager().importWorlds();
        LogSystem.info(LogCategory.CORE, "Plugin cargado correctamente");
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();

        LogSystem.logPluginStart();

        this.gameManager.loadSettings();

        GlobalSettings.LoggingSettings loggingConfig = this.gameManager.getGlobalSettings().getLogging();
        LogSystem.setDebugMode(loggingConfig.isDebugMode());

        for (String category : loggingConfig.getEnabledCategories()) {
            try {
                LogSystem.LogCategory logCategory = LogSystem.LogCategory.valueOf(category.toUpperCase());
                LogSystem.enableDebugCategory(logCategory);
            } catch (IllegalArgumentException e) {
                LogSystem.warn(LogCategory.CORE, "Categoría de log desconocida:", category);
            }
        }

        this.playtimeManager = new PlaytimeManager(this);
        LogSystem.info(LogCategory.CORE, "PlaytimeManager inicializado");

        if (!this.gameManager.getGlobalSettings().isLobbyMode() && !this.gameManager.getGlobalSettings().isConfiguring()) {
            LogSystem.debug(LogCategory.CORE, "Restaurando arenas desde backup...");
            this.getGameManager().getWorldManager().restoreArenas();

            if (this.gameManager.getGlobalSettings().isModeCW()) {
                LogSystem.info(LogCategory.CORE, "Modo Clan War detectado - Cargando arena...");
                this.gameManager.checkArena();
            }
        }

        this.listenerManager.init();
        this.inventories.loadInventories();
        this.menuManager.loadFiles();

        getServer().getPluginManager().registerEvents(new BossBarCleanupListener(), this);

        this.getCommand("msg").setExecutor(new sdfrpe.github.io.ptc.Commands.cmds.MsgCommand(this));
        this.getCommand("coins").setExecutor(new sdfrpe.github.io.ptc.Commands.cmds.CoinsCommand(this));
        this.getCommand("arenas").setExecutor(new sdfrpe.github.io.ptc.Commands.cmds.ArenasCommand(this));
        this.getCommand("cwarenas").setExecutor(new sdfrpe.github.io.ptc.Commands.cmds.CWArenasCommand(this));
        this.getCommand("jugar").setExecutor(new sdfrpe.github.io.ptc.Commands.cmds.JugarCommand());

        if (this.gameManager.getGlobalSettings().isLobbyMode()) {
            try {
                Class<?> holoEasyClass = Class.forName("org.holoeasy.HoloEasy");
                this.holoEasy = holoEasyClass.getConstructor(org.bukkit.plugin.Plugin.class).newInstance(this);
                LogSystem.info(LogCategory.CORE, "HoloEasy inicializado correctamente");

                String databaseUrl = this.gameManager.getGlobalSettings().getDatabaseURL();
                String apiBaseUrl = databaseUrl.replace("/player/", "/").replaceAll("/$", "/");

                this.hologramManager = new HologramManager(this, apiBaseUrl, holoEasy);
                LogSystem.info(LogCategory.CORE, "HologramManager inicializado (Modo Lobby)");
            } catch (ClassNotFoundException e) {
                LogSystem.error(LogCategory.CORE, "HoloEasy no encontrado - Asegúrate de que InkaLobby esté instalado");
            } catch (Exception e) {
                LogSystem.error(LogCategory.CORE, "Error inicializando HoloEasy:", e.getMessage());
                e.printStackTrace();
            }
        }

        if (!this.gameManager.getGlobalSettings().isLobbyMode()) {
            this.getThreadManager().startThreads();
            (new BossBarAPI(this)).runBoss();

            PTCRunnable fastFurnace = new PTCRunnable() {
                public void onTick() {
                    Statics.placedFurnaces.forEach((furnace, uuid) -> {
                        if (furnace.getCookTime() > 0) {
                            furnace.setCookTime((short) Math.min(furnace.getCookTime() + 5, 199));
                        }
                    });
                }
            };
            BossTask.getBossTasks().add(fastFurnace);

            this.serverStatusUpdater = new ServerStatusUpdater();
            this.serverStatusUpdater.run();
            BossTask.getBossTasks().add(this.serverStatusUpdater);
            LogSystem.info(LogCategory.NETWORK, "Server Status Updater iniciado");

            lobbyWaitingTask = new LobbyWaitingTask();
            lobbyWaitingTask.run();
            LogSystem.info(LogCategory.CORE, "LobbyWaitingTask iniciado para monitorear jugadores");
        }

        autoSaveTask = new PTCRunnable() {
            private int tickCounter = 0;
            private final AtomicBoolean saving = new AtomicBoolean(false);

            public void onTick() {
                if (shuttingDown.get()) {
                    return;
                }

                tickCounter++;
                if (tickCounter >= 1200) {
                    tickCounter = 0;

                    if (saving.compareAndSet(false, true)) {
                        Bukkit.getScheduler().runTaskAsynchronously(PTC.getInstance(), () -> {
                            try {
                                int savedPlayers = 0;
                                int errors = 0;
                                for (GamePlayer gamePlayer : getGameManager().getPlayerManager().getPlayerMap().values()) {
                                    try {
                                        getGameManager().getDatabase().savePlayer(gamePlayer.getUuid());
                                        savedPlayers++;
                                    } catch (Exception e) {
                                        errors++;
                                        LogSystem.error(LogCategory.DATABASE, "Error auto-guardando jugador", gamePlayer.getUuid().toString(), e.getMessage());
                                    }
                                }
                                if (errors == 0) {
                                    LogSystem.debug(LogCategory.DATABASE, "Auto-guardado completado:", savedPlayers + " jugadores");
                                } else {
                                    LogSystem.warn(LogCategory.DATABASE, "Auto-guardado con errores -", "Guardados:", String.valueOf(savedPlayers), "Errores:", String.valueOf(errors));
                                }
                            } finally {
                                saving.set(false);
                            }
                        });
                    }
                }
            }
        };
        BossTask.getBossTasks().add(autoSaveTask);
        LogSystem.info(LogCategory.CORE, "Auto-guardado iniciado (cada 60 segundos)");

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        LogSystem.debug(LogCategory.NETWORK, "Canal BungeeCord registrado");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PTCPlaceholder(this).register();
            LogSystem.info(LogCategory.CORE, "PlaceholderAPI conectado correctamente");
        } else {
            LogSystem.warn(LogCategory.CORE, "PlaceholderAPI no encontrado");
        }

        registerShutdownHook();

        LogSystem.info(LogCategory.CORE, "Plugin habilitado correctamente");
        LogSystem.info(LogCategory.CORE, "By ElDaysuu/Daysuke");
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!shuttingDown.get()) {
                LogSystem.warn(LogCategory.CORE, "JVM shutdown detectado - Guardando datos de emergencia");
                performEmergencySave();
            }
        }, "PTC-Emergency-Save"));
    }

    @Override
    public void onDisable() {
        if (shuttingDown.compareAndSet(false, true)) {
            LogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
            LogSystem.info(LogCategory.CORE, "INICIANDO APAGADO CONTROLADO");
            LogSystem.info(LogCategory.CORE, "═══════════════════════════════════");

            if (this.playtimeManager != null) {
                this.playtimeManager.shutdown();
                LogSystem.info(LogCategory.CORE, "PlaytimeManager detenido");
            }

            if (Statics.gameStatus == GameStatus.STARTING) {
                try {
                    this.gameManager.getGameSettings().resetStartingFlag();
                    LogSystem.debug(LogCategory.CORE, "Starting flag reseteado en shutdown");
                } catch (Exception e) {
                    LogSystem.error(LogCategory.CORE, "Error reseteando starting flag:", e.getMessage());
                }
            }

            if (Statics.gameStatus == GameStatus.CAGE_PHASE) {
                LogSystem.info(LogCategory.CORE, "Limpiando cápsulas antes del apagado...");
                try {
                    sdfrpe.github.io.ptc.Game.Cage.CageManager.destroyAllCages();
                } catch (Exception e) {
                    LogSystem.error(LogCategory.CORE, "Error limpiando cápsulas:", e.getMessage());
                }
            }

            if (this.hologramManager != null) {
                try {
                    this.hologramManager.shutdown();
                    LogSystem.info(LogCategory.CORE, "HologramManager detenido");
                } catch (Exception e) {
                    LogSystem.error(LogCategory.CORE, "Error deteniendo HologramManager:", e.getMessage());
                }
            }

            if (autoSaveTask != null) {
                try {
                    autoSaveTask.cancel();
                    LogSystem.debug(LogCategory.CORE, "Auto-save task cancelada");
                } catch (Exception e) {
                    LogSystem.error(LogCategory.CORE, "Error cancelando auto-save:", e.getMessage());
                }
            }

            LogSystem.info(LogCategory.CORE, "Limpiando y guardando datos de jugadores...");
            int cleanedPlayers = 0;
            int errors = 0;

            for (GamePlayer gamePlayer : getGameManager().getPlayerManager().getPlayerMap().values()) {
                try {
                    gamePlayer.cleanup();
                    cleanedPlayers++;
                } catch (Exception e) {
                    errors++;
                    LogSystem.error(LogCategory.DATABASE, "Error limpiando jugador:",
                            gamePlayer.getName(), e.getMessage());
                }
            }

            int savedPlayers = getGameManager().getDatabase().saveAllPlayersSync();

            LogSystem.info(LogCategory.DATABASE, "Jugadores procesados:",
                    "Limpiados:", String.valueOf(cleanedPlayers),
                    "Guardados:", String.valueOf(savedPlayers),
                    (errors > 0 ? "Errores: " + errors : "Sin errores"));

            if (!this.gameManager.getGlobalSettings().isLobbyMode()) {
                try {
                    LogSystem.info(LogCategory.CORE, "Limpiando estructuras de juego...");
                    Statics.cleanupGameStructures();
                } catch (Exception e) {
                    LogSystem.error(LogCategory.CORE, "Error limpiando estructuras:", e.getMessage());
                }
            }

            if (this.serverStatusUpdater != null && !this.gameManager.getGlobalSettings().isLobbyMode()) {
                try {
                    this.serverStatusUpdater.removeServerFromDatabase();
                    LogSystem.info(LogCategory.NETWORK, "Servidor removido de la base de datos");
                } catch (Exception e) {
                    LogSystem.error(LogCategory.NETWORK, "Error removiendo servidor:", e.getMessage());
                }
            }

            try {
                LogSystem.debug(LogCategory.CORE, "Cancelando tareas de BossTask...");
                for (Tick task : BossTask.getBossTasks()) {
                    try {
                        if (task instanceof PTCRunnable) {
                            ((PTCRunnable) task).cancel();
                        }
                    } catch (Exception e) {
                        LogSystem.error(LogCategory.CORE, "Error cancelando tarea:", e.getMessage());
                    }
                }
                BossTask.getBossTasks().clear();
                LogSystem.debug(LogCategory.CORE, "Todas las tareas de BossTask canceladas");
            } catch (Exception e) {
                LogSystem.error(LogCategory.CORE, "Error limpiando BossTask:", e.getMessage());
            }

            try {
                this.getGameManager().saveGlobalSettings();
                LogSystem.debug(LogCategory.CORE, "Configuración global guardada");
            } catch (Exception e) {
                LogSystem.error(LogCategory.CORE, "Error guardando configuración:", e.getMessage());
            }

            if (!this.gameManager.getGlobalSettings().isLobbyMode()) {
                try {
                    this.getThreadManager().stopThreads();
                    LogSystem.debug(LogCategory.CORE, "Threads detenidos");
                } catch (Exception e) {
                    LogSystem.error(LogCategory.CORE, "Error deteniendo threads:", e.getMessage());
                }
            }

            PacketEvents.getAPI().terminate();

            LogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
            LogSystem.logPluginStop();
            LogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
        }
    }

    private void performEmergencySave() {
        try {
            LogSystem.info(LogCategory.DATABASE, "═══════════════════════════════════");
            LogSystem.info(LogCategory.DATABASE, "GUARDADO DE EMERGENCIA");
            LogSystem.info(LogCategory.DATABASE, "═══════════════════════════════════");

            if (this.playtimeManager != null) {
                this.playtimeManager.shutdown();
            }

            int cleanedPlayers = 0;
            int errors = 0;

            for (GamePlayer gamePlayer : getGameManager().getPlayerManager().getPlayerMap().values()) {
                try {
                    gamePlayer.cleanup();
                    cleanedPlayers++;
                } catch (Exception e) {
                    errors++;
                    LogSystem.error(LogCategory.DATABASE, "Error limpiando jugador:", e.getMessage());
                }
            }

            int savedPlayers = getGameManager().getDatabase().saveAllPlayersSync();

            LogSystem.info(LogCategory.DATABASE, "Limpiados:", cleanedPlayers + " jugadores");
            LogSystem.info(LogCategory.DATABASE, "Guardados:", savedPlayers + " jugadores" +
                    (errors > 0 ? " (" + errors + " errores)" : ""));

            if (!this.gameManager.getGlobalSettings().isLobbyMode()) {
                try {
                    Statics.cleanupGameStructures();
                    LogSystem.info(LogCategory.DATABASE, "Estructuras de juego limpiadas");
                } catch (Exception e) {
                    LogSystem.error(LogCategory.DATABASE, "Error limpiando estructuras:", e.getMessage());
                }
            }

            LogSystem.info(LogCategory.DATABASE, "═══════════════════════════════════");
            LogSystem.info(LogCategory.DATABASE, "GUARDADO DE EMERGENCIA COMPLETADO");
            LogSystem.info(LogCategory.DATABASE, "═══════════════════════════════════");
        } catch (Exception e) {
            LogSystem.error(LogCategory.DATABASE, "Error en guardado de emergencia:", e.getMessage());
            e.printStackTrace();
        }
    }

    public static LobbyWaitingTask getLobbyWaitingTask() {
        return lobbyWaitingTask;
    }

    public FileConfiguration getConfig(String name) {
        return this.configUtils.getConfig(this, name);
    }

    public ListenerManager getListenerManager() {
        return this.listenerManager;
    }

    public GameManager getGameManager() {
        return this.gameManager;
    }

    public Inventories getInventories() {
        return this.inventories;
    }

    public IMenuManager getMenuManager() {
        return this.menuManager;
    }

    public ThreadManager getThreadManager() {
        return this.threadManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }
}