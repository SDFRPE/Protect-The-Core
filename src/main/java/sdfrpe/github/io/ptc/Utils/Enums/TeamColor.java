package sdfrpe.github.io.ptc.Utils.Enums;

import org.bukkit.ChatColor;

public enum TeamColor {
   RED("Rojo", ChatColor.DARK_RED),
   BLUE("Azul", ChatColor.DARK_BLUE),
   GREEN("Verde", ChatColor.DARK_GREEN),
   YELLOW("Amarillo", ChatColor.GOLD),
   SPECTATOR("Espectador", ChatColor.GRAY),
   LOBBY("Lobby", ChatColor.AQUA);

   private final String name;
   private final ChatColor chatColor;

   private TeamColor(String name, ChatColor chatColor) {
      this.name = name;
      this.chatColor = chatColor;
   }

   public String getColoredName() {
      return this.chatColor + this.name;
   }

   public String getName() {
      return this.name;
   }

   public ChatColor getChatColor() {
      return this.chatColor;
   }
}
