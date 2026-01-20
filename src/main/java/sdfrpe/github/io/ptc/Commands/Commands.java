package sdfrpe.github.io.ptc.Commands;

import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.Commands.cmds.*;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Extra.GlobalSettings;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {
   public static Map<String, SubCommand> subCommandMap = Maps.newHashMap();
   private final PTC plugin;

   public Commands(PTC plugin) {
      this.plugin = plugin;

      new SetLobbyServer(plugin);
      new SetServerName(plugin);
      new OpenArenaSelector(plugin);
      new OpenCWArenaSelector(plugin);
      new TopCommand(plugin);

      if (plugin.getGameManager().getGlobalSettings().isConfiguring()) {
         new SetLobby(plugin);
         new CreateArena(plugin);
      } else {
         new SetLobby(plugin);
         new SetCore(plugin);
         new ForceStart(plugin);
         new CWTest(plugin);
         new AddCores(plugin);
         new ModifyTime(plugin);
         new ResetArena(plugin);
      }
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      try {
         if (!sender.hasPermission("ptc.admin")) {
            sender.sendMessage(this.c("&cNecesitas el permiso &6ptc.admin &cpara usar este comando."));
            return false;
         } else {
            if (cmd.getName().equalsIgnoreCase("ptc") || cmd.getName().equalsIgnoreCase("admin")) {
               if (args.length < 1) {
                  this.sendHelpMSG(sender);
                  return true;
               }

               String subcmd = args[0].toLowerCase();

               if (subcmd.equals("cwmode")) {
                  return this.handleCWMode(sender, args);
               }

               if (subcmd.equals("cwsync")) {
                  return this.handleCWSync(sender);
               }

               this.execute(sender, subcmd, args);
            }

            return false;
         }
      } catch (Throwable var6) {
         throw var6;
      }
   }

   private boolean handleCWMode(CommandSender sender, String[] args) {
      GlobalSettings settings = plugin.getGameManager().getGlobalSettings();

      if (args.length == 1) {
         boolean newState = !settings.isModeCW();
         settings.setModeCW(newState);

         if (!newState) {
            settings.setActiveClanWarArena(null);
            settings.setClanWarStartTime(0);
         }

         plugin.getGameManager().saveGlobalSettings();

         sender.sendMessage(this.c("&6═══════════════════════════════════"));
         sender.sendMessage(this.c("&eModo CW: " + (newState ?
                 "&a&lACTIVADO" : "&c&lDESACTIVADO")));
         if (newState && settings.getActiveClanWarArena() != null) {
            sender.sendMessage(this.c("&eArena: &f" + settings.getActiveClanWarArena()));
         }
         sender.sendMessage(this.c("&6═══════════════════════════════════"));
         return true;
      }

      if (args.length == 2) {
         String arenaName = args[1];
         settings.setModeCW(true);
         settings.setActiveClanWarArena(arenaName);
         settings.setClanWarStartTime(System.currentTimeMillis());
         plugin.getGameManager().saveGlobalSettings();

         sender.sendMessage(this.c("&6═══════════════════════════════════"));
         sender.sendMessage(this.c("&eModo CW: &a&lACTIVADO"));
         sender.sendMessage(this.c("&eArena forzada: &f" + arenaName));
         sender.sendMessage(this.c("&6═══════════════════════════════════"));
         return true;
      }

      sender.sendMessage(this.c("&cUso: /ptc cwmode [arena]"));
      sender.sendMessage(this.c("&7Sin arena: alterna on/off"));
      sender.sendMessage(this.c("&7Con arena: activa y fuerza arena específica"));
      return true;
   }

   private boolean handleCWSync(CommandSender sender) {
      if (!plugin.getGameManager().getGlobalSettings().isModeCW()) {
         sender.sendMessage(this.c("&cEste comando solo funciona en modo CW."));
         return true;
      }

      sender.sendMessage(this.c("&eSincronizando guerra desde PTCClans..."));

      try {
         Object adapter = plugin.getGameManager().getClanWarAdapter();
         if (adapter == null) {
            sender.sendMessage(this.c("&c✖ ClanWarAdapter no disponible."));
            return false;
         }

         adapter.getClass().getMethod("syncWarFromPTCClans").invoke(adapter);
         sender.sendMessage(this.c("&a✓ Guerra sincronizada correctamente."));
         return true;
      } catch (Exception e) {
         sender.sendMessage(this.c("&c✖ Error sincronizando: " + e.getMessage()));
         e.printStackTrace();
         return false;
      }
   }

   public void execute(CommandSender sender, String subCmd, String[] args) {
      if (!subCommandMap.containsKey(subCmd)) {
         this.sendHelpMSG(sender);
      } else {
         ((SubCommand)subCommandMap.get(subCmd)).onSubCommand(subCmd, sender, args);
      }
   }

   private void sendHelpMSG(CommandSender p) {
      p.sendMessage(this.c("&6═══════════════════════════════════"));
      p.sendMessage(this.c("&e&lPTC v" + this.plugin.getDescription().getVersion()));
      p.sendMessage(this.c("&6═══════════════════════════════════"));

      subCommandMap.values().forEach((subCommand) -> {
         String format = "&2➜ &f%s &8|&7 %s";
         p.sendMessage(this.c(String.format(format, subCommand.getHelp(), subCommand.getDescription())));
      });

      p.sendMessage(this.c("&6─────────────────────────────────"));
      p.sendMessage(this.c("&e&lCOMANDOS ESPECIALES:"));
      p.sendMessage(this.c("&2➜ &f/ptc cwmode [arena] &8|&7 Gestionar modo Clan War"));
      p.sendMessage(this.c("&2➜ &f/ptc cwsync &8|&7 Sincronizar guerra manualmente"));
      p.sendMessage(this.c("&6═══════════════════════════════════"));
   }

   protected String c(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }
}