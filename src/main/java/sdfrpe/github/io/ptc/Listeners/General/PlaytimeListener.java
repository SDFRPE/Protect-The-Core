package sdfrpe.github.io.ptc.Listeners.General;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Playtime.PlaytimeManager;

public class PlaytimeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlaytimeManager manager = PTC.getInstance().getPlaytimeManager();
        if (manager != null) {
            manager.onPlayerJoin(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlaytimeManager manager = PTC.getInstance().getPlaytimeManager();
        if (manager != null) {
            manager.onPlayerQuit(event.getPlayer());
        }
    }
}