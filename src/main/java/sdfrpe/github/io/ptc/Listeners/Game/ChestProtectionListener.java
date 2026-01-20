package sdfrpe.github.io.ptc.Listeners.Game;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ChestProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onChestPlace(BlockPlaceEvent e) {
        Block block = e.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return;
        }

        Player player = e.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
        if (gamePlayer == null) {
            return;
        }

        if (gamePlayer.getChests() >= Statics.MAX_CHESTS) {
            player.sendMessage(Statics.c("&cLímite alcanzado! Máximo de cofres protegidos: &f" + Statics.MAX_CHESTS));
            e.setCancelled(true);
            return;
        }

        gamePlayer.setChests(gamePlayer.getChests() + 1);
        Location chestLocation = block.getLocation();
        Statics.protectedChests.put(chestLocation, player.getUniqueId());

        player.sendMessage(Statics.c(String.format("&aHas protegido un cofre &7(%s/%s)", gamePlayer.getChests(), Statics.MAX_CHESTS)));
        LogSystem.debug(LogCategory.GAME, "Cofre protegido por:", player.getName(), "en", chestLocation.toString());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChestInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return;
        }

        Location chestLocation = block.getLocation();
        UUID ownerUUID = Statics.protectedChests.get(chestLocation);

        if (ownerUUID == null) {
            return;
        }

        Player player = e.getPlayer();
        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
        if (gamePlayer == null) {
            return;
        }

        if (player.getUniqueId().equals(ownerUUID)) {
            return;
        }

        GamePlayer ownerGamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(ownerUUID);
        if (ownerGamePlayer == null) {
            Statics.protectedChests.remove(chestLocation);
            return;
        }

        ArenaTeam playerTeam = gamePlayer.getArenaTeam();
        ArenaTeam ownerTeam = ownerGamePlayer.getArenaTeam();

        if (playerTeam == null || ownerTeam == null) {
            return;
        }

        if (playerTeam.getColor() == TeamColor.SPECTATOR || ownerTeam.getColor() == TeamColor.SPECTATOR) {
            return;
        }

        boolean isEnemy = playerTeam.getColor() != ownerTeam.getColor();

        if (isEnemy) {
            return;
        }

        boolean isModeCW = PTC.getInstance().getGameManager().getGlobalSettings().isModeCW();

        if (isModeCW) {
            return;
        }

        e.setCancelled(true);
        player.sendMessage(Statics.c("&cEste cofre está protegido por &f" + ownerGamePlayer.getName()));
        LogSystem.debug(LogCategory.GAME, "Acceso denegado a cofre protegido:", player.getName(), "intentó abrir cofre de", ownerGamePlayer.getName());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChestBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return;
        }

        Location chestLocation = block.getLocation();
        UUID ownerUUID = Statics.protectedChests.get(chestLocation);

        if (ownerUUID == null) {
            return;
        }

        Statics.protectedChests.remove(chestLocation);

        GamePlayer ownerGamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(ownerUUID);
        if (ownerGamePlayer != null && ownerGamePlayer.getChests() > 0) {
            ownerGamePlayer.setChests(ownerGamePlayer.getChests() - 1);

            Player ownerPlayer = ownerGamePlayer.getPlayer();
            if (ownerPlayer != null && ownerPlayer.isOnline()) {
                ownerPlayer.sendMessage(Statics.c(String.format("&eUn cofre protegido fue destruido &7(%s/%s)", ownerGamePlayer.getChests(), Statics.MAX_CHESTS)));
            }
        }

        Player breaker = e.getPlayer();
        breaker.sendMessage(Statics.c("&eHas destruido un cofre protegido"));
        LogSystem.debug(LogCategory.GAME, "Cofre protegido destruido por:", breaker.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID playerUUID = player.getUniqueId();

        int removedCount = 0;
        Iterator<Map.Entry<Location, UUID>> iterator = Statics.protectedChests.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Location, UUID> entry = iterator.next();
            if (entry.getValue().equals(playerUUID)) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(playerUUID);
            if (gamePlayer != null) {
                gamePlayer.setChests(0);
            }

            LogSystem.info(LogCategory.GAME, "Cofres desprotegidos por desconexión:", player.getName(), "(" + removedCount + " cofres)");
        }
    }
}