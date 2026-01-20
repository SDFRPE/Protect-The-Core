package sdfrpe.github.io.ptc.Utils.Managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.util.HashMap;
import java.util.Map;

public class GlobalTabManager {
    private static GlobalTabManager instance;
    private Scoreboard globalTabScoreboard;
    private final Map<TeamColor, Team> teamMap;

    private GlobalTabManager() {
        this.teamMap = new HashMap<>();
        this.initialize();
    }

    public static GlobalTabManager getInstance() {
        if (instance == null) {
            instance = new GlobalTabManager();
        }
        return instance;
    }

    private void initialize() {
        try {
            this.globalTabScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

            createTeam(TeamColor.RED, "1");
            createTeam(TeamColor.BLUE, "2");
            createTeam(TeamColor.GREEN, "3");
            createTeam(TeamColor.YELLOW, "4");
            createTeam(TeamColor.SPECTATOR, "5");
            createTeam(TeamColor.LOBBY, "6");

            LogSystem.info(LogCategory.CORE, "GlobalTabManager inicializado correctamente");
            LogSystem.debug(LogCategory.CORE, "Equipos TAB creados: RED, BLUE, GREEN, YELLOW, SPECTATOR, LOBBY");
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error inicializando GlobalTabManager:", e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTeam(TeamColor color, String priority) {
        try {
            String teamName = priority + "_" + color.name();
            Team team = globalTabScoreboard.getTeam(teamName);

            if (team == null) {
                team = globalTabScoreboard.registerNewTeam(teamName);
            }

            String prefix = color.getChatColor() + "";

            try {
                if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
                    Object adapter = PTC.getInstance().getGameManager().getClanWarAdapter();
                    if (adapter != null) {
                        if (color == TeamColor.BLUE) {
                            java.lang.reflect.Method colorMethod = adapter.getClass().getMethod("getBlueClanColor");
                            String clanColor = (String) colorMethod.invoke(adapter);
                            if (clanColor != null) {
                                prefix = clanColor.replace("&", "§");
                            }
                        } else if (color == TeamColor.RED) {
                            java.lang.reflect.Method colorMethod = adapter.getClass().getMethod("getRedClanColor");
                            String clanColor = (String) colorMethod.invoke(adapter);
                            if (clanColor != null) {
                                prefix = clanColor.replace("&", "§");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogSystem.debug(LogCategory.TEAM, "CW color no disponible para", color.name());
            }

            team.setPrefix(prefix);
            team.setSuffix("");
            team.setAllowFriendlyFire(false);
            team.setCanSeeFriendlyInvisibles(true);

            if (color == TeamColor.SPECTATOR) {
                team.setNameTagVisibility(org.bukkit.scoreboard.NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
            }

            teamMap.put(color, team);
            LogSystem.debug(LogCategory.TEAM, "Equipo TAB creado:", teamName, "con prefijo:", prefix);

        } catch (Exception e) {
            LogSystem.error(LogCategory.TEAM, "Error creando equipo TAB:", color.name(), e.getMessage());
        }
    }

    public void addPlayerToTeam(Player player, TeamColor color) {
        if (player == null || color == null) {
            LogSystem.debug(LogCategory.TEAM, "Player o color null en addPlayerToTeam");
            return;
        }

        try {
            removePlayerFromAllTeams(player);

            Team team = teamMap.get(color);
            if (team != null) {
                if (!team.hasEntry(player.getName())) {
                    team.addEntry(player.getName());
                    LogSystem.debug(LogCategory.TEAM, "TAB actualizado:", player.getName(), "→", color.name());
                }
            } else {
                LogSystem.error(LogCategory.TEAM, "Equipo TAB no encontrado:", color.name());
            }

        } catch (Exception e) {
            LogSystem.error(LogCategory.TEAM, "Error agregando jugador a TAB:", player.getName(), e.getMessage());
        }
    }

    public void removePlayerFromAllTeams(Player player) {
        if (player == null) return;

        try {
            for (Team team : teamMap.values()) {
                if (team.hasEntry(player.getName())) {
                    team.removeEntry(player.getName());
                }
            }
        } catch (Exception e) {
            LogSystem.debug(LogCategory.TEAM, "Error removiendo jugador de equipos TAB:", e.getMessage());
        }
    }

    public void applyToPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            player.setScoreboard(globalTabScoreboard);
            LogSystem.debug(LogCategory.TEAM, "Scoreboard global TAB aplicado a:", player.getName());

        } catch (Exception e) {
            LogSystem.error(LogCategory.TEAM, "Error aplicando scoreboard TAB:", player.getName(), e.getMessage());
        }
    }

    public void updatePlayerTab(GamePlayer gamePlayer) {
        if (gamePlayer == null || gamePlayer.getPlayer() == null) {
            return;
        }

        Player player = gamePlayer.getPlayer();
        TeamColor color = TeamColor.LOBBY;

        if (gamePlayer.getArenaTeam() != null) {
            color = gamePlayer.getArenaTeam().getColor();
        }

        addPlayerToTeam(player, color);
    }

    public void updateAllPlayerTabs() {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
                if (gamePlayer != null) {
                    updatePlayerTab(gamePlayer);
                }
            }
            LogSystem.debug(LogCategory.TEAM, "TAB actualizado para todos los jugadores");
        } catch (Exception e) {
            LogSystem.error(LogCategory.TEAM, "Error actualizando TAB global:", e.getMessage());
        }
    }
}