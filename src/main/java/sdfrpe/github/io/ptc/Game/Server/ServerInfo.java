package sdfrpe.github.io.ptc.Game.Server;

import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;

public class ServerInfo {
    private String serverName;
    private String mapName;
    private GameStatus status;
    private int currentPlayers;
    private int maxPlayers;
    private String gameTime;
    private long lastUpdate;
    private boolean vipOnly;
    private boolean modeCW;
    private String cwInfo;

    public ServerInfo() {
        this.serverName = "Unknown";
        this.mapName = "Unknown";
        this.status = GameStatus.LOBBY;
        this.currentPlayers = 0;
        this.maxPlayers = 40;
        this.gameTime = "00:00";
        this.lastUpdate = System.currentTimeMillis();
        this.vipOnly = false;
        this.modeCW = false;
        this.cwInfo = null;
    }

    public ServerInfo(String serverName, String mapName, GameStatus status, int currentPlayers, int maxPlayers) {
        this.serverName = serverName;
        this.mapName = mapName;
        this.status = status;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.gameTime = "00:00";
        this.lastUpdate = System.currentTimeMillis();
        this.vipOnly = false;
        this.modeCW = false;
        this.cwInfo = null;
    }

    public boolean isOnline() {
        return (System.currentTimeMillis() - this.lastUpdate) < 30000;
    }

    public String getServerName() {
        return this.serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getMapName() {
        return this.mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public GameStatus getStatus() {
        return this.status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
        this.vipOnly = (status == GameStatus.IN_GAME || status == GameStatus.CAGE_PHASE);
    }

    public int getCurrentPlayers() {
        return this.currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getGameTime() {
        return this.gameTime;
    }

    public void setGameTime(String gameTime) {
        this.gameTime = gameTime;
    }

    public long getLastUpdate() {
        return this.lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isVipOnly() {
        return this.vipOnly;
    }

    public void setVipOnly(boolean vipOnly) {
        this.vipOnly = vipOnly;
    }

    public boolean isModeCW() {
        return this.modeCW;
    }

    public void setModeCW(boolean modeCW) {
        this.modeCW = modeCW;
    }

    public String getCwInfo() {
        return this.cwInfo;
    }

    public void setCwInfo(String cwInfo) {
        this.cwInfo = cwInfo;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "serverName='" + serverName + '\'' +
                ", mapName='" + mapName + '\'' +
                ", status=" + status +
                ", currentPlayers=" + currentPlayers +
                ", maxPlayers=" + maxPlayers +
                ", gameTime='" + gameTime + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", vipOnly=" + vipOnly +
                ", modeCW=" + modeCW +
                ", cwInfo='" + cwInfo + '\'' +
                '}';
    }
}