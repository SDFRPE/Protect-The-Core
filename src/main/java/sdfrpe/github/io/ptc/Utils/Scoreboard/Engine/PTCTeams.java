package sdfrpe.github.io.ptc.Utils.Scoreboard.Engine;

import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Team;

public class PTCTeams {
   private final Spigboard spigboard;
   private final Map<TeamColor, Team> teamMap;

   public PTCTeams(Spigboard spigboard) {
      this.spigboard = spigboard;
      this.teamMap = Maps.newHashMap();
      this.createTeam(TeamColor.SPECTATOR, true);
      this.createTeam(TeamColor.LOBBY, false);
   }

   public void updateTeams() {
      this.verifySpectator();
      PTC.getInstance().getGameManager().getGameSettings().getTeamList().forEach((color, arenaTeam) -> {
         this.verifyTeam(arenaTeam);
      });
   }

   private void verifySpectator() {
      ArenaTeam arenaTeam = PTC.getInstance().getGameManager().getGameSettings().getSpectatorTeam();
      this.verifyTeam(arenaTeam);
   }

   private void verifyTeam(ArenaTeam arenaTeam) {
      Team team = this.createTeam(arenaTeam.getColor(), false);
      Iterator var3 = arenaTeam.getTeamPlayers().iterator();

      while(var3.hasNext()) {
         GamePlayer teamPlayer = (GamePlayer)var3.next();
         if (teamPlayer.getPlayer() != null) {
            OfflinePlayer player = teamPlayer.getPlayer();
            if (!team.hasPlayer(player)) {
               team.addPlayer(player);
            }
         }
      }

   }

   public Team createTeam(TeamColor color, boolean spectator) {
      if (this.spigboard.getScoreboard().getTeam(color.name()) != null) {
         return this.spigboard.getScoreboard().getTeam(color.name());
      } else {
         Team team = this.spigboard.getScoreboard().registerNewTeam(color.name());

         String prefix = color.getChatColor() + "";

         try {
            Object settings = PTC.getInstance().getGameManager().getGameSettings();
            if (settings != null) {
               java.lang.reflect.Method isModeCWMethod = settings.getClass().getMethod("isModeCW");
               Boolean isModeCW = (Boolean) isModeCWMethod.invoke(settings);

               if (isModeCW != null && isModeCW) {
                  if (color == TeamColor.BLUE) {
                     prefix = "§9";
                  } else if (color == TeamColor.RED) {
                     prefix = "§c";
                  }
               }
            }
         } catch (Exception e) {
         }

         team.setPrefix(prefix);
         team.setAllowFriendlyFire(false);
         team.setCanSeeFriendlyInvisibles(true);

         if (spectator) {
            team.setNameTagVisibility(NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
         }

         this.teamMap.put(color, team);
         return team;
      }
   }
}