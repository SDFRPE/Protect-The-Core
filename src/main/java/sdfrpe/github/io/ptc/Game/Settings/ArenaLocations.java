package sdfrpe.github.io.ptc.Game.Settings;

import com.google.common.collect.Lists;
import sdfrpe.github.io.ptc.Utils.Location;
import java.util.List;

public class ArenaLocations {
   private Location yellowSpawn;
   private Location yellowCore;
   private Location blueSpawn;
   private Location blueCore;
   private Location redSpawn;
   private Location redCore;
   private Location greenSpawn;
   private Location greenCore;
   private Location centerSpawn;
   private List<Location> pigsSpawn = Lists.newArrayList();

   public Location getYellowSpawn() {
      return this.yellowSpawn;
   }

   public Location getYellowCore() {
      return this.yellowCore;
   }

   public Location getBlueSpawn() {
      return this.blueSpawn;
   }

   public Location getBlueCore() {
      return this.blueCore;
   }

   public Location getRedSpawn() {
      return this.redSpawn;
   }

   public Location getRedCore() {
      return this.redCore;
   }

   public Location getGreenSpawn() {
      return this.greenSpawn;
   }

   public Location getGreenCore() {
      return this.greenCore;
   }

   public Location getCenterSpawn() {
      return this.centerSpawn;
   }

   public List<Location> getPigsSpawn() {
      return this.pigsSpawn;
   }

   public void setYellowSpawn(Location yellowSpawn) {
      this.yellowSpawn = yellowSpawn;
   }

   public void setYellowCore(Location yellowCore) {
      this.yellowCore = yellowCore;
   }

   public void setBlueSpawn(Location blueSpawn) {
      this.blueSpawn = blueSpawn;
   }

   public void setBlueCore(Location blueCore) {
      this.blueCore = blueCore;
   }

   public void setRedSpawn(Location redSpawn) {
      this.redSpawn = redSpawn;
   }

   public void setRedCore(Location redCore) {
      this.redCore = redCore;
   }

   public void setGreenSpawn(Location greenSpawn) {
      this.greenSpawn = greenSpawn;
   }

   public void setGreenCore(Location greenCore) {
      this.greenCore = greenCore;
   }

   public void setCenterSpawn(Location centerSpawn) {
      this.centerSpawn = centerSpawn;
   }

   public void setPigsSpawn(List<Location> pigsSpawn) {
      this.pigsSpawn = pigsSpawn;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ArenaLocations)) {
         return false;
      } else {
         ArenaLocations other = (ArenaLocations)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            label119: {
               Object this$yellowSpawn = this.getYellowSpawn();
               Object other$yellowSpawn = other.getYellowSpawn();
               if (this$yellowSpawn == null) {
                  if (other$yellowSpawn == null) {
                     break label119;
                  }
               } else if (this$yellowSpawn.equals(other$yellowSpawn)) {
                  break label119;
               }

               return false;
            }

            Object this$yellowCore = this.getYellowCore();
            Object other$yellowCore = other.getYellowCore();
            if (this$yellowCore == null) {
               if (other$yellowCore != null) {
                  return false;
               }
            } else if (!this$yellowCore.equals(other$yellowCore)) {
               return false;
            }

            label105: {
               Object this$blueSpawn = this.getBlueSpawn();
               Object other$blueSpawn = other.getBlueSpawn();
               if (this$blueSpawn == null) {
                  if (other$blueSpawn == null) {
                     break label105;
                  }
               } else if (this$blueSpawn.equals(other$blueSpawn)) {
                  break label105;
               }

               return false;
            }

            Object this$blueCore = this.getBlueCore();
            Object other$blueCore = other.getBlueCore();
            if (this$blueCore == null) {
               if (other$blueCore != null) {
                  return false;
               }
            } else if (!this$blueCore.equals(other$blueCore)) {
               return false;
            }

            label91: {
               Object this$redSpawn = this.getRedSpawn();
               Object other$redSpawn = other.getRedSpawn();
               if (this$redSpawn == null) {
                  if (other$redSpawn == null) {
                     break label91;
                  }
               } else if (this$redSpawn.equals(other$redSpawn)) {
                  break label91;
               }

               return false;
            }

            Object this$redCore = this.getRedCore();
            Object other$redCore = other.getRedCore();
            if (this$redCore == null) {
               if (other$redCore != null) {
                  return false;
               }
            } else if (!this$redCore.equals(other$redCore)) {
               return false;
            }

            label77: {
               Object this$greenSpawn = this.getGreenSpawn();
               Object other$greenSpawn = other.getGreenSpawn();
               if (this$greenSpawn == null) {
                  if (other$greenSpawn == null) {
                     break label77;
                  }
               } else if (this$greenSpawn.equals(other$greenSpawn)) {
                  break label77;
               }

               return false;
            }

            label70: {
               Object this$greenCore = this.getGreenCore();
               Object other$greenCore = other.getGreenCore();
               if (this$greenCore == null) {
                  if (other$greenCore == null) {
                     break label70;
                  }
               } else if (this$greenCore.equals(other$greenCore)) {
                  break label70;
               }

               return false;
            }

            label63: {
               Object this$centerSpawn = this.getCenterSpawn();
               Object other$centerSpawn = other.getCenterSpawn();
               if (this$centerSpawn == null) {
                  if (other$centerSpawn == null) {
                     break label63;
                  }
               } else if (this$centerSpawn.equals(other$centerSpawn)) {
                  break label63;
               }

               return false;
            }

            Object this$pigsSpawn = this.getPigsSpawn();
            Object other$pigsSpawn = other.getPigsSpawn();
            if (this$pigsSpawn == null) {
               if (other$pigsSpawn != null) {
                  return false;
               }
            } else if (!this$pigsSpawn.equals(other$pigsSpawn)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ArenaLocations;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      Object $yellowSpawn = this.getYellowSpawn();
      result = result * PRIME + ($yellowSpawn == null ? 43 : $yellowSpawn.hashCode());
      Object $yellowCore = this.getYellowCore();
      result = result * PRIME + ($yellowCore == null ? 43 : $yellowCore.hashCode());
      Object $blueSpawn = this.getBlueSpawn();
      result = result * PRIME + ($blueSpawn == null ? 43 : $blueSpawn.hashCode());
      Object $blueCore = this.getBlueCore();
      result = result * PRIME + ($blueCore == null ? 43 : $blueCore.hashCode());
      Object $redSpawn = this.getRedSpawn();
      result = result * PRIME + ($redSpawn == null ? 43 : $redSpawn.hashCode());
      Object $redCore = this.getRedCore();
      result = result * PRIME + ($redCore == null ? 43 : $redCore.hashCode());
      Object $greenSpawn = this.getGreenSpawn();
      result = result * PRIME + ($greenSpawn == null ? 43 : $greenSpawn.hashCode());
      Object $greenCore = this.getGreenCore();
      result = result * PRIME + ($greenCore == null ? 43 : $greenCore.hashCode());
      Object $centerSpawn = this.getCenterSpawn();
      result = result * PRIME + ($centerSpawn == null ? 43 : $centerSpawn.hashCode());
      Object $pigsSpawn = this.getPigsSpawn();
      result = result * PRIME + ($pigsSpawn == null ? 43 : $pigsSpawn.hashCode());
      return result;
   }

   public String toString() {
      return "ArenaLocations(yellowSpawn=" + this.getYellowSpawn() + ", yellowCore=" + this.getYellowCore() + ", blueSpawn=" + this.getBlueSpawn() + ", blueCore=" + this.getBlueCore() + ", redSpawn=" + this.getRedSpawn() + ", redCore=" + this.getRedCore() + ", greenSpawn=" + this.getGreenSpawn() + ", greenCore=" + this.getGreenCore() + ", centerSpawn=" + this.getCenterSpawn() + ", pigsSpawn=" + this.getPigsSpawn() + ")";
   }
}