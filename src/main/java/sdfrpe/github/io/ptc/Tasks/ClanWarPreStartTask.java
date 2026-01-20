package sdfrpe.github.io.ptc.Tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.lang.reflect.Method;

public class ClanWarPreStartTask implements Runnable {
    private final Plugin plugin;
    private final GameManager gameManager;
    private String blueTag = "AZUL";
    private String redTag = "ROJO";
    private long warStartTime = 0;
    private boolean initialized = false;
    private boolean countdownFinished = false;
    private boolean infoLoggedOnce = false;
    private boolean forfeitChecked = false;

    public ClanWarPreStartTask(GameManager gameManager) {
        this.plugin = PTC.getInstance();
        this.gameManager = gameManager;
        loadWarInfo();
    }

    private void loadWarInfo() {
        try {
            Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
            if (ptcClans != null && ptcClans.isEnabled()) {
                Class<?> ptcClansClass = Class.forName("sdfrpe.github.io.ptcclans.PTCClans");
                Object instance = ptcClansClass.getMethod("getInstance").invoke(null);

                if (instance != null) {
                    Object clanManager = instance.getClass().getMethod("getClanManager").invoke(instance);
                    Object adapter = instance.getClass().getMethod("getClanWarAdapter").invoke(instance);

                    if (adapter != null) {
                        adapter.getClass().getMethod("syncWithManager").invoke(adapter);

                        Method getBlueTagMethod = adapter.getClass().getMethod("getBlueClanTag");
                        Method getRedTagMethod = adapter.getClass().getMethod("getRedClanTag");
                        Method getWarStartTimeMethod = adapter.getClass().getMethod("getWarStartTime");

                        String loadedBlueTag = (String) getBlueTagMethod.invoke(adapter);
                        String loadedRedTag = (String) getRedTagMethod.invoke(adapter);

                        if (loadedBlueTag != null && !loadedBlueTag.equals("AZUL")) {
                            blueTag = loadedBlueTag;
                            redTag = loadedRedTag;

                            if (clanManager != null) {
                                Method ensureClansLoadedMethod = clanManager.getClass().getMethod("ensureClansLoaded", String[].class);
                                String[] clansToLoad = new String[]{blueTag, redTag};
                                ensureClansLoadedMethod.invoke(clanManager, (Object) clansToLoad);
                            }

                            Long startTime = (Long) getWarStartTimeMethod.invoke(adapter);
                            if (startTime != null && startTime > 0) {
                                warStartTime = startTime;
                            }

                            initialized = true;

                            if (!infoLoggedOnce) {
                                LogSystem.info(LogCategory.NETWORK, "Información de guerra cargada:");
                                LogSystem.info(LogCategory.NETWORK, "  AZUL:", blueTag);
                                LogSystem.info(LogCategory.NETWORK, "  ROJO:", redTag);
                                LogSystem.info(LogCategory.NETWORK, "  Hora de inicio:", String.valueOf(warStartTime));
                                LogSystem.info(LogCategory.NETWORK, "  Clanes forzados a recargar desde API");
                                infoLoggedOnce = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error cargando información de guerra:", e.getMessage());
        }
    }

    @Override
    public void run() {
        if (Statics.gameStatus == GameStatus.IN_GAME || Statics.gameStatus == GameStatus.ENDED) {
            return;
        }

        if (!initialized || warStartTime == 0) {
            loadWarInfo();
            if (!initialized || warStartTime == 0) {
                showWaitingMessage();
                return;
            }
        }

        long currentTime = System.currentTimeMillis();
        long timeUntilStart = warStartTime - currentTime;

        if (!forfeitChecked && timeUntilStart <= 60000 && timeUntilStart > 0) {
            checkTeamPresenceAndDeclareOutcome();
            return;
        }

        if (timeUntilStart <= 0) {
            if (!countdownFinished) {
                countdownFinished = true;
                LogSystem.info(LogCategory.GAME, "Countdown finalizado - Verificando jugadores para inicio");
            }

            tryStartGame();
            return;
        }

        long minutes = timeUntilStart / 60000;
        long seconds = (timeUntilStart % 60000) / 1000;

        String message;
        float progress;

        if (minutes > 5) {
            message = Statics.c(String.format("&e&lGUERRA PROGRAMADA &8│ &6%s &cvs &9%s &8│ &7Inicia en: &f%d minutos",
                    blueTag, redTag, minutes));
            progress = 100f;
        } else if (minutes > 0) {
            message = Statics.c(String.format("&e&lGUERRA PRÓXIMA &8│ &6%s &cvs &9%s &8│ &7Inicia en: &f%dm %ds",
                    blueTag, redTag, minutes, seconds));
            progress = (float) (timeUntilStart / (5.0 * 60000)) * 100f;
        } else if (seconds > 30) {
            message = Statics.c(String.format("&c&lGUERRA INICIANDO &8│ &6%s &cvs &9%s &8│ &e%d segundos",
                    blueTag, redTag, seconds));
            progress = (float) (timeUntilStart / 30000.0) * 100f;
        } else {
            message = Statics.c(String.format("&c&l¡PREPARADOS! &8│ &6%s &cvs &9%s &8│ &4&l%d",
                    blueTag, redTag, seconds));
            progress = (float) (timeUntilStart / 30000.0) * 100f;

            if (seconds <= 5 && seconds > 0) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.NOTE_PLING, 1.0f, 1.0f);
                }
            }
        }

        BossBarAPI.update(message, Math.max(1f, Math.min(100f, progress)));
    }

    private void checkTeamPresenceAndDeclareOutcome() {
        forfeitChecked = true;

        try {
            Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
            if (ptcClans == null || !ptcClans.isEnabled()) {
                LogSystem.error(LogCategory.GAME, "PTCClans no disponible para verificar equipos");
                return;
            }

            Class<?> ptcClansClass = Class.forName("sdfrpe.github.io.ptcclans.PTCClans");
            Object instance = ptcClansClass.getMethod("getInstance").invoke(null);
            Object adapter = instance.getClass().getMethod("getClanWarAdapter").invoke(instance);

            if (adapter == null) {
                LogSystem.error(LogCategory.GAME, "Adapter no disponible");
                return;
            }

            boolean hasBlueTeamPlayers = false;
            boolean hasRedTeamPlayers = false;

            for (Player player : Bukkit.getOnlinePlayers()) {
                Method isPlayerInBlueMethod = adapter.getClass().getMethod("isPlayerInBlueClan", java.util.UUID.class);
                Method isPlayerInRedMethod = adapter.getClass().getMethod("isPlayerInRedClan", java.util.UUID.class);

                Boolean isBlue = (Boolean) isPlayerInBlueMethod.invoke(adapter, player.getUniqueId());
                Boolean isRed = (Boolean) isPlayerInRedMethod.invoke(adapter, player.getUniqueId());

                if (isBlue != null && isBlue) {
                    hasBlueTeamPlayers = true;
                }
                if (isRed != null && isRed) {
                    hasRedTeamPlayers = true;
                }
            }

            LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
            LogSystem.info(LogCategory.GAME, "VERIFICACIÓN DE EQUIPOS (60s antes)");
            LogSystem.info(LogCategory.GAME, "Equipo AZUL (" + blueTag + "):", hasBlueTeamPlayers ? "PRESENTE" : "AUSENTE");
            LogSystem.info(LogCategory.GAME, "Equipo ROJO (" + redTag + "):", hasRedTeamPlayers ? "PRESENTE" : "AUSENTE");

            Object currentWar = adapter.getClass().getMethod("getCurrentWar").invoke(adapter);

            if (currentWar == null) {
                LogSystem.error(LogCategory.GAME, "No hay guerra actual");
                return;
            }

            if (hasBlueTeamPlayers && hasRedTeamPlayers) {
                LogSystem.info(LogCategory.GAME, "Ambos equipos presentes - Guerra procederá normalmente");
                LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
                return;
            }

            if (hasBlueTeamPlayers && !hasRedTeamPlayers) {
                LogSystem.info(LogCategory.GAME, "Solo equipo AZUL presente - FORFEIT INMEDIATO");
                LogSystem.info(LogCategory.GAME, "Ganador: " + blueTag);
                LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

                currentWar.getClass().getMethod("setForfeitReason", String.class)
                        .invoke(currentWar, "forfeit_red_no_show_at_start");
                currentWar.getClass().getMethod("setFinished", boolean.class)
                        .invoke(currentWar, true);
                currentWar.getClass().getMethod("setWinnerClanTag", String.class)
                        .invoke(currentWar, blueTag);

                Method onWarFinishedMethod = adapter.getClass().getMethod("onWarFinished", String.class);
                onWarFinishedMethod.invoke(adapter, blueTag);

                gameManager.stopClanWarCountdown();
                BossBarAPI.setEnabled(false);

                Bukkit.broadcastMessage(Statics.c("&c&l════════════════════════════════════"));
                Bukkit.broadcastMessage(Statics.c("&e&lGUERRA FINALIZADA POR FORFEIT"));
                Bukkit.broadcastMessage(Statics.c(""));
                Bukkit.broadcastMessage(Statics.c("&a&lGanador: &f" + blueTag));
                Bukkit.broadcastMessage(Statics.c("&7Motivo: &cEquipo contrario no se presentó"));
                Bukkit.broadcastMessage(Statics.c("&c&l════════════════════════════════════"));

                return;
            }

            if (!hasBlueTeamPlayers && hasRedTeamPlayers) {
                LogSystem.info(LogCategory.GAME, "Solo equipo ROJO presente - FORFEIT INMEDIATO");
                LogSystem.info(LogCategory.GAME, "Ganador: " + redTag);
                LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

                currentWar.getClass().getMethod("setForfeitReason", String.class)
                        .invoke(currentWar, "forfeit_blue_no_show_at_start");
                currentWar.getClass().getMethod("setFinished", boolean.class)
                        .invoke(currentWar, true);
                currentWar.getClass().getMethod("setWinnerClanTag", String.class)
                        .invoke(currentWar, redTag);

                Method onWarFinishedMethod = adapter.getClass().getMethod("onWarFinished", String.class);
                onWarFinishedMethod.invoke(adapter, redTag);

                gameManager.stopClanWarCountdown();
                BossBarAPI.setEnabled(false);

                Bukkit.broadcastMessage(Statics.c("&c&l════════════════════════════════════"));
                Bukkit.broadcastMessage(Statics.c("&e&lGUERRA FINALIZADA POR FORFEIT"));
                Bukkit.broadcastMessage(Statics.c(""));
                Bukkit.broadcastMessage(Statics.c("&a&lGanador: &f" + redTag));
                Bukkit.broadcastMessage(Statics.c("&7Motivo: &cEquipo contrario no se presentó"));
                Bukkit.broadcastMessage(Statics.c("&c&l════════════════════════════════════"));

                return;
            }

            if (!hasBlueTeamPlayers && !hasRedTeamPlayers) {
                LogSystem.info(LogCategory.GAME, "Ningún equipo presente - GUERRA CANCELADA");
                LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

                currentWar.getClass().getMethod("setForfeitReason", String.class)
                        .invoke(currentWar, "cancelled_both_no_show");
                currentWar.getClass().getMethod("setFinished", boolean.class)
                        .invoke(currentWar, true);
                currentWar.getClass().getMethod("setWinnerClanTag", String.class)
                        .invoke(currentWar, null);

                Method onWarFinishedMethod = adapter.getClass().getMethod("onWarFinished", String.class);
                onWarFinishedMethod.invoke(adapter, null);

                gameManager.stopClanWarCountdown();
                BossBarAPI.setEnabled(false);

                Bukkit.broadcastMessage(Statics.c("&c&l════════════════════════════════════"));
                Bukkit.broadcastMessage(Statics.c("&e&lGUERRA CANCELADA"));
                Bukkit.broadcastMessage(Statics.c(""));
                Bukkit.broadcastMessage(Statics.c("&7Motivo: &cNingún equipo se presentó"));
                Bukkit.broadcastMessage(Statics.c("&c&l════════════════════════════════════"));

                return;
            }

        } catch (Exception e) {
            LogSystem.error(LogCategory.GAME, "Error verificando presencia de equipos:", e.getMessage());
            e.printStackTrace();
        }
    }

    private void showWaitingMessage() {
        String message = Statics.c("&e&lESPERANDO GUERRA &8│ &7El servidor está listo para recibir un desafío");
        BossBarAPI.update(message, 100f);
    }

    private void tryStartGame() {
        if (Statics.gameStatus != GameStatus.LOBBY) {
            LogSystem.info(LogCategory.GAME, "Estado ya cambió a:", Statics.gameStatus.name());
            gameManager.stopClanWarCountdown();
            return;
        }

        int onlinePlayers = Bukkit.getOnlinePlayers().size();

        try {
            Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
            if (ptcClans != null && ptcClans.isEnabled()) {
                Class<?> ptcClansClass = Class.forName("sdfrpe.github.io.ptcclans.PTCClans");
                Object instance = ptcClansClass.getMethod("getInstance").invoke(null);
                Object adapter = instance.getClass().getMethod("getClanWarAdapter").invoke(instance);

                if (adapter != null) {
                    Boolean bothConnected = (Boolean) adapter.getClass().getMethod("areBothTeamsConnected").invoke(adapter);

                    if (bothConnected != null && bothConnected) {
                        if (onlinePlayers >= 2) {
                            LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
                            LogSystem.info(LogCategory.GAME, "INICIANDO CLAN WAR");
                            LogSystem.info(LogCategory.GAME, "Ambos equipos conectados");
                            LogSystem.info(LogCategory.GAME, "Jugadores:", String.valueOf(onlinePlayers));
                            LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

                            Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
                                try {
                                    gameManager.stopClanWarCountdown();
                                    PTC.getInstance().getGameManager().getGameSettings().startGame();
                                } catch (Exception e) {
                                    LogSystem.error(LogCategory.GAME, "Error iniciando juego:", e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.GAME, "Error intentando iniciar juego:", e.getMessage());
            e.printStackTrace();
        }

        String waitMessage = Statics.c(String.format(
                "&c&lFALTAN JUGADORES &8│ &6%s &cvs &9%s &8│ &7Jugadores: &f%d/2",
                blueTag, redTag, onlinePlayers
        ));
        BossBarAPI.update(waitMessage, 100f);
    }
}