package sdfrpe.github.io.ptc.Listeners.Game;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Statics;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class FurnacesListener implements Listener {
   @EventHandler
   public void blockPlace(BlockPlaceEvent e) {
      if (e.getBlock().getState() instanceof Furnace) {
         GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(e.getPlayer().getUniqueId());
         if (gamePlayer.getFurnaces() >= Statics.MAX_FURNACES) {
            e.getPlayer().sendMessage(Statics.c("&cLímite alcanzado! Máximo de hornos: &f" + Statics.MAX_FURNACES));
            e.setCancelled(true);
         } else {
            gamePlayer.setFurnaces(gamePlayer.getFurnaces() + 1);
            Furnace furnace = (Furnace)e.getBlock().getState();
            Statics.placedFurnaces.put(furnace, e.getPlayer().getUniqueId());
            e.getPlayer().sendMessage(Statics.c(String.format("&aHas protegido un horno &7(%s/%s)", gamePlayer.getFurnaces(), Statics.MAX_FURNACES)));
         }
      }
   }

   @EventHandler
   public void blockBreak(BlockBreakEvent e) {
      if (e.getBlock().getState() instanceof Furnace) {
         Furnace furnace = (Furnace) e.getBlock().getState();

         if (Statics.placedFurnaces.containsKey(furnace)) {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(e.getPlayer().getUniqueId());

            if (gamePlayer != null && gamePlayer.getFurnaces() > 0) {
               gamePlayer.setFurnaces(gamePlayer.getFurnaces() - 1);
               Statics.placedFurnaces.remove(furnace);
               e.getPlayer().sendMessage(Statics.c(String.format("&eHas roto un horno protegido &7(%s/%s)", gamePlayer.getFurnaces(), Statics.MAX_FURNACES)));
            }
         }
      }
   }
}