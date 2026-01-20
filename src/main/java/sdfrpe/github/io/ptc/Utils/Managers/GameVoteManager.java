package sdfrpe.github.io.ptc.Utils.Managers;

import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Enums.TimeOfDay;

import java.util.HashMap;
import java.util.Map;

public class GameVoteManager {
    private final Map<GamePlayer, TimeOfDay> timeVotes;
    private final Map<GamePlayer, Integer> healthVotes;

    public GameVoteManager() {
        this.timeVotes = new HashMap<>();
        this.healthVotes = new HashMap<>();
    }

    public void voteTime(GamePlayer player, TimeOfDay time) {
        this.timeVotes.put(player, time);
        Console.debug("Player " + player.getName() + " voted for time: " + time.getDisplayName());
    }

    public void voteHealth(GamePlayer player, int extraHearts) {
        this.healthVotes.put(player, extraHearts);
        Console.debug("Player " + player.getName() + " voted for extra hearts: " + extraHearts);
    }

    public void removePlayerVotes(GamePlayer player) {
        this.timeVotes.remove(player);
        this.healthVotes.remove(player);
        Console.debug("Removed all game configuration votes for player: " + player.getName());
    }

    public TimeOfDay getMostVotedTime() {
        if (timeVotes.isEmpty()) {
            Console.debug("No time votes, defaulting to DAY");
            return TimeOfDay.DAY;
        }

        Map<TimeOfDay, Integer> voteCounts = new HashMap<>();
        for (TimeOfDay time : timeVotes.values()) {
            voteCounts.put(time, voteCounts.getOrDefault(time, 0) + 1);
        }

        TimeOfDay winner = TimeOfDay.DAY;
        int maxVotes = 0;
        for (Map.Entry<TimeOfDay, Integer> entry : voteCounts.entrySet()) {
            Console.debug("  Time " + entry.getKey().getDisplayName() + ": " + entry.getValue() + " votes");
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
            }
        }

        Console.log("Most voted time: " + winner.getDisplayName() + " with " + maxVotes + " votes");
        return winner;
    }

    public int getMostVotedHealth() {
        if (healthVotes.isEmpty()) {
            Console.debug("No health votes, defaulting to 0 extra hearts");
            return 0;
        }

        Map<Integer, Integer> voteCounts = new HashMap<>();
        for (Integer hearts : healthVotes.values()) {
            voteCounts.put(hearts, voteCounts.getOrDefault(hearts, 0) + 1);
        }

        int winner = 0;
        int maxVotes = 0;
        for (Map.Entry<Integer, Integer> entry : voteCounts.entrySet()) {
            Console.debug("  Extra hearts " + entry.getKey() + ": " + entry.getValue() + " votes");
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
            }
        }

        Console.log("Most voted extra hearts: " + winner + " with " + maxVotes + " votes");
        return winner;
    }

    public void clearAllVotes() {
        this.timeVotes.clear();
        this.healthVotes.clear();
        Console.debug("Cleared all game configuration votes");
    }

    public int getTimeVoteCount(TimeOfDay time) {
        return (int) timeVotes.values().stream().filter(t -> t == time).count();
    }

    public int getHealthVoteCount(int hearts) {
        return (int) healthVotes.values().stream().filter(h -> h == hearts).count();
    }
}