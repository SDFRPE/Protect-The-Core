package sdfrpe.github.io.ptc.Utils.Scoreboard;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Scoreboard.Engine.PTCTeams;
import sdfrpe.github.io.ptc.Utils.Scoreboard.Engine.Spigboard;
import sdfrpe.github.io.ptc.Utils.Scoreboard.Engine.SpigboardEntry;
import org.bukkit.ChatColor;
import java.util.Iterator;
import java.util.Map.Entry;

public class PBoard {
   private Spigboard pBoard;
   private PTCTeams PTCTeams;

   public PBoard create(GamePlayer gamePlayer) {
      String displayName;
      if (gamePlayer.getArenaTeam() != null && gamePlayer.getArenaTeam().getColor() != null) {
         displayName = gamePlayer.getArenaTeam().getColor().getChatColor() + gamePlayer.getName();
      } else {
         displayName = ChatColor.GRAY + gamePlayer.getName();
      }

      this.pBoard = new Spigboard(displayName);
      this.PTCTeams = new PTCTeams(this.pBoard);
      this.PTCTeams.updateTeams();
      if (gamePlayer.getPlayer() != null) {
         this.pBoard.add(gamePlayer.getPlayer());
      }

      this.update(gamePlayer, true);
      return this;
   }

   public void update(GamePlayer player, boolean create) {
      String displayName;
      if (player.getArenaTeam() != null && player.getArenaTeam().getColor() != null) {
         displayName = player.getArenaTeam().getColor().getChatColor() + player.getName();
      } else {
         displayName = ChatColor.GRAY + player.getName();
      }
      this.pBoard.setTitle(displayName);

      this.PTCTeams.updateTeams();
      int coins = player.getCoins();
      int domination = player.getDominated().size();
      int kills = player.getLocalStats().getKills();
      int killStreak = player.getLocalStats().getBKillStreak();
      int deaths = player.getLocalStats().getDeaths();

      boolean isModeCW = PTC.getInstance().getGameManager().getGlobalSettings().isModeCW();

      Iterator var8;
      Entry entry;
      TeamColor color;
      ArenaTeam arenaTeam;
      String name;

      if (create) {
         var8 = PTC.getInstance().getGameManager().getGameSettings().getTeamList().entrySet().iterator();

         while(var8.hasNext()) {
            entry = (Entry)var8.next();
            color = (TeamColor)entry.getKey();
            arenaTeam = (ArenaTeam)entry.getValue();
            if (!arenaTeam.isDeathTeam()) {
               String teamDisplayName = color.getName();
               String teamColor = color.getChatColor().toString();

               if (isModeCW) {
                  if (color == TeamColor.BLUE) {
                     teamDisplayName = "AZUL";
                     teamColor = "§9";
                  } else if (color == TeamColor.RED) {
                     teamDisplayName = "ROJO";
                     teamColor = "§c";
                  }
               }

               name = String.format("%s[%s] %s", teamColor, arenaTeam.countPlayers(), teamDisplayName);
               this.pBoard.add(color.name(), name, arenaTeam.getCores());
            }
         }

         this.pBoard.add("coins", Statics.c("&eCoins"), coins);
         this.pBoard.add("domination", Statics.c("&9Dominados"), domination);
         this.pBoard.add("kills", Statics.c("&2Asesinatos"), kills);
         this.pBoard.add("killStreak", Statics.c("&bRacha"), killStreak);
         this.pBoard.add("deaths", Statics.c("&cMuertes"), deaths);
         this.pBoard.add("website", Statics.c("&6&omc.rankedsmc.com"), -2025);
      } else {
         var8 = PTC.getInstance().getGameManager().getGameSettings().getTeamList().entrySet().iterator();

         while(var8.hasNext()) {
            entry = (Entry)var8.next();
            color = (TeamColor)entry.getKey();
            arenaTeam = (ArenaTeam)entry.getValue();
            if (!arenaTeam.isDeathTeam()) {
               String teamDisplayName = color.getName();
               String teamColor = color.getChatColor().toString();

               if (isModeCW) {
                  if (color == TeamColor.BLUE) {
                     teamDisplayName = "AZUL";
                     teamColor = "§9";
                  } else if (color == TeamColor.RED) {
                     teamDisplayName = "ROJO";
                     teamColor = "§c";
                  }
               }

               name = String.format("%s[%s] %s", teamColor, arenaTeam.countPlayers(), teamDisplayName);
               SpigboardEntry boardEntry = this.pBoard.getEntry(color.name());
               if (boardEntry != null) {
                  boardEntry.update(name);
                  boardEntry.setValue(arenaTeam.getCores());
               }
            }
         }

         SpigboardEntry coinsEntry = this.pBoard.getEntry("coins");
         if (coinsEntry != null) coinsEntry.setValue(coins);

         SpigboardEntry dominationEntry = this.pBoard.getEntry("domination");
         if (dominationEntry != null) dominationEntry.setValue(domination);

         SpigboardEntry killsEntry = this.pBoard.getEntry("kills");
         if (killsEntry != null) killsEntry.setValue(kills);

         SpigboardEntry killStreakEntry = this.pBoard.getEntry("killStreak");
         if (killStreakEntry != null) killStreakEntry.setValue(killStreak);

         SpigboardEntry deathsEntry = this.pBoard.getEntry("deaths");
         if (deathsEntry != null) deathsEntry.setValue(deaths);
      }
   }

   public Spigboard getPBoard() {
      return this.pBoard;
   }

   public PTCTeams getPTCTeams() {
      return this.PTCTeams;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PBoard)) {
         return false;
      } else {
         PBoard other = (PBoard)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$pBoard = this.getPBoard();
            Object other$pBoard = other.getPBoard();
            if (this$pBoard == null) {
               if (other$pBoard != null) {
                  return false;
               }
            } else if (!this$pBoard.equals(other$pBoard)) {
               return false;
            }

            Object this$ptcTeams = this.getPTCTeams();
            Object other$ptcTeams = other.getPTCTeams();
            if (this$ptcTeams == null) {
               if (other$ptcTeams != null) {
                  return false;
               }
            } else if (!this$ptcTeams.equals(other$ptcTeams)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof PBoard;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      Object $pBoard = this.getPBoard();
      result = result * PRIME + ($pBoard == null ? 43 : $pBoard.hashCode());
      Object $ptcTeams = this.getPTCTeams();
      result = result * PRIME + ($ptcTeams == null ? 43 : $ptcTeams.hashCode());
      return result;
   }

   public String toString() {
      return "PBoard(pBoard=" + this.getPBoard() + ", PTCTeams=" + this.getPTCTeams() + ")";
   }
}