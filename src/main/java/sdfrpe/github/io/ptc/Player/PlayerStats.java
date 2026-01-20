package sdfrpe.github.io.ptc.Player;

import org.bukkit.entity.Player;

public class PlayerStats {
   private PlayerLevels playerLevels;
   private int multiplier;
   private long multiplierExpiration;
   private int wins;
   private int kills;
   private int deaths;
   private int cores;
   private int bDomination;
   private int bKillStreak;
   private int coins;
   private int clanLevel;
   private int clanXP;
   private long playtime;
   private long lastSessionEnd;
   private transient boolean loadingExp = false;

   public PlayerStats() {
      this(new PlayerLevels(0, 0.0F, 0), 1, 0L, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0L, 0L);
   }

   public PlayerStats(PlayerLevels playerLevels, int multiplier, long multiplierExpiration, int wins, int kills, int deaths, int cores, int bDomination, int bKillStreak, int coins, int clanLevel, int clanXP) {
      this(playerLevels, multiplier, multiplierExpiration, wins, kills, deaths, cores, bDomination, bKillStreak, coins, clanLevel, clanXP, 0L, 0L);
   }

   public PlayerStats(PlayerLevels playerLevels, int multiplier, long multiplierExpiration, int wins, int kills, int deaths, int cores, int bDomination, int bKillStreak, int coins, int clanLevel, int clanXP, long playtime, long lastSessionEnd) {
      this.playerLevels = playerLevels;
      this.multiplier = multiplier;
      this.multiplierExpiration = multiplierExpiration;
      this.wins = wins;
      this.kills = kills;
      this.deaths = deaths;
      this.cores = cores;
      this.bDomination = bDomination;
      this.bKillStreak = bKillStreak;
      this.coins = coins;
      this.clanLevel = clanLevel;
      this.clanXP = clanXP;
      this.playtime = playtime;
      this.lastSessionEnd = lastSessionEnd;
      this.loadingExp = false;
   }

   public void setExp(Player player) {
      if (loadingExp) {
         return;
      }
      this.getPlayerLevels().setLevel(player.getLevel());
      this.getPlayerLevels().setExpLevel(player.getExp());
      this.getPlayerLevels().setTotalExp(player.getTotalExperience());
   }

   public void applyExp(Player player) {
      loadingExp = true;
      int totalExp = this.getPlayerLevels().getTotalExp();
      player.setTotalExperience(0);
      player.setLevel(0);
      player.setExp(0);
      player.giveExp(totalExp);
      loadingExp = false;
   }

   public void checkMultiplierExpiration() {
      if (this.multiplierExpiration > 0 && System.currentTimeMillis() >= this.multiplierExpiration) {
         this.multiplier = 1;
         this.multiplierExpiration = 0;
      }
   }

   public long getTimeRemaining() {
      if (this.multiplierExpiration == 0 || this.multiplier == 1) {
         return 0;
      }
      long remaining = this.multiplierExpiration - System.currentTimeMillis();
      return remaining > 0 ? remaining : 0;
   }

   public void setMultiplierWithExpiration(int multiplier, long durationMillis) {
      this.multiplier = multiplier;
      this.multiplierExpiration = System.currentTimeMillis() + durationMillis;
   }

   public void removeMultiplier() {
      this.multiplier = 1;
      this.multiplierExpiration = 0;
   }

   public PlayerLevels getPlayerLevels() {
      return this.playerLevels;
   }

   public int getMultiplier() {
      checkMultiplierExpiration();
      return this.multiplier;
   }

   public long getMultiplierExpiration() {
      return this.multiplierExpiration;
   }

   public int getWins() {
      return this.wins;
   }

   public int getKills() {
      return this.kills;
   }

   public int getDeaths() {
      return this.deaths;
   }

   public int getCores() {
      return this.cores;
   }

   public int getBDomination() {
      return this.bDomination;
   }

   public int getBKillStreak() {
      return this.bKillStreak;
   }

   public int getCoins() {
      return this.coins;
   }

   public int getClanLevel() {
      return this.clanLevel;
   }

   public int getClanXP() {
      return this.clanXP;
   }

   public long getPlaytime() {
      return this.playtime;
   }

   public long getLastSessionEnd() {
      return this.lastSessionEnd;
   }

   public void setWins(int wins) {
      this.wins = wins;
   }

   public void setKills(int kills) {
      this.kills = kills;
   }

   public void setDeaths(int deaths) {
      this.deaths = deaths;
   }

   public void setCores(int cores) {
      this.cores = cores;
   }

   public void setBDomination(int bDomination) {
      this.bDomination = bDomination;
   }

   public void setBKillStreak(int bKillStreak) {
      this.bKillStreak = bKillStreak;
   }

   public void setCoins(int coins) {
      this.coins = coins;
   }

   public void setClanLevel(int clanLevel) {
      this.clanLevel = Math.max(1, Math.min(100, clanLevel));
   }

   public void setClanXP(int clanXP) {
      this.clanXP = Math.max(0, clanXP);
   }

   public void setPlaytime(long playtime) {
      this.playtime = Math.max(0, playtime);
   }

   public void setLastSessionEnd(long lastSessionEnd) {
      this.lastSessionEnd = lastSessionEnd;
   }

   public void addPlaytime(long milliseconds) {
      this.playtime = Math.max(0, this.playtime + milliseconds);
   }

   public String getFormattedPlaytime() {
      long seconds = this.playtime / 1000;
      long minutes = seconds / 60;
      long hours = minutes / 60;
      long days = hours / 24;

      if (days > 0) {
         return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
      } else if (hours > 0) {
         return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
      } else if (minutes > 0) {
         return minutes + "m " + (seconds % 60) + "s";
      } else {
         return seconds + "s";
      }
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PlayerStats)) {
         return false;
      } else {
         PlayerStats other = (PlayerStats) o;
         if (!other.canEqual(this)) {
            return false;
         }
         Object this$playerLevels = this.getPlayerLevels();
         Object other$playerLevels = other.getPlayerLevels();
         if (this$playerLevels == null) {
            if (other$playerLevels != null) {
               return false;
            }
         } else if (!this$playerLevels.equals(other$playerLevels)) {
            return false;
         }
         return this.multiplier == other.multiplier &&
                 this.multiplierExpiration == other.multiplierExpiration &&
                 this.wins == other.wins &&
                 this.kills == other.kills &&
                 this.deaths == other.deaths &&
                 this.cores == other.cores &&
                 this.bDomination == other.bDomination &&
                 this.bKillStreak == other.bKillStreak &&
                 this.coins == other.coins &&
                 this.clanLevel == other.clanLevel &&
                 this.clanXP == other.clanXP &&
                 this.playtime == other.playtime &&
                 this.lastSessionEnd == other.lastSessionEnd;
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof PlayerStats;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      Object $playerLevels = this.getPlayerLevels();
      result = result * PRIME + ($playerLevels == null ? 43 : $playerLevels.hashCode());
      result = result * PRIME + this.multiplier;
      long $multiplierExpiration = this.multiplierExpiration;
      result = result * PRIME + (int) ($multiplierExpiration >>> 32 ^ $multiplierExpiration);
      result = result * PRIME + this.wins;
      result = result * PRIME + this.kills;
      result = result * PRIME + this.deaths;
      result = result * PRIME + this.cores;
      result = result * PRIME + this.bDomination;
      result = result * PRIME + this.bKillStreak;
      result = result * PRIME + this.coins;
      result = result * PRIME + this.clanLevel;
      result = result * PRIME + this.clanXP;
      long $playtime = this.playtime;
      result = result * PRIME + (int) ($playtime >>> 32 ^ $playtime);
      long $lastSessionEnd = this.lastSessionEnd;
      result = result * PRIME + (int) ($lastSessionEnd >>> 32 ^ $lastSessionEnd);
      return result;
   }

   public String toString() {
      return "PlayerStats(playerLevels=" + this.getPlayerLevels() +
              ", multiplier=" + this.getMultiplier() +
              ", multiplierExpiration=" + this.getMultiplierExpiration() +
              ", wins=" + this.getWins() +
              ", kills=" + this.getKills() +
              ", deaths=" + this.getDeaths() +
              ", cores=" + this.cores +
              ", bDomination=" + this.getBDomination() +
              ", bKillStreak=" + this.getBKillStreak() +
              ", coins=" + this.getCoins() +
              ", clanLevel=" + this.getClanLevel() +
              ", clanXP=" + this.getClanXP() +
              ", playtime=" + this.getPlaytime() +
              ", lastSessionEnd=" + this.getLastSessionEnd() + ")";
   }
}