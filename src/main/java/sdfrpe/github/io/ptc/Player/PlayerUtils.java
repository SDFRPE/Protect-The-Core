package sdfrpe.github.io.ptc.Player;

import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.ReflectionUtils;
import sdfrpe.github.io.ptc.Utils.Location;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public class PlayerUtils {
   public static final ItemStack[] emptyInventory = new ItemStack[36];
   public static final ItemStack[] emptyArmor = new ItemStack[4];

   public static void clean(Player p, PotionEffect... potionEffects) {
      clean(p, true, potionEffects);
   }

   public static void clean(Player p, boolean resetGameMode, PotionEffect... potionEffects) {
      if (resetGameMode) {
         p.setGameMode(GameMode.SURVIVAL);
      }

      p.setFoodLevel(20);
      p.setExhaustion(20.0F);
      p.setSaturation(20.0F);
      p.setHealth(p.getMaxHealth());
      p.setFireTicks(0);
      p.getInventory().setContents(emptyInventory);
      p.getInventory().setArmorContents(emptyArmor);

      if (!p.getActivePotionEffects().isEmpty()) {
         p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
      }

      if (potionEffects.length != 0) {
         Arrays.stream(potionEffects).forEach(p::addPotionEffect);
      }
   }

   public static void teleport(Player player, org.bukkit.Location spawnLocation, org.bukkit.Location centerView) {
      Vector dirBetweenLocations = centerView.toVector().subtract(spawnLocation.toVector());
      org.bukkit.Location newLocation = spawnLocation.clone();
      newLocation.setDirection(dirBetweenLocations);
      newLocation.setPitch(0.0F);
      player.teleport(newLocation);
   }

   public static void fakeSpectator(Player player) {
      try {
         Class<?> packetClass = ReflectionUtils.getNMSClass("PacketPlayOutGameStateChange");
         Constructor<?> gameStateConstructor = packetClass.getConstructor(int.class, float.class);
         Object packetInstance = gameStateConstructor.newInstance(3, 3);
         ReflectionUtils.sendPacket(player, packetInstance);
      } catch (NoSuchMethodException e) {
         LogSystem.error(LogCategory.CORE, "Error creando fake spectator:", e.getMessage());
      } catch (Exception e) {
         LogSystem.error(LogCategory.CORE, "Error creando fake spectator:", e.getMessage());
      }
   }

   public static Location fixTeleport(Location sSpawnLocation, Location sCenterView) {
      if (sSpawnLocation == null) {
         LogSystem.error(LogCategory.CORE, "Spawn location null en fixTeleport");
         return null;
      }

      if (sCenterView == null) {
         LogSystem.error(LogCategory.CORE, "Center view null en fixTeleport");
         return sSpawnLocation;
      }

      org.bukkit.Location spawnLocation = sSpawnLocation.getLocation();
      org.bukkit.Location centerView = sCenterView.getLocation();

      if (spawnLocation == null || centerView == null) {
         LogSystem.error(LogCategory.CORE, "Conversion de location falló en fixTeleport");
         return sSpawnLocation;
      }

      Vector dirBetweenLocations = centerView.toVector().subtract(spawnLocation.toVector());
      org.bukkit.Location newLocation = spawnLocation.clone();
      newLocation.setDirection(dirBetweenLocations);
      newLocation.setPitch(0.0F);
      return new Location(newLocation);
   }
}