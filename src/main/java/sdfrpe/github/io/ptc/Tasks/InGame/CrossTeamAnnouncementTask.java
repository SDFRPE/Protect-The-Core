package sdfrpe.github.io.ptc.Tasks.InGame;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

public class CrossTeamAnnouncementTask extends PTCRunnable {
    private int tickCounter = 0;

    private static final int TICKS_PER_MINUTE = 450;
    private static final int MINUTE_15_TICKS = 900;

    private boolean minute15AnnouncementSent = false;

    @Override
    public void onTick() {
        if (Statics.gameStatus != GameStatus.IN_GAME) {
            return;
        }

        tickCounter++;

        if (tickCounter % TICKS_PER_MINUTE == 0) {
            sendRegularAnnouncement();
            LogSystem.debug(LogCategory.GAME, "Tick " + tickCounter + " - Anuncio regular enviado");
        }

        if (tickCounter >= MINUTE_15_TICKS && !minute15AnnouncementSent) {
            send15MinuteAnnouncement();
            minute15AnnouncementSent = true;
            LogSystem.debug(LogCategory.GAME, "Tick " + tickCounter + " - Anuncio de 15 minutos enviado");
        }
    }

    private void sendRegularAnnouncement() {
        String message = ChatColor.YELLOW + "Recuerda que el equipo cruzado no es permitido y será sancionado.";

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }

        LogSystem.debug(LogCategory.GAME, "Anuncio regular de equipo cruzado enviado");
    }

    private void send15MinuteAnnouncement() {
        String[] messages = {
                ChatColor.GOLD + "---------------------------------------",
                ChatColor.RED + "Recordatorio: " + ChatColor.WHITE + "El equipo cruzado no está permitido, tiene como consecuencia una suspensión temporal de 6h en adelante.",
                ChatColor.GOLD + "---------------------------------------"
        };

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String message : messages) {
                player.sendMessage(message);
            }
        }

        LogSystem.info(LogCategory.GAME, "Anuncio de 15 minutos sobre equipo cruzado enviado");
    }
}