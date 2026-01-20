package sdfrpe.github.io.ptc.Game.Arena;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.Game.Settings.ArenaSettings;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import java.util.List;
import java.util.Map;

public class Arena {
   private ArenaSettings arenaSettings;
   private Map<TeamColor, ArenaTeam> teamList;
   private List<GamePlayer> votes;

   public Arena(ArenaSettings arenaSettings) {
      this.arenaSettings = arenaSettings;
      this.teamList = Maps.newConcurrentMap();
      this.votes = Lists.newArrayList();
   }

   public String getKey() {
      return this.arenaSettings.getName();
   }

   public ArenaSettings getArenaSettings() {
      return this.arenaSettings;
   }

   public Map<TeamColor, ArenaTeam> getTeamList() {
      return this.teamList;
   }

   public List<GamePlayer> getVotes() {
      return this.votes;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Arena)) {
         return false;
      } else {
         Arena other = (Arena) o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            label47: {
               Object this$arenaSettings = this.getArenaSettings();
               Object other$arenaSettings = other.getArenaSettings();
               if (this$arenaSettings == null) {
                  if (other$arenaSettings == null) {
                     break label47;
                  }
               } else if (this$arenaSettings.equals(other$arenaSettings)) {
                  break label47;
               }

               return false;
            }

            Object this$teamList = this.getTeamList();
            Object other$teamList = other.getTeamList();
            if (this$teamList == null) {
               if (other$teamList != null) {
                  return false;
               }
            } else if (!this$teamList.equals(other$teamList)) {
               return false;
            }

            Object this$votes = this.getVotes();
            Object other$votes = other.getVotes();
            if (this$votes == null) {
               if (other$votes != null) {
                  return false;
               }
            } else if (!this$votes.equals(other$votes)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Arena;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      Object $arenaSettings = this.getArenaSettings();
      result = result * PRIME + ($arenaSettings == null ? 43 : $arenaSettings.hashCode());
      Object $teamList = this.getTeamList();
      result = result * PRIME + ($teamList == null ? 43 : $teamList.hashCode());
      Object $votes = this.getVotes();
      result = result * PRIME + ($votes == null ? 43 : $votes.hashCode());
      return result;
   }

   public String toString() {
      return "Arena(arenaSettings=" + this.getArenaSettings() + ", teamList=" + this.getTeamList() + ", votes=" + this.getVotes() + ")";
   }
}