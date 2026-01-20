package sdfrpe.github.io.ptc.Listeners.Game;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;

public class FriendlyFireListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent e) {
        if (Statics.gameStatus != GameStatus.IN_GAME) {
            return;
        }

        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) e.getEntity();
        Player attacker = getAttacker(e);

        if (attacker == null) {
            return;
        }

        GamePlayer victimGP = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(victim.getUniqueId());
        GamePlayer attackerGP = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(attacker.getUniqueId());

        if (victimGP == null || attackerGP == null) {
            return;
        }

        ArenaTeam victimTeam = victimGP.getArenaTeam();
        ArenaTeam attackerTeam = attackerGP.getArenaTeam();

        if (victimTeam == null || attackerTeam == null) {
            return;
        }

        if (victimTeam.getColor() == attackerTeam.getColor()) {
            e.setCancelled(true);
            LogSystem.debug(LogCategory.GAME, "Friendly fire bloqueado:",
                    attacker.getName(), "→", victim.getName(),
                    "(" + victimTeam.getColor().getName() + ")");
        }
    }

    private Player getAttacker(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            return (Player) e.getDamager();
        }

        if (e.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }

        return null;
    }
}