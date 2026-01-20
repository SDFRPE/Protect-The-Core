package sdfrpe.github.io.ptc.Utils.Managers;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Scoreboard.Engine.PTCTeams;
import sdfrpe.github.io.ptc.Utils.Scoreboard.Engine.Spigboard;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import org.bukkit.entity.Player;

public class ScoreManager {
   private Spigboard lobby = new Spigboard(Statics.c("&bVotaciones"));
   private GlobalTabManager globalTabManager;

   public ScoreManager() {
      new PTCTeams(this.lobby);
      this.globalTabManager = GlobalTabManager.getInstance();
      this.startLobby();
   }

   public void startLobby() {
      final ArenaManager arenaManager = PTC.getInstance().getGameManager().getArenaManager();
      (new PTCRunnable() {
         boolean setup = false;

         public void onTick() {
            if (!this.setup) {
               ScoreManager.this.setLines(arenaManager.getVotes(), ScoreManager.this.lobby, false);
               this.setup = true;
            } else {
               if (Statics.gameStatus == GameStatus.CAGE_PHASE || Statics.gameStatus == GameStatus.IN_GAME) {
                  this.cancel();
               } else {
                  ScoreManager.this.setLines(arenaManager.getVotes(), ScoreManager.this.lobby, true);
               }
            }
         }
      }).run();
   }

   public void setLines(Set<Entry<String, Integer>> lines, Spigboard spigboard, boolean update) {
      Iterator<Entry<String, Integer>> iterator = lines.iterator();

      for (int i = 0; i < lines.size(); i++) {
         Entry<String, Integer> entry = iterator.next();
         if (update) {
            spigboard.getEntry(entry.getKey()).setValue(entry.getValue());
         } else {
            spigboard.add(entry.getKey(), entry.getKey(), entry.getValue());
         }
      }
   }

   public void addLobby(Player player) {
      this.lobby.add(player);
   }

   public Spigboard getLobby() {
      return this.lobby;
   }

   public void setLobby(Spigboard lobby) {
      this.lobby = lobby;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ScoreManager)) {
         return false;
      } else {
         ScoreManager other = (ScoreManager) o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$lobby = this.getLobby();
            Object other$lobby = other.getLobby();
            if (this$lobby == null) {
               if (other$lobby != null) {
                  return false;
               }
            } else if (!this$lobby.equals(other$lobby)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ScoreManager;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      Object $lobby = this.getLobby();
      result = result * PRIME + ($lobby == null ? 43 : $lobby.hashCode());
      return result;
   }

   public String toString() {
      return "ScoreManager(lobby=" + this.getLobby() + ")";
   }
}