package sdfrpe.github.io.ptc.Utils.Inventories;

import org.bukkit.inventory.ItemStack;

public class GItem {
   private ItemStack itemStack;
   private int slot;
   private String action;
   private String need;

   public GItem() {
      this(null, 0, "none", "none");
   }

   public GItem(ItemStack itemStack, int slot, String action, String need) {
      this.itemStack = itemStack;
      this.slot = slot;
      this.action = action;
      this.need = need;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public int getSlot() {
      return this.slot;
   }

   public String getAction() {
      return this.action;
   }

   public String getNeed() {
      return this.need;
   }

   public void setItemStack(ItemStack itemStack) {
      this.itemStack = itemStack;
   }

   public void setSlot(int slot) {
      this.slot = slot;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public void setNeed(String need) {
      this.need = need;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof GItem)) {
         return false;
      } else {
         GItem other = (GItem)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getSlot() != other.getSlot()) {
            return false;
         } else {
            label49: {
               Object this$itemStack = this.getItemStack();
               Object other$itemStack = other.getItemStack();
               if (this$itemStack == null) {
                  if (other$itemStack == null) {
                     break label49;
                  }
               } else if (this$itemStack.equals(other$itemStack)) {
                  break label49;
               }

               return false;
            }

            Object this$action = this.getAction();
            Object other$action = other.getAction();
            if (this$action == null) {
               if (other$action != null) {
                  return false;
               }
            } else if (!this$action.equals(other$action)) {
               return false;
            }

            Object this$need = this.getNeed();
            Object other$need = other.getNeed();
            if (this$need == null) {
               if (other$need != null) {
                  return false;
               }
            } else if (!this$need.equals(other$need)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof GItem;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.getSlot();
      Object $itemStack = this.getItemStack();
      result = result * PRIME + ($itemStack == null ? 43 : $itemStack.hashCode());
      Object $action = this.getAction();
      result = result * PRIME + ($action == null ? 43 : $action.hashCode());
      Object $need = this.getNeed();
      result = result * PRIME + ($need == null ? 43 : $need.hashCode());
      return result;
   }

   public String toString() {
      return "GItem(itemStack=" + this.getItemStack() + ", slot=" + this.getSlot() + ", action=" + this.getAction() + ", need=" + this.getNeed() + ")";
   }
}