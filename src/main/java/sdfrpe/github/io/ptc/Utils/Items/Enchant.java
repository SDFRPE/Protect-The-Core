package sdfrpe.github.io.ptc.Utils.Items;

public class Enchant {
   private String name;
   private int level;

   public Enchant(String name, int level) {
      this.name = name;
      this.level = level;
   }

   public String getName() {
      return this.name;
   }

   public int getLevel() {
      return this.level;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setLevel(int level) {
      this.level = level;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Enchant)) {
         return false;
      } else {
         Enchant other = (Enchant)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getLevel() != other.getLevel()) {
            return false;
         } else {
            Object this$name = this.getName();
            Object other$name = other.getName();
            if (this$name == null) {
               if (other$name != null) {
                  return false;
               }
            } else if (!this$name.equals(other$name)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Enchant;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.getLevel();
      Object $name = this.getName();
      result = result * PRIME + ($name == null ? 43 : $name.hashCode());
      return result;
   }

   public String toString() {
      return "PTCEnchant(name=" + this.getName() + ", level=" + this.getLevel() + ")";
   }
}