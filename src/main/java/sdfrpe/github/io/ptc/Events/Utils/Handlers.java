package sdfrpe.github.io.ptc.Events.Utils;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class Handlers extends Event {
   private static final HandlerList handlers = new HandlerList();

   public Handlers() {
      this(false);
   }

   public Handlers(boolean isAsync) {
      super(isAsync);
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   public HandlerList getHandlers() {
      return handlers;
   }
}
