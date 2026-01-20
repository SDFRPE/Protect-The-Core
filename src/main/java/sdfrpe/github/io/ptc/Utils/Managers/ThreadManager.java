package sdfrpe.github.io.ptc.Utils.Managers;

import com.google.common.collect.Sets;
import sdfrpe.github.io.ptc.Tasks.BossTask;
import sdfrpe.github.io.ptc.Tasks.GameTask;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import java.util.Iterator;
import java.util.Set;

public class ThreadManager {
   Set<Thread> threadList = Sets.newConcurrentHashSet();

   public ThreadManager() {
      this.threadList.add(new Thread(new GameTask(), "PTC-GameTask"));
      this.threadList.add(new Thread(new BossTask(), "PTC-BossTask"));
   }

   public void startThreads() {
      Iterator var1 = this.threadList.iterator();

      while(var1.hasNext()) {
         Thread thread = (Thread)var1.next();
         thread.setDaemon(true);
         thread.start();
         LogSystem.debug(LogCategory.CORE, "Thread iniciado:", thread.getName());
      }
   }

   public void stopThreads() {
      LogSystem.info(LogCategory.CORE, "Deteniendo threads...");
      Iterator var1 = this.threadList.iterator();

      while(var1.hasNext()) {
         Thread thread = (Thread)var1.next();
         thread.interrupt();

         try {
            thread.join(5000);
            if (thread.isAlive()) {
               LogSystem.warn(LogCategory.CORE, "Thread no terminó a tiempo:", thread.getName());

               thread.join(2000);
               if (thread.isAlive()) {
                  LogSystem.error(LogCategory.CORE, "CRÍTICO: Thread aún vivo, se forzará en shutdown:", thread.getName());
               } else {
                  LogSystem.info(LogCategory.CORE, "Thread terminó en segundo intento:", thread.getName());
               }
            } else {
               LogSystem.debug(LogCategory.CORE, "Thread detenido correctamente:", thread.getName());
            }
         } catch (InterruptedException e) {
            LogSystem.warn(LogCategory.CORE, "Interrupción esperando thread:", thread.getName());
            Thread.currentThread().interrupt();
         }
      }

      LogSystem.info(LogCategory.CORE, "Todos los threads procesados");
   }
}