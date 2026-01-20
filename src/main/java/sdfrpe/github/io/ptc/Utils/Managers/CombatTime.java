package sdfrpe.github.io.ptc.Utils.Managers;

import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;

public class CombatTime {
   public static int COMBAT_TIME = 10;
   public static LinkedHashMap<LivingEntity, LivingEntity> lastDamager = Maps.newLinkedHashMap();
   public static LinkedHashMap<LivingEntity, Integer> lastDmgTime = Maps.newLinkedHashMap();

   public static boolean getInflicted(LivingEntity e) {
      return lastDamager.containsKey(e) && (Integer)lastDmgTime.get(e) <= COMBAT_TIME;
   }

   public static LivingEntity getWho(LivingEntity e) {
      return lastDamager.containsKey(e) ? (LivingEntity)lastDamager.get(e) : null;
   }

   public static void createMessage(LivingEntity dead, DamageCause cause) {
      boolean customName = false;
      if (dead.getCustomName() != null) {
         customName = true;
      }

      if (dead instanceof Player || customName) {
         String message;
         if (getInflicted(dead)) {
            if (getWho(dead) instanceof Player) {
               message = messageReplaceAndColor(dead, true, getWho(dead), cause, true);
            } else {
               message = messageReplaceAndColor(dead, true, getWho(dead), cause, false);
            }

            print(message, dead.getWorld());
         } else {
            message = messageReplaceAndColor(dead, false, (LivingEntity)null, cause, false);
            print(message, dead.getWorld());
         }
      }
   }

   private static String getDeathMessageFromConfig(DamageCause cause, Boolean inflicted, Boolean playerAttack) {
      String path = "other";
      String messageType = null;
      if (inflicted) {
         messageType = "inflicted";
      }

      if (!inflicted) {
         messageType = "other";
      }

      if (cause == DamageCause.FALL) {
         path = "fall";
      }

      if (cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION) {
         path = "explosion";
      }

      if (cause == DamageCause.CONTACT) {
         path = "cactus";
      }

      if (cause == DamageCause.DROWNING) {
         path = "drown";
      }

      if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK) {
         path = "fire";
      }

      if (cause == DamageCause.LAVA) {
         path = "lava";
      }

      if (cause == DamageCause.LIGHTNING) {
         path = "lightning";
      }

      if (cause == DamageCause.MELTING) {
         path = "melting";
      }

      if (cause == DamageCause.PROJECTILE) {
         path = "projectile";
      }

      if (cause == DamageCause.STARVATION) {
         path = "starvation";
      }

      if (cause == DamageCause.SUFFOCATION) {
         path = "suffocation";
      }

      if (cause == DamageCause.SUICIDE) {
         path = "suicide";
      }

      if (cause == DamageCause.THORNS) {
         path = "thorns";
      }

      if (cause == DamageCause.VOID) {
         path = "void";
      }

      if (cause == DamageCause.WITHER) {
         path = "wither";
      }

      if (cause == DamageCause.MAGIC) {
         path = "potion";
      }

      if (cause == DamageCause.ENTITY_ATTACK) {
         if (playerAttack) {
            path = "player-attack";
         } else {
            path = "entity-attack";
         }
      }

      return PTC.getInstance().getConfig("Messages").getString("Format." + path + "." + messageType);
   }

   private static String messageReplaceAndColor(LivingEntity dead, Boolean inflicted, LivingEntity attacker, DamageCause cause, Boolean playerAttack) {
      String raw = getDeathMessageFromConfig(cause, inflicted, playerAttack);
      String raw2 = "";
      String returnRaw = "";
      Player p2;
      GamePlayer gamePlayerAttacker;
      GamePlayer gamePlayerDead = null;

      if (dead instanceof Player) {
         p2 = (Player)dead;
         gamePlayerDead = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(p2.getUniqueId());
         if (gamePlayerDead != null) {
            gamePlayerDead.addDeath();
            raw2 = raw.replace("{dead}", gamePlayerDead.getArenaTeam().getColor().getChatColor() + dead.getName());
         } else {
            raw2 = raw.replace("{dead}", dead.getName());
         }
      } else if (dead.getCustomName() == null) {
         raw2 = raw.replace("{dead}", "&a" + dead.getType().name());
      } else {
         raw2 = raw.replace("{dead}", "&a" + dead.getCustomName() + "(" + dead.getType().name() + ")");
      }

      if (attacker != null) {
         if (attacker instanceof Player) {
            p2 = (Player)attacker;
            gamePlayerAttacker = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(p2.getUniqueId());

            if (gamePlayerAttacker != null) {
               gamePlayerAttacker.addKill();
               gamePlayerAttacker.addCoins(Statics.KILL_COINS);
               returnRaw = raw2.replace("{killer}", gamePlayerAttacker.getArenaTeam().getColor().getChatColor() + attacker.getName());

               if (dead instanceof Player && gamePlayerDead != null && attacker != dead) {
                  boolean isDirectPvP = (cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.PROJECTILE);

                  if (isDirectPvP) {
                     gamePlayerAttacker.addDominated(gamePlayerDead);
                     LogSystem.debug(LogCategory.GAME, "PvP:", gamePlayerAttacker.getName(), "mató a", gamePlayerDead.getName());

                     if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
                        notifyClanWarKill(gamePlayerAttacker, gamePlayerDead);
                     }
                  } else {
                     LogSystem.debug(LogCategory.GAME, "Muerte indirecta:", gamePlayerDead.getName(), "causa:", cause.name());
                  }
               }
            } else {
               returnRaw = raw2.replace("{killer}", attacker.getName());
            }
         } else if (attacker.getCustomName() == null) {
            returnRaw = raw2.replace("{killer}", attacker.getType().toString());
         } else {
            returnRaw = raw2.replace("{killer}", attacker.getCustomName() + "(" + attacker.getType().name() + ")");
         }
      } else {
         returnRaw = raw2;
      }

      return Statics.c(returnRaw);
   }

   private static void notifyClanWarKill(GamePlayer killer, GamePlayer victim) {
      try {
         Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
         if (ptcClans != null && ptcClans.isEnabled()) {
            Class<?> clanWarManagerClass = Class.forName("sdfrpe.github.io.ptcclans.Managers.ClanWarManager");
            Object clanWarManagerInstance = clanWarManagerClass.getMethod("getInstance").invoke(null);

            clanWarManagerClass.getMethod("onPlayerKill", java.util.UUID.class, java.util.UUID.class)
                    .invoke(clanWarManagerInstance, killer.getUuid(), victim.getUuid());

            LogSystem.debug(LogCategory.NETWORK, "Kill registrado en ClanWar:", killer.getName(), "→", victim.getName());
         }
      } catch (Exception e) {
         LogSystem.error(LogCategory.NETWORK, "Error notificando kill a PTCClans:", e.getMessage());
      }
   }

   private static void print(String message, World w) {
      LogSystem.info(LogCategory.GAME, "Mensaje de muerte:", message);
      Iterator var2 = Bukkit.getOnlinePlayers().iterator();

      while(var2.hasNext()) {
         Player p = (Player)var2.next();
         p.sendMessage(message);
      }
   }
}