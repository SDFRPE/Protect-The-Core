package sdfrpe.github.io.ptc.Listeners.General;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.util.UUID;

public class ClanWarAccessListener implements Listener {
    private static final long TEN_MINUTES_IN_MILLIS = 10 * 60 * 1000;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
            return;
        }

        UUID playerUuid = event.getPlayer().getUniqueId();

        WarAccessStatus accessStatus = checkWarAccess();

        switch (accessStatus.status) {
            case NO_WAR:
                event.disallow(
                        PlayerLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "═══════════════════════════════════\n" +
                                ChatColor.YELLOW + "GUERRA DE CLANES\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.WHITE + "La arena aún no está lista.\n" +
                                ChatColor.WHITE + "Espera a que se programe el inicio.\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.RED + "═══════════════════════════════════"
                );
                LogSystem.info(LogCategory.NETWORK, "Jugador bloqueado - Guerra no asignada aún:", playerUuid.toString());
                return;

            case TOO_EARLY:
                long minutesRemaining = accessStatus.minutesUntilAccess;
                event.disallow(
                        PlayerLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "═══════════════════════════════════\n" +
                                ChatColor.YELLOW + "GUERRA PROGRAMADA\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.WHITE + "Podrás entrar en:\n" +
                                ChatColor.AQUA + "▸ " + ChatColor.BOLD + minutesRemaining + " minutos\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.GOLD + "Clanes:\n" +
                                ChatColor.BLUE + "▸ " + accessStatus.warInfo.blueTag + " " +
                                ChatColor.GRAY + "vs " + ChatColor.RED + accessStatus.warInfo.redTag + "\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.RED + "═══════════════════════════════════"
                );
                LogSystem.info(LogCategory.NETWORK, "Jugador bloqueado - Faltan",
                        minutesRemaining + " minutos:", playerUuid.toString());
                return;

            case ERROR:
                event.disallow(
                        PlayerLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "═══════════════════════════════════\n" +
                                ChatColor.YELLOW + "ERROR DE CONFIGURACIÓN\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.WHITE + "No se pudo cargar la información\n" +
                                ChatColor.WHITE + "de la guerra. Intenta más tarde.\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.RED + "═══════════════════════════════════"
                );
                LogSystem.warn(LogCategory.NETWORK, "Error cargando guerra, jugador rechazado:", playerUuid.toString());
                return;

            case READY:
                PlayerClanStatus clanStatus = getPlayerClanStatus(playerUuid, accessStatus.warInfo);

                if (clanStatus.getType() == PlayerClanStatusType.BLUE_TEAM ||
                        clanStatus.getType() == PlayerClanStatusType.RED_TEAM) {

                    try {
                        Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
                        if (ptcClans != null && ptcClans.isEnabled()) {
                            Class<?> ptcClansClass = Class.forName("sdfrpe.github.io.ptcclans.PTCClans");
                            Object instance = ptcClansClass.getMethod("getInstance").invoke(null);
                            Object adapter = instance.getClass().getMethod("getClanWarAdapter").invoke(instance);

                            if (adapter != null) {
                                String clanTag = clanStatus.getClanTag();
                                adapter.getClass().getMethod("notifyTeamConnected", String.class)
                                        .invoke(adapter, clanTag);

                                LogSystem.info(LogCategory.NETWORK, "Notificado conexión de clan:", clanTag);
                            }
                        }
                    } catch (Exception e) {
                        LogSystem.error(LogCategory.NETWORK, "Error notificando conexión:", e.getMessage());
                    }
                }
                break;
        }

        PlayerClanStatus status = getPlayerClanStatus(playerUuid, accessStatus.warInfo);

        switch (status.getType()) {
            case BLUE_TEAM:
                LogSystem.info(LogCategory.TEAM, "Jugador autorizado (Equipo AZUL):",
                        playerUuid.toString(), "-", accessStatus.warInfo.blueTag);
                break;

            case RED_TEAM:
                LogSystem.info(LogCategory.TEAM, "Jugador autorizado (Equipo ROJO):",
                        playerUuid.toString(), "-", accessStatus.warInfo.redTag);
                break;

            case NO_CLAN:
                event.disallow(
                        PlayerLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "═══════════════════════════════════\n" +
                                ChatColor.YELLOW + "ARENA DE GUERRA\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.WHITE + "Debes pertenecer a un clan\n" +
                                ChatColor.WHITE + "para participar en esta guerra.\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.AQUA + "Usa /clan para crear o unirte\n" +
                                ChatColor.AQUA + "a un clan.\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.RED + "═══════════════════════════════════"
                );
                LogSystem.info(LogCategory.NETWORK, "Jugador sin clan rechazado:", playerUuid.toString());
                break;

            case WRONG_CLAN:
                event.disallow(
                        PlayerLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "═══════════════════════════════════\n" +
                                ChatColor.YELLOW + "GUERRA DE CLANES\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.WHITE + "Tu clan " + ChatColor.AQUA + "[" + status.getClanTag() + "]\n" +
                                ChatColor.WHITE + "no participa en esta guerra.\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.YELLOW + "Clanes en guerra:\n" +
                                ChatColor.BLUE + "▸ " + accessStatus.warInfo.blueTag + " " +
                                ChatColor.GRAY + "vs " + ChatColor.RED + accessStatus.warInfo.redTag + "\n" +
                                ChatColor.GRAY + "\n" +
                                ChatColor.RED + "═══════════════════════════════════"
                );
                LogSystem.info(LogCategory.NETWORK, "Jugador de clan no participante rechazado:",
                        playerUuid.toString(), "- Clan:", status.getClanTag());
                break;
        }
    }

    private WarAccessStatus checkWarAccess() {
        try {
            Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
            if (ptcClans == null || !ptcClans.isEnabled()) {
                return new WarAccessStatus(AccessStatusType.NO_WAR);
            }

            Class<?> ptcClansClass = Class.forName("sdfrpe.github.io.ptcclans.PTCClans");
            Object instance = ptcClansClass.getMethod("getInstance").invoke(null);

            Object adapter = instance.getClass().getMethod("getClanWarAdapter").invoke(instance);
            if (adapter == null) {
                return new WarAccessStatus(AccessStatusType.NO_WAR);
            }

            Boolean hasActiveWar = (Boolean) adapter.getClass().getMethod("hasActiveWar").invoke(adapter);
            if (!hasActiveWar) {
                return new WarAccessStatus(AccessStatusType.NO_WAR);
            }

            String warId = (String) adapter.getClass().getMethod("getWarId").invoke(adapter);
            if (warId == null) {
                return new WarAccessStatus(AccessStatusType.NO_WAR);
            }

            String blueTag = (String) adapter.getClass().getMethod("getBlueClanTag").invoke(adapter);
            String redTag = (String) adapter.getClass().getMethod("getRedClanTag").invoke(adapter);
            Long warStartTime = (Long) adapter.getClass().getMethod("getWarStartTime").invoke(adapter);

            if (blueTag == null || redTag == null || warStartTime == null || warStartTime == 0) {
                return new WarAccessStatus(AccessStatusType.ERROR);
            }

            ClanWarInfo warInfo = new ClanWarInfo(blueTag, redTag);

            long currentTime = System.currentTimeMillis();
            long timeUntilStart = warStartTime - currentTime;

            if (timeUntilStart > TEN_MINUTES_IN_MILLIS) {
                long minutesUntilAccess = (timeUntilStart - TEN_MINUTES_IN_MILLIS) / 60000;
                return new WarAccessStatus(AccessStatusType.TOO_EARLY, warInfo, minutesUntilAccess);
            }

            return new WarAccessStatus(AccessStatusType.READY, warInfo, 0);

        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error verificando acceso a guerra:", e.getMessage());
            return new WarAccessStatus(AccessStatusType.ERROR);
        }
    }

    private PlayerClanStatus getPlayerClanStatus(UUID playerUuid, ClanWarInfo warInfo) {
        try {
            Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
            if (ptcClans == null || !ptcClans.isEnabled()) {
                return new PlayerClanStatus(PlayerClanStatusType.NO_CLAN, null);
            }

            Class<?> clanManagerClass = Class.forName("sdfrpe.github.io.ptcclans.Managers.ClanManager");
            Object clanManagerInstance = clanManagerClass.getMethod("getInstance").invoke(null);

            Object playerClan = clanManagerClass.getMethod("getPlayerClan", UUID.class)
                    .invoke(clanManagerInstance, playerUuid);

            if (playerClan == null) {
                return new PlayerClanStatus(PlayerClanStatusType.NO_CLAN, null);
            }

            String clanTag = (String) playerClan.getClass().getMethod("getTag").invoke(playerClan);

            if (clanTag.equals(warInfo.blueTag)) {
                return new PlayerClanStatus(PlayerClanStatusType.BLUE_TEAM, clanTag);
            } else if (clanTag.equals(warInfo.redTag)) {
                return new PlayerClanStatus(PlayerClanStatusType.RED_TEAM, clanTag);
            } else {
                return new PlayerClanStatus(PlayerClanStatusType.WRONG_CLAN, clanTag);
            }

        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error verificando clan del jugador:", e.getMessage());
            return new PlayerClanStatus(PlayerClanStatusType.NO_CLAN, null);
        }
    }

    private static class WarAccessStatus {
        final AccessStatusType status;
        final ClanWarInfo warInfo;
        final long minutesUntilAccess;

        WarAccessStatus(AccessStatusType status) {
            this(status, null, 0);
        }

        WarAccessStatus(AccessStatusType status, ClanWarInfo warInfo, long minutesUntilAccess) {
            this.status = status;
            this.warInfo = warInfo;
            this.minutesUntilAccess = minutesUntilAccess;
        }
    }

    private enum AccessStatusType {
        NO_WAR,
        TOO_EARLY,
        READY,
        ERROR
    }

    private static class ClanWarInfo {
        final String blueTag;
        final String redTag;

        ClanWarInfo(String blueTag, String redTag) {
            this.blueTag = blueTag;
            this.redTag = redTag;
        }
    }

    private static class PlayerClanStatus {
        private final PlayerClanStatusType type;
        private final String clanTag;

        PlayerClanStatus(PlayerClanStatusType type, String clanTag) {
            this.type = type;
            this.clanTag = clanTag;
        }

        PlayerClanStatusType getType() {
            return type;
        }

        String getClanTag() {
            return clanTag;
        }
    }

    private enum PlayerClanStatusType {
        BLUE_TEAM,
        RED_TEAM,
        NO_CLAN,
        WRONG_CLAN
    }
}