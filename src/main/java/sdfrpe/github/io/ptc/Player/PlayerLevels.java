package sdfrpe.github.io.ptc.Player;

public class PlayerLevels {
   private int level;
   private float expLevel;
   private int totalExp;

   public PlayerLevels(int level, float expLevel, int totalExp) {
      this.level = level;
      this.expLevel = expLevel;
      this.totalExp = totalExp;
   }

   public int getLevel() {
      return this.level;
   }

   public float getExpLevel() {
      return this.expLevel;
   }

   public int getTotalExp() {
      return this.totalExp;
   }

   public void setLevel(int level) {
      this.level = level;
   }

   public void setExpLevel(float expLevel) {
      this.expLevel = expLevel;
   }

   public void setTotalExp(int totalExp) {
      this.totalExp = totalExp;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PlayerLevels)) {
         return false;
      } else {
         PlayerLevels other = (PlayerLevels)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getLevel() != other.getLevel()) {
            return false;
         } else if (Float.compare(this.getExpLevel(), other.getExpLevel()) != 0) {
            return false;
         } else {
            return this.getTotalExp() == other.getTotalExp();
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof PlayerLevels;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.getLevel();
      result = result * PRIME + Float.floatToIntBits(this.getExpLevel());
      result = result * PRIME + this.getTotalExp();
      return result;
   }

   public String toString() {
      return "PlayerLevels(level=" + this.getLevel() + ", expLevel=" + this.getExpLevel() + ", totalExp=" + this.getTotalExp() + ")";
   }
}