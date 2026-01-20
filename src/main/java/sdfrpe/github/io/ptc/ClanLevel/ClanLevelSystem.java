package sdfrpe.github.io.ptc.ClanLevel;

public class ClanLevelSystem {

    public static final int MAX_LEVEL = 100;
    public static final int EASY_LEVELS = 20;
    public static final int XP_PER_CORE = 5;

    public static int getXPForLevel(int level) {
        if (level <= 0) return 0;
        if (level > MAX_LEVEL) return getXPForLevel(MAX_LEVEL);

        if (level <= EASY_LEVELS) {
            int baseXP = 250;
            int increment = 50;
            return baseXP + ((level - 1) * (increment + ((level - 1) * 20)));
        } else {
            int levelAbove20 = level - EASY_LEVELS;
            int level20XP = getXPForLevel(EASY_LEVELS);
            double multiplier = Math.pow(1.35, levelAbove20);
            int exponentialXP = (int)(level20XP * multiplier);
            return exponentialXP;
        }
    }

    public static int getLevelFromXP(int totalXP) {
        if (totalXP <= 0) return 1;

        int level = 1;
        int accumulatedXP = 0;

        while (level <= MAX_LEVEL) {
            int xpForNextLevel = getXPForLevel(level);
            if (accumulatedXP + xpForNextLevel > totalXP) {
                break;
            }
            accumulatedXP += xpForNextLevel;
            level++;
        }

        return Math.min(level, MAX_LEVEL);
    }

    public static int getTotalXPForLevel(int level) {
        int total = 0;
        for (int i = 1; i < level && i <= MAX_LEVEL; i++) {
            total += getXPForLevel(i);
        }
        return total;
    }

    public static int getCurrentLevelProgress(int currentLevel, int currentXP) {
        if (currentLevel >= MAX_LEVEL) return 0;

        int totalXPForCurrentLevel = getTotalXPForLevel(currentLevel);
        return currentXP - totalXPForCurrentLevel;
    }

    public static LevelUpResult checkLevelUp(int currentLevel, int oldXP, int newXP) {
        int oldLevel = getLevelFromXP(oldXP);
        int newLevel = getLevelFromXP(newXP);
        boolean leveledUp = newLevel > oldLevel;
        return new LevelUpResult(newLevel, leveledUp, oldLevel);
    }

    public static class LevelUpResult {
        private final int newLevel;
        private final boolean leveledUp;
        private final int previousLevel;

        public LevelUpResult(int newLevel, boolean leveledUp, int previousLevel) {
            this.newLevel = newLevel;
            this.leveledUp = leveledUp;
            this.previousLevel = previousLevel;
        }

        public int getNewLevel() {
            return newLevel;
        }

        public boolean hasLeveledUp() {
            return leveledUp;
        }
    }

    public static String formatLevelInfo(int level, int currentXP) {
        if (level >= MAX_LEVEL) {
            return "Nivel " + MAX_LEVEL + ": MAX";
        }

        int currentProgress = getCurrentLevelProgress(level, currentXP);
        int xpForLevel = getXPForLevel(level);

        return "Nivel " + level + ": " + currentProgress + "/" + xpForLevel;
    }
}