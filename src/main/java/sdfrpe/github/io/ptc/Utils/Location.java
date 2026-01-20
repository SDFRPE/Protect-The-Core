package sdfrpe.github.io.ptc.Utils;

import org.bukkit.Bukkit;

public class Location implements Cloneable {
   private String world;
   private double x;
   private double y;
   private double z;
   private float pitch;
   private float yaw;

   public Location(org.bukkit.Location loc) {
      this(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
   }

   public Location(String world, double x, double y, double z) {
      this(world, x, y, z, 90.0F, 0.0F);
   }

   public Location(String world, double x, double y, double z, float pitch, float yaw) {
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
      this.pitch = pitch;
      this.yaw = yaw;
   }

   public org.bukkit.Location getLocation() {
      return new org.bukkit.Location(Bukkit.getWorld(this.world), this.x, this.y, this.z, this.yaw, this.pitch);
   }

   public Location clone() {
      try {
         return (Location)super.clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException("Failed to clone Location", e);
      }
   }

   public String getWorld() {
      return this.world;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getPitch() {
      return this.pitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setWorld(String world) {
      this.world = world;
   }

   public void setX(double x) {
      this.x = x;
   }

   public void setY(double y) {
      this.y = y;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Location)) {
         return false;
      } else {
         Location other = (Location)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (Double.compare(this.getX(), other.getX()) != 0) {
            return false;
         } else if (Double.compare(this.getY(), other.getY()) != 0) {
            return false;
         } else if (Double.compare(this.getZ(), other.getZ()) != 0) {
            return false;
         } else if (Float.compare(this.getPitch(), other.getPitch()) != 0) {
            return false;
         } else if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
            return false;
         } else {
            Object this$world = this.getWorld();
            Object other$world = other.getWorld();
            if (this$world == null) {
               if (other$world != null) {
                  return false;
               }
            } else if (!this$world.equals(other$world)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Location;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      long $x = Double.doubleToLongBits(this.getX());
      result = result * PRIME + (int)($x >>> 32 ^ $x);
      long $y = Double.doubleToLongBits(this.getY());
      result = result * PRIME + (int)($y >>> 32 ^ $y);
      long $z = Double.doubleToLongBits(this.getZ());
      result = result * PRIME + (int)($z >>> 32 ^ $z);
      result = result * PRIME + Float.floatToIntBits(this.getPitch());
      result = result * PRIME + Float.floatToIntBits(this.getYaw());
      Object $world = this.getWorld();
      result = result * PRIME + ($world == null ? 43 : $world.hashCode());
      return result;
   }

   public String toString() {
      return "Location(world=" + this.getWorld() + ", x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ", pitch=" + this.getPitch() + ", yaw=" + this.getYaw() + ")";
   }
}