package sdfrpe.github.io.ptc.Commands.cmds;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Game.Arena.GameSettings;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import org.bukkit.command.CommandSender;

public class SetCore extends SubCommand {
   public SetCore(PTC ptc) {
      super(ptc, "Set Cores", "setCores", "Set cores to a team.", "/ptc setCores (teamColor) (cores)");
   }

   public boolean onSubCommand(String command, CommandSender sender, String[] args) {
      if (args.length < 3) {
         this.sendHelp(sender);
         return false;
      } else {
         String team = args[1].toUpperCase();
         int cores = Math.max(0, Integer.parseInt(args[2]));
         GameSettings gameSettings = this.plugin.getGameManager().getGameSettings();
         if (team.equalsIgnoreCase("all")) {
            gameSettings.getTeamList().forEach((color, arenaTeamx) -> {
               arenaTeamx.setCores(cores);
               if (cores == 0) {
                  arenaTeamx.setDeathTeam(true);
               }
            });
            return false;
         } else {
            ArenaTeam arenaTeam = gameSettings.getTeamList().getOrDefault(TeamColor.valueOf(team), null);
            if (arenaTeam == null) {
               sender.sendMessage(this.c(String.format("&cEl team &4%s&c no existe.", team)));
               return false;
            } else {
               arenaTeam.setCores(cores);
               if (cores == 0) {
                  arenaTeam.setDeathTeam(true);
               }

               return false;
            }
         }
      }
   }
}