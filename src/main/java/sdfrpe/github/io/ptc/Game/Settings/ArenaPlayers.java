package sdfrpe.github.io.ptc.Game.Settings;

public class ArenaPlayers {
   private int maxPlayers = 40;
   private int minPlayers = 4;

   public int getMaxPlayers() {
      return this.maxPlayers;
   }

   public int getMinPlayers() {
      return this.minPlayers;
   }

   public void setMaxPlayers(int maxPlayers) {
      this.maxPlayers = maxPlayers;
   }

   public void setMinPlayers(int minPlayers) {
      this.minPlayers = minPlayers;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ArenaPlayers)) {
         return false;
      } else {
         ArenaPlayers other = (ArenaPlayers)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getMaxPlayers() != other.getMaxPlayers()) {
            return false;
         } else {
            return this.getMinPlayers() == other.getMinPlayers();
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ArenaPlayers;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.getMaxPlayers();
      result = result * PRIME + this.getMinPlayers();
      return result;
   }

   public String toString() {
      return "ArenaPlayers(maxPlayers=" + this.getMaxPlayers() + ", minPlayers=" + this.getMinPlayers() + ")";
   }
}