package sdfrpe.github.io.ptc.Listeners.General;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;

public class BossBarWorldChangeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        BossBarAPI.removePlayer(player);

        Bukkit.getScheduler().runTaskLater(
                PTC.getInstance(),
                () -> {
                    if (player.isOnline()) {
                        BossBarAPI.updatePlayer(player, "Cargando...", 100.0f);
                    }
                },
                5L
        );
    }
}