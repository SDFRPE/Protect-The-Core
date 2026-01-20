package sdfrpe.github.io.ptc.Events.Player;

import sdfrpe.github.io.ptc.Events.Utils.Handlers;
import java.util.UUID;

public class PlayerUnloadEvent extends Handlers {
   private UUID uniqueId;

   public PlayerUnloadEvent(UUID uniqueId) {
      super(false);
      this.uniqueId = uniqueId;
   }

   public UUID getUniqueId() {
      return this.uniqueId;
   }

   public void setUniqueId(UUID uniqueId) {
      this.uniqueId = uniqueId;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PlayerUnloadEvent)) {
         return false;
      } else {
         PlayerUnloadEvent other = (PlayerUnloadEvent)o;
         if (!other.canEqual(this)) {
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

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof PlayerUnloadEvent;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      Object $uniqueId = this.getUniqueId();
      result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
      return result;
   }

   public String toString() {
      return "PlayerUnloadEvent(uniqueId=" + this.getUniqueId() + ")";
   }
}