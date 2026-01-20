package sdfrpe.github.io.ptc.Tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Interfaces.Tick;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.scheduler.BukkitRunnable;

public class BossTask extends BukkitRunnable {
   private static final List<Tick> bossTasks = new CopyOnWriteArrayList<>();
   private final Map<Class<?>, Long> runnableTimings = Maps.newHashMap();
   private volatile boolean running = true;

   public static List<Tick> getBossTasks() {
      return bossTasks;
   }

   public void run() {
      Console.log("Started: BossTask (BukkitRunnable)");

      try {
         while(running && !Thread.currentThread().isInterrupted()) {
            long startG = System.currentTimeMillis();

            for (Tick bossBar : new ArrayList<>(bossTasks)) {
               try {
                  long start = System.currentTimeMillis();
                  bossBar.onTick();
                  long end = System.currentTimeMillis();
                  long elapsed = end - start;
                  this.runnableTimings.put(bossBar.getClass(), elapsed);
               } catch (Exception e) {
                  Console.error("Error in BossTask tick: " + e.getMessage());
                  e.printStackTrace();
               }
            }

            long endG = System.currentTimeMillis();
            long totalElapsed = endG - startG;
            this.runnableTimings.put(BossTask.class, totalElapsed);

            Thread.sleep(50L);
         }
      } catch (InterruptedException e) {
         Console.debug("BossTask interrupted - stopping gracefully");
         Thread.currentThread().interrupt();
      } catch (Exception e) {
         Console.error("Critical error in BossTask: " + e.getMessage());
         e.printStackTrace();
      } finally {
         cleanup();
         Console.log("BossTask stopped");
      }
   }

   public void stopTask() {
      this.running = false;
      this.cancel();
   }

   private void cleanup() {
      bossTasks.clear();
      runnableTimings.clear();
      Console.debug("BossTask cleanup completed");
   }

   public Map<Class<?>, Long> getRunnableTimings() {
      return new java.util.HashMap<>(runnableTimings);
   }
}