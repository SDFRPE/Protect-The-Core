package sdfrpe.github.io.ptc.Listeners.General;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

public class PlayerCleanupListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setGameMode(GameMode.SURVIVAL);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(player);
            player.showPlayer(online);
        }

        LogSystem.debug(LogCategory.PLAYER, "Estado visual limpiado al entrar:", player.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

        if (gamePlayer != null && gamePlayer.getArenaTeam() != null) {
            if (gamePlayer.getArenaTeam().getColor() == TeamColor.SPECTATOR) {
                gamePlayer.getArenaTeam().getTeamPlayers().remove(gamePlayer);
                gamePlayer.setArenaTeam(null);
                LogSystem.debug(LogCategory.PLAYER, "Espectador limpiado al salir:", player.getName());
            }
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
                online.showPlayer(player);
            }
        }
    }
}