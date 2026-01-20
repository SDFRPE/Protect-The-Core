package sdfrpe.github.io.ptc.Listeners.Game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Extra.GlobalSettings;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

public class ClanWarIntegrationListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GlobalSettings settings = PTC.getInstance().getGameManager().getGlobalSettings();

        if (settings.isModeCW()) {
            player.sendMessage(ChatColor.RED + "═══════════════════════════════════");
            player.sendMessage(ChatColor.GOLD + "MODO CLAN WAR ACTIVO");
            player.sendMessage(ChatColor.YELLOW + "Arena: " + ChatColor.WHITE + settings.getActiveClanWarArena());
            player.sendMessage(ChatColor.RED + "═══════════════════════════════════");

            if (Bukkit.getPluginManager().isPluginEnabled("PTCClans")) {
                try {
                    Object clansPlugin = Bukkit.getPluginManager().getPlugin("PTCClans");
                    if (clansPlugin != null) {
                        Object clanManager = clansPlugin.getClass().getMethod("getClanManager").invoke(clansPlugin);
                        Object playerClan = clanManager.getClass().getMethod("getPlayerClan", java.util.UUID.class)
                                .invoke(clanManager, player.getUniqueId());

                        if (playerClan == null) {
                            player.kickPlayer(ChatColor.RED + "Solo miembros de clanes en guerra pueden unirse.");
                            LogSystem.info(LogCategory.NETWORK, "Jugador sin clan expulsado del modo CW:", player.getName());
                        }
                    }
                } catch (Exception e) {
                    LogSystem.error(LogCategory.NETWORK, "Error verificando clan del jugador:", e.getMessage());
                }
            }
        }
    }
}