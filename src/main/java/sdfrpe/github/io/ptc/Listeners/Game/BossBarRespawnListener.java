package sdfrpe.github.io.ptc.Listeners.Game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Tasks.InGame.ArenaTask;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import org.bukkit.ChatColor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BossBarRespawnListener implements Listener {

    private static final Set<UUID> pendingBossBarRestore = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        LogSystem.debug(LogCategory.GAME, "Jugador murió, BossBar se mantendrá:", e.getEntity().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (Statics.gameStatus != GameStatus.IN_GAME) {
            return;
        }

        Player player = e.getPlayer();

        GamePlayer gp = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
        if (gp == null) {
            return;
        }

        if (gp.getArenaTeam() == null ||
                gp.getArenaTeam().getColor() == TeamColor.SPECTATOR ||
                gp.getArenaTeam().isDeathTeam()) {
            return;
        }

        BossBarAPI.removePlayer(player);

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            pendingBossBarRestore.add(player.getUniqueId());
            LogSystem.debug(LogCategory.GAME, "Jugador listo para restaurar BossBar al moverse:", player.getName());
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        if (!pendingBossBarRestore.contains(player.getUniqueId())) {
            return;
        }

        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
                e.getFrom().getBlockY() == e.getTo().getBlockY() &&
                e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        pendingBossBarRestore.remove(player.getUniqueId());

        if (Statics.gameStatus != GameStatus.IN_GAME) {
            return;
        }

        ArenaTask task = ArenaTask.getInstance();
        if (task == null) {
            return;
        }

        String timeFormatted = task.getFormattedTime();
        int timeRemaining = task.getTimeRemainingSeconds();
        int dTime = task.getDurationSeconds();
        float percentage = (float) (timeRemaining * 100) / (float) dTime;
        String message = String.format("%sTiempo restante: %s", ChatColor.GREEN, timeFormatted);

        BossBarAPI.updatePlayer(player, message, percentage);

        LogSystem.info(LogCategory.GAME, "✓ BossBar restaurada tras movimiento de respawn:", player.getName());
    }
}