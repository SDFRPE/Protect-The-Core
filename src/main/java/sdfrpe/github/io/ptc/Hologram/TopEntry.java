package sdfrpe.github.io.ptc.Hologram;

public class TopEntry {
    private final int position;
    private final String playerName;
    private final long value;
    private final String clanName;
    private final String formattedValue;

    public TopEntry(int position, String playerName, long value, String clanName) {
        this.position = position;
        this.playerName = playerName;
        this.value = value;
        this.clanName = clanName;
        this.formattedValue = null;
    }

    public TopEntry(int position, String playerName, long value, String clanName, String formattedValue) {
        this.position = position;
        this.playerName = playerName;
        this.value = value;
        this.clanName = clanName;
        this.formattedValue = formattedValue;
    }

    public int getPosition() {
        return position;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getValue() {
        return value;
    }

    public int getValueAsInt() {
        return (int) value;
    }

    public String getClanName() {
        return clanName;
    }

    public boolean hasClan() {
        return clanName != null && !clanName.isEmpty();
    }

    public String getFormattedValue() {
        return formattedValue;
    }

    public boolean hasFormattedValue() {
        return formattedValue != null && !formattedValue.isEmpty();
    }
}