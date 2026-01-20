package sdfrpe.github.io.ptc.Listeners.Game;

import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerUtils;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Location;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class DeathListener implements Listener {
   @EventHandler
   public void PlayerDeath(PlayerDeathEvent e) {
      Player victim = e.getEntity();
      Player killer = victim.getKiller();

      List<ItemStack> newItems = (List)e.getDrops().stream().filter((itemStack) -> {
         return !itemStack.getData().getItemType().name().contains("WOOD_");
      }).filter((itemStack) -> {
         return !itemStack.getData().getItemType().name().contains("LEATHER");
      }).collect(Collectors.toList());
      e.getDrops().clear();
      e.getDrops().addAll(newItems);

      GamePlayer victimGamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(victim.getUniqueId());

      if (victimGamePlayer != null) {
         victimGamePlayer.addDeath();
      }

      if (killer != null && killer != victim) {
         GamePlayer killerGamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(killer.getUniqueId());

         if (killerGamePlayer != null && victimGamePlayer != null) {
            killerGamePlayer.addKill();
            killerGamePlayer.addCoins(Statics.KILL_COINS);
            killerGamePlayer.addClanXP(Statics.KILL_XP, "Kill");
            killerGamePlayer.addDominated(victimGamePlayer);

            LogSystem.debug(LogCategory.GAME, "Recompensas otorgadas:",
                    killer.getName(), "mató a", victim.getName());
         }

         if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
            trackClanWarKill(killer.getUniqueId(), victim.getUniqueId());
         }
      }

      Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
         victim.spigot().respawn();
      }, 40L);
   }

   private void trackClanWarKill(UUID killerUuid, UUID victimUuid) {
      try {
         Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
         if (adapter != null) {
            java.lang.reflect.Method onPlayerKillMethod = adapter.getClass().getMethod("onPlayerKill", UUID.class, UUID.class);
            onPlayerKillMethod.invoke(adapter, killerUuid, victimUuid);
            LogSystem.debug(LogCategory.GAME, "Kill registrado en guerra CW");
         }
      } catch (Exception e) {
         LogSystem.error(LogCategory.GAME, "Error registrando kill en guerra:", e.getMessage());
      }
   }

   @EventHandler
   public void playerRespawn(PlayerRespawnEvent e) {
      Player player = e.getPlayer();
      GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

      if (gamePlayer == null) {
         LogSystem.error(LogCategory.PLAYER, "GamePlayer null en respawn:", player.getName());
         return;
      }

      if (gamePlayer.getArenaTeam() == null) {
         LogSystem.error(LogCategory.PLAYER, "ArenaTeam null en respawn:", player.getName());
         return;
      }

      if (gamePlayer.getArenaTeam().isDeathTeam()) {
         convertToSpectator(player, gamePlayer, gamePlayer.getArenaTeam());

         Location center = PTC.getInstance().getGameManager().getArena().getArenaSettings().getArenaLocations().getCenterSpawn();
         if (center != null) {
            e.setRespawnLocation(center.getLocation());
         }
         return;
      }

      if (gamePlayer.getArenaTeam().getColor() == TeamColor.SPECTATOR) {
         LogSystem.warn(LogCategory.PLAYER, "Espectador intentando respawnear - bloqueado:", player.getName());

         Location center = PTC.getInstance().getGameManager().getArena().getArenaSettings().getCenter();
         if (center != null) {
            e.setRespawnLocation(center.getLocation());
         }
         return;
      }

      Location respawnLoc = gamePlayer.getArenaTeam().handleRespawn(gamePlayer);
      if (respawnLoc != null) {
         e.setRespawnLocation(respawnLoc.getLocation());
         LogSystem.debug(LogCategory.PLAYER, "Jugador respawneado:", player.getName());
      } else {
         LogSystem.error(LogCategory.PLAYER, "Respawn location null para:", player.getName());
      }
   }

   private void convertToSpectator(Player player, GamePlayer gamePlayer, ArenaTeam eliminatedTeam) {
      int participations = gamePlayer.getParticipationCount();
      boolean canPlayAgain = gamePlayer.canUseJugarCommand();
      int remaining = gamePlayer.getRemainingParticipations();

      LogSystem.info(LogCategory.PLAYER, "Convirtiendo a espectador:",
              player.getName(), "(Participaciones:", participations + "/2)");

      eliminatedTeam.removePlayer(gamePlayer);

      ArenaTeam spectatorTeam = PTC.getInstance().getGameManager()
              .getGameSettings().getSpectatorTeam();
      spectatorTeam.addPlayer(gamePlayer, false);

      sdfrpe.github.io.ptc.Listeners.General.DBLoadListener.savePlayerParticipation(
              player.getUniqueId(), participations
      );

      player.setGameMode(GameMode.CREATIVE);
      PlayerUtils.fakeSpectator(player);

      for (Player online : Bukkit.getOnlinePlayers()) {
         if (online != player) {
            online.hidePlayer(player);
         }
      }

      gamePlayer.updateTabList();
      gamePlayer.forceUpdateNameTag();

      for (Player online : Bukkit.getOnlinePlayers()) {
         if (online != player) {
            GamePlayer otherGP = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(online.getUniqueId());
            if (otherGP != null) {
               otherGP.forceUpdateNameTag();
            }
         }
      }

      Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
         sdfrpe.github.io.ptc.Utils.PlayerTabUpdater.updateAllPlayerTabs();
      }, 3L);

      Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
         if (canPlayAgain) {
            player.sendMessage(Statics.c("&c&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.sendMessage(Statics.c("&c&lTU EQUIPO HA SIDO ELIMINADO"));
            player.sendMessage(Statics.c(""));
            player.sendMessage(Statics.c("&7Puedes usar &e/jugar &7para unirte"));
            player.sendMessage(Statics.c("&7a otro equipo activo"));
            player.sendMessage(Statics.c(""));
            player.sendMessage(Statics.c("&7Participaciones: &e" + participations + "&7/&c2"));
            player.sendMessage(Statics.c("&c⚠ &7Solo puedes jugar &c" + remaining +
                    " vez" + (remaining == 1 ? "" : "es") + " &7más"));
            player.sendMessage(Statics.c("&c&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
         } else {
            player.sendMessage(Statics.c("&c&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.sendMessage(Statics.c("&c&lTU EQUIPO HA SIDO ELIMINADO"));
            player.sendMessage(Statics.c(""));
            player.sendMessage(Statics.c("&c&lLÍMITE DE PARTICIPACIONES ALCANZADO"));
            player.sendMessage(Statics.c("&7Ya has jugado &c2 veces &7en esta partida"));
            player.sendMessage(Statics.c("&7Continuarás como espectador"));
            player.sendMessage(Statics.c(""));
            player.sendMessage(Statics.c("&7Participaciones: &c2&7/&c2 &c(MÁXIMO)"));
            player.sendMessage(Statics.c("&c&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
         }
      }, 40L);

      LogSystem.info(LogCategory.PLAYER, "Jugador convertido a espectador:",
              player.getName(), "- Puede jugar más:", String.valueOf(canPlayAgain));
   }

   @EventHandler
   public void playerDropItem(PlayerDropItemEvent e) {
      ItemStack itemStack = e.getItemDrop().getItemStack();
      if (itemStack.getData().getItemType().name().contains("WOOD_") || itemStack.getData().getItemType().name().contains("LEATHER")) {
         e.getItemDrop().remove();
      }
   }
}