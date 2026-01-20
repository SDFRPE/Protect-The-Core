package sdfrpe.github.io.ptc.Events.Player;

import sdfrpe.github.io.ptc.Events.Utils.Handlers;
import java.util.UUID;
import org.bukkit.event.Cancellable;

public class PlayerLoadEvent extends Handlers implements Cancellable {
   private UUID uniqueId;
   private boolean cancelled;
   private String message;

   public PlayerLoadEvent(UUID uniqueId) {
      super(true);
      this.uniqueId = uniqueId;
      this.message = "PlayerLoadEvent was cancelled the login, please try again later.";
   }

   public UUID getUniqueId() {
      return this.uniqueId;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public String getMessage() {
      return this.message;
   }

   public void setUniqueId(UUID uniqueId) {
      this.uniqueId = uniqueId;
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PlayerLoadEvent)) {
         return false;
      } else {
         PlayerLoadEvent other = (PlayerLoadEvent)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (!super.equals(o)) {
            return false;
         } else if (this.isCancelled() != other.isCancelled()) {
            return false;
         } else {
            Object this$uniqueId = this.getUniqueId();
            Object other$uniqueId = other.getUniqueId();
            if (this$uniqueId == null) {
               if (other$uniqueId != null) {
                  return false;
               }
            } else if (!this$uniqueId.equals(other$uniqueId)) {
               return false;
            }

            Object this$message = this.getMessage();
            Object other$message = other.getMessage();
            if (this$message == null) {
               if (other$message != null) {
                  return false;
               }
            } else if (!this$message.equals(other$message)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof PlayerLoadEvent;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = super.hashCode();
      result = result * PRIME + (this.isCancelled() ? 79 : 97);
      Object $uniqueId = this.getUniqueId();
      result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
      Object $message = this.getMessage();
      result = result * PRIME + ($message == null ? 43 : $message.hashCode());
      return result;
   }

   public String toString() {
      return "PlayerLoadEvent(uniqueId=" + this.getUniqueId() + ", cancelled=" + this.isCancelled() + ", message=" + this.getMessage() + ")";
   }
}