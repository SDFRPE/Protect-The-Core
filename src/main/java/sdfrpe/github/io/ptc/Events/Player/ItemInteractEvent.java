package sdfrpe.github.io.ptc.Events.Player;

import sdfrpe.github.io.ptc.Utils.Enums.Action;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class ItemInteractEvent extends Event implements Cancellable {
   private static final HandlerList handlers = new HandlerList();
   private boolean isCancelled;
   private final Player player;
   private final ItemStack itemStack;
   private final Action action;

   public ItemInteractEvent(Player player, ItemStack itemStack, Action action) {
      this.player = player;
      this.itemStack = itemStack;
      this.action = action;
   }

   public Player getPlayer() {
      return this.player;
   }

   public HandlerList getHandlers() {
      return handlers;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   public boolean isCancelled() {
      return this.isCancelled;
   }

   public void setCancelled(boolean cancel) {
      this.isCancelled = cancel;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public Action getAction() {
      return this.action;
   }
}