package sdfrpe.github.io.ptc.Commands.cmds;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptc.Commands.SubCommand;
import sdfrpe.github.io.ptc.Hologram.TopType;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class TopCommand extends SubCommand {
    public TopCommand(PTC plugin) {
        super(plugin, "Top Holograms", "top", "Gestionar hologramas de TOPs", "/ptc top <create/remove/list/reload> [tipo]");
    }

    @Override
    public boolean onSubCommand(String command, CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(c("&c&lUso incorrecto."));
            sender.sendMessage(c("&e/ptc top create <tipo> &7- Crear holograma"));
            sender.sendMessage(c("&e/ptc top remove &7- Eliminar holograma cercano"));
            sender.sendMessage(c("&e/ptc top list &7- Listar hologramas"));
            sender.sendMessage(c("&e/ptc top reload &7- Recargar configuración"));
            sender.sendMessage(c("&e/ptc top update &7- Actualizar todos los hologramas"));
            sender.sendMessage(c(""));
            sender.sendMessage(c("&eTipos All-Time: &fwins_alltime, kills_alltime, cores_alltime, domination_alltime, clan_level, best_killstreak, playtime_alltime"));
            sender.sendMessage(c("&eTipos Weekly: &fwins_weekly, kills_weekly, cores_weekly, domination_weekly, playtime_weekly"));
            return false;
        }

        String subCmd = args[1].toLowerCase();

        switch (subCmd) {
            case "create":
                return handleCreate(sender, args);

            case "remove":
                return handleRemove(sender);

            case "list":
                return handleList(sender);

            case "reload":
                return handleReload(sender);

            case "update":
                return handleUpdate(sender);

            default:
                sender.sendMessage(c("&cSubcomando desconocido: &f" + subCmd));
                return false;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser ejecutado por un jugador."));
            return false;
        }

        if (args.length < 3) {
            sender.sendMessage(c("&cUso: &e/ptc top create <tipo>"));
            sender.sendMessage(c("&eTipos All-Time: &fwins_alltime, kills_alltime, cores_alltime, domination_alltime, clan_level, best_killstreak, playtime_alltime"));
            sender.sendMessage(c("&eTipos Weekly: &fwins_weekly, kills_weekly, cores_weekly, domination_weekly, playtime_weekly"));
            return false;
        }

        Player player = (Player) sender;
        String typeStr = args[2].toLowerCase();
        TopType type = TopType.fromString(typeStr);

        if (type == null) {
            sender.sendMessage(c("&cTipo inválido: &f" + typeStr));
            sender.sendMessage(c("&eTipos All-Time: &fwins_alltime, kills_alltime, cores_alltime, domination_alltime, clan_level, best_killstreak, playtime_alltime"));
            sender.sendMessage(c("&eTipos Weekly: &fwins_weekly, kills_weekly, cores_weekly, domination_weekly, playtime_weekly"));
            return false;
        }

        Location location = player.getLocation();
        String id = "top_" + type.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);

        plugin.getHologramManager().createHologram(id, type, location);

        sender.sendMessage(c("&a&l✓ Holograma creado exitosamente"));
        sender.sendMessage(c("&eID: &f" + id));
        sender.sendMessage(c("&eTipo: &f" + type.getDisplayName()));
        sender.sendMessage(c("&eModo: " + (type.isWeekly() ? "&bSemanal (resetea lunes)" : "&dPermanente")));
        sender.sendMessage(c("&eUbicación: &f" + String.format("%.1f, %.1f, %.1f",
                location.getX(), location.getY(), location.getZ())));

        LogSystem.info(LogCategory.CORE, "Holograma creado por", player.getName() + ":", id);
        return true;
    }

    private boolean handleRemove(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser ejecutado por un jugador."));
            return false;
        }

        Player player = (Player) sender;
        boolean removed = plugin.getHologramManager().removeNearestHologram(player.getLocation(), 5.0);

        if (removed) {
            sender.sendMessage(c("&a&l✓ Holograma eliminado"));
        } else {
            sender.sendMessage(c("&c&l✖ No se encontró ningún holograma cerca (radio 5 bloques)"));
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<Object> holograms = plugin.getHologramManager().getHolograms();

        if (holograms.isEmpty()) {
            sender.sendMessage(c("&c&l✖ No hay hologramas creados"));
            return true;
        }

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&e&lHOLOGRAMAS ACTIVOS &7(" + holograms.size() + ")"));
        sender.sendMessage(c("&6═══════════════════════════════════"));

        try {
            for (Object hologram : holograms) {
                Method getLocationMethod = hologram.getClass().getMethod("getLocation");
                Method getHologramIdMethod = hologram.getClass().getMethod("getHologramId");
                Method getTypeMethod = hologram.getClass().getMethod("getType");

                Location loc = (Location) getLocationMethod.invoke(hologram);
                String id = (String) getHologramIdMethod.invoke(hologram);
                TopType type = (TopType) getTypeMethod.invoke(hologram);

                String mode = type.isWeekly() ? "&b[Weekly]" : "&d[All-Time]";

                sender.sendMessage(c(String.format("&e• &f%s %s &e%s",
                        id,
                        mode,
                        type.getDisplayName())));
                sender.sendMessage(c(String.format("  &7Ubicación: &f%.1f, %.1f, %.1f",
                        loc.getX(), loc.getY(), loc.getZ())));
            }
        } catch (Exception e) {
            sender.sendMessage(c("&c&l✖ Error al listar hologramas"));
            LogSystem.error(LogCategory.CORE, "Error listando hologramas:", e.getMessage());
            e.printStackTrace();
        }

        sender.sendMessage(c("&6═══════════════════════════════════"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        sender.sendMessage(c("&eRecargando hologramas..."));

        plugin.getHologramManager().reload();

        sender.sendMessage(c("&a&l✓ Hologramas recargados exitosamente"));
        sender.sendMessage(c("&7Se recargaron las configuraciones y se actualizaron los hologramas"));
        return true;
    }

    private boolean handleUpdate(CommandSender sender) {
        sender.sendMessage(c("&eActualizando hologramas..."));

        plugin.getHologramManager().updateAllHolograms();

        sender.sendMessage(c("&a&l✓ Hologramas actualizados"));
        return true;
    }
}