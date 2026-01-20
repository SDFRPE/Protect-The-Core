package sdfrpe.github.io.ptc.Tasks.InGame;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Game.Settings.ArenaSettings;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Location;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArenaTask extends PTCRunnable {
   private final ArenaSettings arenaSettings;
   private final int dTime;
   private int count;
   private int elapsedTime;
   private static ArenaTask instance;
   private int zeroPlayersCounter = 0;
   private static final int ZERO_PLAYERS_THRESHOLD = 10;
   private CrossTeamAnnouncementTask crossTeamTask;

   public ArenaTask() {
      if (PTC.getInstance().getGameManager().getArena() == null) {
         Console.error("Arena es null - No se puede iniciar ArenaTask");
         throw new IllegalStateException("Arena no está disponible");
      }

      this.arenaSettings = PTC.getInstance().getGameManager().getArena().getArenaSettings();

      if (this.arenaSettings == null) {
         Console.error("ArenaSettings es null - No se puede iniciar ArenaTask");
         throw new IllegalStateException("ArenaSettings no está disponible");
      }

      this.dTime = this.arenaSettings.getDuration() / 20;
      this.count = this.dTime;
      this.elapsedTime = 0;

      Console.debug("ArenaTask initialized: " + this.dTime + " seconds (" + (this.dTime / 60) + " minutes)");

      BossBarAPI.setEnabled(true);
      this.updateBoss();

      Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
         this.spawnPigs();
         Console.log("Initial pigs spawned successfully!");
      }, 20L);

      instance = this;

      this.crossTeamTask = new CrossTeamAnnouncementTask();
      this.crossTeamTask.run();
      LogSystem.info(LogCategory.GAME, "CrossTeamAnnouncementTask iniciado para monitorear equipo cruzado");
   }

   public static ArenaTask getInstance() {
      return instance;
   }

   public int getTimeRemainingSeconds() {
      return this.count;
   }

   public int getDurationSeconds() {
      return this.dTime;
   }

   public String getFormattedTime() {
      int minutes = this.count / 60;
      int seconds = this.count % 60;
      return String.format("%02d:%02d", minutes, seconds);
   }

   public void onTick() {
      this.updateBoss();
      this.checkWin();
      this.checkZeroPlayers();
      this.updateInventories();

      if (this.elapsedTime % 600 == 0 && this.elapsedTime > 0) {
         Console.log("Regenerating pigs at " + this.elapsedTime + "s...");
         this.spawnPigs();
      }

      if (this.elapsedTime % 600 == 0 && this.elapsedTime > 0) {
         Console.debug("Resetting mines at " + this.elapsedTime + "s");
         this.resetMines();
      }

      if (this.count <= 0) {
         if (Statics.gameStatus != GameStatus.ENDED) {
            Console.debug("Time's up! Ending game...");

            if (this.crossTeamTask != null) {
               this.crossTeamTask.cancel();
               this.crossTeamTask = null;
               LogSystem.debug(LogCategory.GAME, "CrossTeamAnnouncementTask cancelado (tiempo agotado)");
            }

            (new EndTask(null)).run();
         }

         instance = null;
         this.cancel();
         return;
      }

      --this.count;
      ++this.elapsedTime;
   }

   private void checkZeroPlayers() {
      int onlinePlayers = Bukkit.getOnlinePlayers().size();

      if (onlinePlayers == 0) {
         zeroPlayersCounter++;

         if (zeroPlayersCounter >= ZERO_PLAYERS_THRESHOLD) {
            LogSystem.warn(LogCategory.GAME, "═══════════════════════════════════");
            LogSystem.warn(LogCategory.GAME, "0 JUGADORES DETECTADOS - AUTO-RESET");
            LogSystem.warn(LogCategory.GAME, "Contador:", zeroPlayersCounter + "/" + ZERO_PLAYERS_THRESHOLD);
            LogSystem.warn(LogCategory.GAME, "Iniciando proceso de finalización...");
            LogSystem.warn(LogCategory.GAME, "═══════════════════════════════════");

            if (this.crossTeamTask != null) {
               this.crossTeamTask.cancel();
               this.crossTeamTask = null;
               LogSystem.debug(LogCategory.GAME, "CrossTeamAnnouncementTask cancelado (0 jugadores)");
            }

            if (Statics.gameStatus != GameStatus.ENDED) {
               Console.log("Auto-ending game due to 0 players for " + ZERO_PLAYERS_THRESHOLD + " seconds");
               (new EndTask(null)).run();
            }

            instance = null;
            this.cancel();
         } else {
            if (zeroPlayersCounter % 5 == 0) {
               LogSystem.debug(LogCategory.GAME, "Arena vacía detectada -", zeroPlayersCounter + "/" + ZERO_PLAYERS_THRESHOLD + "s sin jugadores");
            }
         }
      } else {
         if (zeroPlayersCounter > 0) {
            LogSystem.debug(LogCategory.GAME, "Jugadores detectados nuevamente - Reset contador (era", zeroPlayersCounter + ")");
            zeroPlayersCounter = 0;
         }
      }
   }

   private void spawnPigs() {
      Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
         Location centerSpawn = this.arenaSettings.getArenaLocations().getCenterSpawn();

         if (centerSpawn == null || centerSpawn.getWorld() == null) {
            Console.error("Center spawn or world is null, cannot spawn pigs!");
            return;
         }

         int pigsRemoved = 0;
         for (Entity entity : Bukkit.getWorld(centerSpawn.getWorld()).getEntities()) {
            if (entity instanceof Pig) {
               entity.remove();
               pigsRemoved++;
            }
         }

         if (pigsRemoved > 0) {
            Console.debug("Removed " + pigsRemoved + " existing pigs before respawning");
         }

         int totalPigsSpawned = 0;
         for (Location location : this.arenaSettings.getArenaLocations().getPigsSpawn()) {
            org.bukkit.Location loc = location.getLocation();

            if (loc == null || loc.getWorld() == null) {
               Console.error("Pig spawn location or world is null! Skipping this location.");
               continue;
            }

            for (int i = 0; i < 15; ++i) {
               Pig pig = (Pig) loc.getWorld().spawnEntity(loc, EntityType.PIG);
               pig.setMaxHealth(0.5D);
               pig.setHealth(0.5D);
               totalPigsSpawned++;
            }
         }

         Console.log("Successfully spawned " + totalPigsSpawned + " pigs at " +
                 this.arenaSettings.getArenaLocations().getPigsSpawn().size() + " spawn points");
      });
   }

   private void resetMines() {
      Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
         int blocksReset = 0;
         for (Block block : Statics.mineralsMined.keySet()) {
            if (block != null) {
               Material originalMaterial = Statics.mineralsMined.get(block);
               if (originalMaterial != null) {
                  block.setType(originalMaterial);
                  blocksReset++;
               }
            }
         }

         Statics.mineralsMined.clear();

         if (blocksReset > 0) {
            Console.debug("Reset " + blocksReset + " mined blocks");
         }
      });
   }

   public void addTime(int seconds) {
      this.count += seconds;
      Console.log(String.format("Added %d seconds to game time. New time: %s", seconds, this.getFormattedTime()));
      this.updateBoss();
   }

   public void removeTime(int seconds) {
      this.count = Math.max(0, this.count - seconds);
      Console.log(String.format("Removed %d seconds from game time. New time: %s", seconds, this.getFormattedTime()));
      this.updateBoss();
   }

   public void setTime(int seconds) {
      this.count = Math.max(0, seconds);
      Console.log(String.format("Set game time to %d seconds. New time: %s", seconds, this.getFormattedTime()));
      this.updateBoss();
   }

   private void updateInventories() {
      for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
         int itemsToFix = 0;

         for (ItemStack item : onlinePlayer.getInventory().getContents()) {
            if (this.isValid(item) && item.getDurability() > 0) {
               ++itemsToFix;
               byte toFix = (byte) (item.getType().equals(Material.BOW) ? 10 : 50);
               item.setDurability((short) Math.max(item.getDurability() - toFix, 0));
            }
         }

         if (itemsToFix != 0) {
            onlinePlayer.updateInventory();
         }
      }
   }

   public void checkWin() {
      if (PTC.getInstance().getGameManager().checkWin()) {
         if (this.crossTeamTask != null) {
            this.crossTeamTask.cancel();
            this.crossTeamTask = null;
            LogSystem.debug(LogCategory.GAME, "CrossTeamAnnouncementTask cancelado (victoria detectada)");
         }

         this.cancel();
         Console.debug("One team is the winner of this game!");

         ArenaTeam arenaTeam = PTC.getInstance().getGameManager().getGameSettings().checkWinnerTeam();

         instance = null;
         (new EndTask(arenaTeam)).run();
      }
   }

   public void updateBoss() {
      String timeFormatted = this.getFormattedTime();
      float percentage = (float) (this.count * 100) / (float) this.dTime;

      BossBarAPI.update(
              String.format("%sTiempo restante: %s", ChatColor.GREEN, timeFormatted),
              percentage
      );
   }

   private boolean isValid(ItemStack itemStack) {
      return itemStack != null && (
              itemStack.containsEnchantment(Enchantment.DAMAGE_ALL) ||
                      itemStack.containsEnchantment(Enchantment.KNOCKBACK) ||
                      itemStack.containsEnchantment(Enchantment.ARROW_DAMAGE) ||
                      itemStack.containsEnchantment(Enchantment.FIRE_ASPECT)
      );
   }
}