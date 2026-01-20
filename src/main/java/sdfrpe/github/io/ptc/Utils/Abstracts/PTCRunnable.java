package sdfrpe.github.io.ptc.Utils.Abstracts;

import sdfrpe.github.io.ptc.Tasks.GameTask;
import sdfrpe.github.io.ptc.Utils.Interfaces.Tick;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class PTCRunnable implements Tick {
   private final AtomicBoolean cancelled = new AtomicBoolean(false);

   public abstract void onTick();

   public void run() {
      GameTask.getTickList().add(this);
   }

   public void cancel() {
      if (cancelled.compareAndSet(false, true)) {
         GameTask.getTickList().remove(this);
      }
   }

   public boolean isCancelled() {
      return cancelled.get();
   }
}