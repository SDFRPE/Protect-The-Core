package sdfrpe.github.io.ptc.Utils.BossbarAPI;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import sdfrpe.github.io.ptc.Tasks.BossTask;
import sdfrpe.github.io.ptc.Utils.Abstracts.PTCRunnable;

import java.util.*;

public class BossBarAPI extends PTCRunnable {

    private static final Map<UUID, Integer> activeBars = new HashMap<>();
    private static final Map<UUID, BossBarData> currentData = new HashMap<>();
    private static boolean enabled = false;
    private static int entityCounter = 2000000;
    private final JavaPlugin plugin;

    private static class BossBarData {
        String message;
        float percentage;

        BossBarData(String message, float percentage) {
            this.message = message;
            this.percentage = percentage;
        }
    }

    public BossBarAPI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void runBoss() {
        BossTask.getBossTasks().add(this);
    }

    @Override
    public void onTick() {
        if (!enabled) {
            return;
        }

        Map<UUID, Integer> barsCopy = new HashMap<>(activeBars);
        Map<UUID, BossBarData> dataCopy = new HashMap<>(currentData);

        for (Map.Entry<UUID, Integer> entry : barsCopy.entrySet()) {
            UUID playerUUID = entry.getKey();
            Integer entityId = entry.getValue();

            Player player = Bukkit.getPlayer(playerUUID);

            if (player == null || !player.isOnline()) {
                activeBars.remove(playerUUID);
                currentData.remove(playerUUID);
                continue;
            }

            try {
                teleport(player, entityId);

                BossBarData data = dataCopy.get(playerUUID);
                if (data != null) {
                    updateData(player, entityId, data.message, data.percentage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setEnabled(boolean enable) {
        enabled = enable;
        if (!enable) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                removePlayer(p);
            }
            activeBars.clear();
            currentData.clear();
        }
    }

    public static void update(String message, float percentage) {
        if (!enabled) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayer(p, message, percentage);
        }
    }

    public static void updatePlayer(Player player, String message, float percentage) {
        if (!enabled || player == null || !player.isOnline()) return;

        Integer entityId = activeBars.get(player.getUniqueId());
        if (entityId == null) {
            entityId = entityCounter++;
            activeBars.put(player.getUniqueId(), entityId);
            spawn(player, entityId);
        }

        currentData.put(player.getUniqueId(), new BossBarData(message, percentage));

        updateData(player, entityId, message, percentage);
        teleport(player, entityId);
    }

    public static void removePlayer(Player player) {
        Integer entityId = activeBars.remove(player.getUniqueId());
        currentData.remove(player.getUniqueId());

        if (entityId != null) {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }

    private static Location getWitherLocation(Player player) {
        Location loc = player.getLocation().clone();
        Vector direction = loc.getDirection().normalize();
        direction.multiply(40);
        loc.add(direction);
        return loc;
    }

    private static void spawn(Player player, int entityId) {
        Location loc = getWitherLocation(player);

        List<EntityData> metadata = Arrays.asList(
                new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20),
                new EntityData(2, EntityDataTypes.STRING, ""),
                new EntityData(3, EntityDataTypes.BYTE, (byte) 1),
                new EntityData(6, EntityDataTypes.FLOAT, 300.0F),
                new EntityData(17, EntityDataTypes.INT, 0),
                new EntityData(20, EntityDataTypes.INT, 0)
        );

        WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(
                entityId,
                UUID.randomUUID(),
                EntityTypes.WITHER,
                SpigotConversionUtil.fromBukkitLocation(loc),
                0f,
                new Vector3d(0, 0, 0),
                metadata
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private static void updateData(Player player, int entityId, String text, float pct) {
        float health = Math.max(1.0f, Math.min(300.0f, (pct / 100.0f) * 300.0f));

        List<EntityData> metadata = Arrays.asList(
                new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20),
                new EntityData(2, EntityDataTypes.STRING, text),
                new EntityData(3, EntityDataTypes.BYTE, (byte) 1),
                new EntityData(6, EntityDataTypes.FLOAT, health),
                new EntityData(17, EntityDataTypes.INT, 0),
                new EntityData(20, EntityDataTypes.INT, 0)
        );

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private static void teleport(Player player, int entityId) {
        Location loc = getWitherLocation(player);

        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                0f,
                0f,
                false
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}