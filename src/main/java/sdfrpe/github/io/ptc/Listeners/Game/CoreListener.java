package sdfrpe.github.io.ptc.Listeners.Game;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.ClanLevel.ClanLevelSystem;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Game.Arena.GameSettings;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Location;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import com.cryptomorin.xseries.messages.ActionBar;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CoreListener implements Listener {
   @EventHandler(priority = EventPriority.LOWEST)
   public void handleCoreBreak(BlockBreakEvent e) {
      Block block = e.getBlock();
      Player player = e.getPlayer();
      GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

      if (gamePlayer == null) {
         LogSystem.warn(LogCategory.GAME, "GamePlayer null intentando romper bloque:", player.getName());
         e.setCancelled(true);
         return;
      }

      if (gamePlayer.getArenaTeam() == null) {
         LogSystem.warn(LogCategory.GAME, "Jugador sin equipo intentando romper core:", player.getName());
         e.setCancelled(true);
         player.sendMessage(ChatColor.RED + "No puedes romper cores sin estar en un equipo");
         return;
      }

      if (gamePlayer.getArenaTeam().getColor() == TeamColor.SPECTATOR ||
              gamePlayer.getArenaTeam().getColor() == TeamColor.LOBBY) {
         LogSystem.warn(LogCategory.GAME, "Espectador intentando romper core:", player.getName());
         e.setCancelled(true);
         player.sendMessage(ChatColor.RED + "Los espectadores no pueden romper cores");
         return;
      }

      GameSettings gameSettings = PTC.getInstance().getGameManager().getGameSettings();
      Iterator var5 = gameSettings.getTeamList().entrySet().iterator();

      while(var5.hasNext()) {
         Entry<TeamColor, ArenaTeam> entry = (Entry)var5.next();
         TeamColor color = (TeamColor)entry.getKey();
         ArenaTeam arenaTeam = (ArenaTeam)entry.getValue();

         if (arenaTeam.getCoreLocation() == null) {
            continue;
         }

         boolean isThatBlock = this.compareLoc(arenaTeam.getCoreLocation(), block.getLocation());
         if (isThatBlock) {
            e.setCancelled(true);
            if (gamePlayer.getArenaTeam() != arenaTeam && arenaTeam.getCores() > 0) {
               arenaTeam.setCores(arenaTeam.getCores() - 1);
               LogSystem.logCoreDestroyed(color.getChatColor() + color.getName(), arenaTeam.getCores());

               gamePlayer.getPlayerStats().setCores(gamePlayer.getPlayerStats().getCores() + 1);
               gamePlayer.addClanXP(ClanLevelSystem.XP_PER_CORE, "core_destruction");

               this.handlingAnnounces(arenaTeam, gamePlayer);
               if (arenaTeam.getCores() <= 0) {
                  LogSystem.warn(LogCategory.GAME, "Equipo eliminado:", color.getChatColor() + color.getName());
                  gamePlayer.addCoins(Statics.WINNER_COINS);
                  ActionBar.sendPlayersActionBar(Statics.c(String.format("%sEl equipo %s%s%s ha perdido", ChatColor.RED, arenaTeam.getColor().getChatColor(), arenaTeam.getColor().getName().toUpperCase(), ChatColor.RED)));
                  block.setType(Material.BEDROCK);
                  this.handleSpectators(arenaTeam);

                  if (PTC.getInstance().getGameManager().checkWin()) {
                     ArenaTeam winnerTeam = gameSettings.checkWinnerTeam();
                     if (winnerTeam != null) {
                        notifyClanWarEnd(winnerTeam);
                     }
                  }
               } else {
                  gamePlayer.addCoins(Statics.KILL_COINS);
               }
            } else if (gamePlayer.getArenaTeam() == arenaTeam) {
               player.sendMessage(ChatColor.RED + "¡No puedes destruir el core de tu propio equipo!");
            }
            break;
         }
      }
   }

   private void notifyClanWarEnd(ArenaTeam winnerTeam) {
      if (!PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
         return;
      }

      try {
         Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
         if (adapter == null) {
            LogSystem.warn(LogCategory.GAME, "ClanWarAdapter no disponible para notificar fin");
            return;
         }

         String winnerClanTag = null;

         Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
         if (ptcClans == null || !ptcClans.isEnabled()) {
            LogSystem.warn(LogCategory.GAME, "PTCClans no disponible para notificar fin");
            return;
         }

         Class<?> clanManagerClass = Class.forName("sdfrpe.github.io.ptcclans.Managers.ClanManager");
         Object clanManager = clanManagerClass.getMethod("getInstance").invoke(null);

         for (GamePlayer gp : winnerTeam.getTeamPlayers()) {
            if (gp != null) {
               Object clan = clanManagerClass.getMethod("getPlayerClan", UUID.class)
                       .invoke(clanManager, gp.getUuid());

               if (clan != null) {
                  winnerClanTag = (String) clan.getClass().getMethod("getTag").invoke(clan);
                  break;
               }
            }
         }

         if (winnerClanTag != null) {
            Method onWarFinishedMethod = adapter.getClass().getMethod("onWarFinished", String.class);
            onWarFinishedMethod.invoke(adapter, winnerClanTag);

            LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
            LogSystem.info(LogCategory.GAME, "CLAN WAR FINALIZADA");
            LogSystem.info(LogCategory.GAME, "Ganador:", winnerClanTag);
            LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
         } else {
            LogSystem.warn(LogCategory.GAME, "No se pudo determinar clan ganador");
         }

      } catch (Exception e) {
         LogSystem.error(LogCategory.GAME, "Error notificando fin de guerra:", e.getMessage());
         e.printStackTrace();
      }
   }

   private void handleSpectators(ArenaTeam arenaTeam) {
      arenaTeam.setDeathTeam(true);

      Iterator var2 = Bukkit.getOnlinePlayers().iterator();
      while(var2.hasNext()) {
         Player onlinePlayer = (Player)var2.next();
         GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(onlinePlayer.getUniqueId());
         if (gamePlayer != null) {
            gamePlayer.createPBoard();
         }
      }

      var2 = arenaTeam.getTeamPlayers().iterator();
      while(var2.hasNext()) {
         GamePlayer teamPlayer = (GamePlayer)var2.next();
         if (teamPlayer.getPlayer() != null) {
            Player player = teamPlayer.getPlayer();
            teamPlayer.setSpectator();
            LogSystem.debug(LogCategory.TEAM, "Removiendo jugador", player.getName(), "del equipo", arenaTeam.getColor().getName());

            this.sendToLobby(player, arenaTeam);
         }
      }
   }

   private void sendToLobby(Player player, ArenaTeam arenaTeam) {
      new TitleAPI()
              .title(ChatColor.RED + "¡EQUIPO ELIMINADO!")
              .subTitle(ChatColor.YELLOW + "Regresando al lobby...")
              .fadeInTime(10)
              .showTime(40)
              .fadeOutTime(10)
              .send(player);

      player.sendMessage(Statics.c(String.format("&c&l¡Tu equipo %s%s &c&lha sido eliminado!",
              arenaTeam.getColor().getChatColor(),
              arenaTeam.getColor().getName().toUpperCase())));
      player.sendMessage(Statics.c("&eSerás enviado al lobby en 2 segundos..."));

      new BukkitRunnable() {
         @Override
         public void run() {
            if (player != null && player.isOnline()) {
               try {
                  String lobbyServerName = PTC.getInstance().getGameManager().getGlobalSettings().getLobbyServerName();

                  if (lobbyServerName == null || lobbyServerName.isEmpty()) {
                     LogSystem.error(LogCategory.NETWORK, "Lobby server no configurado");
                     player.kickPlayer(ChatColor.RED + "Tu equipo ha sido eliminado.");
                     return;
                  }

                  ByteArrayDataOutput out = ByteStreams.newDataOutput();
                  out.writeUTF("Connect");
                  out.writeUTF(lobbyServerName);

                  player.sendPluginMessage(PTC.getInstance(), "BungeeCord", out.toByteArray());

                  LogSystem.info(LogCategory.NETWORK, "Jugador enviado a lobby:", player.getName());
               } catch (Exception ex) {
                  LogSystem.error(LogCategory.NETWORK, "Error enviando a lobby:", player.getName(), ex.getMessage());
                  player.kickPlayer(ChatColor.RED + "Tu equipo ha sido eliminado.");
               }
            }
         }
      }.runTaskLater(PTC.getInstance(), 40L);
   }

   private void handlingAnnounces(ArenaTeam arenaTeam, GamePlayer gamePlayer) {
      ActionBar.sendPlayersActionBar(Statics.c(String.format("%sDestrucciones restantes: %s", arenaTeam.getColor().getChatColor(), arenaTeam.getCores())));
      Iterator var3 = arenaTeam.getTeamPlayers().iterator();

      while(var3.hasNext()) {
         GamePlayer teamPlayer = (GamePlayer)var3.next();

         if (teamPlayer.getPlayer() == null) {
            continue;
         }

         (new TitleAPI()).fadeInTime(0).title(String.format("%s%s", ChatColor.RED, arenaTeam.getCores())).subTitle(String.format("%s%s %sdestruye su nucleo.", gamePlayer.getArenaTeam().getColor().getChatColor(), gamePlayer.getName(), ChatColor.RED)).send(teamPlayer.getPlayer());

         try {
            teamPlayer.getPlayer().playSound(teamPlayer.getPlayer().getLocation(), Sound.ANVIL_LAND, 1.0F, 1.0F);
         } catch (Exception soundEx) {
            LogSystem.debug(LogCategory.GAME, "Error reproduciendo sonido para:", teamPlayer.getName());
         }
      }
   }

   private boolean compareLoc(Location loc1, org.bukkit.Location loc2) {
      return loc1.getX() == loc2.getX() && loc1.getY() == loc2.getY() && loc1.getZ() == loc2.getZ();
   }
}