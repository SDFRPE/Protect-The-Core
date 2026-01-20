package sdfrpe.github.io.ptc.Game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.scheduler.BukkitTask;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Database.IDatabase;
import sdfrpe.github.io.ptc.Database.Structures.GameLoader;
import sdfrpe.github.io.ptc.Game.Arena.Arena;
import sdfrpe.github.io.ptc.Game.Arena.GameSettings;
import sdfrpe.github.io.ptc.Game.Extra.GlobalSettings;
import sdfrpe.github.io.ptc.Game.Settings.ArenaSettings;
import sdfrpe.github.io.ptc.Tasks.ClanWarPreStartTask;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Configuration.ConfigCreator;
import sdfrpe.github.io.ptc.Utils.Configuration.JsonConfig;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TimeOfDay;
import sdfrpe.github.io.ptc.Utils.Managers.ArenaManager;
import sdfrpe.github.io.ptc.Utils.Managers.GameVoteManager;
import sdfrpe.github.io.ptc.Utils.Managers.ScoreManager;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import sdfrpe.github.io.ptc.Utils.Managers.WorldManager;
import sdfrpe.github.io.ptc.Utils.Menu.Vanilla.ArenaSelectorMenu;
import sdfrpe.github.io.ptc.Utils.Menu.Vanilla.ExtrasMenu;
import sdfrpe.github.io.ptc.Utils.Menu.Vanilla.TeamSelectorMenu;
import sdfrpe.github.io.ptc.Utils.Menu.Vanilla.VoteMenu;
import sdfrpe.github.io.ptc.Utils.Menu.Vanilla.StatisticsMenu;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sdfrpe.github.io.ptc.Utils.Menu.Vanilla.ClanWarArenaSelectorMenu;

public class GameManager {
   private final PTC ptc;
   private final Gson gson;
   private final File databasesFolder;
   private final File arenasFolder;
   private final File settingsFile;
   private final PlayerManager playerManager;
   private final WorldManager worldManager;
   private final ArenaManager arenaManager;
   private final VoteMenu voteMenu;
   private final TeamSelectorMenu teamSelectorMenu;
   private final ArenaSelectorMenu arenaSelectorMenu;
   private final ExtrasMenu extrasMenu;
   private final StatisticsMenu statisticsMenu;
   private final GameVoteManager gameVoteManager;
   private final GameSettings gameSettings;
   public Arena arena;
   private ScoreManager scoreManager;
   private GlobalSettings globalSettings;
   private IDatabase database;
   private Object clanWarAdapter;
   private BukkitTask clanWarCountdownTask;
   private final ClanWarArenaSelectorMenu cwArenaSelectorMenu;

   public GameManager(PTC ptc) {
      this.ptc = ptc;
      this.gson = new GsonBuilder().setPrettyPrinting().create();
      this.arenaManager = new ArenaManager(this);
      this.gameVoteManager = new GameVoteManager();
      this.voteMenu = new VoteMenu(this);
      this.teamSelectorMenu = new TeamSelectorMenu(this);
      this.arenaSelectorMenu = new ArenaSelectorMenu();
      this.extrasMenu = new ExtrasMenu(this);
      this.statisticsMenu = new StatisticsMenu(this);
      this.gameSettings = new GameSettings(this);
      this.playerManager = new PlayerManager();
      this.worldManager = new WorldManager();
      this.databasesFolder = new File(ptc.getDataFolder(), "Databases");
      this.arenasFolder = new File(ptc.getDataFolder(), "Arenas");
      this.settingsFile = new File(ptc.getDataFolder(), "GlobalSettings.json");
      this.clanWarAdapter = null;
      this.clanWarCountdownTask = null;
      this.cwArenaSelectorMenu = new ClanWarArenaSelectorMenu();
   }

   public void loadSettings() {
      try {
         this.scoreManager = new ScoreManager();
         ConfigCreator.get().setup(this.ptc, "Settings");
         ConfigCreator.get().setup(this.ptc, "Messages");

         if (this.databasesFolder.mkdir()) {
            LogSystem.debug(LogCategory.CORE, "Carpeta Databases creada");
         }

         if (this.arenasFolder.mkdir()) {
            LogSystem.debug(LogCategory.CORE, "Carpeta Arenas creada");
         }

         boolean ignored = this.settingsFile.createNewFile();
         String data = JsonConfig.readFile(this.settingsFile);
         this.globalSettings = this.gson.fromJson(data, GlobalSettings.class);

         if (this.globalSettings == null) {
            this.globalSettings = new GlobalSettings();
         }

         if (this.globalSettings.isLobbyMode()) {
            LogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
            LogSystem.info(LogCategory.CORE, "SERVIDOR EN MODO LOBBY");
            LogSystem.info(LogCategory.CORE, "Solo se cargará el selector de arenas");
            LogSystem.info(LogCategory.CORE, "═══════════════════════════════════");

            Bukkit.getPluginManager().registerEvents(this.arenaSelectorMenu, this.ptc);
            this.arenaSelectorMenu.initialize();
            LogSystem.info(LogCategory.CORE, "Arena Selector Menu registrado e inicializado (Modo Lobby)");
            this.database = new GameLoader();
            Bukkit.getPluginManager().registerEvents(this.cwArenaSelectorMenu, this.ptc);
            this.cwArenaSelectorMenu.initialize();
            LogSystem.debug(LogCategory.CORE, "CW Arena Selector Menu registrado e inicializado");
            return;
         }

         this.arenaManager.loadArenas();

         if (this.globalSettings.isModeCW()) {
            if (!validateClanWarMode()) {
               this.globalSettings.setModeCW(false);
               this.saveGlobalSettings();
               throw new RuntimeException("Modo CW inválido - Arena no configurada o no existe");
            }
            this.initializeClanWarAdapter();
         }

         this.voteMenu.updateArenas();
         this.teamSelectorMenu.updateTeams();
         this.gameSettings.createTeams();
         this.database = new GameLoader();

         Bukkit.getPluginManager().registerEvents(this.arenaSelectorMenu, this.ptc);
         this.arenaSelectorMenu.initialize();
         Bukkit.getPluginManager().registerEvents(this.extrasMenu, this.ptc);
         Bukkit.getPluginManager().registerEvents(this.cwArenaSelectorMenu, this.ptc);
         this.cwArenaSelectorMenu.initialize();
         LogSystem.debug(LogCategory.CORE, "Menús registrados e inicializados correctamente");

         if (this.globalSettings.isModeCW()) {
            LogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
            LogSystem.info(LogCategory.CORE, "MODO CLAN WAR ACTIVO");
            LogSystem.info(LogCategory.CORE, "Arena por defecto:", this.globalSettings.getActiveClanWarArena());
            LogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed to load settings", e);
      }
   }

   private boolean validateClanWarMode() {
      String arenaName = this.globalSettings.getActiveClanWarArena();

      if (arenaName == null || arenaName.isEmpty()) {
         LogSystem.error(LogCategory.CORE, "═══════════════════════════════════");
         LogSystem.error(LogCategory.CORE, "ERROR: Arena CW no configurada");
         LogSystem.error(LogCategory.CORE, "Usa /ptc setcwarena <nombre>");
         LogSystem.error(LogCategory.CORE, "═══════════════════════════════════");
         return false;
      }

      Arena arena = this.arenaManager.getArena(arenaName);
      if (arena == null) {
         LogSystem.error(LogCategory.CORE, "═══════════════════════════════════");
         LogSystem.error(LogCategory.CORE, "ERROR: Arena CW no existe:", arenaName);
         LogSystem.error(LogCategory.CORE, "Crea la arena con /ptc create", arenaName);
         LogSystem.error(LogCategory.CORE, "═══════════════════════════════════");
         return false;
      }

      return true;
   }

   private void initializeClanWarAdapter() {
      try {
         Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
         if (ptcClans == null || !ptcClans.isEnabled()) {
            LogSystem.error(LogCategory.CORE, "═══════════════════════════════════");
            LogSystem.error(LogCategory.CORE, "ERROR: PTCClans no está instalado");
            LogSystem.error(LogCategory.CORE, "El modo Clan War requiere PTCClans");
            LogSystem.error(LogCategory.CORE, "Desactivando modo CW...");
            LogSystem.error(LogCategory.CORE, "═══════════════════════════════════");
            this.globalSettings.setModeCW(false);
            this.saveGlobalSettings();
            return;
         }

         Class<?> ptcClansClass = Class.forName("sdfrpe.github.io.ptcclans.PTCClans");
         Method getInstanceMethod = ptcClansClass.getMethod("getInstance");
         Object instance = getInstanceMethod.invoke(null);

         Method getAdapterMethod = instance.getClass().getMethod("getClanWarAdapter");
         this.clanWarAdapter = getAdapterMethod.invoke(instance);

         if (this.clanWarAdapter == null) {
            LogSystem.error(LogCategory.CORE, "ClanWarAdapter es null - PTCClans no está en modo CW");
            this.globalSettings.setModeCW(false);
            this.saveGlobalSettings();
            return;
         }

         LogSystem.info(LogCategory.CORE, "ClanWarAdapter inicializado correctamente");
      } catch (Exception e) {
         LogSystem.error(LogCategory.CORE, "Error inicializando ClanWarAdapter:", e.getMessage());
         e.printStackTrace();
         this.globalSettings.setModeCW(false);
         this.saveGlobalSettings();
      }
   }

   public void saveGlobalSettings() {
      String data = this.gson.toJson(this.globalSettings);
      JsonConfig.writeFile(this.settingsFile, data);
   }

   public void saveArena(ArenaSettings arenaSettings) {
      String fileName = arenaSettings.getName() + ".json";
      File arenaFile = new File(this.arenasFolder, fileName);
      String data = this.gson.toJson(arenaSettings);
      JsonConfig.writeFile(arenaFile, data);
   }

   public boolean checkWin() {
      long validTeams = this.getGameSettings().countValidTeams();

      if (Statics.gameStatus == GameStatus.ENDED) {
         return false;
      }

      int totalTeams = this.getGameSettings().getTeamList().size();

      if (totalTeams == 1) {
         boolean shouldEnd = validTeams == 0;
         if (shouldEnd) {
            LogSystem.info(LogCategory.GAME, "Fin forzado: El único equipo ha sido eliminado");
         }
         return shouldEnd;
      }

      boolean shouldEnd = validTeams <= 1;
      if (shouldEnd && validTeams == 1) {
         LogSystem.info(LogCategory.GAME, "Fin de partida: Solo queda un equipo");
      } else if (shouldEnd && validTeams == 0) {
         LogSystem.info(LogCategory.GAME, "Fin de partida: No quedan equipos (empate)");
      }

      return shouldEnd;
   }

   public void checkArena() {
      Set<Entry<String, Integer>> allVotes = this.arenaManager.getVotes();

      if (this.globalSettings.isModeCW()) {
         if (this.clanWarAdapter != null) {
            try {
               Method loadNextWarMethod = this.clanWarAdapter.getClass().getMethod("loadNextWar");
               Object war = loadNextWarMethod.invoke(this.clanWarAdapter);

               if (war == null) {
                  LogSystem.warn(LogCategory.GAME, "No hay guerras programadas en API");
                  LogSystem.warn(LogCategory.GAME, "Esperando guerra programada...");
                  LogSystem.warn(LogCategory.GAME, "Acceso BLOQUEADO hasta asignación de guerra");
                  LogSystem.warn(LogCategory.GAME, "═══════════════════════════════════");

                  stopClanWarCountdown();

                  BossBarAPI.setEnabled(true);
                  this.clanWarCountdownTask = Bukkit.getScheduler().runTaskTimer(
                          PTC.getInstance(),
                          new ClanWarPreStartTask(this),
                          0L, 20L
                  );
                  return;
               }

               Method getBlueTagMethod = this.clanWarAdapter.getClass().getMethod("getBlueClanTag");
               Method getRedTagMethod = this.clanWarAdapter.getClass().getMethod("getRedClanTag");

               String blueTag = (String) getBlueTagMethod.invoke(this.clanWarAdapter);
               String redTag = (String) getRedTagMethod.invoke(this.clanWarAdapter);

               LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
               LogSystem.info(LogCategory.GAME, "MODO CLAN WAR - Cargando arena votada");
               LogSystem.info(LogCategory.GAME, "Guerra cargada exitosamente:");
               LogSystem.info(LogCategory.GAME, "  AZUL:", blueTag);
               LogSystem.info(LogCategory.GAME, "  ROJO:", redTag);

            } catch (Exception e) {
               LogSystem.error(LogCategory.GAME, "Error cargando guerra:", e.getMessage());
               e.printStackTrace();
            }
         } else {
            LogSystem.warn(LogCategory.GAME, "ClanWarAdapter no disponible");
            LogSystem.warn(LogCategory.GAME, "Esperando integración PTCClans...");
         }
      }

      LogSystem.debug(LogCategory.GAME, "=== VOTACIONES DE ARENAS ===");
      for (Entry<String, Integer> vote : allVotes) {
         LogSystem.debug(LogCategory.GAME, "Arena:", vote.getKey(), "- Votos:", String.valueOf(vote.getValue()));
      }
      LogSystem.debug(LogCategory.GAME, "===========================");

      Entry<String, Integer> arena = allVotes.stream()
              .max(Comparator.comparingInt(Entry::getValue))
              .orElse(null);

      if (arena != null) {
         LogSystem.info(LogCategory.GAME, "Arena seleccionada:", arena.getKey(), "(" + arena.getValue() + " votos)");

         TimeOfDay votedTime = this.gameVoteManager.getMostVotedTime();
         int votedHealth = this.gameVoteManager.getMostVotedHealth();

         LogSystem.debug(LogCategory.GAME, "=== CONFIGURACIÓN ===");
         LogSystem.debug(LogCategory.GAME, "Tiempo:", votedTime.getDisplayName());
         LogSystem.debug(LogCategory.GAME, "Vida extra:", "+" + votedHealth + " corazones");

         Arena selectedArena = this.arenaManager.getArena(arena.getKey());
         if (selectedArena != null) {
            selectedArena.getArenaSettings().setTimeOfDay(votedTime);
            selectedArena.getArenaSettings().setExtraHearts(votedHealth);
            LogSystem.info(LogCategory.GAME, "Configuración aplicada - Tiempo:", votedTime.getDisplayName(), "Vida:", "+" + votedHealth);
         }

         this.gameVoteManager.clearAllVotes();

         this.arena = selectedArena;
         PTC.getInstance().getGameManager().getWorldManager().loadWorld(arena.getKey());

         if (this.arena == null) {
            LogSystem.error(LogCategory.GAME, "Arena es null después de selección");
            throw new NullPointerException("Arena is null.");
         }

         PTC.getInstance().getGameManager().getGameSettings().setupTeams(this.arena);

         if (this.globalSettings.isModeCW() && this.clanWarAdapter != null) {
            try {
               Method assignArenaMethod = this.clanWarAdapter.getClass().getMethod("assignArenaToWar", String.class);
               assignArenaMethod.invoke(this.clanWarAdapter, arena.getKey());

               Method getBlueTagMethod = this.clanWarAdapter.getClass().getMethod("getBlueClanTag");
               Method getRedTagMethod = this.clanWarAdapter.getClass().getMethod("getRedClanTag");

               String blueTag = (String) getBlueTagMethod.invoke(this.clanWarAdapter);
               String redTag = (String) getRedTagMethod.invoke(this.clanWarAdapter);

               LogSystem.info(LogCategory.GAME, "Arena asignada a guerra:", arena.getKey());
               LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

               stopClanWarCountdown();

               BossBarAPI.setEnabled(true);
               this.clanWarCountdownTask = Bukkit.getScheduler().runTaskTimer(
                       PTC.getInstance(),
                       new ClanWarPreStartTask(this),
                       0L, 20L
               );

               for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                  new TitleAPI()
                          .title(ChatColor.RED + "CLAN WAR")
                          .subTitle(ChatColor.YELLOW + blueTag + " vs " + redTag)
                          .send(onlinePlayer);
               }
            } catch (Exception e) {
               LogSystem.error(LogCategory.GAME, "Error asignando arena a guerra:", e.getMessage());
               e.printStackTrace();
            }
         } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
               new TitleAPI()
                       .title(ChatColor.AQUA + this.arena.getArenaSettings().getName())
                       .subTitle(ChatColor.GREEN + "ha ganado.")
                       .send(onlinePlayer);
            }
         }
      } else {
         LogSystem.error(LogCategory.GAME, "No se pudo seleccionar arena (¿todas con 0 votos?)");
      }
   }

   public void stopClanWarCountdown() {
      if (this.clanWarCountdownTask != null) {
         this.clanWarCountdownTask.cancel();
         this.clanWarCountdownTask = null;
         BossBarAPI.setEnabled(false);
         LogSystem.info(LogCategory.GAME, "Countdown de Clan War detenido");
      }
   }

   public PTC getPtc() {
      return this.ptc;
   }

   public Gson getGson() {
      return this.gson;
   }

   public File getDatabasesFolder() {
      return this.databasesFolder;
   }

   public File getArenasFolder() {
      return this.arenasFolder;
   }

   public File getSettingsFile() {
      return this.settingsFile;
   }

   public PlayerManager getPlayerManager() {
      return this.playerManager;
   }

   public WorldManager getWorldManager() {
      return this.worldManager;
   }

   public ArenaManager getArenaManager() {
      return this.arenaManager;
   }

   public VoteMenu getVoteMenu() {
      return this.voteMenu;
   }

   public TeamSelectorMenu getTeamSelectorMenu() {
      return this.teamSelectorMenu;
   }

   public ArenaSelectorMenu getArenaSelectorMenu() {
      return this.arenaSelectorMenu;
   }

   public ExtrasMenu getExtrasMenu() {
      return this.extrasMenu;
   }

   public StatisticsMenu getStatisticsMenu() {
      return this.statisticsMenu;
   }

   public GameVoteManager getGameVoteManager() {
      return this.gameVoteManager;
   }

   public GameSettings getGameSettings() {
      return this.gameSettings;
   }

   public Arena getArena() {
      return this.arena;
   }

   public ScoreManager getScoreManager() {
      return this.scoreManager;
   }

   public GlobalSettings getGlobalSettings() {
      return this.globalSettings;
   }

   public IDatabase getDatabase() {
      return this.database;
   }

   public Object getClanWarAdapter() {
      return this.clanWarAdapter;
   }

   public ClanWarArenaSelectorMenu getCWArenaSelectorMenu() {
      return this.cwArenaSelectorMenu;
   }
}