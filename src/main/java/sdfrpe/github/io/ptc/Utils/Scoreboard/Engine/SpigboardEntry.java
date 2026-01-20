package sdfrpe.github.io.ptc.Utils.Scoreboard.Engine;

import com.google.common.base.Splitter;
import java.util.Iterator;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

public class SpigboardEntry {
   private String key;
   private Spigboard spigboard;
   private String name;
   private Team team;
   private Score score;
   private int value;
   private String origName;
   private int count;

   public SpigboardEntry(String key, Spigboard spigboard, int value, String origName, int count) {
      this.key = key;
      this.spigboard = spigboard;
      this.value = value;
      this.origName = origName;
      this.count = count;
   }

   public String getKey() {
      return this.key;
   }

   public Spigboard getSpigboard() {
      return this.spigboard;
   }

   public String getName() {
      return this.name;
   }

   public Team getTeam() {
      return this.team;
   }

   public int getValue() {
      return this.score != null ? (this.value = this.score.getScore()) : this.value;
   }

   public void setValue(int value) {
      if (!this.score.isScoreSet()) {
         this.score.setScore(-1);
      }

      this.score.setScore(value);
   }

   public void update(String newName) {
      int value = this.getValue();
      if (newName.equals(this.origName)) {
         for(int i = 0; i < this.count; ++i) {
            newName = ChatColor.RESET + newName;
         }
      } else if (newName.equals(this.name)) {
         return;
      }

      this.create(newName);
      this.setValue(value);
   }

   void remove() {
      if (this.score != null) {
         this.score.getScoreboard().resetScores(this.score.getEntry());
      }

      if (this.team != null) {
         this.team.unregister();
      }

   }

   private void create(String name) {
      this.name = name;
      this.remove();
      if (name.length() <= 16) {
         int value = this.getValue();
         this.score = this.spigboard.getObjective().getScore(name);
         this.score.setScore(value);
      } else {
         this.team = this.spigboard.getScoreboard().registerNewTeam("spigboard-" + this.spigboard.getTeamId());
         Iterator<String> iterator = Splitter.fixedLength(16).split(name).iterator();
         this.team.setPrefix((String)iterator.next());
         String entry = (String)iterator.next();
         this.score = this.spigboard.getObjective().getScore(entry);
         if (name.length() > 32) {
            this.team.setSuffix((String)iterator.next());
         }

         this.team.addEntry(entry);
      }
   }
}
