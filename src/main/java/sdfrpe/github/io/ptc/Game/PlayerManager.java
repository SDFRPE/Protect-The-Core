package sdfrpe.github.io.ptc.Game;

import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
   private Map<UUID, GamePlayer> playerMap = Maps.newHashMap();

   public void addPlayer(UUID uuid, GamePlayer gamePlayer) {
      if (!this.containsPlayer(uuid)) {
         this.playerMap.put(uuid, gamePlayer);
      }
   }

   public GamePlayer getPlayer(UUID uuid) {
      return this.playerMap.getOrDefault(uuid, null);
   }

   public void removePlayer(UUID uuid) {
      this.playerMap.remove(uuid);
   }

   public boolean containsPlayer(UUID uuid) {
      return this.playerMap.containsKey(uuid);
   }

   public Map<UUID, GamePlayer> getPlayerMap() {
      return this.playerMap;
   }
}