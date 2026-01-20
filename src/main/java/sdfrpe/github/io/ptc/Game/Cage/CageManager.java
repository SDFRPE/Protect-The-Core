package sdfrpe.github.io.ptc.Game.Cage;

import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class CageManager {
    private static final Map<UUID, List<Block>> cageBlocks = Maps.newConcurrentMap();
    private static final int CAGE_HEIGHT = 10;

    public static void createCages(ArenaTeam team) {
        if (team == null || team.getSpawnLocation() == null) {
            LogSystem.error(LogCategory.GAME, "Team o spawn null al crear cápsulas");
            return;
        }

        List<GamePlayer> players = new ArrayList<>(team.getTeamPlayers());
        if (players.isEmpty()) {
            return;
        }

        Location teamSpawn = team.getSpawnLocation().getLocation();
        int playerCount = players.size();
        double radius = calculateRadius(playerCount);
        byte teamGlassColor = getTeamGlassColor(team.getColor());

        LogSystem.info(LogCategory.GAME, "Creando cápsulas para equipo", team.getColor().getName());
        LogSystem.debug(LogCategory.GAME, "Jugadores:", String.valueOf(playerCount), "Radio:", String.valueOf(radius));

        List<Location> cagePositions = calculateCirclePositions(teamSpawn, playerCount, radius);

        Set<Chunk> chunksToLoad = new HashSet<>();
        for (Location cagePos : cagePositions) {
            chunksToLoad.add(cagePos.getChunk());
        }

        if (!chunksToLoad.isEmpty()) {
            LogSystem.info(LogCategory.GAME, "Pre-cargando", chunksToLoad.size() + " chunks para cápsulas...");
            for (Chunk chunk : chunksToLoad) {
                chunk.load(true);
            }
        }

        for (int i = 0; i < players.size(); i++) {
            GamePlayer gamePlayer = players.get(i);
            Player player = gamePlayer.getPlayer();

            if (player == null || !player.isOnline()) {
                continue;
            }

            Location cagePos = cagePositions.get(i);
            createSingleCage(player, cagePos, teamGlassColor);

            Location playerPos = cagePos.clone().add(0.5, 1.0, 0.5);
            playerPos.setYaw(player.getLocation().getYaw());
            playerPos.setPitch(player.getLocation().getPitch());

            final Player finalPlayer = player;
            final Location finalPlayerPos = playerPos;

            Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
                if (finalPlayer.isOnline()) {
                    finalPlayer.teleport(finalPlayerPos);
                    LogSystem.debug(LogCategory.GAME, "Jugador", finalPlayer.getName(), "teleportado a cápsula");
                }
            }, i * 2L);
        }
    }

    private static byte getTeamGlassColor(TeamColor teamColor) {
        switch (teamColor) {
            case BLUE:
                return 11;
            case RED:
                return 14;
            case GREEN:
                return 13;
            case YELLOW:
                return 4;
            default:
                return 0;
        }
    }

    private static double calculateRadius(int playerCount) {
        if (playerCount <= 1) {
            return 0;
        } else if (playerCount <= 4) {
            return 6;
        } else if (playerCount <= 8) {
            return 9;
        } else {
            return 12;
        }
    }

    private static List<Location> calculateCirclePositions(Location center, int count, double radius) {
        List<Location> positions = new ArrayList<>();

        if (count == 1 || radius == 0) {
            positions.add(center.clone().add(0, CAGE_HEIGHT, 0));
            return positions;
        }

        double angleStep = (2 * Math.PI) / count;

        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;

            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = center.getY() + CAGE_HEIGHT;

            positions.add(new Location(center.getWorld(), x, y, z));
        }

        return positions;
    }

    private static void createSingleCage(Player player, Location location, byte teamColor) {
        List<Block> blocks = new ArrayList<>();

        Location baseLoc = location.clone();
        baseLoc.setY(Math.floor(baseLoc.getY()));

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location floorLoc = baseLoc.clone().add(x, 0, z);
                Block floorBlock = floorLoc.getBlock();
                blocks.add(floorBlock);

                if (x == 0 && z == 0) {
                    floorBlock.setType(Material.STAINED_GLASS);
                    floorBlock.setData(teamColor);
                } else {
                    floorBlock.setType(Material.GLASS);
                }
            }
        }

        for (int y = 1; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    boolean isEdge = (x == -1 || x == 1 || z == -1 || z == 1);

                    Location wallLoc = baseLoc.clone().add(x, y, z);

                    if (isEdge) {
                        Block wallBlock = wallLoc.getBlock();
                        blocks.add(wallBlock);
                        wallBlock.setType(Material.GLASS);
                    } else {
                        wallLoc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location roofLoc = baseLoc.clone().add(x, 3, z);
                Block roofBlock = roofLoc.getBlock();
                blocks.add(roofBlock);

                if (x == 0 && z == 0) {
                    roofBlock.setType(Material.STAINED_GLASS);
                    roofBlock.setData(teamColor);
                } else {
                    roofBlock.setType(Material.GLASS);
                }
            }
        }

        cageBlocks.put(player.getUniqueId(), blocks);
        LogSystem.debug(LogCategory.GAME, "Cápsula 3x3x4 creada para:", player.getName());
    }

    public static void destroyAllCages() {
        LogSystem.info(LogCategory.GAME, "Destruyendo todas las cápsulas...");

        int totalBlocks = 0;
        for (List<Block> blocks : cageBlocks.values()) {
            for (Block block : blocks) {
                if (block != null && (block.getType() == Material.GLASS || block.getType() == Material.STAINED_GLASS)) {
                    block.setType(Material.AIR);
                    totalBlocks++;
                }
            }
        }

        cageBlocks.clear();
        LogSystem.info(LogCategory.GAME, "Cápsulas destruidas:", totalBlocks + " bloques removidos");
    }

    public static void destroyCage(Player player) {
        List<Block> blocks = cageBlocks.remove(player.getUniqueId());

        if (blocks != null) {
            for (Block block : blocks) {
                if (block != null && (block.getType() == Material.GLASS || block.getType() == Material.STAINED_GLASS)) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public static boolean isInCagePhase() {
        return !cageBlocks.isEmpty();
    }
}