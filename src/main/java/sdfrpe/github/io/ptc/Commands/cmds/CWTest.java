package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Game.Extra.GlobalSettings;

public class CWTest extends SubCommand {

    public CWTest(PTC plugin) {
        super(plugin, "cwtest", "cwtest", "Comandos de testing para Clan Wars", "/ptc cwtest <start|stop|status>");
    }

    @Override
    public boolean onSubCommand(String subCmd, CommandSender sender, String[] args) {
        if (!sender.hasPermission("ptc.admin")) {
            sender.sendMessage(c("&c✖ Necesitas el permiso &6ptc.admin &cpara usar este comando."));
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "start":
            case "force":
                return handleForceStart(sender);

            case "stop":
                return handleStop(sender);

            case "status":
                return handleStatus(sender);

            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleForceStart(CommandSender sender) {
        GlobalSettings settings = plugin.getGameManager().getGlobalSettings();

        if (!settings.isModeCW()) {
            sender.sendMessage(c("&c✖ El modo Clan War no está activado"));
            sender.sendMessage(c("&7Usa: &e/ptc cwmode &7para activarlo"));
            return true;
        }

        if (settings.getActiveClanWarArena() != null) {
            sender.sendMessage(c("&c✖ Ya hay una guerra activa"));
            sender.sendMessage(c("&7Arena: &e" + settings.getActiveClanWarArena()));
            sender.sendMessage(c("&7Usa: &e/ptc cwtest stop &7para detenerla primero"));
            return true;
        }

        String arenaName = plugin.getGameManager().getArenaManager().getRandomArenaKey();

        if (arenaName == null) {
            sender.sendMessage(c("&c✖ No hay arenas disponibles"));
            return true;
        }

        settings.setActiveClanWarArena(arenaName);
        settings.setClanWarStartTime(System.currentTimeMillis());
        plugin.getGameManager().saveGlobalSettings();

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&a&l✔ GUERRA DE PRUEBA FORZADA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Arena asignada: &e" + arenaName));
        sender.sendMessage(c("&7Estado: &aACTIVA"));
        sender.sendMessage(c("&7Hora inicio: &eAHORA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7La guerra iniciará en el próximo ciclo"));
        sender.sendMessage(c("&7del GameManager (máx 20 segundos)"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Para detener: &e/ptc cwtest stop"));
        sender.sendMessage(c("&6═══════════════════════════════════"));

        plugin.getLogger().info("═══════════════════════════════════");
        plugin.getLogger().info("[TESTING] Guerra forzada por: " + sender.getName());
        plugin.getLogger().info("[TESTING] Arena: " + arenaName);
        plugin.getLogger().info("═══════════════════════════════════");

        return true;
    }

    private boolean handleStop(CommandSender sender) {
        GlobalSettings settings = plugin.getGameManager().getGlobalSettings();

        if (!settings.isModeCW()) {
            sender.sendMessage(c("&c✖ El modo Clan War no está activado"));
            return true;
        }

        String currentArena = settings.getActiveClanWarArena();

        if (currentArena == null) {
            sender.sendMessage(c("&c✖ No hay guerra activa para detener"));
            return true;
        }

        settings.setActiveClanWarArena(null);
        settings.setClanWarStartTime(0);
        plugin.getGameManager().saveGlobalSettings();

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&e&l✖ GUERRA DETENIDA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Arena anterior: &e" + currentArena));
        sender.sendMessage(c("&7Estado: &cDETENIDA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7El servidor volverá al modo normal"));
        sender.sendMessage(c("&7en el próximo ciclo del GameManager"));
        sender.sendMessage(c("&6═══════════════════════════════════"));

        plugin.getLogger().info("═══════════════════════════════════");
        plugin.getLogger().info("[TESTING] Guerra detenida por: " + sender.getName());
        plugin.getLogger().info("[TESTING] Arena anterior: " + currentArena);
        plugin.getLogger().info("═══════════════════════════════════");

        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        GlobalSettings settings = plugin.getGameManager().getGlobalSettings();

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&e&lESTADO CLAN WAR - TESTING"));
        sender.sendMessage(c(""));

        sender.sendMessage(c("&7Modo CW: " + (settings.isModeCW() ? "&a&lACTIVO" : "&c&lINACTIVO")));

        if (settings.isModeCW()) {
            String arena = settings.getActiveClanWarArena();
            sender.sendMessage(c("&7Arena activa: " + (arena != null ? "&e" + arena : "&7Ninguna")));

            long startTime = settings.getClanWarStartTime();
            if (startTime > 0) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long minutes = elapsed / 60;
                long seconds = elapsed % 60;
                sender.sendMessage(c("&7Tiempo transcurrido: &e" + minutes + "m " + seconds + "s"));
            }

            sender.sendMessage(c(""));
            sender.sendMessage(c("&7Estado servidor: " +
                    (arena != null ? "&aEN GUERRA" : "&eESPERANDO")));
        }

        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Arenas disponibles: &e" +
                plugin.getGameManager().getArenaManager().getArenas().size()));

        sender.sendMessage(c("&6═══════════════════════════════════"));

        return true;
    }

    @Override
    protected void sendHelp(CommandSender sender) {
        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&e&lCOMANDOS DE TESTING - PTC"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/ptc cwtest start"));
        sender.sendMessage(c("&7  Forzar inicio de guerra inmediato"));
        sender.sendMessage(c("&7  Asigna arena aleatoria y activa"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/ptc cwtest stop"));
        sender.sendMessage(c("&7  Detener guerra activa"));
        sender.sendMessage(c("&7  Libera la arena y desactiva"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/ptc cwtest status"));
        sender.sendMessage(c("&7  Ver estado actual de Clan War"));
        sender.sendMessage(c("&7  Muestra arena, tiempo, etc."));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Nota: Estos comandos son para testing"));
        sender.sendMessage(c("&7y requieren permiso ptc.admin"));
        sender.sendMessage(c("&6═══════════════════════════════════"));
    }
}