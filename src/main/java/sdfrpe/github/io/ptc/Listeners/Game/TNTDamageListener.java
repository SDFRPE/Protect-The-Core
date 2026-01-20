package sdfrpe.github.io.ptc.Listeners.Game;

import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.MetadataValue;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Statics;

import java.util.List;
import java.util.UUID;

public class TNTDamageListener implements Listener {

    private static final double TNT_MAX_RADIUS = 4.0;
    private static final double TNT_MIN_DAMAGE = 4.0;
    private static final double TNT_MAX_DAMAGE = 10.0;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTDamage(EntityDamageByEntityEvent e) {
        if (Statics.gameStatus != GameStatus.IN_GAME) {
            return;
        }

        if (!(e.getDamager() instanceof TNTPrimed)) {
            return;
        }

        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        TNTPrimed tnt = (TNTPrimed) e.getDamager();
        Player victim = (Player) e.getEntity();
        boolean isSharpnessTNT = tnt.hasMetadata("sharpnessTNT");

        GamePlayer victimGP = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(victim.getUniqueId());
        if (victimGP == null) {
            return;
        }

        if (victimGP.getArenaTeam() != null && victimGP.getArenaTeam().getColor() == TeamColor.SPECTATOR) {
            e.setCancelled(true);
            return;
        }

        double distance = tnt.getLocation().distance(victim.getLocation());
        double normalizedDamage;

        if (distance <= 1.0) {
            normalizedDamage = TNT_MAX_DAMAGE;
        } else if (distance <= TNT_MAX_RADIUS) {
            double ratio = 1.0 - ((distance - 1.0) / (TNT_MAX_RADIUS - 1.0));
            normalizedDamage = TNT_MIN_DAMAGE + (ratio * (TNT_MAX_DAMAGE - TNT_MIN_DAMAGE));
        } else {
            normalizedDamage = TNT_MIN_DAMAGE * 0.5;
        }

        if (isSharpnessTNT) {
            List<MetadataValue> metaList = tnt.getMetadata("sharpnessTNT");
            if (!metaList.isEmpty()) {
                String throwerUuidStr = metaList.get(0).asString();
                try {
                    UUID throwerUuid = UUID.fromString(throwerUuidStr);

                    if (victim.getUniqueId().equals(throwerUuid)) {
                        normalizedDamage *= 0.5;
                    } else {
                        GamePlayer throwerGP = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(throwerUuid);
                        if (throwerGP != null && victimGP.getArenaTeam() != null && throwerGP.getArenaTeam() != null) {
                            if (victimGP.getArenaTeam().getColor() == throwerGP.getArenaTeam().getColor()) {
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        e.setDamage(normalizedDamage);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent e) {
        if (Statics.gameStatus != GameStatus.IN_GAME) {
            return;
        }

        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION &&
                e.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        Player player = (Player) e.getEntity();
        GamePlayer gp = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

        if (gp != null && gp.getArenaTeam() != null && gp.getArenaTeam().getColor() == TeamColor.SPECTATOR) {
            e.setCancelled(true);
        }
    }
}