package sdfrpe.github.io.ptc.Listeners.Game;

import sdfrpe.github.io.ptc.Utils.Managers.CombatTime;
import java.util.Iterator;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class DeathMessagesListener implements Listener {
   @EventHandler
   public void onDamage(EntityDamageByEntityEvent e) {
      if (!(e.getEntity() instanceof Chicken)) {
         LivingEntity en;
         if (e.getEntity() != null && e.getDamager() != null && e.getEntity() instanceof LivingEntity && e.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity)e.getDamager();
            en = (LivingEntity)e.getEntity();
            CombatTime.lastDamager.put(en, attacker);
            CombatTime.lastDmgTime.put(en, 0);
         } else if (e.getEntity() != null && e.getEntity() instanceof LivingEntity && e.getDamager() != null && e.getDamager() instanceof Projectile && e.getDamager().getCustomName() != null) {
            Iterator var2 = e.getDamager().getWorld().getLivingEntities().iterator();

            while(var2.hasNext()) {
               en = (LivingEntity)var2.next();
               if (en.getEntityId() == Integer.parseInt(e.getDamager().getCustomName())) {
                  LivingEntity damaged2 = (LivingEntity)e.getEntity();
                  CombatTime.lastDamager.put(damaged2, en);
                  CombatTime.lastDmgTime.put(damaged2, 0);
               }
            }
         }
      }
   }

   @EventHandler
   public void onShoot(EntityShootBowEvent e) {
      e.getProjectile().setCustomName(Integer.toString(e.getEntity().getEntityId()));
   }

   @EventHandler
   public void onLaunch(ProjectileLaunchEvent e) {
      if (e.getEntity().getShooter() instanceof LivingEntity) {
         LivingEntity shooter = (LivingEntity)e.getEntity().getShooter();
         e.getEntity().setCustomName(Integer.toString(shooter.getEntityId()));
      }
   }

   @EventHandler
   public void onDie(PlayerDeathEvent e) {
      e.setDeathMessage((String)null);
   }

   @EventHandler
   public void onEntityDie(EntityDeathEvent e) {
      if (e.getEntity() != null) {
         if (e.getEntity() instanceof Chicken) {
            return;
         }

         EntityDamageEvent lastDamageCause = e.getEntity().getLastDamageCause();
         if (lastDamageCause != null) {
            CombatTime.createMessage(e.getEntity(), lastDamageCause.getCause());
         }

         CombatTime.lastDamager.remove(e.getEntity());
         CombatTime.lastDmgTime.remove(e.getEntity());
      }
   }
}