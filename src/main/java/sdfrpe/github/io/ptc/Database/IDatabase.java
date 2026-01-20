package sdfrpe.github.io.ptc.Database;

import sdfrpe.github.io.ptc.Player.PlayerStats;
import java.util.UUID;

public interface IDatabase {
    void loadPlayer(UUID uuid);

    PlayerStats loadPlayerSync(UUID uuid);

    void savePlayer(UUID uuid);

    void savePlayerSync(UUID uuid);

    int saveAllPlayersSync();
}