package sdfrpe.github.io.ptc.Game.Arena;

import com.google.common.collect.Sets;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerUtils;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Location;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Items.KitController;
import sdfrpe.github.io.ptc.Utils.Managers.GlobalTabManager;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import sdfrpe.github.io.ptc.Utils.Statics;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ArenaTeam {
   private TeamColor color;
   private boolean deathTeam;
   private Set<GamePlayer> teamPlayers;
   private Location spawnLocation;
   private Location coreLocation;
   private int cores;

   public ArenaTeam(TeamColor color, int cores) {
      this(color, false, Sets.newConcurrentHashSet(), null, null, cores);
   }

   public ArenaTeam(TeamColor color, Location spawnLocation, Location coreLocation, int cores) {
      this(color, false, Sets.newConcurrentHashSet(), spawnLocation, coreLocation, cores);
   }

   public ArenaTeam(TeamColor color, boolean deathTeam, Set<GamePlayer> teamPlayers, Location spawnLocation, Location coreLocation, int cores) {
      this.color = color;
      this.deathTeam = deathTeam;
      this.teamPlayers = teamPlayers;
      this.spawnLocation = spawnLocation;
      this.coreLocation = coreLocation;
      this.cores = cores;
   }

   public void addPlayers(List<GamePlayer> next) {
      for (GamePlayer gamePlayer : next) {
         if (gamePlayer.getArenaTeam() == null) {
            this.addPlayer(gamePlayer, false);
         }
      }
   }

   public void addPlayer(GamePlayer gamePlayer, boolean lobby) {
      this.teamPlayers.add(gamePlayer);
      gamePlayer.setArenaTeam(this);

      Player player = gamePlayer.getPlayer();

      if (this.color != TeamColor.SPECTATOR && this.color != TeamColor.LOBBY) {
         if (player != null && player.isOnline()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
               online.showPlayer(player);
               player.showPlayer(online);
            }

            if (Statics.gameStatus == GameStatus.LOBBY || Statics.gameStatus == GameStatus.STARTING) {
               GlobalTabManager.getInstance().addPlayerToTeam(player, this.color);
            }

            LogSystem.debug(LogCategory.TEAM, "Visibilidad restaurada para:", player.getName());
         }

         if (!lobby) {
            new TitleAPI()
                    .subTitle(String.format("%sCompañeros: %s", this.color.getChatColor(), this.countPlayers()))
                    .send(player);
         }
      } else if (player != null && player.isOnline()) {
         if (Statics.gameStatus == GameStatus.LOBBY || Statics.gameStatus == GameStatus.STARTING) {
            GlobalTabManager.getInstance().addPlayerToTeam(player, this.color);
         }
      }
   }

   public void removePlayer(GamePlayer gamePlayer) {
      this.teamPlayers.remove(gamePlayer);
      Player player = gamePlayer.getPlayer();

      if (player == null) {
         LogSystem.debug(LogCategory.TEAM, "Player null en removePlayer:", gamePlayer.getName());
         return;
      }

      PlayerUtils.clean(player, !hasVIPPermissions(player));

      for (Player online : Bukkit.getOnlinePlayers()) {
         online.showPlayer(player);
         player.showPlayer(online);
      }

      if (!hasVIPPermissions(player)) {
         LogSystem.debug(LogCategory.PLAYER, "Enviando", player.getName(), "al lobby (sin permisos VIP)");

         try {
            String lobbyServerName = PTC.getInstance().getGameManager().getGlobalSettings().getLobbyServerName();

            if (lobbyServerName == null || lobbyServerName.isEmpty()) {
               LogSystem.error(LogCategory.NETWORK, "Lobby server no configurado - Expulsando jugador");
               player.kickPlayer(ChatColor.RED + "Tu equipo ha sido eliminado.");
               return;
            }

            player.sendMessage(ChatColor.RED + "Tu equipo ha sido eliminado.");
            player.sendMessage(ChatColor.YELLOW + "Regresando al lobby...");

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(lobbyServerName);

            player.sendPluginMessage(PTC.getInstance(), "BungeeCord", out.toByteArray());

            LogSystem.info(LogCategory.NETWORK, "Jugador", player.getName(), "enviado a lobby:", lobbyServerName);

         } catch (Exception ex) {
            LogSystem.error(LogCategory.NETWORK, "Error enviando a lobby:", player.getName(), ex.getMessage());
            ex.printStackTrace();
            player.kickPlayer(ChatColor.RED + "Tu equipo ha sido eliminado.");
         }

         return;

      } else {
         LogSystem.debug(LogCategory.PLAYER, "Convirtiendo", player.getName(), "a espectador (VIP)");

         player.setGameMode(GameMode.CREATIVE);
         PlayerUtils.fakeSpectator(player);

         for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
               online.hidePlayer(player);
            }
         }

         player.sendMessage(ChatColor.GOLD + "Tu equipo ha sido eliminado.");
         player.sendMessage(ChatColor.GRAY + "Continúas como espectador.");

         try {
            Location center = null;

            if (PTC.getInstance().getGameManager().getArena() != null &&
                    PTC.getInstance().getGameManager().getArena().getArenaSettings() != null) {
               center = PTC.getInstance().getGameManager().getArena().getArenaSettings().getCenter();
            }

            if (center != null && center.getLocation() != null) {
               player.teleport(center.getLocation());
               LogSystem.debug(LogCategory.PLAYER, "Espectador VIP teleportado al centro:", player.getName());
            } else {
               LogSystem.error(LogCategory.GAME, "Arena center es null - Usando spawn del mundo");
               player.teleport(player.getWorld().getSpawnLocation());
            }
         } catch (Exception ex) {
            LogSystem.error(LogCategory.PLAYER, "Error teleportando espectador:", ex.getMessage());
            player.teleport(player.getWorld().getSpawnLocation());
         }
      }

      ArenaTeam spectator = PTC.getInstance().getGameManager().getGameSettings().getSpectatorTeam();
      spectator.addPlayer(gamePlayer, false);
   }

   private boolean hasVIPPermissions(Player player) {
      return player.hasPermission("ptc.joiningame") ||
              player.hasPermission("ptc.joinspectator") ||
              player.hasPermission("ptc.rejoin") ||
              player.hasPermission("ptc.spectate");
   }

   public Location getSpawn() {
      return this.spawnLocation;
   }

   public void setInventory(GamePlayer gamePlayer) {
      KitController.setTeamKit(this.color, gamePlayer.getPlayer());
   }

   public Location handleRespawn(GamePlayer gamePlayer) {
      this.setInventory(gamePlayer);

      Player player = gamePlayer.getPlayer();
      if (player != null && player.isOnline()) {
         try {
            GameSettings gameSettings = PTC.getInstance().getGameManager().getGameSettings();
            int extraHearts = gameSettings.getCurrentArenaExtraHearts();
            gameSettings.applyHealthToPlayer(player, extraHearts);
         } catch (Exception e) {
            LogSystem.error(LogCategory.PLAYER, "Error aplicando vida en respawn:", gamePlayer.getName(), e.getMessage());
         }
      }

      return this.spawnLocation;
   }

   public void setLocations(Location spawnLocation, Location coreLocation) {
      this.setSpawnLocation(spawnLocation);
      this.setCoreLocation(coreLocation);
   }

   public int countPlayers() {
      return this.teamPlayers.size();
   }

   public TeamColor getColor() {
      return this.color;
   }

   public boolean isDeathTeam() {
      return this.deathTeam;
   }

   public Set<GamePlayer> getTeamPlayers() {
      return this.teamPlayers;
   }

   public Location getSpawnLocation() {
      return this.spawnLocation;
   }

   public Location getCoreLocation() {
      return this.coreLocation;
   }

   public int getCores() {
      return this.cores;
   }

   public void setColor(TeamColor color) {
      this.color = color;
   }

   public void setDeathTeam(boolean deathTeam) {
      this.deathTeam = deathTeam;
   }

   public void setSpawnLocation(Location spawnLocation) {
      this.spawnLocation = spawnLocation;
   }

   public void setCoreLocation(Location coreLocation) {
      this.coreLocation = coreLocation;
   }

   public void setCores(int cores) {
      this.cores = cores;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ArenaTeam)) {
         return false;
      } else {
         ArenaTeam other = (ArenaTeam)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.isDeathTeam() != other.isDeathTeam()) {
            return false;
         } else if (this.getCores() != other.getCores()) {
            return false;
         } else {
            label64: {
               Object this$color = this.getColor();
               Object other$color = other.getColor();
               if (this$color == null) {
                  if (other$color == null) {
                     break label64;
                  }
               } else if (this$color.equals(other$color)) {
                  break label64;
               }

               return false;
            }

            label57: {
               Object this$teamPlayers = this.getTeamPlayers();
               Object other$teamPlayers = other.getTeamPlayers();
               if (this$teamPlayers == null) {
                  if (other$teamPlayers == null) {
                     break label57;
                  }
               } else if (this$teamPlayers.equals(other$teamPlayers)) {
                  break label57;
               }

               return false;
            }

            Object this$spawnLocation = this.getSpawnLocation();
            Object other$spawnLocation = other.getSpawnLocation();
            if (this$spawnLocation == null) {
               if (other$spawnLocation != null) {
                  return false;
               }
            } else if (!this$spawnLocation.equals(other$spawnLocation)) {
               return false;
            }

            Object this$coreLocation = this.getCoreLocation();
            Object other$coreLocation = other.getCoreLocation();
            if (this$coreLocation == null) {
               if (other$coreLocation != null) {
                  return false;
               }
            } else if (!this$coreLocation.equals(other$coreLocation)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ArenaTeam;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + (this.isDeathTeam() ? 79 : 97);
      result = result * PRIME + this.getCores();
      Object $color = this.getColor();
      result = result * PRIME + ($color == null ? 43 : $color.hashCode());
      Object $teamPlayers = this.getTeamPlayers();
      result = result * PRIME + ($teamPlayers == null ? 43 : $teamPlayers.hashCode());
      Object $spawnLocation = this.getSpawnLocation();
      result = result * PRIME + ($spawnLocation == null ? 43 : $spawnLocation.hashCode());
      Object $coreLocation = this.getCoreLocation();
      result = result * PRIME + ($coreLocation == null ? 43 : $coreLocation.hashCode());
      return result;
   }

   public String toString() {
      return "ArenaTeam(color=" + this.getColor() + ", deathTeam=" + this.isDeathTeam() + ", teamPlayers=" + this.getTeamPlayers() + ", spawnLocation=" + this.getSpawnLocation() + ", coreLocation=" + this.getCoreLocation() + ", cores=" + this.getCores() + ")";
   }
}