package sdfrpe.github.io.ptc.Utils;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Managers.GlobalTabManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerTabUpdater {

    public static void updateAllPlayerTabs() {
        Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
                if (gamePlayer != null) {
                    gamePlayer.updateTabList();
                }
            }

            GlobalTabManager.getInstance().updateAllPlayerTabs();
        });
    }
}