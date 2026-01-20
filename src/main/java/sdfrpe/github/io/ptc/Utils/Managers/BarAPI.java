package sdfrpe.github.io.ptc.Utils.Managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import net.kyori.adventure.text.Component;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BarAPI {

   public static void sendActionbar(Player player, String message) {
      try {
         WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(Component.text(message));
         PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void broadcastActionbar(String message) {
      Iterator<? extends Player> it = Bukkit.getOnlinePlayers().iterator();
      while (it.hasNext()) {
         Player onlinePlayer = it.next();
         sendActionbar(onlinePlayer, message);
      }
   }
}