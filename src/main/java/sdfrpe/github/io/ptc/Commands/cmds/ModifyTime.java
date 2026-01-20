package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Tasks.InGame.ArenaTask;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;

public class ModifyTime extends SubCommand {

    public ModifyTime(PTC plugin) {
        super(plugin, "Modify Time", "modifyTime", "Modifica el tiempo de la arena actual.", "/ptc modifyTime <add/remove/set> <segundos>");
    }

    @Override
    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        if (Statics.gameStatus != GameStatus.IN_GAME) {
            sender.sendMessage(this.c("&c✖ Este comando solo funciona durante una partida activa."));
            return false;
        }

        ArenaTask arenaTask = ArenaTask.getInstance();
        if (arenaTask == null) {
            sender.sendMessage(this.c("&c✖ No hay una ArenaTask activa en este momento."));
            return false;
        }

        if (args.length < 3) {
            this.sendHelp(sender);
            sender.sendMessage(this.c("&7Operaciones disponibles:"));
            sender.sendMessage(this.c("&e  add    &7- Añade tiempo a la partida"));
            sender.sendMessage(this.c("&e  remove &7- Quita tiempo a la partida"));
            sender.sendMessage(this.c("&e  set    &7- Establece un tiempo exacto"));
            return false;
        }

        String operation = args[1].toLowerCase();
        int seconds;

        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(this.c("&c✖ Los segundos deben ser un número válido."));
            sender.sendMessage(this.c("&7Ejemplo: &e/ptc modifyTime add 300 &7(5 minutos)"));
            return false;
        }

        int currentTime = arenaTask.getTimeRemainingSeconds();
        int newTime;

        switch (operation) {
            case "add":
                if (seconds <= 0) {
                    sender.sendMessage(this.c("&c✖ La cantidad a añadir debe ser mayor a 0."));
                    return false;
                }
                arenaTask.addTime(seconds);
                newTime = currentTime + seconds;

                Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));
                Bukkit.broadcastMessage(Statics.c("&a&l⏱ TIEMPO AÑADIDO"));
                Bukkit.broadcastMessage(Statics.c("&7Tiempo añadido: &e+" + formatSeconds(seconds)));
                Bukkit.broadcastMessage(Statics.c("&7Tiempo anterior: &e" + formatSeconds(currentTime)));
                Bukkit.broadcastMessage(Statics.c("&7Tiempo nuevo: &a" + formatSeconds(newTime)));
                Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));

                LogSystem.info(LogCategory.GAME, "Admin", sender.getName(), "añadió", seconds + "s", "de tiempo. Nuevo total:", newTime + "s");
                break;

            case "remove":
                if (seconds <= 0) {
                    sender.sendMessage(this.c("&c✖ La cantidad a quitar debe ser mayor a 0."));
                    return false;
                }
                arenaTask.removeTime(seconds);
                newTime = arenaTask.getTimeRemainingSeconds();

                Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));
                Bukkit.broadcastMessage(Statics.c("&c&l⏱ TIEMPO REDUCIDO"));
                Bukkit.broadcastMessage(Statics.c("&7Tiempo quitado: &c-" + formatSeconds(seconds)));
                Bukkit.broadcastMessage(Statics.c("&7Tiempo anterior: &e" + formatSeconds(currentTime)));
                Bukkit.broadcastMessage(Statics.c("&7Tiempo nuevo: &e" + formatSeconds(newTime)));
                Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));

                LogSystem.info(LogCategory.GAME, "Admin", sender.getName(), "quitó", seconds + "s", "de tiempo. Nuevo total:", newTime + "s");
                break;

            case "set":
                if (seconds < 0) {
                    sender.sendMessage(this.c("&c✖ El tiempo no puede ser negativo."));
                    return false;
                }
                arenaTask.setTime(seconds);
                newTime = seconds;

                Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));
                Bukkit.broadcastMessage(Statics.c("&e&l⏱ TIEMPO ESTABLECIDO"));
                Bukkit.broadcastMessage(Statics.c("&7Tiempo anterior: &e" + formatSeconds(currentTime)));
                Bukkit.broadcastMessage(Statics.c("&7Tiempo nuevo: &a" + formatSeconds(newTime)));
                Bukkit.broadcastMessage(Statics.c("&6═══════════════════════════════════"));

                LogSystem.info(LogCategory.GAME, "Admin", sender.getName(), "estableció tiempo a", seconds + "s");
                break;

            default:
                sender.sendMessage(this.c("&c✖ Operación no válida: &4" + operation));
                sender.sendMessage(this.c("&7Operaciones disponibles: &eadd, remove, set"));
                return false;
        }

        return true;
    }

    private String formatSeconds(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}