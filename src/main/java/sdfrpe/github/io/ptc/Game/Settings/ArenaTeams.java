package sdfrpe.github.io.ptc.Game.Settings;

public class ArenaTeams {
   private int coresPerTeam = 10;
   private int teamSize = 10;

   public int getCoresPerTeam() {
      return this.coresPerTeam;
   }

   public int getTeamSize() {
      return this.teamSize;
   }

   public void setCoresPerTeam(int coresPerTeam) {
      this.coresPerTeam = coresPerTeam;
   }

   public void setTeamSize(int teamSize) {
      this.teamSize = teamSize;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ArenaTeams)) {
         return false;
      } else {
         ArenaTeams other = (ArenaTeams)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getCoresPerTeam() != other.getCoresPerTeam()) {
            return false;
         } else {
            return this.getTeamSize() == other.getTeamSize();
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ArenaTeams;
   }

   public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.getCoresPerTeam();
      result = result * PRIME + this.getTeamSize();
      return result;
   }

   public String toString() {
      return "ArenaTeams(coresPerTeam=" + this.getCoresPerTeam() + ", teamSize=" + this.getTeamSize() + ")";
   }
}