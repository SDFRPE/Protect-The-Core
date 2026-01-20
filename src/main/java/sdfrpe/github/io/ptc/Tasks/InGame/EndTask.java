package sdfrpe.github.io.ptc.Tasks.InGame;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Listeners.General.DBLoadListener;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class EndTask extends PTCRunnable {
   private final ArenaTeam arenaTeam;
   private int time;

   public EndTask(ArenaTeam arenaTeam) {
      this.arenaTeam = arenaTeam;
      this.time = 10;
      Statics.gameStatus = GameStatus.ENDED;
      this.updateBoss();
   }

   public void onTick() {
      this.updateBoss();
      Console.debug(String.format("[%s] EndTask | Winners Reward == %s", this.time, this.arenaTeam != null && this.time == 10));

      if (this.arenaTeam != null && this.time == 10) {
         Console.debug("Saving player data..");
         this.savePlayers();

         if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
            notifyClanWarEnd(this.arenaTeam);
         }
      }

      if (this.time == 9) {
         Console.debug("Clearing players list for next game...");
         DBLoadListener.clearGamePlayers();

         PTC.getInstance().getGameManager().getGameSettings().resetStartingFlag();
         LogSystem.debug(LogCategory.GAME, "Starting flag reseteado");
      }

      if (this.time == 5) {
         Console.debug("Clearing arena votes for next game...");
         PTC.getInstance().getGameManager().getArenaManager().clearAllVotes();

         Console.debug("Cleaning up all game structures...");
         Statics.cleanupGameStructures();
      }

      if (this.time == 1) {
         Console.log("Sending all players to lobby before shutdown...");
         this.sendAllPlayersToLobby();
      }

      if (this.time == 0) {
         Console.log("Shutting down server...");
         Bukkit.shutdown();
      }

      --this.time;
   }

   private void notifyClanWarEnd(ArenaTeam winnerTeam) {
      try {
         Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
         if (adapter == null) {
            LogSystem.warn(LogCategory.NETWORK, "ClanWarAdapter no disponible al finalizar guerra");
            return;
         }

         Method hasActiveWarMethod = adapter.getClass().getMethod("hasActiveWar");
         Boolean hasActiveWar = (Boolean) hasActiveWarMethod.invoke(adapter);

         if (!hasActiveWar) {
            LogSystem.debug(LogCategory.NETWORK, "Guerra ya finalizada anteriormente - ignorando llamada duplicada");
            return;
         }

         Method getBlueTagMethod = adapter.getClass().getMethod("getBlueClanTag");
         Method getRedTagMethod = adapter.getClass().getMethod("getRedClanTag");

         String blueTag = (String) getBlueTagMethod.invoke(adapter);
         String redTag = (String) getRedTagMethod.invoke(adapter);

         String winnerClanTag = null;

         if (winnerTeam != null) {
            if (winnerTeam.getColor().name().equals("BLUE")) {
               winnerClanTag = blueTag;
            } else if (winnerTeam.getColor().name().equals("RED")) {
               winnerClanTag = redTag;
            }
         }

         Method onWarFinishedMethod = adapter.getClass().getMethod("onWarFinished", String.class);
         onWarFinishedMethod.invoke(adapter, winnerClanTag);

         LogSystem.info(LogCategory.NETWORK, "═══════════════════════════════════");
         LogSystem.info(LogCategory.NETWORK, "CLAN WAR FINALIZADA");
         if (winnerClanTag != null) {
            LogSystem.info(LogCategory.NETWORK, "Ganador:", winnerClanTag);
         } else {
            LogSystem.info(LogCategory.NETWORK, "Resultado: EMPATE o ERROR");
         }
         LogSystem.info(LogCategory.NETWORK, "═══════════════════════════════════");

         PTC.getInstance().getGameManager().getGlobalSettings().setModeCW(false);
         PTC.getInstance().getGameManager().getGlobalSettings().setActiveClanWarArena(null);
         PTC.getInstance().getGameManager().saveGlobalSettings();

      } catch (Exception e) {
         LogSystem.error(LogCategory.NETWORK, "Error notificando fin de guerra:", e.getMessage());
         e.printStackTrace();
      }
   }

   private void sendAllPlayersToLobby() {
      String lobbyServerName = PTC.getInstance().getGameManager().getGlobalSettings().getLobbyServerName();

      if (lobbyServerName == null || lobbyServerName.isEmpty()) {
         Console.error("Lobby server name is not configured! Cannot send players to lobby.");
         return;
      }

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            for (Player online : Bukkit.getOnlinePlayers()) {
               online.showPlayer(player);
               player.showPlayer(online);
            }

            LogSystem.debug(LogCategory.PLAYER, "Visibilidad restaurada para:", player.getName());
         }
      }

      final CountDownLatch latch = new CountDownLatch(Bukkit.getOnlinePlayers().size());
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger failCount = new AtomicInteger(0);

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTaskAsynchronously(PTC.getInstance(), () -> {
               try {
                  player.sendMessage(Statics.c("&e¡Partida terminada! Regresando al lobby..."));

                  new TitleAPI()
                          .title(ChatColor.GOLD + "¡PARTIDA TERMINADA!")
                          .subTitle(ChatColor.YELLOW + "Regresando al lobby...")
                          .fadeInTime(10)
                          .showTime(40)
                          .fadeOutTime(10)
                          .send(player);

                  ByteArrayDataOutput out = ByteStreams.newDataOutput();
                  out.writeUTF("Connect");
                  out.writeUTF(lobbyServerName);

                  player.sendPluginMessage(PTC.getInstance(), "BungeeCord", out.toByteArray());

                  successCount.incrementAndGet();
                  Console.log(String.format("Sent player %s to lobby server: %s",
                          player.getName(), lobbyServerName));

               } catch (Exception ex) {
                  failCount.incrementAndGet();
                  Console.error("Failed to send player " + player.getName() + " to lobby: " + ex.getMessage());
               } finally {
                  latch.countDown();
               }
            });
         } else {
            latch.countDown();
         }
      }

      try {
         boolean completed = latch.await(5, TimeUnit.SECONDS);
         if (!completed) {
            Console.warning("Timeout esperando envío de jugadores al lobby (" +
                    successCount.get() + " exitosos, " + failCount.get() + " fallidos)");
         } else {
            Console.log("Todos los jugadores procesados: " +
                    successCount.get() + " exitosos, " + failCount.get() + " fallidos");
         }
      } catch (InterruptedException e) {
         Console.error("Interrupted while sending players to lobby: " + e.getMessage());
         Thread.currentThread().interrupt();
      }

      Console.log("Server will shutdown in 1 seconds...");
   }

   public void savePlayers() {
      for (GamePlayer gp : PTC.getInstance().getGameManager().getPlayerManager().getPlayerMap().values()) {
         gp.resetParticipation();
      }
      LogSystem.debug(LogCategory.GAME, "Participaciones reseteadas para todos los jugadores");

      Iterator var1 = this.arenaTeam.getTeamPlayers().iterator();

      String winnerClanName = null;
      String winnerClanTag = null;

      if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
         try {
            Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
            if (adapter != null) {
               if (this.arenaTeam.getColor() == TeamColor.BLUE) {
                  Method getDisplayNameMethod = adapter.getClass().getMethod("getBlueClanDisplayName");
                  Method getTagMethod = adapter.getClass().getMethod("getBlueClanTag");
                  winnerClanName = (String) getDisplayNameMethod.invoke(adapter);
                  winnerClanTag = (String) getTagMethod.invoke(adapter);
               } else if (this.arenaTeam.getColor() == TeamColor.RED) {
                  Method getDisplayNameMethod = adapter.getClass().getMethod("getRedClanDisplayName");
                  Method getTagMethod = adapter.getClass().getMethod("getRedClanTag");
                  winnerClanName = (String) getDisplayNameMethod.invoke(adapter);
                  winnerClanTag = (String) getTagMethod.invoke(adapter);
               }
            }
         } catch (Exception e) {
            LogSystem.debug(LogCategory.GAME, "No se pudo obtener nombre del clan ganador");
         }
      }

      boolean broadcastSent = false;

      while(var1.hasNext()) {
         GamePlayer teamPlayer = (GamePlayer)var1.next();
         if (teamPlayer.getPlayer() != null) {
            int coinsToAdd = Statics.WINNER_COINS * teamPlayer.getPlayerStats().getMultiplier();
            teamPlayer.addCoinsRaw(coinsToAdd);
            teamPlayer.getPlayerStats().setWins(teamPlayer.getPlayerStats().getWins() + 1);
            teamPlayer.addClanXP(Statics.WINNER_XP, "Victoria");

            String title;
            String subtitle;

            if (winnerClanName != null && winnerClanTag != null) {
               title = ChatColor.GOLD + "¡" + winnerClanName.replace("&", "§") + " GANÓ!";
               subtitle = ChatColor.GREEN + "+" + coinsToAdd + " Coins";

               if (!broadcastSent) {
                  Bukkit.broadcastMessage(Statics.c("&6&l════════════════════════════════════"));
                  Bukkit.broadcastMessage(Statics.c("&e&l¡GUERRA FINALIZADA!"));
                  Bukkit.broadcastMessage(Statics.c(""));
                  Bukkit.broadcastMessage(Statics.c("&a&lGanador: &f" + winnerClanName.replace("&", "§")));
                  Bukkit.broadcastMessage(Statics.c("&7Tag: &e[" + winnerClanTag + "]"));
                  Bukkit.broadcastMessage(Statics.c("&6&l════════════════════════════════════"));
                  broadcastSent = true;
               }
            } else {
               title = ChatColor.GREEN + "¡Ganaste!";
               subtitle = String.format("%s+%s Coins", ChatColor.GREEN, coinsToAdd);
            }

            (new TitleAPI())
                    .title(title)
                    .subTitle(subtitle)
                    .showTime(160)
                    .send(teamPlayer.getPlayer());
         }
      }

      var1 = Bukkit.getOnlinePlayers().iterator();

      while(var1.hasNext()) {
         Player onlinePlayer = (Player)var1.next();
         PTC.getInstance().getGameManager().getDatabase().savePlayerSync(onlinePlayer.getUniqueId());
      }

      Console.debug("All player data saved to database.");
   }

   public void updateBoss() {
      int dTime = 10;
      BossBarAPI.update(String.format("%sEl juego ha terminado, reiniciando en: %s", ChatColor.RED, this.time), (float)(this.time * 100) / (float)dTime);
   }
}