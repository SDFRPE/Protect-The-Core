package sdfrpe.github.io.ptc.Commands.cmds;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLobby extends SubCommand {
   public SetLobby(PTC plugin) {
      super(plugin, "Set Lobby", "setLobby", "Set the waiting lobby.", "/ptc setLobby");
   }

   public boolean onSubCommand(String command, CommandSender sender, String[] args) {
      if (sender instanceof Player) {
         Player player = (Player)sender;
         Location loc = new Location(player.getLocation());

         this.plugin.getGameManager().getGlobalSettings().setLobbyLocation(loc);
         this.plugin.getGameManager().saveGlobalSettings();

         Console.debug("Lobby location set to: " + loc.toString());
         Console.debug("Saving GlobalSettings to: " + this.plugin.getGameManager().getSettingsFile().getAbsolutePath());

         sender.sendMessage(this.c("&aLobby guardado en: &f" + loc.toString()));
         sender.sendMessage(this.c("&7Reinicia el servidor para aplicar los cambios."));
      } else {
         sender.sendMessage("Need to be a player bro :O");
      }

      return false;
   }
}