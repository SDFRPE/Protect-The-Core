package sdfrpe.github.io.ptc.Utils.Scoreboard.Engine;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class Spigboard {
   private Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
   private Objective objective;
   private BiMap<String, SpigboardEntry> entries;
   private int teamId;

   public Spigboard(String title) {
      this.objective = this.scoreboard.registerNewObjective("spigobjective", "dummy");
      this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
      this.setTitle(title);
      this.entries = HashBiMap.create();
      this.teamId = 1;
   }

   public Scoreboard getScoreboard() {
      return this.scoreboard;
   }

   public Objective getObjective() {
      return this.objective;
   }

   public void setTitle(String title) {
      this.objective.setDisplayName(title);
   }

   public SpigboardEntry getEntry(String key) {
      return (SpigboardEntry)this.entries.get(key);
   }

   public SpigboardEntry add(String name, int value) {
      return this.add((String)null, name, value, true);
   }

   public SpigboardEntry add(Enum key, String name, int value) {
      return this.add(key.name(), name, value);
   }

   public SpigboardEntry add(String key, String name, int value) {
      return this.add(key, name, value, false);
   }

   public SpigboardEntry add(Enum key, String name, int value, boolean overwrite) {
      return this.add(key.name(), name, value, overwrite);
   }

   public SpigboardEntry add(String key, String name, int value, boolean overwrite) {
      if (key == null && !this.contains(name)) {
         throw new IllegalArgumentException("Entry could not be found with the supplied name and no key was supplied");
      } else if (overwrite && this.contains(name)) {
         SpigboardEntry entry = this.getEntryByName(name);
         if (key != null && this.entries.get(key) != entry) {
            throw new IllegalArgumentException("Supplied key references a different score than the one to be overwritten");
         } else {
            entry.setValue(value);
            return entry;
         }
      } else if (this.entries.get(key) != null) {
         throw new IllegalArgumentException("Score already exists with that key");
      } else {
         int count = 0;
         if (!overwrite) {
            Map<Integer, String> created = this.create(name);

            Entry entry;
            for(Iterator var8 = created.entrySet().iterator(); var8.hasNext(); name = (String)entry.getValue()) {
               entry = (Entry)var8.next();
               count = (Integer)entry.getKey();
            }
         }

         SpigboardEntry entry = new SpigboardEntry(key, this, value, name, count);
         entry.update(name);
         this.entries.put(key, entry);
         return entry;
      }
   }

   public void remove(String key) {
      this.remove(this.getEntry(key));
   }

   public void remove(SpigboardEntry entry) {
      if (entry.getSpigboard() != this) {
         throw new IllegalArgumentException("Supplied entry does not belong to this Spigboard");
      } else {
         String key = (String)this.entries.inverse().get(entry);
         if (key != null) {
            this.entries.remove(key);
         }

         entry.remove();
      }
   }

   private Map<Integer, String> create(String name) {
      int count;
      for(count = 0; this.contains(name); ++count) {
         name = ChatColor.RESET + name;
      }

      if (name.length() > 48) {
         name = name.substring(0, 47);
      }

      if (this.contains(name)) {
         throw new IllegalArgumentException("Could not find a suitable replacement name for '" + name + "'");
      } else {
         Map<Integer, String> created = new HashMap();
         created.put(count, name);
         return created;
      }
   }

   public int getTeamId() {
      return this.teamId++;
   }

   public SpigboardEntry getEntryByName(String name) {
      Iterator var2 = this.entries.values().iterator();

      SpigboardEntry entry;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         entry = (SpigboardEntry)var2.next();
      } while(!entry.getName().equals(name));

      return entry;
   }

   public boolean contains(String name) {
      Iterator var2 = this.entries.values().iterator();

      SpigboardEntry entry;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         entry = (SpigboardEntry)var2.next();
      } while(!entry.getName().equals(name));

      return true;
   }

   public void add(Player player) {
      player.setScoreboard(this.scoreboard);
   }
}
