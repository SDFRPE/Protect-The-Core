package sdfrpe.github.io.ptc.Listeners.Game;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class ExperienceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExpChange(PlayerExpChangeEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

        if (gamePlayer != null) {
            gamePlayer.getPlayerStats().setExp(player);

            LogSystem.debug(LogCategory.GAME, "XP actualizada para", player.getName(),
                    "Level:", String.valueOf(player.getLevel()),
                    "TotalExp:", String.valueOf(player.getTotalExperience()));
        }
    }
}