package sdfrpe.github.io.ptc.Utils.Managers;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.Arena;
import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.Utils.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

public class WorldManager {
   private static final int UNLOAD_MAX_ATTEMPTS = 40;
   private static final long UNLOAD_WAIT_MS = 500;

   public void importWorlds() {
      File worldContainer = PTC.getInstance().getServer().getWorldContainer();
      File backup = new File(worldContainer.getAbsolutePath() + "_Bakups");

      if (!backup.exists() || !backup.isDirectory()) {
         Console.debug("Backup directory does not exist. Creating initial backups...");
         this.cloneWorlds();
         return;
      }

      Console.debug("Importing worlds from backup: " + backup.getAbsolutePath());

      File[] backupWorlds = backup.listFiles();
      if (backupWorlds != null) {
         for (File backupWorld : backupWorlds) {
            if (backupWorld.isDirectory() && new File(backupWorld, "level.dat").exists()) {
               String worldName = backupWorld.getName();
               File targetWorld = new File(worldContainer, worldName);

               if (!targetWorld.exists() || !new File(targetWorld, "level.dat").exists()) {
                  Console.debug("Importing world from backup: " + worldName);
                  this.copyDir(backupWorld, targetWorld);
               }
            }
         }
      }
   }

   public void cloneWorlds() {
      File worldContainer = PTC.getInstance().getServer().getWorldContainer();
      File backup = new File(worldContainer.getAbsolutePath() + "_Bakups");

      try {
         if (!backup.exists()) {
            if (!backup.mkdirs()) {
               Console.error("Failed to create backup directory: " + backup.getAbsolutePath());
               return;
            }
         }

         Console.debug("Cloning worlds to backup: " + backup.getAbsolutePath());

         File[] worlds = worldContainer.listFiles();
         if (worlds != null) {
            for (File world : worlds) {
               if (world.isDirectory() &&
                       !world.getName().endsWith("_Bakups") &&
                       !world.getName().equals("plugins") &&
                       !world.getName().equals("logs") &&
                       new File(world, "level.dat").exists()) {

                  File targetWorld = new File(backup, world.getName());
                  Console.debug("Cloning world: " + world.getName());
                  this.copyDir(world, targetWorld);
               }
            }
         }

         Console.debug("World cloning completed");
      } catch (Exception e) {
         Console.error("Error cloning worlds: " + e.getMessage());
         e.printStackTrace();
      }
   }

   public void restoreWorld(String worldName) {
      try {
         Console.debug("Restaurando mundo: " + worldName);

         File worldContainer = PTC.getInstance().getServer().getWorldContainer();
         File backup = new File(worldContainer.getAbsolutePath() + "_Bakups");

         if (!backup.exists()) {
            Console.error("El directorio de respaldo no existe: " + backup.getAbsolutePath());
            return;
         }

         File backupWorld = new File(backup, worldName);
         if (!backupWorld.exists()) {
            Console.error("El mundo de respaldo no existe: " + backupWorld.getAbsolutePath());
            return;
         }

         World world = Bukkit.getWorld(worldName);
         if (world != null) {
            Console.debug("Teleportando jugadores fuera del mundo antes de descargarlo");
            for (Player player : world.getPlayers()) {
               World spawnWorld = Bukkit.getWorlds().get(0);
               if (spawnWorld != null && !spawnWorld.getName().equals(worldName)) {
                  player.teleport(spawnWorld.getSpawnLocation());
                  Console.debug("Jugador " + player.getName() + " teleportado fuera de " + worldName);
               }
            }

            Console.debug("Guardando mundo antes de descargar: " + worldName);
            world.save();

            Console.debug("Descargando mundo: " + worldName);
            if (!Bukkit.unloadWorld(world, false)) {
               Console.error("unloadWorld() retornó false para: " + worldName);
            }

            if (!waitForWorldUnload(worldName, UNLOAD_MAX_ATTEMPTS)) {
               Console.error("CRÍTICO: Mundo no se descargó después de " + (UNLOAD_MAX_ATTEMPTS * UNLOAD_WAIT_MS / 1000) + "s: " + worldName);
               Console.error("ABORTANDO restauración para prevenir corrupción");
               return;
            }
         }

         File currentWorld = new File(worldContainer, worldName);
         if (currentWorld.exists()) {
            Console.debug("Eliminando mundo actual: " + worldName);
            deleteDirectory(currentWorld);
         }

         Console.debug("Copiando desde respaldo al mundo actual");
         this.copyDir(backupWorld, currentWorld);

         this.cleanWorldLocks(currentWorld, worldName);

         Console.debug("Mundo restaurado exitosamente: " + worldName);
      } catch (Exception e) {
         Console.error("Error al restaurar mundo: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private boolean waitForWorldUnload(String worldName, int maxAttempts) {
      for (int i = 0; i < maxAttempts; i++) {
         if (Bukkit.getWorld(worldName) == null) {
            Console.debug("Mundo descargado exitosamente en intento " + (i + 1) + ": " + worldName);
            return true;
         }

         if (i == 5 || i == 10 || i == 15 || i == 20 || i == 30) {
            Console.debug("Aún esperando descarga de mundo (intento " + (i + 1) + "/" + maxAttempts + "): " + worldName);
         }

         try {
            Thread.sleep(UNLOAD_WAIT_MS);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Console.error("Interrupción durante espera de descarga: " + worldName);
            return false;
         }
      }
      Console.error("Timeout esperando descarga de mundo: " + worldName);
      return false;
   }

   private void cleanWorldLocks(File worldFolder, String worldName) {
      File sessionLock = new File(worldFolder, "session.lock");
      if (sessionLock.exists()) {
         if (sessionLock.delete()) {
            Console.debug("Eliminado session.lock de: " + worldName);
         }
      }

      File uidDat = new File(worldFolder, "uid.dat");
      if (uidDat.exists()) {
         if (uidDat.delete()) {
            Console.debug("Eliminado uid.dat de: " + worldName);
         }
      }

      String[] dimensions = {"_nether", "_the_end"};
      for (String dimension : dimensions) {
         File dimFolder = new File(worldFolder.getParent(), worldName + dimension);
         if (dimFolder.exists() && dimFolder.isDirectory()) {
            File dimSessionLock = new File(dimFolder, "session.lock");
            if (dimSessionLock.exists()) {
               if (dimSessionLock.delete()) {
                  Console.debug("Eliminado session.lock de: " + worldName + dimension);
               }
            }
            File dimUidDat = new File(dimFolder, "uid.dat");
            if (dimUidDat.exists()) {
               if (dimUidDat.delete()) {
                  Console.debug("Eliminado uid.dat de: " + worldName + dimension);
               }
            }
         }
      }
   }

   private void deleteDirectory(File directory) {
      if (!directory.exists()) {
         return;
      }

      File[] files = directory.listFiles();
      if (files != null) {
         for (File file : files) {
            if (file.isDirectory()) {
               deleteDirectory(file);
            } else {
               file.delete();
            }
         }
      }
      directory.delete();
   }

   public void restoreArenas() {
      Console.debug("Restoring arenas from ArenaManager");

      GameManager gameManager = PTC.getInstance().getGameManager();
      if (gameManager.getArenaManager() == null) {
         Console.error("ArenaManager is null, cannot restore arenas");
         return;
      }

      Map<String, Arena> arenas = gameManager.getArenaManager().getArenas();
      if (arenas == null || arenas.isEmpty()) {
         Console.debug("No arenas to restore");
         return;
      }

      File worldContainer = PTC.getInstance().getServer().getWorldContainer();
      File backup = new File(worldContainer.getAbsolutePath() + "_Bakups");

      if (!backup.exists()) {
         Console.error("Backup directory does not exist");
         return;
      }

      for (String arenaName : arenas.keySet()) {
         File backupWorld = new File(backup, arenaName);
         if (backupWorld.exists() && new File(backupWorld, "level.dat").exists()) {
            Console.debug("Restoring arena: " + arenaName);
            restoreWorld(arenaName);
         } else {
            Console.warning("Backup for arena " + arenaName + " does not exist");
         }
      }

      Console.debug("Arena restoration completed");
   }

   public void loadWorld(String name) {
      this.createEmptyWorld(name);
   }

   public void deleteWorlds() {
      try {
         PTC.getInstance().getServer().getWorldContainer().delete();
      } catch (Exception var2) {
         Console.error(var2.getMessage());
      }
   }

   public World createEmptyWorld(String name) {
      WorldCreator wc = new WorldCreator(name);
      wc.type(WorldType.FLAT);
      wc.generatorSettings("2;0;1;");
      World world = wc.createWorld();
      world.setSpawnLocation(0, 30, 0);
      world.setGameRuleValue("doMobSpawning", "false");
      return world;
   }

   private void copyDir(File source, File target) {
      this.copyDir(source, target, 0);
   }

   private void copyDir(File source, File target, int depth) {
      try {
         final int MAX_DEPTH = 20;
         if (depth > MAX_DEPTH) {
            Console.error("Maximum recursion depth reached at: " + source.getAbsolutePath());
            return;
         }

         if (!source.exists()) {
            Console.error("Source does not exist: " + source.getAbsolutePath());
            return;
         }

         String sourceCanonical = source.getCanonicalPath();
         String targetCanonical = target.getCanonicalPath();

         if (sourceCanonical.equals(targetCanonical)) {
            Console.error("Cannot copy directory to itself: " + sourceCanonical);
            return;
         }

         if (targetCanonical.startsWith(sourceCanonical + File.separator)) {
            Console.error("Cannot copy parent directory into child: " + sourceCanonical + " -> " + targetCanonical);
            return;
         }

         if (sourceCanonical.startsWith(targetCanonical + File.separator)) {
            Console.error("Cannot copy child directory into parent: " + sourceCanonical + " -> " + targetCanonical);
            return;
         }

         if (source.isDirectory()) {
            if (!target.exists()) {
               if (!target.mkdirs()) {
                  Console.error("Failed to create directory: " + target.getAbsolutePath());
                  return;
               }
            }

            String[] files = source.list();

            if (files == null) {
               Console.error("Cannot list files in directory: " + source.getAbsolutePath());
               return;
            }

            for (String file : files) {
               if (file.equals("uid.dat") ||
                       file.equals("session.lock") ||
                       file.endsWith("_Bakups") ||
                       file.equals("plugins") ||
                       file.equals("logs")) {
                  continue;
               }

               File srcFile = new File(source, file);
               File destFile = new File(target, file);

               this.copyDir(srcFile, destFile, depth + 1);
            }
         } else {
            this.copyFile(source, target);
         }
      } catch (IOException e) {
         Console.error("IO Error copying " + source.getAbsolutePath() + " to " + target.getAbsolutePath() + ": " + e.getMessage());
      } catch (Exception e) {
         Console.error("Error copying " + source.getAbsolutePath() + " to " + target.getAbsolutePath() + ": " + e.getMessage());
      }
   }

   private void copyFile(File source, File target) {
      try {
         if (target.getParentFile() != null && !target.getParentFile().exists()) {
            target.getParentFile().mkdirs();
         }

         InputStream in = new FileInputStream(source);
         OutputStream out = new FileOutputStream(target);
         byte[] buffer = new byte[1024];
         int length;

         while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
         }

         in.close();
         out.close();
      } catch (IOException e) {
         Console.error("Error copying file: " + e.getMessage());
      }
   }

   public Map<String, Arena> getArenas() {
      return PTC.getInstance().getGameManager().getArenaManager().getArenas();
   }
}