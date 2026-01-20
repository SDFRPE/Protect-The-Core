package sdfrpe.github.io.ptc.Game.Extra;

import sdfrpe.github.io.ptc.Game.Settings.ArenaPlayers;
import sdfrpe.github.io.ptc.Game.Settings.ArenaTeams;
import sdfrpe.github.io.ptc.Utils.Location;
import java.util.ArrayList;
import java.util.List;

public class GlobalSettings {
   private boolean configuring = true;
   private Location lobbyLocation;
   private String databaseURL = "http://127.0.0.1:25637/ptc/player/";
   private String serverName = "PTC-1";
   private String lobbyServerName = "PTC-Lobby";
   private boolean lobbyMode = false;
   private boolean modeCW = false;
   private String activeClanWarArena = null;
   private long clanWarStartTime = 0;
   private double coreProtectionRadius = 10.0;
   private ArenaPlayers arenaPlayers = new ArenaPlayers();
   private ArenaTeams arenaTeams = new ArenaTeams();
   private LoggingSettings logging = new LoggingSettings();

   public boolean isConfiguring() {
      return this.configuring;
   }

   public Location getLobbyLocation() {
      return this.lobbyLocation;
   }

   public String getDatabaseURL() {
      return this.databaseURL;
   }

   public String getServerName() {
      return this.serverName;
   }

   public String getLobbyServerName() {
      return this.lobbyServerName;
   }

   public boolean isLobbyMode() {
      return this.lobbyMode;
   }

   public boolean isModeCW() {
      return this.modeCW;
   }

   public String getActiveClanWarArena() {
      return this.activeClanWarArena;
   }

   public long getClanWarStartTime() {
      return this.clanWarStartTime;
   }

   public double getCoreProtectionRadius() {
      return this.coreProtectionRadius;
   }

   public ArenaPlayers getArenaPlayers() {
      return this.arenaPlayers;
   }

   public ArenaTeams getArenaTeams() {
      return this.arenaTeams;
   }

   public LoggingSettings getLogging() {
      if (this.logging == null) {
         this.logging = new LoggingSettings();
      }
      return this.logging;
   }

   public void setConfiguring(boolean configuring) {
      this.configuring = configuring;
   }

   public void setLobbyLocation(Location lobbyLocation) {
      this.lobbyLocation = lobbyLocation;
   }

   public void setDatabaseURL(String databaseURL) {
      this.databaseURL = databaseURL;
   }

   public void setServerName(String serverName) {
      this.serverName = serverName;
   }

   public void setLobbyServerName(String lobbyServerName) {
      this.lobbyServerName = lobbyServerName;
   }

   public void setLobbyMode(boolean lobbyMode) {
      this.lobbyMode = lobbyMode;
   }

   public void setModeCW(boolean modeCW) {
      this.modeCW = modeCW;
   }

   public void setActiveClanWarArena(String activeClanWarArena) {
      this.activeClanWarArena = activeClanWarArena;
   }

   public void setClanWarStartTime(long clanWarStartTime) {
      this.clanWarStartTime = clanWarStartTime;
   }

   public void setCoreProtectionRadius(double coreProtectionRadius) {
      this.coreProtectionRadius = coreProtectionRadius;
   }

   public void setArenaPlayers(ArenaPlayers arenaPlayers) {
      this.arenaPlayers = arenaPlayers;
   }

   public void setArenaTeams(ArenaTeams arenaTeams) {
      this.arenaTeams = arenaTeams;
   }

   public void setLogging(LoggingSettings logging) {
      this.logging = logging;
   }

   public static class LoggingSettings {
      private boolean debugMode = false;
      private List<String> enabledCategories = new ArrayList<>();

      public boolean isDebugMode() {
         return debugMode;
      }

      public void setDebugMode(boolean debugMode) {
         this.debugMode = debugMode;
      }

      public List<String> getEnabledCategories() {
         if (this.enabledCategories == null) {
            this.enabledCategories = new ArrayList<>();
         }
         return enabledCategories;
      }

      public void setEnabledCategories(List<String> enabledCategories) {
         this.enabledCategories = enabledCategories;
      }
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof GlobalSettings)) {
         return false;
      } else {
         GlobalSettings other = (GlobalSettings)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.isConfiguring() != other.isConfiguring()) {
            return false;
         } else if (this.isLobbyMode() != other.isLobbyMode()) {
            return false;
         } else if (this.isModeCW() != other.isModeCW()) {
            return false;
         } else if (this.getClanWarStartTime() != other.getClanWarStartTime()) {
            return false;
         } else {
            label61: {
               Object this$lobbyLocation = this.getLobbyLocation();
               Object other$lobbyLocation = other.getLobbyLocation();
               if (this$lobbyLocation == null) {
                  if (other$lobbyLocation == null) {
                     break label61;
                  }
               } else if (this$lobbyLocation.equals(other$lobbyLocation)) {
                  break label61;
               }

               return false;
            }

            label54: {
               Object this$databaseURL = this.getDatabaseURL();
               Object other$databaseURL = other.getDatabaseURL();
               if (this$databaseURL == null) {
                  if (other$databaseURL == null) {
                     break label54;
                  }
               } else if (this$databaseURL.equals(other$databaseURL)) {
                  break label54;
               }

               return false;
            }

            label48: {
               Object this$serverName = this.getServerName();
               Object other$serverName = other.getServerName();
               if (this$serverName == null) {
                  if (other$serverName == null) {
                     break label48;
                  }
               } else if (this$serverName.equals(other$serverName)) {
                  break label48;
               }

               return false;
            }

            label42: {
               Object this$lobbyServerName = this.getLobbyServerName();
               Object other$lobbyServerName = other.getLobbyServerName();
               if (this$lobbyServerName == null) {
                  if (other$lobbyServerName == null) {
                     break label42;
                  }
               } else if (this$lobbyServerName.equals(other$lobbyServerName)) {
                  break label42;
               }

               return false;
            }

            label36: {
               Object this$activeClanWarArena = this.getActiveClanWarArena();
               Object other$activeClanWarArena = other.getActiveClanWarArena();
               if (this$activeClanWarArena == null) {
                  if (other$activeClanWarArena == null) {
                     break label36;
                  }
               } else if (this$activeClanWarArena.equals(other$activeClanWarArena)) {
                  break label36;
               }

               return false;
            }

            Object this$arenaPlayers = this.getArenaPlayers();
            Object other$arenaPlayers = other.getArenaPlayers();
            if (this$arenaPlayers == null) {
               if (other$arenaPlayers != null) {
                  return false;
               }
            } else if (!this$arenaPlayers.equals(other$arenaPlayers)) {
               return false;
            }

            Object this$arenaTeams = this.getArenaTeams();
            Object other$arenaTeams = other.getArenaTeams();
            if (this$arenaTeams == null) {
               if (other$arenaTeams != null) {
                  return false;
               }
            } else if (!this$arenaTeams.equals(other$arenaTeams)) {
               return false;
            }

            Object this$logging = this.getLogging();
            Object other$logging = other.getLogging();
            if (this$logging == null) {
               if (other$logging != null) {
                  return false;
               }
            } else if (!this$logging.equals(other$logging)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof GlobalSettings;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + (this.isConfiguring() ? 79 : 97);
      result = result * PRIME + (this.isLobbyMode() ? 79 : 97);
      result = result * PRIME + (this.isModeCW() ? 79 : 97);
      long $clanWarStartTime = this.getClanWarStartTime();
      result = result * PRIME + (int)($clanWarStartTime >>> 32 ^ $clanWarStartTime);
      Object $lobbyLocation = this.getLobbyLocation();
      result = result * PRIME + ($lobbyLocation == null ? 43 : $lobbyLocation.hashCode());
      Object $databaseURL = this.getDatabaseURL();
      result = result * PRIME + ($databaseURL == null ? 43 : $databaseURL.hashCode());
      Object $serverName = this.getServerName();
      result = result * PRIME + ($serverName == null ? 43 : $serverName.hashCode());
      Object $lobbyServerName = this.getLobbyServerName();
      result = result * PRIME + ($lobbyServerName == null ? 43 : $lobbyServerName.hashCode());
      Object $activeClanWarArena = this.getActiveClanWarArena();
      result = result * PRIME + ($activeClanWarArena == null ? 43 : $activeClanWarArena.hashCode());
      Object $arenaPlayers = this.getArenaPlayers();
      result = result * PRIME + ($arenaPlayers == null ? 43 : $arenaPlayers.hashCode());
      Object $arenaTeams = this.getArenaTeams();
      result = result * PRIME + ($arenaTeams == null ? 43 : $arenaTeams.hashCode());
      Object $logging = this.getLogging();
      result = result * PRIME + ($logging == null ? 43 : $logging.hashCode());
      return result;
   }

   public String toString() {
      return "GlobalSettings(configuring=" + this.isConfiguring() +
              ", lobbyLocation=" + this.getLobbyLocation() +
              ", databaseURL=" + this.getDatabaseURL() +
              ", serverName=" + this.getServerName() +
              ", lobbyServerName=" + this.getLobbyServerName() +
              ", lobbyMode=" + this.isLobbyMode() +
              ", modeCW=" + this.isModeCW() +
              ", activeClanWarArena=" + this.getActiveClanWarArena() +
              ", clanWarStartTime=" + this.getClanWarStartTime() +
              ", coreProtectionRadius=" + this.getCoreProtectionRadius() +
              ", arenaPlayers=" + this.getArenaPlayers() +
              ", arenaTeams=" + this.getArenaTeams() +
              ", logging=" + this.getLogging() + ")";
   }
}