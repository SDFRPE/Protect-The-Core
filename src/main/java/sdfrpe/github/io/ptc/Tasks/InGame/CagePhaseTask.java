package sdfrpe.github.io.ptc.Tasks.InGame;

import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Game.Cage.CageManager;
import sdfrpe.github.io.ptc.Game.Settings.ArenaSettings;
import sdfrpe.github.io.ptc.Listeners.Game.CagePhaseListener;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TimeOfDay;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Managers.GlobalTabManager;
import sdfrpe.github.io.ptc.Utils.PlayerTabUpdater;
import sdfrpe.github.io.ptc.Utils.Statics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CagePhaseTask extends PTCRunnable {
    private int countdown = 15;
    private CagePhaseListener listener;

    public CagePhaseTask() {
        Console.log("Started: CagePhaseTask (15 segundos)");

        this.listener = new CagePhaseListener();
        PTC.getInstance().getListenerManager().register(listener);

        Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
            applyArenaWeatherAndTime();
            createAllCages();
            createScoreboards();
            PlayerTabUpdater.updateAllPlayerTabs();
        });

        this.updateBoss();
    }

    private void applyArenaWeatherAndTime() {
        try {
            ArenaSettings settings = PTC.getInstance().getGameManager().getArena().getArenaSettings();
            if (settings == null) {
                LogSystem.error(LogCategory.GAME, "ArenaSettings es null durante cage phase");
                return;
            }

            World world = Bukkit.getWorld(settings.getName());
            if (world == null) {
                LogSystem.error(LogCategory.GAME, "Mundo no encontrado:", settings.getName());
                return;
            }

            TimeOfDay timeOfDay = settings.getTimeOfDay();
            if (timeOfDay != null) {
                world.setTime(timeOfDay.getTicks());
                LogSystem.debug(LogCategory.GAME, "Tiempo aplicado en cage phase:", timeOfDay.getDisplayName());
            }

            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(0);
            world.setGameRuleValue("doWeatherCycle", "false");
            LogSystem.info(LogCategory.GAME, "Clima desactivado durante cage phase");

        } catch (Exception e) {
            LogSystem.error(LogCategory.GAME, "Error aplicando clima en cage phase:", e.getMessage());
        }
    }

    private void createAllCages() {
        LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
        LogSystem.info(LogCategory.GAME, "CREANDO CÁPSULAS PARA TODOS LOS EQUIPOS");
        LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

        for (ArenaTeam team : PTC.getInstance().getGameManager().getGameSettings().getTeamList().values()) {
            if (team.countPlayers() > 0) {
                CageManager.createCages(team);
            }
        }
    }

    private void createScoreboards() {
        LogSystem.info(LogCategory.GAME, "Creando scoreboards para cage phase...");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(onlinePlayer.getUniqueId());
            if (gamePlayer != null) {
                try {
                    gamePlayer.createPBoard();
                    LogSystem.debug(LogCategory.GAME, "Scoreboard creado para:", gamePlayer.getName());
                } catch (Exception e) {
                    LogSystem.error(LogCategory.GAME, "Error creando scoreboard:", gamePlayer.getName(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void onTick() {
        this.updateBoss();

        if (this.countdown <= 0) {
            finishCagePhase();
            this.cancel();
            return;
        }

        this.countdown--;
    }

    private void finishCagePhase() {
        LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
        LogSystem.info(LogCategory.GAME, "FINALIZANDO FASE DE CÁPSULAS");
        LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

        PTC.getInstance().getListenerManager().unregister(CagePhaseListener.class);

        Statics.gameStatus = GameStatus.IN_GAME;

        Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
            CageManager.destroyAllCages();

            PTC.getInstance().getGameManager().getGameSettings().teleportPlayersToSpawns();

            PlayerTabUpdater.updateAllPlayerTabs();

            (new ArenaTask()).run();
        });
    }

    private void updateBoss() {
        float percentage = (float) (this.countdown * 100) / 15.0f;
        BossBarAPI.update(Statics.c(String.format("&e&lPREPARÁNDOSE... &f%ds", this.countdown)), percentage);
    }
}