package sdfrpe.github.io.ptc.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ReflectionUtils {
   private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

   public static Class<?> getNMSClass(String classname) {
      try {
         return Class.forName("net.minecraft.server." + VERSION + "." + classname);
      } catch (ClassNotFoundException e) {
         throw new RuntimeException("Failed to find NMS class: " + classname, e);
      }
   }

   public static void sendPacket(Player player, Object packet) {
      try {
         Object handle = player.getClass().getMethod("getHandle").invoke(player);
         Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
         playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
      } catch (Exception e) {
         throw new RuntimeException("Failed to send packet to " + player.getName(), e);
      }
   }

   public static Object getConnection(Player player) {
      try {
         Method getHandle = player.getClass().getMethod("getHandle");
         Object nmsPlayer = getHandle.invoke(player);
         Field conField = nmsPlayer.getClass().getField("playerConnection");
         return conField.get(nmsPlayer);
      } catch (Exception e) {
         throw new RuntimeException("Failed to get connection for " + player.getName(), e);
      }
   }

   public static int getPing(Player player) {
      try {
         Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftPlayer");
         Object craftPlayer = craftPlayerClass.cast(player);
         Method getHandle = craftPlayerClass.getMethod("getHandle");
         Object entityPlayer = getHandle.invoke(craftPlayer);
         Field pingField = entityPlayer.getClass().getDeclaredField("ping");
         return pingField.getInt(entityPlayer);
      } catch (Exception e) {
         e.printStackTrace();
         return 0;
      }
   }
}