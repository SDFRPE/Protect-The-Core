package sdfrpe.github.io.ptc.Game.Settings;

import sdfrpe.github.io.ptc.Utils.Location;
import sdfrpe.github.io.ptc.Utils.Enums.TimeOfDay;

public class ArenaSettings {
   private String name;
   private Location center;
   private double border_size;
   private int duration;
   private TimeOfDay timeOfDay = TimeOfDay.DAY;
   private int extraHearts = 0;
   private ArenaLocations arenaLocations = new ArenaLocations();

   public String getName() {
      return this.name;
   }

   public Location getCenter() {
      return this.center;
   }

   public double getBorder_size() {
      return this.border_size;
   }

   public int getDuration() {
      return this.duration;
   }

   public TimeOfDay getTimeOfDay() {
      return this.timeOfDay;
   }

   public int getExtraHearts() {
      return this.extraHearts;
   }

   public ArenaLocations getArenaLocations() {
      return this.arenaLocations;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setCenter(Location center) {
      this.center = center;
   }

   public void setBorder_size(double border_size) {
      this.border_size = border_size;
   }

   public void setDuration(int duration) {
      this.duration = duration;
   }

   public void setTimeOfDay(TimeOfDay timeOfDay) {
      this.timeOfDay = timeOfDay;
   }

   public void setExtraHearts(int extraHearts) {
      this.extraHearts = Math.max(0, Math.min(3, extraHearts));
   }

   public void setArenaLocations(ArenaLocations arenaLocations) {
      this.arenaLocations = arenaLocations;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ArenaSettings)) {
         return false;
      } else {
         ArenaSettings other = (ArenaSettings)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (Double.compare(this.getBorder_size(), other.getBorder_size()) != 0) {
            return false;
         } else if (this.getDuration() != other.getDuration()) {
            return false;
         } else if (this.getExtraHearts() != other.getExtraHearts()) {
            return false;
         } else {
            label52: {
               Object this$name = this.getName();
               Object other$name = other.getName();
               if (this$name == null) {
                  if (other$name == null) {
                     break label52;
                  }
               } else if (this$name.equals(other$name)) {
                  break label52;
               }

               return false;
            }

            Object this$center = this.getCenter();
            Object other$center = other.getCenter();
            if (this$center == null) {
               if (other$center != null) {
                  return false;
               }
            } else if (!this$center.equals(other$center)) {
               return false;
            }

            Object this$timeOfDay = this.getTimeOfDay();
            Object other$timeOfDay = other.getTimeOfDay();
            if (this$timeOfDay == null) {
               if (other$timeOfDay != null) {
                  return false;
               }
            } else if (!this$timeOfDay.equals(other$timeOfDay)) {
               return false;
            }

            Object this$arenaLocations = this.getArenaLocations();
            Object other$arenaLocations = other.getArenaLocations();
            if (this$arenaLocations == null) {
               if (other$arenaLocations != null) {
                  return false;
               }
            } else if (!this$arenaLocations.equals(other$arenaLocations)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ArenaSettings;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      long $border_size = Double.doubleToLongBits(this.getBorder_size());
      result = result * PRIME + (int)($border_size >>> 32 ^ $border_size);
      result = result * PRIME + this.getDuration();
      result = result * PRIME + this.getExtraHearts();
      Object $name = this.getName();
      result = result * PRIME + ($name == null ? 43 : $name.hashCode());
      Object $center = this.getCenter();
      result = result * PRIME + ($center == null ? 43 : $center.hashCode());
      Object $timeOfDay = this.getTimeOfDay();
      result = result * PRIME + ($timeOfDay == null ? 43 : $timeOfDay.hashCode());
      Object $arenaLocations = this.getArenaLocations();
      result = result * PRIME + ($arenaLocations == null ? 43 : $arenaLocations.hashCode());
      return result;
   }

   public String toString() {
      return "ArenaSettings(name=" + this.getName() + ", center=" + this.getCenter() + ", border_size=" + this.getBorder_size() + ", duration=" + this.getDuration() + ", timeOfDay=" + this.getTimeOfDay() + ", extraHearts=" + this.getExtraHearts() + ", arenaLocations=" + this.getArenaLocations() + ")";
   }
}