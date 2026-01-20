package sdfrpe.github.io.ptc.Tasks;

import com.google.common.collect.Sets;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Interfaces.Tick;
import java.util.Iterator;
import java.util.Set;

public class GameTask implements Runnable {
   private static final Set<Tick> tickList = Sets.newConcurrentHashSet();
   private volatile boolean running = true;

   public static Set<Tick> getTickList() {
      return tickList;
   }

   public void run() {
      Console.log("Started: GameTask Thread");

      try {
         while (running && !Thread.currentThread().isInterrupted()) {
            Iterator<Tick> it = tickList.iterator();
            while (it.hasNext()) {
               Tick tick = it.next();

               if (tick instanceof PTCRunnable && ((PTCRunnable) tick).isCancelled()) {
                  tickList.remove(tick);
                  continue;
               }

               try {
                  tick.onTick();
               } catch (Exception e) {
                  Console.error("Error in GameTask tick: " + e.getMessage());
                  tickList.remove(tick);
                  e.printStackTrace();
               }
            }

            Thread.sleep(1000L);
         }
      } catch (InterruptedException e) {
         Console.debug("GameTask interrupted - stopping gracefully");
         Thread.currentThread().interrupt();
      } catch (Exception e) {
         Console.error("Critical error in GameTask: " + e.getMessage());
         e.printStackTrace();
      } finally {
         cleanup();
         Console.log("GameTask Thread stopped");
      }
   }

   public void stop() {
      this.running = false;
   }

   private void cleanup() {
      tickList.clear();
      Console.debug("GameTask cleanup completed");
   }
}