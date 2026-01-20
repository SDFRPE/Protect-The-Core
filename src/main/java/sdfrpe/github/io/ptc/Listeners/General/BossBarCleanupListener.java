package sdfrpe.github.io.ptc.Listeners.General;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;

public class BossBarCleanupListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        BossBarAPI.removePlayer(e.getPlayer());
    }
}