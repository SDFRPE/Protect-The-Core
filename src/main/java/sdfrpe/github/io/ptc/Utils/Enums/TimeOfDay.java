package sdfrpe.github.io.ptc.Utils.Enums;

public enum TimeOfDay {
    DAY(1000L, "Día"),
    AFTERNOON(12000L, "Tarde"),
    NIGHT(18000L, "Noche");

    private final long ticks;
    private final String displayName;

    TimeOfDay(long ticks, String displayName) {
        this.ticks = ticks;
        this.displayName = displayName;
    }

    public long getTicks() {
        return this.ticks;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public TimeOfDay next() {
        TimeOfDay[] values = values();
        int nextIndex = (this.ordinal() + 1) % values.length;
        return values[nextIndex];
    }
}