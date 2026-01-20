package sdfrpe.github.io.ptc.Utils.Menu.Vanilla;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClanWarArenaSelectorMenu implements Listener {
    private Inventory serverInventory;
    private final ServerAPI serverAPI;
    private List<CWServerInfo> cwServers;

    private final Map<UUID, BukkitTask> viewerTasks = new ConcurrentHashMap<>();
    private static final int UPDATE_INTERVAL = 60;

    private List<CWServerInfo> cachedServers;
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 2000;

    private BukkitTask batchUpdateTask;
    private volatile boolean updatingServers = false;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public ClanWarArenaSelectorMenu() {
        this.serverAPI = new ServerAPI();
        this.cwServers = new ArrayList<>();
        this.cachedServers = new ArrayList<>();
    }

    public void initialize() {
        startBatchUpdateTask();
        Console.debug("ClanWarArenaSelectorMenu initialized");
    }

    private void startBatchUpdateTask() {
        batchUpdateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(PTC.getInstance(), () -> {
            if (!viewerTasks.isEmpty() && !updatingServers) {
                updateServersAsync();
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    private void updateServersAsync() {
        updatingServers = true;

        Bukkit.getScheduler().runTaskAsynchronously(PTC.getInstance(), () -> {
            try {
                List<ServerInfo> cwServers = serverAPI.getCWServers();
                Console.debug("[CWMENU] ═══════════════════════════════════");
                Console.debug("[CWMENU] Found " + cwServers.size() + " CW servers from API");
                for (ServerInfo s : cwServers) {
                    Console.debug("[CWMENU]   → " + s.getServerName() + " | Status: " + s.getStatus() + " | ModeCW: true");
                }

                List<JsonObject> upcomingWars = getUpcomingWars();
                Console.debug("[CWMENU] Found " + upcomingWars.size() + " upcoming wars from API");
                for (JsonObject w : upcomingWars) {
                    String assigned = w.has("assignedServer") ? w.get("assignedServer").getAsString() : "NULL";
                    String blue = w.has("challengerClanTag") ? w.get("challengerClanTag").getAsString() : "?";
                    String red = w.has("challengedClanTag") ? w.get("challengedClanTag").getAsString() : "?";
                    Console.debug("[CWMENU]   → " + blue + " vs " + red + " @ " + assigned);
                }
                Console.debug("[CWMENU] ═══════════════════════════════════");

                List<CWServerInfo> enrichedServers = new ArrayList<>();

                for (ServerInfo server : cwServers) {
                    CWServerInfo cwInfo = new CWServerInfo(server);

                    for (JsonObject war : upcomingWars) {
                        String arenaKey = war.has("arenaKey") ? war.get("arenaKey").getAsString() : null;

                        Console.debug("[CWMENU] Comparing: '" + server.getServerName() + "' vs '" + arenaKey + "'");

                        if (arenaKey != null && server.getServerName().equalsIgnoreCase(arenaKey)) {
                            Console.debug("[CWMENU] ✓ MATCH FOUND! Setting war info for: " + server.getServerName());
                            cwInfo.setWarInfo(war);
                            break;
                        }
                    }

                    boolean hasWar = cwInfo.hasWarInfo();
                    boolean inGame = server.getStatus() == sdfrpe.github.io.ptc.Utils.Enums.GameStatus.IN_GAME;

                    if (hasWar || inGame) {
                        Console.debug("[CWMENU] ✓ Adding: " + server.getServerName() + " (hasWar=" + hasWar + ", inGame=" + inGame + ")");
                        enrichedServers.add(cwInfo);
                    } else {
                        Console.debug("[CWMENU] ✗ Skipping: " + server.getServerName() + " (no war, not in game)");
                    }
                }

                Console.debug("[CWMENU] Final result: " + enrichedServers.size() + " servers to display");
                Console.debug("[CWMENU] ═══════════════════════════════════");

                enrichedServers.sort((a, b) -> {
                    long timeA = a.getScheduledTime();
                    long timeB = b.getScheduledTime();
                    if (timeA == 0) return 1;
                    if (timeB == 0) return -1;
                    return Long.compare(timeA, timeB);
                });

                Bukkit.getScheduler().runTask(PTC.getInstance(), () -> {
                    this.cwServers = enrichedServers;
                    this.lastCacheUpdate = System.currentTimeMillis();
                    this.cachedServers = new ArrayList<>(enrichedServers);

                    for (UUID playerId : viewerTasks.keySet()) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline() && serverInventory != null) {
                            updateInventory();
                        }
                    }

                    updatingServers = false;
                });
            } catch (Exception e) {
                Console.error("[CWMENU] Error updating CW servers: " + e.getMessage());
                e.printStackTrace();
                updatingServers = false;
            }
        });
    }

    private List<JsonObject> getUpcomingWars() {
        try {
            String baseURL = PTC.getInstance().getGameManager().getGlobalSettings().getDatabaseURL();
            baseURL = baseURL.replace("/player/", "/war/all");

            Console.debug("[CWMENU] Fetching wars from: " + baseURL);

            URL obj = new URL(baseURL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int responseCode = con.getResponseCode();
            Console.debug("[CWMENU] Response code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                Console.debug("[CWMENU] Response: " + response.toString());

                com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
                JsonObject root = parser.parse(response.toString()).getAsJsonObject();

                if (!root.get("error").getAsBoolean()) {
                    JsonArray dataArray = root.getAsJsonArray("data");
                    List<JsonObject> wars = new ArrayList<>();

                    for (JsonElement element : dataArray) {
                        JsonObject war = element.getAsJsonObject();

                        boolean accepted = war.has("accepted") && war.get("accepted").getAsBoolean();
                        boolean finished = war.has("finished") && war.get("finished").getAsBoolean();
                        boolean hasServer = war.has("arenaKey") && war.get("arenaKey").getAsString() != null;

                        Console.debug("[CWMENU] War check - accepted:" + accepted + " finished:" + finished + " hasServer:" + hasServer);

                        if (accepted && !finished && hasServer) {
                            wars.add(war);
                        }
                    }

                    Console.debug("[CWMENU] Filtered to " + wars.size() + " valid wars");
                    return wars;
                } else {
                    Console.error("[CWMENU] API returned error: " + root.get("message").getAsString());
                }
            }
        } catch (Exception e) {
            Console.error("[CWMENU] Error getting wars: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void open(Player player) {
        Console.debug("[CWMENU] Opening CW menu for: " + player.getName());

        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_TTL_MS) {
            updateServersAsync();
        }

        updateInventory();
        player.openInventory(serverInventory);

        startAutoUpdate(player);
    }

    private void startAutoUpdate(Player player) {
        stopAutoUpdate(player);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(PTC.getInstance(), () -> {
            if (player.isOnline() && serverInventory != null &&
                    player.getOpenInventory().getTopInventory().equals(serverInventory)) {

                if (System.currentTimeMillis() - lastCacheUpdate > CACHE_TTL_MS && !updatingServers) {
                    updateServersAsync();
                } else {
                    updateInventory();
                }
            } else {
                stopAutoUpdate(player);
            }
        }, 20L, 20L);

        viewerTasks.put(player.getUniqueId(), task);
    }

    private void stopAutoUpdate(Player player) {
        BukkitTask task = viewerTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopAutoUpdate(event.getPlayer());
    }

    private void updateInventory() {
        int serverCount = cwServers.isEmpty() ? 1 : cwServers.size();
        int rows = Math.min(6, Math.max(1, (serverCount + 8) / 9));

        if (serverInventory == null || serverInventory.getSize() != rows * 9) {
            serverInventory = Bukkit.createInventory(null, rows * 9, "§4§lGuerras de Clanes");
        }

        serverInventory.clear();

        if (cwServers.isEmpty()) {
            ItemStack noWars = new ItemStack(Material.BARRIER);
            ItemMeta meta = noWars.getItemMeta();
            meta.setDisplayName("§c§lNo hay guerras próximas");
            List<String> lore = new ArrayList<>();
            lore.add("§7No hay guerras programadas");
            lore.add("§7en la próxima hora.");
            lore.add("");
            lore.add("§6Usa §e/clan §6para desafiar");
            meta.setLore(lore);
            noWars.setItemMeta(meta);
            serverInventory.setItem(0, noWars);
            Console.debug("[CWMENU] Inventory updated: NO WARS");
            return;
        }

        for (int i = 0; i < Math.min(this.cwServers.size(), 54); i++) {
            CWServerInfo server = this.cwServers.get(i);
            ItemStack item = createCWServerItem(server);
            serverInventory.setItem(i, item);
        }

        cachedServers = new ArrayList<>(this.cwServers);
        lastCacheUpdate = System.currentTimeMillis();

        Console.debug("[CWMENU] Inventory updated with " + this.cwServers.size() + " wars");
    }

    private ItemStack createCWServerItem(CWServerInfo cwServer) {
        ServerInfo server = cwServer.getServerInfo();
        Material material = Material.BANNER;
        short dataValue = 1;

        switch (server.getStatus()) {
            case IN_GAME:
                dataValue = 14;
                break;
            case STARTING:
                dataValue = 1;
                break;
            case LOBBY:
                dataValue = 11;
                break;
            default:
                dataValue = 8;
        }

        ItemStack item = new ItemStack(material, 1, dataValue);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§c§l" + server.getServerName());

        List<String> lore = new ArrayList<>();
        lore.add("§7§m-------------------");

        if (cwServer.hasWarInfo()) {
            String blueTag = cwServer.getBlueClanTag();
            String redTag = cwServer.getRedClanTag();

            lore.add("§9§l" + blueTag + " §fvs §c§l" + redTag);
            lore.add("");
            lore.add("§fArena: §e" + server.getMapName());

            long scheduledTime = cwServer.getScheduledTime();
            if (scheduledTime > 0) {
                long now = System.currentTimeMillis();
                long diff = scheduledTime - now;

                if (diff > 0) {
                    long minutes = diff / 60000;
                    long seconds = (diff % 60000) / 1000;

                    if (minutes > 0) {
                        lore.add("§fInicia en: §a" + minutes + "m " + seconds + "s");
                    } else {
                        lore.add("§fInicia en: §a" + seconds + "s");
                    }

                    String timeStr = timeFormat.format(new Date(scheduledTime));
                    lore.add("§fHora: §7" + timeStr);
                } else {
                    lore.add("§fEstado: §a§lEN CURSO");
                }
            }
        } else {
            lore.add("§fArena: §e" + server.getMapName());
            if (server.getCwInfo() != null) {
                lore.add("§f" + server.getCwInfo());
            }
        }

        lore.add("");
        lore.add("§fJugadores: §6" + server.getCurrentPlayers() + "§7/§6" + server.getMaxPlayers());

        switch (server.getStatus()) {
            case IN_GAME:
                lore.add("§fTiempo: §6" + server.getGameTime());
                lore.add("");
                lore.add("§c§l⚔ EN BATALLA");
                lore.add("§7¡Haz clic para unirte!");
                break;
            case STARTING:
                lore.add("");
                lore.add("§e§l⏳ INICIANDO");
                lore.add("§7¡Haz clic para unirte!");
                break;
            case LOBBY:
                lore.add("");
                lore.add("§a§l✓ DISPONIBLE");
                lore.add("§7¡Haz clic para unirte!");
                break;
            default:
                lore.add("");
                lore.add("§7Esperando...");
        }

        lore.add("§7§m-------------------");
        meta.setLore(lore);
        item.setItemMeta(meta);

        return ItemBuilder.hideAttributes(item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (this.serverInventory == null) return;
        if (e.getInventory() == null || e.getInventory().getName() == null) return;

        if (e.getInventory().getName().equals(this.serverInventory.getName())) {
            e.setCancelled(true);

            if (!(e.getWhoClicked() instanceof Player)) return;

            Player player = (Player) e.getWhoClicked();
            ItemStack clickedItem = e.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
                return;
            }

            int slot = e.getSlot();

            if (this.cwServers == null || this.cwServers.isEmpty()) {
                player.sendMessage("§cNo hay guerras disponibles en este momento.");
                return;
            }

            if (slot < 0 || slot >= this.cwServers.size()) return;

            CWServerInfo cwServer = this.cwServers.get(slot);
            ServerInfo server = cwServer.getServerInfo();

            if (server == null) {
                player.sendMessage("§cError al obtener información del servidor.");
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

    private void connectToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(PTC.getInstance(), "BungeeCord", out.toByteArray());
            player.sendMessage("§6¡Conectando a guerra de clanes en " + serverName + "...");

            Console.debug("Connecting player " + player.getName() + " to CW server: " + serverName);
        } catch (Exception ex) {
            player.sendMessage("§c¡Error al conectar al servidor!");
            Console.error("Failed to connect player to CW server: " + ex.getMessage());
        }
    }

    public void cleanup() {
        if (batchUpdateTask != null) {
            batchUpdateTask.cancel();
            Console.debug("CW Batch update task cancelled");
        }

        for (BukkitTask task : this.viewerTasks.values()) {
            task.cancel();
        }
        this.viewerTasks.clear();

        cachedServers.clear();

        Console.log("CW Arena Selector Menu cleaned up");
    }

    private static class CWServerInfo {
        private final ServerInfo serverInfo;
        private JsonObject warInfo;

        public CWServerInfo(ServerInfo serverInfo) {
            this.serverInfo = serverInfo;
        }

        public ServerInfo getServerInfo() {
            return serverInfo;
        }

        public void setWarInfo(JsonObject warInfo) {
            this.warInfo = warInfo;
        }

        public boolean hasWarInfo() {
            return warInfo != null;
        }

        public String getBlueClanTag() {
            if (warInfo == null) return "???";
            return warInfo.has("challengerClanTag") ? warInfo.get("challengerClanTag").getAsString() : "???";
        }

        public String getRedClanTag() {
            if (warInfo == null) return "???";
            return warInfo.has("challengedClanTag") ? warInfo.get("challengedClanTag").getAsString() : "???";
        }

        public long getScheduledTime() {
            if (warInfo == null) return 0;
            return warInfo.has("scheduledTime") ? warInfo.get("scheduledTime").getAsLong() : 0;
        }
    }
}