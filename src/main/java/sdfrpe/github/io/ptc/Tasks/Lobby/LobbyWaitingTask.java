package sdfrpe.github.io.ptc.Tasks.Lobby;

import org.bukkit.Bukkit;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.BossbarAPI.BossBarAPI;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import org.bukkit.ChatColor;

public class LobbyWaitingTask extends PTCRunnable {
    private boolean hasAutoStarted = false;

    @Override
    public void onTick() {
        if (Statics.gameStatus != GameStatus.LOBBY) {
            return;
        }

        int currentPlayers = Bukkit.getOnlinePlayers().size();
        int minPlayers = PTC.getInstance().getGameManager().getGlobalSettings().getArenaPlayers().getMinPlayers();
        int neededPlayers = Math.max(0, minPlayers - currentPlayers);

        if (neededPlayers > 0) {
            hasAutoStarted = false;

            String message = String.format("%s%sEsperando a %s%d %sjugador%s para iniciar",
                    ChatColor.YELLOW,
                    ChatColor.BOLD,
                    ChatColor.WHITE,
                    neededPlayers,
                    ChatColor.YELLOW,
                    neededPlayers == 1 ? "" : "es"
            );

            float percentage = (float) currentPlayers / (float) minPlayers * 100.0f;
            percentage = Math.min(percentage, 95.0f);

            BossBarAPI.update(message, percentage);
            BossBarAPI.setEnabled(true);
        } else {
            String message = String.format("%s%s¡Listos para comenzar! %s(%d/%d jugadores)",
                    ChatColor.GREEN,
                    ChatColor.BOLD,
                    ChatColor.WHITE,
                    currentPlayers,
                    minPlayers
            );

            BossBarAPI.update(message, 100.0f);
            BossBarAPI.setEnabled(true);

            if (!hasAutoStarted) {
                hasAutoStarted = true;
                LogSystem.info(LogCategory.GAME, "Auto-iniciando partida:", currentPlayers + "/" + minPlayers + " jugadores");

                Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
                    try {
                        PTC.getInstance().getGameManager().getGameSettings().startGame();
                    } catch (Exception e) {
                        hasAutoStarted = false;
                        LogSystem.error(LogCategory.GAME, "Error auto-iniciando partida:", e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}