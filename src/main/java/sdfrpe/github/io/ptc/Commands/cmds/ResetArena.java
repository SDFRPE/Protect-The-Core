package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Tasks.InGame.ArenaTask;
import sdfrpe.github.io.ptc.Tasks.InGame.EndTask;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;

public class ResetArena extends SubCommand {

    public ResetArena(PTC plugin) {
        super(plugin, "Reset Arena", "resetArena", "Reinicia la arena actual inmediatamente.", "/ptc resetArena");
    }

    @Override
    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        if (Statics.gameStatus != GameStatus.IN_GAME) {
            sender.sendMessage(this.c("&c✖ No hay una partida activa para reiniciar."));
            sender.sendMessage(this.c("&7Estado actual: &e" + Statics.gameStatus.name()));
            return false;
        }

        ArenaTask arenaTask = ArenaTask.getInstance();
        if (arenaTask != null) {
            arenaTask.cancel();
            LogSystem.info(LogCategory.GAME, "ArenaTask cancelada por reset manual");
        }

        Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));
        Bukkit.broadcastMessage(Statics.c("&c&l⚠ REINICIO FORZADO DE ARENA"));
        Bukkit.broadcastMessage(Statics.c("&7La partida ha sido detenida por un administrador"));
        Bukkit.broadcastMessage(Statics.c("&7Iniciando proceso de reinicio..."));
        Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));

        LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");
        LogSystem.info(LogCategory.GAME, "RESET MANUAL DE ARENA");
        LogSystem.info(LogCategory.GAME, "Ejecutado por:", sender.getName());
        LogSystem.info(LogCategory.GAME, "Razón: Comando manual");
        LogSystem.info(LogCategory.GAME, "═══════════════════════════════════");

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            new EndTask(null).run();
            LogSystem.info(LogCategory.GAME, "EndTask iniciada para reinicio manual");
        });

        sender.sendMessage(this.c("&a✓ Proceso de reinicio iniciado correctamente."));
        return true;
    }
}