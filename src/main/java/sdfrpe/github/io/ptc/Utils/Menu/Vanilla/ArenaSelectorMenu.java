package sdfrpe.github.io.ptc.Utils.Menu.Vanilla;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Database.Engines.ServerAPI;
import sdfrpe.github.io.ptc.Game.Server.ServerInfo;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Items.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaSelectorMenu implements Listener {
    private Inventory serverInventory;
    private final ServerAPI serverAPI;
    private List<ServerInfo> servers;

    private final Map<UUID, BukkitTask> viewerTasks = new ConcurrentHashMap<>();
    private static final int UPDATE_INTERVAL = 60;

    private List<ServerInfo> cachedServers;
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 2000;

    private BukkitTask batchUpdateTask;
    private volatile boolean updatingServers = false;

    public ArenaSelectorMenu() {
        this.serverAPI = new ServerAPI();
        this.servers = new ArrayList<>();
        this.cachedServers = new ArrayList<>();
    }

    public void initialize() {
        startBatchUpdateTask();
        Console.debug("ArenaSelectorMenu initialized");
    }

    private void startBatchUpdateTask() {
        batchUpdateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(PTC.getInstance(), () -> {
            if (!viewerTasks.isEmpty() && !updatingServers) {
                updateServersAsync();
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL);
        Console.debug("Batch update task started for ArenaSelectorMenu");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.stopAutoUpdate(e.getPlayer());
    }

    public void open(Player player) {
        this.updateServers();
        player.openInventory(this.serverInventory);
        this.startAutoUpdate(player);
    }

    private void startAutoUpdate(Player player) {
        BukkitTask oldTask = this.viewerTasks.get(player.getUniqueId());
        if (oldTask != null) {
            oldTask.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(PTC.getInstance(), () -> {
            if (player.isOnline() && player.getOpenInventory() != null &&
                    player.getOpenInventory().getTopInventory().getName() != null &&
                    player.getOpenInventory().getTopInventory().getName().equals(this.serverInventory.getName())) {
                this.refreshInventory(player);
            } else {
                this.stopAutoUpdate(player);
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL);

        this.viewerTasks.put(player.getUniqueId(), task);
        Console.debug("Started auto-update for player: " + player.getName());
    }

    private void stopAutoUpdate(Player player) {
        BukkitTask task = this.viewerTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            Console.debug("Stopped auto-update for player: " + player.getName());
        }
    }

    private void refreshInventory(Player player) {
        try {
            long now = System.currentTimeMillis();
            List<ServerInfo> serversToUse;

            if (cachedServers != null && !cachedServers.isEmpty() &&
                    (now - lastCacheUpdate) < CACHE_TTL_MS) {
                serversToUse = cachedServers;
            } else {
                serversToUse = this.servers;
            }

            if (serversToUse == null || serversToUse.isEmpty()) {
                return;
            }

            for (int i = 0; i < serversToUse.size() && i < 54; i++) {
                ServerInfo server = serversToUse.get(i);
                ItemStack item = this.createServerItem(server);
                this.serverInventory.setItem(i, item);
            }

        } catch (Exception e) {
            Console.error("Error refreshing inventory for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void updateServersAsync() {
        if (updatingServers) {
            return;
        }

        updatingServers = true;

        Bukkit.getScheduler().runTaskAsynchronously(PTC.getInstance(), () -> {
            try {
                List<ServerInfo> updatedServers = this.serverAPI.getNormalServers();

                if (updatedServers == null || updatedServers.isEmpty()) {
                    updatingServers = false;
                    return;
                }

                if (PTC.getInstance().getGameManager().getGlobalSettings().isLobbyMode()) {
                    updatedServers.sort((s1, s2) -> {
                        try {
                            String name1 = s1.getServerName();
                            String name2 = s2.getServerName();

                            if (name1.startsWith("PTC-") && name2.startsWith("PTC-")) {
                                int num1 = Integer.parseInt(name1.substring(4));
                                int num2 = Integer.parseInt(name2.substring(4));
                                return Integer.compare(num1, num2);
                            }

                            return name1.compareTo(name2);
                        } catch (NumberFormatException e) {
                            return s1.getServerName().compareTo(s2.getServerName());
                        }
                    });
                }

                cachedServers = new ArrayList<>(updatedServers);
                lastCacheUpdate = System.currentTimeMillis();

                Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
                    this.servers = updatedServers;
                    updateInventoryContents();
                    updatingServers = false;
                });

            } catch (Exception e) {
                Console.error("Error updating servers async: " + e.getMessage());
                updatingServers = false;
            }
        });
    }

    private void updateInventoryContents() {
        if (this.servers == null || this.servers.isEmpty()) {
            return;
        }

        for (int i = 0; i < this.servers.size() && i < 54; i++) {
            ServerInfo server = this.servers.get(i);
            ItemStack item = this.createServerItem(server);
            this.serverInventory.setItem(i, item);
        }
    }

    public void updateServers() {
        Console.debug("Updating arena selector menu...");
        this.servers = this.serverAPI.getNormalServers();

        if (PTC.getInstance().getGameManager().getGlobalSettings().isLobbyMode()) {
            this.servers.sort((s1, s2) -> {
                try {
                    String name1 = s1.getServerName();
                    String name2 = s2.getServerName();

                    if (name1.startsWith("PTC-") && name2.startsWith("PTC-")) {
                        int num1 = Integer.parseInt(name1.substring(4));
                        int num2 = Integer.parseInt(name2.substring(4));
                        return Integer.compare(num1, num2);
                    }

                    return name1.compareTo(name2);
                } catch (NumberFormatException e) {
                    return s1.getServerName().compareTo(s2.getServerName());
                }
            });
            Console.debug("Servers sorted by arena number (Lobby Mode)");
        }

        int serverCount = this.servers.size();
        int rows = Math.min(6, Math.max(1, (serverCount + 8) / 9));

        this.serverInventory = Bukkit.createInventory(null, rows * 9, "§6§lArenas Protect The Core");
        this.serverInventory.clear();

        for (int i = 0; i < this.servers.size() && i < 54; i++) {
            ServerInfo server = this.servers.get(i);
            ItemStack item = this.createServerItem(server);
            this.serverInventory.setItem(i, item);
        }

        cachedServers = new ArrayList<>(this.servers);
        lastCacheUpdate = System.currentTimeMillis();

        Console.debug("Arena selector updated with " + this.servers.size() + " normal servers (excluding CW)");
    }

    private ItemStack createServerItem(ServerInfo server) {
        Material material = Material.WOOL;
        short dataValue;

        switch (server.getStatus()) {
            case LOBBY:
                dataValue = 5;
                break;
            case STARTING:
                dataValue = 4;
                break;
            case IN_GAME:
                dataValue = 14;
                break;
            case ENDED:
                dataValue = 7;
                break;
            default:
                dataValue = 7;
        }

        ItemStack item = new ItemStack(material, 1, dataValue);
        ItemMeta meta = item.getItemMeta();

        String displayName = server.getServerName();
        if (displayName.startsWith("PTC-")) {
            displayName = "PTC" + displayName.substring(4);
        }
        meta.setDisplayName("§b" + displayName);

        List<String> lore = new ArrayList<>();

        switch (server.getStatus()) {
            case LOBBY:
                lore.add("§fEstado: §aLobby");
                lore.add("§fJugadores: §a" + server.getCurrentPlayers() + "§7/§a" + server.getMaxPlayers());
                break;

            case STARTING:
                lore.add("§fEstado: §eIniciando");
                lore.add("§fJugadores: §e" + server.getCurrentPlayers() + "§7/§e" + server.getMaxPlayers());
                break;

            case IN_GAME:
                lore.add("§fEstado: §6En Juego");
                lore.add("§fJugadores: §6" + server.getCurrentPlayers() + "§7/§6" + server.getMaxPlayers());
                lore.add("§fMapa: §b" + server.getMapName());
                lore.add("§fTermina en: §c" + server.getGameTime());
                lore.add("");
                lore.add("§6Con VIP puede entrar!");
                break;

            case ENDED:
                lore.add("§fEstado: §cTerminando");
                lore.add("§fJugadores: §c" + server.getCurrentPlayers() + "§7/§c" + server.getMaxPlayers());
                break;

            default:
                lore.add("§fEstado: §7Desconocido");
                lore.add("§fJugadores: §7" + server.getCurrentPlayers() + "§7/§7" + server.getMaxPlayers());
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return ItemBuilder.hideAttributes(item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (this.serverInventory == null) {
            return;
        }

        if (e.getInventory() == null || e.getInventory().getName() == null) {
            return;
        }

        if (e.getInventory().getName().equals(this.serverInventory.getName())) {
            e.setCancelled(true);

            if (!(e.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) e.getWhoClicked();
            ItemStack clickedItem = e.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
                return;
            }

            int slot = e.getSlot();

            if (this.servers == null || this.servers.isEmpty()) {
                player.sendMessage("§cNo hay servidores disponibles en este momento.");
                return;
            }

            if (slot < 0 || slot >= this.servers.size()) {
                return;
            }

            ServerInfo server = this.servers.get(slot);

            if (server == null) {
                player.sendMessage("§cError al obtener información del servidor.");
                return;
            }

            if (!this.canJoinServer(player, server)) {
                return;
            }

            this.connectToServer(player, server.getServerName());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (this.serverInventory != null &&
                e.getInventory() != null &&
                e.getInventory().getName() != null &&
                e.getInventory().getName().equals(this.serverInventory.getName())) {
            if (e.getPlayer() instanceof Player) {
                Player player = (Player) e.getPlayer();
                this.stopAutoUpdate(player);
            }
        }
    }

    private boolean canJoinServer(Player player, ServerInfo server) {
        switch (server.getStatus()) {
            case LOBBY:
            case STARTING:
                return true;

            case IN_GAME:
                if (server.isVipOnly() && !player.hasPermission("ptc.joiningame")) {
                    player.sendMessage("§c¡Necesitas ser &eVIP &cpara unirse a partidas en curso!");
                    return false;
                }
                return true;

            case ENDED:
                player.sendMessage("§c¡Esta partida está terminando! Espera a la próxima.");
                return false;

            default:
                return false;
        }
    }

    private void connectToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(PTC.getInstance(), "BungeeCord", out.toByteArray());
            player.sendMessage("§a¡Conectando a " + serverName + "...");

            Console.debug("Connecting player " + player.getName() + " to server: " + serverName);
        } catch (Exception ex) {
            player.sendMessage("§c¡Error al conectar al servidor!");
            Console.error("Failed to connect player to server: " + ex.getMessage());
        }
    }

    public void cleanup() {
        if (batchUpdateTask != null) {
            batchUpdateTask.cancel();
            Console.debug("Batch update task cancelled");
        }

        for (BukkitTask task : this.viewerTasks.values()) {
            task.cancel();
        }
        this.viewerTasks.clear();

        cachedServers.clear();

        Console.log("Arena Selector Menu cleaned up");
    }

    public List<ServerInfo> getServers() {
        return this.servers;
    }
}