package sdfrpe.github.io.ptc.Commands;

import sdfrpe.github.io.ptc.PTC;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public abstract class SubCommand {
   protected PTC plugin;
   private String name;
   private String command;
   private String description;
   private String help;
   private String examples;

   protected SubCommand(PTC plugin, String name, String command, String description, String help) {
      this.plugin = plugin;
      this.name = name;
      this.command = command;
      this.description = description;
      this.help = help;
      this.examples = "none";
      if (!Commands.subCommandMap.containsKey(command.toLowerCase())) {
         Commands.subCommandMap.put(command.toLowerCase(), this);
      }

   }

   public abstract boolean onSubCommand(String var1, CommandSender var2, String[] var3);

   protected void sendHelp(CommandSender sender) {
      String format = "&6● &f%s &8|&f %s";
      sender.sendMessage(this.c(String.format(format, this.getHelp(), this.getDescription())));
   }

   protected void sendExamples(CommandSender sender) {
      sender.sendMessage(this.c(this.examples));
   }

   protected String c(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }

   public PTC getPlugin() {
      return this.plugin;
   }

   public String getName() {
      return this.name;
   }

   public String getCommand() {
      return this.command;
   }

   public String getDescription() {
      return this.description;
   }

   public String getHelp() {
      return this.help;
   }

   public String getExamples() {
      return this.examples;
   }

   public void setExamples(String examples) {
      this.examples = examples;
   }
}
