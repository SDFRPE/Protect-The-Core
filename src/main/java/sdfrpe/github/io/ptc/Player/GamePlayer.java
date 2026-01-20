package sdfrpe.github.io.ptc.Player;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.ClanLevel.ClanLevelSystem;
import sdfrpe.github.io.ptc.Game.Arena.Arena;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Game.Settings.ArenaSettings;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.PlayerTabUpdater;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.TabListUtils;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import sdfrpe.github.io.ptc.Utils.Menu.IMenu;
import sdfrpe.github.io.ptc.Utils.Scoreboard.PBoard;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class GamePlayer {
    private UUID uuid;
    private String name;
    private ArenaSettings arenaSettings;
    private ArenaTeam arenaTeam;
    private IMenu menu;
    private PBoard pBoard;
    private PlayerStats playerStats;
    private PlayerStats localStats;
    private int furnaces;
    private int chests;
    private Map<GamePlayer, Integer> dominated;
    private Set<GamePlayer> revenge;
    private TeamColor lastTeamColor = null;
    private long lastNametagUpdate = 0;
    private static final long NAMETAG_UPDATE_COOLDOWN = 1000;
    private static final int DOMINATION_KILLS = 5;
    public static Gson GSON = new Gson();
    private PTCRunnable playerTask = null;

    private int participationCount = 0;
    private static final int MAX_PARTICIPATIONS = 2;

    public GamePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.menu = null;
        this.playerStats = new PlayerStats();
        this.localStats = new PlayerStats();
        this.dominated = Maps.newHashMap();
        this.revenge = Sets.newConcurrentHashSet();
        this.participationCount = 0;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.uuid);
    }

    public void resetLocalStats() {
        this.localStats = new PlayerStats();
        this.dominated.clear();
        this.revenge.clear();
        LogSystem.debug(LogCategory.PLAYER, "Stats locales reseteadas para:", this.name);
    }

    public void incrementParticipation() {
        if (participationCount < MAX_PARTICIPATIONS) {
            participationCount++;
            LogSystem.debug(LogCategory.PLAYER, "Participación incrementada:",
                    this.name, "(" + participationCount + "/" + MAX_PARTICIPATIONS + ")");
        }
    }

    public boolean canUseJugarCommand() {
        return participationCount < MAX_PARTICIPATIONS;
    }

    public int getParticipationCount() {
        return participationCount;
    }

    public int getRemainingParticipations() {
        return Math.max(0, MAX_PARTICIPATIONS - participationCount);
    }

    public void resetParticipation() {
        int previousCount = participationCount;
        participationCount = 0;
        if (previousCount > 0) {
            LogSystem.debug(LogCategory.PLAYER, "Participaciones reseteadas:",
                    this.name, "(era " + previousCount + "/2)");
        }
    }

    public boolean isCurrentlyPlaying() {
        return arenaTeam != null &&
                arenaTeam.getColor() != TeamColor.SPECTATOR &&
                arenaTeam.getColor() != TeamColor.LOBBY &&
                !arenaTeam.isDeathTeam();
    }

    public void startPlayerRunnable() {
        if (this.playerTask != null) {
            this.playerTask.cancel();
            LogSystem.debug(LogCategory.PLAYER, "Tarea anterior cancelada para:", this.name);
        }

        this.playerTask = new PTCRunnable() {
            public void onTick() {
                Player player = GamePlayer.this.getPlayer();
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    GamePlayer.this.playerTask = null;
                    LogSystem.debug(LogCategory.PLAYER, "Cancelando task de", GamePlayer.this.name, "- jugador offline");
                    return;
                }

                if (!PTC.getInstance().getGameManager().getGlobalSettings().isLobbyMode()) {
                    if (GamePlayer.this.pBoard != null) {
                        GamePlayer.this.pBoard.update(GamePlayer.this.getGamePlayer(), false);
                    }
                    if (System.currentTimeMillis() % 1000 < 50) {
                        GamePlayer.this.updateTabList();
                    }
                    GamePlayer.this.updateNameTag();
                    TeamColor currentTeam = GamePlayer.this.arenaTeam != null ? GamePlayer.this.arenaTeam.getColor() : null;
                    if (currentTeam != GamePlayer.this.lastTeamColor) {
                        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
                            PlayerTabUpdater.updateAllPlayerTabs();
                        }, 2L);
                        GamePlayer.this.lastTeamColor = currentTeam;
                    }
                }
                if (GamePlayer.this.menu != null) {
                    GamePlayer.this.menu.addItems(player);
                }
            }
        };

        this.playerTask.run();
    }

    public void createPBoard() {
        if (PTC.getInstance().getGameManager().getGlobalSettings().isLobbyMode()) {
            return;
        }

        Player player = this.getPlayer();
        if (player != null) {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            player.setScoreboard(mainScoreboard);
        }

        if (this.pBoard != null) {
            this.pBoard = null;
        }

        this.pBoard = (new PBoard()).create(this);
        this.updateTabList();
        this.updateNameTag();

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            PlayerTabUpdater.updateAllPlayerTabs();
        }, 3L);
    }

    public void updateTabList() {
        if (PTC.getInstance().getGameManager().getGlobalSettings().isLobbyMode()) {
            return;
        }
        Player player = this.getPlayer();
        if (player == null) return;
        try {
            String header = "&b&l✦ PTC CORE ✦\n&7";
            String footer = "";

            int currentPlayers = Bukkit.getOnlinePlayers().size();
            int maxPlayers = PTC.getInstance().getGameManager().getGlobalSettings().getArenaPlayers().getMaxPlayers();
            boolean isModeCW = PTC.getInstance().getGameManager().getGlobalSettings().isModeCW();

            if (Statics.gameStatus == GameStatus.LOBBY) {
                footer = String.format("\n&7Jugadores: &b%d&7/&b%d\n&eSelecciona tu equipo\n", currentPlayers, maxPlayers);

                if (isModeCW) {
                    try {
                        Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
                        if (adapter != null) {
                            String blueTag = (String) adapter.getClass().getMethod("getBlueClanTag").invoke(adapter);
                            String redTag = (String) adapter.getClass().getMethod("getRedClanTag").invoke(adapter);
                            if (blueTag != null && redTag != null) {
                                footer += String.format("&9%s &7vs &c%s\n", blueTag, redTag);
                            }
                        }
                    } catch (Exception e) {
                        LogSystem.debug(LogCategory.PLAYER, "Error obteniendo info CW para tab:", e.getMessage());
                    }
                }

                Arena mostVotedArena = PTC.getInstance().getGameManager().getArenaManager().getMostVotedArena();
                if (mostVotedArena != null && mostVotedArena.getArenaSettings() != null) {
                    footer += String.format("&7Arena votada: &b%s\n", mostVotedArena.getArenaSettings().getName());
                }

            } else if (Statics.gameStatus == GameStatus.STARTING) {
                footer = String.format("\n&e&l¡INICIANDO PARTIDA!\n&7Jugadores: &b%d&7/&b%d\n", currentPlayers, maxPlayers);

                Arena selectedArena = PTC.getInstance().getGameManager().getArena();
                if (selectedArena != null && selectedArena.getArenaSettings() != null) {
                    footer += String.format("&7Arena: &b%s\n", selectedArena.getArenaSettings().getName());
                } else {
                    Arena mostVotedArena = PTC.getInstance().getGameManager().getArenaManager().getMostVotedArena();
                    if (mostVotedArena != null && mostVotedArena.getArenaSettings() != null) {
                        footer += String.format("&7Arena: &b%s\n", mostVotedArena.getArenaSettings().getName());
                    }
                }

                if (isModeCW) {
                    try {
                        Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
                        if (adapter != null) {
                            String blueTag = (String) adapter.getClass().getMethod("getBlueClanTag").invoke(adapter);
                            String redTag = (String) adapter.getClass().getMethod("getRedClanTag").invoke(adapter);
                            if (blueTag != null && redTag != null) {
                                footer += String.format("&9%s &7vs &c%s\n", blueTag, redTag);
                            }
                        }
                    } catch (Exception e) {
                        LogSystem.debug(LogCategory.PLAYER, "Error obteniendo info CW para tab:", e.getMessage());
                    }
                }

            } else if (Statics.gameStatus == GameStatus.CAGE_PHASE) {
                String arenaName = "Desconocida";
                if (PTC.getInstance().getGameManager().getArena() != null && PTC.getInstance().getGameManager().getArena().getArenaSettings() != null) {
                    arenaName = PTC.getInstance().getGameManager().getArena().getArenaSettings().getName();
                }

                footer = String.format("\n&e&lPREPARÁNDOSE...\n&7Arena: &b%s\n&7Jugadores: &b%d\n", arenaName, currentPlayers);

                if (isModeCW) {
                    try {
                        Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
                        if (adapter != null) {
                            String blueTag = (String) adapter.getClass().getMethod("getBlueClanTag").invoke(adapter);
                            String redTag = (String) adapter.getClass().getMethod("getRedClanTag").invoke(adapter);
                            if (blueTag != null && redTag != null) {
                                footer += String.format("&9%s &7vs &c%s\n", blueTag, redTag);
                            }
                        }
                    } catch (Exception e) {
                        LogSystem.debug(LogCategory.PLAYER, "Error obteniendo info CW para tab:", e.getMessage());
                    }
                }

                int activeTeams = 0;
                for (ArenaTeam team : PTC.getInstance().getGameManager().getGameSettings().getTeamList().values()) {
                    if (!team.isDeathTeam() && team.countPlayers() > 0) {
                        activeTeams++;
                    }
                }
                if (activeTeams > 0) {
                    footer += String.format("&7Equipos: &b%d\n", activeTeams);
                }

            } else if (Statics.gameStatus == GameStatus.IN_GAME || Statics.gameStatus == GameStatus.ENDED) {
                String arenaName = "Desconocida";
                if (PTC.getInstance().getGameManager().getArena() != null && PTC.getInstance().getGameManager().getArena().getArenaSettings() != null) {
                    arenaName = PTC.getInstance().getGameManager().getArena().getArenaSettings().getName();
                }

                String serverName = PTC.getInstance().getGameManager().getGlobalSettings().getServerName();
                if (serverName == null || serverName.isEmpty()) {
                    serverName = "Desconocido";
                }

                footer = String.format("\n&7Arena: &b%s\n&7Servidor: &b%s\n&7Jugadores: &b%d\n", arenaName, serverName, currentPlayers);

                if (isModeCW) {
                    try {
                        Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
                        if (adapter != null) {
                            String blueTag = (String) adapter.getClass().getMethod("getBlueClanTag").invoke(adapter);
                            String redTag = (String) adapter.getClass().getMethod("getRedClanTag").invoke(adapter);
                            if (blueTag != null && redTag != null) {
                                footer += String.format("&9%s &7vs &c%s\n", blueTag, redTag);
                            }
                        }
                    } catch (Exception e) {
                        LogSystem.debug(LogCategory.PLAYER, "Error obteniendo info CW para tab:", e.getMessage());
                    }
                }
            }

            TabListUtils.setHeaderFooter(player, Statics.c(header), Statics.c(footer));
        } catch (Exception e) {
            LogSystem.error(LogCategory.PLAYER, "Error actualizando tab list:", this.name, e.getMessage());
        }
    }

    public void updateNameTag() {
        updateNameTag(false);
    }

    public void forceUpdateNameTag() {
        updateNameTag(true);
    }

    private void updateNameTag(boolean force) {
        if (PTC.getInstance().getGameManager().getGlobalSettings().isLobbyMode()) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (!force && currentTime - lastNametagUpdate < NAMETAG_UPDATE_COOLDOWN) {
            return;
        }
        lastNametagUpdate = currentTime;
        Player player = this.getPlayer();
        if (player == null) return;
        try {
            Scoreboard scoreboard = player.getScoreboard();
            if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                player.setScoreboard(scoreboard);
            }
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                GamePlayer otherGamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(onlinePlayer.getUniqueId());
                if (otherGamePlayer != null) {
                    updatePlayerNameTag(scoreboard, onlinePlayer, otherGamePlayer);
                }
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.PLAYER, "Error actualizando nametag:", this.name, e.getMessage());
        }
    }

    private void updatePlayerNameTag(Scoreboard scoreboard, Player targetPlayer, GamePlayer targetGamePlayer) {
        try {
            String playerName = targetPlayer.getName();
            String teamName = "nt_" + playerName.substring(0, Math.min(13, playerName.length()));
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            if (!team.hasEntry(playerName)) {
                team.addEntry(playerName);
            }
            ChatColor teamColor = ChatColor.GRAY;
            if (targetGamePlayer.getArenaTeam() != null) {
                TeamColor color = targetGamePlayer.getArenaTeam().getColor();
                if (color != TeamColor.LOBBY && color != TeamColor.SPECTATOR) {
                    teamColor = color.getChatColor();
                    LogSystem.debug(LogCategory.PLAYER, "Aplicando color nametag:",
                            targetPlayer.getName(), "Color:", color.getName(), "ChatColor:", teamColor.name());
                }
            }
            team.setPrefix(teamColor.toString());
            team.setSuffix("");
        } catch (Exception e) {
            LogSystem.error(LogCategory.PLAYER, "Error en updatePlayerNameTag:", e.getMessage());
        }
    }

    public String toString() {
        return String.format("%s", this.name);
    }

    public boolean isSpectator() {
        return this.arenaTeam.getColor() == TeamColor.SPECTATOR;
    }

    public void setSpectator() {
        this.arenaTeam.removePlayer(this);
        Iterator var1 = Bukkit.getOnlinePlayers().iterator();
        while (var1.hasNext()) {
            Player onlinePlayer = (Player) var1.next();
            if (onlinePlayer != this.getPlayer()) {
                onlinePlayer.hidePlayer(this.getPlayer());
            }
        }
    }

    public int getCoins() {
        return this.playerStats.getCoins();
    }

    public void setCoins(int coins) {
        this.playerStats.setCoins(Math.max(0, coins));
    }

    public void addCoinsRaw(int coins) {
        this.playerStats.setCoins(this.playerStats.getCoins() + coins);
    }

    public void removeCoins(int coins) {
        int newBalance = this.playerStats.getCoins() - coins;
        this.playerStats.setCoins(Math.max(0, newBalance));
    }

    private boolean hasClan() {
        try {
            org.bukkit.plugin.Plugin ptcClans = Bukkit.getPluginManager().getPlugin("PTCClans");
            if (ptcClans != null && ptcClans.isEnabled()) {
                Class<?> clanManagerClass = Class.forName("sdfrpe.github.io.ptcclans.Managers.ClanManager");
                Object clanManager = clanManagerClass.getMethod("getInstance").invoke(null);
                Object clan = clanManagerClass.getMethod("getPlayerClan", UUID.class).invoke(clanManager, this.uuid);
                return clan != null;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void addClanXP(int xp, String reason) {
        if (!hasClan()) {
            return;
        }
        int oldXP = this.playerStats.getClanXP();
        int newXP = oldXP + xp;
        this.playerStats.setClanXP(newXP);
        ClanLevelSystem.LevelUpResult result = ClanLevelSystem.checkLevelUp(this.playerStats.getClanLevel(), oldXP, newXP);
        if (result.hasLeveledUp()) {
            this.playerStats.setClanLevel(result.getNewLevel());
            Player player = this.getPlayer();
            if (player != null) {
                new TitleAPI().title(ChatColor.GOLD + "¡NIVEL " + result.getNewLevel() + "!").subTitle(ChatColor.YELLOW + "Has subido de nivel").send(player);
                player.sendMessage(Statics.c("&6&l¡SUBISTE DE NIVEL! &eAhora eres nivel &a" + result.getNewLevel()));
            }
            LogSystem.info(LogCategory.PLAYER, "Nivel subido:", this.name, "Nivel", String.valueOf(result.getNewLevel()), "XP:", String.valueOf(newXP));
        }
    }

    public void addKill() {
        this.localStats.setKills(this.localStats.getKills() + 1);
        this.playerStats.setKills(this.playerStats.getKills() + 1);

        int killStreak = this.localStats.getBKillStreak() + 1;
        this.localStats.setBKillStreak(killStreak);

        if (killStreak > this.playerStats.getBKillStreak()) {
            this.playerStats.setBKillStreak(killStreak);
        }

        if (killStreak % 5 == 0) {
            int streakBonus = (killStreak / 5) * 5;
            (new TitleAPI()).title(ChatColor.GOLD + "¡RACHA!").subTitle(ChatColor.GREEN + "+" + streakBonus + " Coins Bonus").send(this.getPlayer());

            String publicMessage = ChatColor.GOLD + "¡RACHA! " + ChatColor.YELLOW + this.getName() +
                    ChatColor.WHITE + " tiene " + ChatColor.AQUA + killStreak +
                    ChatColor.WHITE + " asesinatos seguidos " + ChatColor.GREEN + "(+" + streakBonus + " coins)";

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(publicMessage);
            }

            this.getPlayer().sendMessage(Statics.c("&6&l¡RACHA! &e" + killStreak + " asesinatos seguidos &a(+" + streakBonus + " coins)"));
        }
    }

    public void addDeath() {
        this.localStats.setDeaths(this.localStats.getDeaths() + 1);
        this.playerStats.setDeaths(this.playerStats.getDeaths() + 1);
        this.localStats.setBKillStreak(0);
        this.dominated.clear();
    }

    public void addCoins(int coins) {
        int newCoins = coins * this.getPlayerStats().getMultiplier();
        this.addCoinsRaw(newCoins);
        showCoinsAndLevel(newCoins);
    }

    private void showCoinsAndLevel(int coins) {
        Player player = this.getPlayer();
        if (player == null) return;
        TitleAPI titleAPI = new TitleAPI();
        titleAPI.title(String.format("%s+%s Coins", ChatColor.GREEN, coins));
        if (hasClan()) {
            int level = this.playerStats.getClanLevel();
            int currentXP = this.playerStats.getClanXP();
            String levelInfo = ClanLevelSystem.formatLevelInfo(level, currentXP);
            titleAPI.subTitle(ChatColor.YELLOW + levelInfo);
        } else {
            titleAPI.subTitle("");
        }
        titleAPI.send(player);
    }

    public void addDominated(GamePlayer dominated) {
        if (this.revenge.contains(dominated)) {
            int newCoins = 5 * this.getPlayerStats().getMultiplier();
            (new TitleAPI()).title(ChatColor.RED + "Venganza").subTitle(ChatColor.GREEN + String.format("+%s Coins", newCoins)).send(this.getPlayer());
            this.revenge.remove(dominated);
            dominated.getDominated().remove(this);
            this.addCoinsRaw(newCoins);
            LogSystem.info(LogCategory.GAME, "Venganza:", this.getName(), "se vengó de", dominated.getName());
            return;
        }
        int currentKills = this.dominated.getOrDefault(dominated, 0) + 1;
        this.dominated.put(dominated, currentKills);
        LogSystem.debug(LogCategory.GAME, "Contador dominación:", this.getName(), "tiene", currentKills + "/5", "kills contra", dominated.getName());
        if (currentKills == DOMINATION_KILLS) {
            dominated.addRevenge(this);
            this.playerStats.setBDomination(this.playerStats.getBDomination() + 1);
            int newCoins = 5 * this.getPlayerStats().getMultiplier();
            Bukkit.broadcastMessage(Statics.c(String.format("%s&a esta dominando a %s", this.getArenaTeam().getColor().getChatColor() + this.getName(), dominated.getArenaTeam().getColor().getChatColor() + dominated.getName())));
            (new TitleAPI()).title(ChatColor.GREEN + "Dominado").subTitle(ChatColor.GREEN + String.format("+%s Coins", newCoins)).send(this.getPlayer());
            this.addCoinsRaw(newCoins);
            LogSystem.info(LogCategory.GAME, "Dominación:", this.getName(), "dominó a", dominated.getName(), "(5 kills)");
        }
    }

    public void addRevenge(GamePlayer killer) {
        this.revenge.add(killer);
        LogSystem.debug(LogCategory.GAME, "Venganza añadida:", this.getName(), "puede vengarse de", killer.getName());
    }

    public void cleanup() {
        if (this.playerTask != null) {
            this.playerTask.cancel();
            this.playerTask = null;
            LogSystem.debug(LogCategory.PLAYER, "PlayerTask cancelada en cleanup:", this.name);
        }

        if (this.pBoard != null) {
            this.pBoard = null;
        }

        this.dominated.clear();
        this.revenge.clear();
    }

    public GamePlayer getGamePlayer() {
        return this;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public ArenaSettings getArenaSettings() {
        return this.arenaSettings;
    }

    public ArenaTeam getArenaTeam() {
        return this.arenaTeam;
    }

    public IMenu getMenu() {
        return this.menu;
    }

    public PBoard getPBoard() {
        return this.pBoard;
    }

    public PlayerStats getPlayerStats() {
        return this.playerStats;
    }

    public PlayerStats getLocalStats() {
        return this.localStats;
    }

    public int getFurnaces() {
        return this.furnaces;
    }

    public int getChests() {
        return this.chests;
    }

    public Map<GamePlayer, Integer> getDominated() {
        return this.dominated;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArenaSettings(ArenaSettings arenaSettings) {
        this.arenaSettings = arenaSettings;
    }

    public void setArenaTeam(ArenaTeam arenaTeam) {
        this.arenaTeam = arenaTeam;
    }

    public void setMenu(IMenu menu) {
        this.menu = menu;
    }

    public void setPlayerStats(PlayerStats playerStats) {
        this.playerStats = playerStats;
    }

    public void setFurnaces(int furnaces) {
        this.furnaces = furnaces;
    }

    public void setChests(int chests) {
        this.chests = chests;
    }
}