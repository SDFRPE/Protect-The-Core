package sdfrpe.github.io.ptc.Events.Arena;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockExpEvent;

public class OnMiningBlockPlaceEvent extends BlockExpEvent implements Cancellable {
   private final Player player;
   private boolean cancelled;

   public OnMiningBlockPlaceEvent(Block theBlock, Player player) {
      super(theBlock, 0);
      this.player = player;
      this.cancelled = false;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean cancel) {
      this.cancelled = cancel;
   }

   public Player getPlayer() {
      return this.player;
   }
}
