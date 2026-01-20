package sdfrpe.github.io.ptc.Tasks;

import org.bukkit.Bukkit;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Database.Engines.ServerAPI;
import sdfrpe.github.io.ptc.Game.Arena.Arena;
import sdfrpe.github.io.ptc.Game.Server.ServerInfo;
import sdfrpe.github.io.ptc.Tasks.InGame.ArenaTask;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;

public class ServerStatusUpdater extends PTCRunnable {
    private final ServerAPI serverAPI;
    private final PTC ptc;
    private int tickCounter = 0;
    private final int UPDATE_INTERVAL = 20;

    public ServerStatusUpdater() {
        this.serverAPI = new ServerAPI();
        this.ptc = PTC.getInstance();
    }

    @Override
    public void onTick() {
        this.tickCounter++;

        if (this.tickCounter >= this.UPDATE_INTERVAL) {
            this.tickCounter = 0;
            this.updateServerStatus();
        }
    }

    private void updateServerStatus() {
        try {
            ServerInfo serverInfo = this.createCurrentServerInfo();

            boolean success = this.serverAPI.updateServer(serverInfo);

            if (!success) {
                LogSystem.warn(LogCategory.NETWORK, "Fallo actualizando estado del servidor");
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error actualizando estado:", e.getMessage());
        }
    }

    private ServerInfo createCurrentServerInfo() {
        ServerInfo info = new ServerInfo();

        String serverName = this.ptc.getGameManager().getGlobalSettings().getServerName();
        info.setServerName(serverName);

        Arena currentArena = this.ptc.getGameManager().getArena();
        String mapName = "Clasica";
        if (currentArena != null && currentArena.getArenaSettings() != null) {
            mapName = currentArena.getArenaSettings().getName();
        }
        info.setMapName(mapName);

        GameStatus currentStatus = sdfrpe.github.io.ptc.Utils.Statics.gameStatus;
        info.setStatus(currentStatus);

        int currentPlayers = Bukkit.getOnlinePlayers().size();
        info.setCurrentPlayers(currentPlayers);

        int maxPlayers = this.ptc.getGameManager().getGlobalSettings().getArenaPlayers().getMaxPlayers();
        info.setMaxPlayers(maxPlayers);

        boolean isModeCW = this.ptc.getGameManager().getGlobalSettings().isModeCW();
        info.setModeCW(isModeCW);

        if (isModeCW) {
            try {
                Object adapter = this.ptc.getGameManager().getClanWarAdapter();
                if (adapter != null) {
                    String blueTag = (String) adapter.getClass().getMethod("getBlueClanTag").invoke(adapter);
                    String redTag = (String) adapter.getClass().getMethod("getRedClanTag").invoke(adapter);

                    if (blueTag != null && redTag != null) {
                        info.setCwInfo(blueTag + " vs " + redTag);
                    }
                }
            } catch (Exception e) {
                LogSystem.debug(LogCategory.NETWORK, "No se pudo obtener info CW:", e.getMessage());
            }
        }

        if (currentStatus == GameStatus.IN_GAME || currentStatus == GameStatus.CAGE_PHASE) {
            String gameTime = this.getGameTime();
            info.setGameTime(gameTime);
            info.setVipOnly(true);
        } else {
            info.setGameTime("00:00");
            info.setVipOnly(false);
        }

        info.setLastUpdate(System.currentTimeMillis());

        return info;
    }

    private String getGameTime() {
        try {
            ArenaTask arenaTask = ArenaTask.getInstance();

            if (arenaTask == null) {
                return "00:00";
            }

            int timeRemaining = arenaTask.getTimeRemainingSeconds();
            int minutes = timeRemaining / 60;
            int seconds = timeRemaining % 60;

            return String.format("%02d:%02d", minutes, seconds);
        } catch (Exception e) {
            LogSystem.debug(LogCategory.PERFORMANCE, "Error obteniendo tiempo de juego:", e.getMessage());
            return "00:00";
        }
    }

    public void removeServerFromDatabase() {
        try {
            String serverName = this.ptc.getGameManager().getGlobalSettings().getServerName();
            boolean success = this.serverAPI.deleteServer(serverName);

            if (success) {
                LogSystem.info(LogCategory.NETWORK, "Servidor removido de DB:", serverName);
            } else {
                LogSystem.warn(LogCategory.NETWORK, "Fallo removiendo servidor de DB");
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error removiendo servidor:", e.getMessage());
        }
    }
}