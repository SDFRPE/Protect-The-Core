package sdfrpe.github.io.ptc.Utils.PlaceholderAPI;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerStats;
import sdfrpe.github.io.ptc.Playtime.PlaytimeManager;
import sdfrpe.github.io.ptc.Utils.ClanChatUtils;

import java.util.concurrent.TimeUnit;

public class PTCPlaceholder extends PlaceholderExpansion {

    private final PTC plugin;

    public PTCPlaceholder(PTC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "ptc";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        GamePlayer gamePlayer = plugin.getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
        if (gamePlayer == null) {
            return "0";
        }

        if (identifier.equals("coins")) {
            return String.valueOf(gamePlayer.getCoins());
        }

        if (identifier.equals("kills")) {
            return String.valueOf(gamePlayer.getPlayerStats().getKills());
        }

        if (identifier.equals("deaths")) {
            return String.valueOf(gamePlayer.getPlayerStats().getDeaths());
        }

        if (identifier.equals("wins")) {
            return String.valueOf(gamePlayer.getPlayerStats().getWins());
        }

        if (identifier.equals("cores")) {
            return String.valueOf(gamePlayer.getPlayerStats().getCores());
        }

        if (identifier.equals("multiplier")) {
            return String.valueOf(gamePlayer.getPlayerStats().getMultiplier());
        }

        if (identifier.equals("killstreak")) {
            return String.valueOf(gamePlayer.getLocalStats().getBKillStreak());
        }

        if (identifier.equals("display")) {
            String display = ClanChatUtils.getClanDisplayForChat(player.getUniqueId());
            return display.isEmpty() ? "" : display;
        }

        if (identifier.equals("playtime")) {
            return gamePlayer.getPlayerStats().getFormattedPlaytime();
        }

        if (identifier.equals("playtime_raw")) {
            return String.valueOf(gamePlayer.getPlayerStats().getPlaytime());
        }

        if (identifier.equals("playtime_hours")) {
            long hours = gamePlayer.getPlayerStats().getPlaytime() / 3600000;
            return String.valueOf(hours);
        }

        if (identifier.equals("playtime_minutes")) {
            long minutes = gamePlayer.getPlayerStats().getPlaytime() / 60000;
            return String.valueOf(minutes);
        }

        if (identifier.equals("playtime_days")) {
            long days = gamePlayer.getPlayerStats().getPlaytime() / 86400000;
            return String.valueOf(days);
        }

        if (identifier.equals("session_time")) {
            PlaytimeManager pm = plugin.getPlaytimeManager();
            if (pm != null && pm.hasActiveSession(player.getUniqueId())) {
                long sessionMs = pm.getCurrentSessionTime(player.getUniqueId());
                return PlaytimeManager.formatPlaytime(sessionMs);
            }
            return "0s";
        }

        if (identifier.equals("session_time_raw")) {
            PlaytimeManager pm = plugin.getPlaytimeManager();
            if (pm != null && pm.hasActiveSession(player.getUniqueId())) {
                return String.valueOf(pm.getCurrentSessionTime(player.getUniqueId()));
            }
            return "0";
        }

        if (identifier.equals("is_online")) {
            PlaytimeManager pm = plugin.getPlaytimeManager();
            if (pm != null) {
                return pm.hasActiveSession(player.getUniqueId()) ? "true" : "false";
            }
            return "false";
        }

        if (identifier.equals("kdr")) {
            int kills = gamePlayer.getPlayerStats().getKills();
            int deaths = gamePlayer.getPlayerStats().getDeaths();
            if (deaths == 0) {
                return String.valueOf(kills);
            }
            double kdr = (double) kills / deaths;
            return String.format("%.2f", kdr);
        }

        if (identifier.equals("level")) {
            return String.valueOf(gamePlayer.getPlayerStats().getPlayerLevels().getLevel());
        }

        if (identifier.equals("clanlevel")) {
            return String.valueOf(gamePlayer.getPlayerStats().getClanLevel());
        }

        if (identifier.equals("clanxp")) {
            return String.valueOf(gamePlayer.getPlayerStats().getClanXP());
        }

        if (identifier.equals("domination")) {
            return String.valueOf(gamePlayer.getPlayerStats().getBDomination());
        }

        if (identifier.equals("best_killstreak")) {
            return String.valueOf(gamePlayer.getPlayerStats().getBKillStreak());
        }

        return null;
    }
}