package sdfrpe.github.io.ptc.Hologram;

public enum TopType {
    WINS_ALLTIME("wins_alltime", "All-Time Wins", "wins", false, false),
    WINS_WEEKLY("wins_weekly", "Weekly Wins", "wins", true, false),

    KILLS_ALLTIME("kills_alltime", "All-Time Kills", "kills", false, false),
    KILLS_WEEKLY("kills_weekly", "Weekly Kills", "kills", true, false),

    DEATHS_ALLTIME("deaths_alltime", "All-Time Deaths", "deaths", false, false),
    DEATHS_WEEKLY("deaths_weekly", "Weekly Deaths", "deaths", true, false),

    CORES_ALLTIME("cores_alltime", "All-Time Cores", "cores", false, false),
    CORES_WEEKLY("cores_weekly", "Weekly Cores", "cores", true, false),

    DOMINATION_ALLTIME("domination_alltime", "All-Time Domination", "bDomination", false, false),
    DOMINATION_WEEKLY("domination_weekly", "Weekly Domination", "bDomination", true, false),

    CLAN_LEVEL("clan_level", "Clan Level", "clanLevel", false, false),
    BEST_KILLSTREAK("best_killstreak", "Best Kill Streak", "bKillStreak", false, false),

    PLAYTIME_ALLTIME("playtime_alltime", "All-Time Playtime", "playtime", false, true),
    PLAYTIME_WEEKLY("playtime_weekly", "Weekly Playtime", "playtime", true, true);

    private final String commandName;
    private final String displayName;
    private final String fieldName;
    private final boolean isWeekly;
    private final boolean isPlaytime;

    TopType(String commandName, String displayName, String fieldName, boolean isWeekly, boolean isPlaytime) {
        this.commandName = commandName;
        this.displayName = displayName;
        this.fieldName = fieldName;
        this.isWeekly = isWeekly;
        this.isPlaytime = isPlaytime;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isWeekly() {
        return isWeekly;
    }

    public boolean isPlaytime() {
        return isPlaytime;
    }

    public static TopType fromString(String name) {
        for (TopType type : values()) {
            if (type.commandName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}