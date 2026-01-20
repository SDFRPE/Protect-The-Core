package sdfrpe.github.io.ptc.Commands.cmds;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Game.Settings.ArenaSettings;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateArena extends SubCommand {
   public CreateArena(PTC plugin) {
      super(plugin, "Create Arena", "create", "Create new arena.", "/ptc create (arenaName)");
   }

   public boolean onSubCommand(String command, CommandSender sender, String[] args) {
      if (args.length < 2) {
         this.sendHelp(sender);
         return false;
      } else if (!(sender instanceof Player)) {
         return false;
      } else {
         Player player = (Player)sender;
         GamePlayer gamePlayer = this.plugin.getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
         if (gamePlayer.getArenaSettings() != null) {
            sender.sendMessage(this.c(String.format("&cPor favor termine de configurar la arena %s", gamePlayer.getArenaSettings().getName())));
            return false;
         } else {
            String name = args[1];
            ArenaSettings arenaSettings = new ArenaSettings();
            arenaSettings.setName(name);
            gamePlayer.setArenaSettings(arenaSettings);
            World world = this.plugin.getGameManager().getWorldManager().createEmptyWorld(name);
            player.setFlying(true);
            player.teleport(world.getSpawnLocation());
            this.plugin.getInventories().giveSetup(player, name);
            sender.sendMessage(this.c(String.format("Se ha creado correctamente la arena: %s", name)));
            return false;
         }
      }
   }
}
