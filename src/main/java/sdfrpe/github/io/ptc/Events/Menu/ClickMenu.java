package sdfrpe.github.io.ptc.Events.Menu;

import sdfrpe.github.io.ptc.Events.Utils.Handlers;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Menu.utils.ordItems;

public class ClickMenu extends Handlers {
   private final ordItems ordItems;
   private final GamePlayer gamePlayer;

   public ClickMenu(GamePlayer gamePlayer, ordItems ordItems) {
      this.ordItems = ordItems;
      this.gamePlayer = gamePlayer;
   }

   public GamePlayer getPtcPlayer() {
      return this.gamePlayer;
   }

   public ordItems getOrdItems() {
      return this.ordItems;
   }
}
