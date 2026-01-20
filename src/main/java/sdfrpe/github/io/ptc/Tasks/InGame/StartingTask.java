package sdfrpe.github.io.ptc.Tasks.InGame;

import sdfrpe.github.io.ptc.Listeners.Game.*;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.Arena;
import sdfrpe.github.io.ptc.Listeners.General.DBLoadListener;
import sdfrpe.github.io.ptc.Listeners.Lobby.LobbyListener;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Tasks.Lobby.LobbyWaitingTask;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Managers.GlobalTabManager;
import sdfrpe.github.io.ptc.Utils.PlayerTabUpdater;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Menu.Vanilla.TeamSelectorMenu;
import sdfrpe.github.io.ptc.Utils.Menu.Vanilla.VoteMenu;
import com.cryptomorin.xseries.messages.ActionBar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class StartingTask extends PTCRunnable {
   private int count = 60;
   private boolean forceStart;
   private boolean cancelled = false;
   private static volatile StartingTask currentTask = null;
   private static final Object TASK_LOCK = new Object();

   public StartingTask() {
      this(false);
   }

   public StartingTask(boolean forceStart) {
      synchronized (TASK_LOCK) {
         if (currentTask != null && !currentTask.isCancelled()) {
            Console.log("Cancelando StartingTask anterior antes de crear nueva");
            currentTask.cancel();
         }

         this.forceStart = forceStart;
         currentTask = this;
      }

      Console.log("Started: " + this.getClass().getSimpleName() + (forceStart ? " (FORCE START)" : ""));

      if (PTC.getLobbyWaitingTask() != null) {
         PTC.getLobbyWaitingTask().cancel();
         LogSystem.debug(LogCategory.GAME, "LobbyWaitingTask cancelado - Iniciando cuenta regresiva");
      }

      BossBarAPI.setEnabled(true);
      this.updateBoss();

      PlayerTabUpdater.updateAllPlayerTabs();
      LogSystem.debug(LogCategory.GAME, "TAB actualizado al iniciar cuenta regresiva");
   }

   @Override
   public void cancel() {
      super.cancel();
      this.cancelled = true;
      synchronized (TASK_LOCK) {
         if (currentTask == this) {
            currentTask = null;
         }
      }
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void onTick() {
      this.updateBoss();

      int minPlayers = this.forceStart ? 1 : PTC.getInstance().getGameManager().getGlobalSettings().getArenaPlayers().getMinPlayers();

      if (Bukkit.getOnlinePlayers().size() < minPlayers) {
         this.count = 10;
         Statics.gameStatus = GameStatus.LOBBY;
         BossBarAPI.setEnabled(false);
         ActionBar.sendPlayersActionBar("La partida fue cancelada. Esperando jugadores...");
         Console.debug("Game cancelled: Not enough players (" + Bukkit.getOnlinePlayers().size() + " / " + minPlayers + ")");

         PlayerTabUpdater.updateAllPlayerTabs();

         LobbyWaitingTask newLobbyTask = new LobbyWaitingTask();
         newLobbyTask.run();
         LogSystem.debug(LogCategory.GAME, "LobbyWaitingTask reiniciado - Volviendo a espera");

         this.cancel();
      } else {
         if (this.count == 1) {
            PTC.getInstance().getListenerManager().unregister(LobbyListener.class);
            PTC.getInstance().getListenerManager().unregister(VoteMenu.class);
            PTC.getInstance().getListenerManager().unregister(TeamSelectorMenu.class);

            PTC.getInstance().getListenerManager().register(
                    new InGameInteracts(),
                    new DeathListener(),
                    new MiningListener(),
                    new WorldListeners(),
                    new FurnacesListener(),
                    new ChestProtectionListener(),
                    new CoreListener(),
                    new CoreProtectionListener(),
                    new DeathMessagesListener(),
                    new FriendlyFireListener()
            );

            Console.log("In-game listeners registered (ExperienceListener ya activo desde el inicio).");

            Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
               if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
                  Set<Entry<String, Integer>> allVotes = PTC.getInstance().getGameManager().getArenaManager().getVotes();

                  Entry<String, Integer> votedArena = allVotes.stream()
                          .max(Comparator.comparingInt(Entry::getValue))
                          .orElse(null);

                  String arenaName;
                  if (votedArena != null && votedArena.getValue() > 0) {
                     arenaName = votedArena.getKey();
                     Console.log("Arena votada seleccionada: " + arenaName + " (" + votedArena.getValue() + " votos)");
                  } else {
                     arenaName = PTC.getInstance().getGameManager().getGlobalSettings().getActiveClanWarArena();
                     Console.log("Sin votos - usando arena por defecto: " + arenaName);
                  }

                  if (arenaName == null || arenaName.isEmpty()) {
                     Console.error("CRÍTICO: Arena CW no configurada");
                     Bukkit.broadcastMessage("§c§lERROR: Arena de Clan War no configurada");
                     Statics.gameStatus = GameStatus.LOBBY;
                     BossBarAPI.setEnabled(false);
                     this.cancel();
                     return;
                  }

                  Arena arena = PTC.getInstance().getGameManager().getArenaManager().getArena(arenaName);

                  if (arena == null) {
                     Console.error("CRÍTICO: Arena CW no encontrada: " + arenaName);
                     Bukkit.broadcastMessage("§c§lERROR: Arena '" + arenaName + "' no existe");
                     Statics.gameStatus = GameStatus.LOBBY;
                     BossBarAPI.setEnabled(false);
                     this.cancel();
                     return;
                  }

                  PTC.getInstance().getGameManager().arena = arena;
                  PTC.getInstance().getGameManager().getWorldManager().loadWorld(arenaName);
                  PTC.getInstance().getGameManager().getGameSettings().setupTeams(arena);

                  Console.log("Arena CW cargada: " + arenaName);

                  PTC.getInstance().getGameManager().getArenaManager().clearAllVotes();

                  notifyClanWarStart();
               } else {
                  PTC.getInstance().getGameManager().checkArena();
                  Console.debug("Clearing arena votes after arena selection...");
                  PTC.getInstance().getGameManager().getArenaManager().clearAllVotes();
               }
            });
         }

         if (this.count <= -1) {
            Collection<GamePlayer> players = PTC.getInstance().getGameManager().getPlayerManager().getPlayerMap().values();
            Console.log("Players to assign to teams: " + players.size());

            Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
               Console.log("Resetting local stats for all players...");
               for (GamePlayer gamePlayer : players) {
                  gamePlayer.resetLocalStats();
               }

               LobbyItemsProtectionListener.cleanLobbyItemsFromAllPlayers();

               PTC.getInstance().getGameManager().getGameSettings().insertPlayersInTeams(players);

               GlobalTabManager.getInstance().updateAllPlayerTabs();

               DBLoadListener.registerGameStart();

               Console.log("═══════════════════════════════════");
               Console.log("INICIANDO FASE DE CÁPSULAS");
               Console.log("═══════════════════════════════════");

               Statics.gameStatus = GameStatus.CAGE_PHASE;
               (new CagePhaseTask()).run();
            });

            this.cancel();
         }

         --this.count;
      }
   }

   private void notifyClanWarStart() {
      try {
         Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
         if (ptcClans != null && ptcClans.isEnabled()) {
            Class<?> clanWarManagerClass = Class.forName("sdfrpe.github.io.ptcclans.Managers.ClanWarManager");
            Object clanWarManagerInstance = clanWarManagerClass.getMethod("getInstance").invoke(null);
            clanWarManagerClass.getMethod("onWarStarted").invoke(clanWarManagerInstance);

            LogSystem.info(LogCategory.NETWORK, "═══════════════════════════════════");
            LogSystem.info(LogCategory.NETWORK, "CLAN WAR INICIADA");
            LogSystem.info(LogCategory.NETWORK, "Arena:", PTC.getInstance().getGameManager().getArena().getArenaSettings().getName());
            LogSystem.info(LogCategory.NETWORK, "═══════════════════════════════════");
         }
      } catch (Exception e) {
         LogSystem.error(LogCategory.NETWORK, "Error notificando inicio de guerra a PTCClans:", e.getMessage());
      }
   }

   public void updateBoss() {
      int a = this.count * 100 / 60;
      if (this.count >= 0) {
         BossBarAPI.update(Statics.c(String.format("&aLa partida comenzará en: %ss", this.count)), (float)a);
      }
   }
}