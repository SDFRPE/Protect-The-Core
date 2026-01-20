package sdfrpe.github.io.ptc.Utils.Menu.Vanilla;

import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Items.ItemBuilder;
import sdfrpe.github.io.ptc.Utils.Statics;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatisticsMenu implements Listener {
    private final GameManager gameManager;
    private Inventory statsInventory;
    private int animationCycle = 0;

    private final Map<UUID, BukkitTask> viewerTasks = new HashMap<>();
    private static final int UPDATE_INTERVAL = 20;

    public StatisticsMenu(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        GamePlayer gamePlayer = this.gameManager.getPlayerManager().getPlayer(player.getUniqueId());
        if (gamePlayer == null) {
            return;
        }

        this.updateMenu(gamePlayer);
        player.openInventory(this.statsInventory);
        this.startAutoUpdate(player);
    }

    private void startAutoUpdate(Player player) {
        BukkitTask oldTask = this.viewerTasks.get(player.getUniqueId());
        if (oldTask != null) {
            oldTask.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                this.gameManager.getPtc(),
                () -> {
                    if (player.getOpenInventory() != null &&
                            player.getOpenInventory().getTopInventory() != null &&
                            player.getOpenInventory().getTopInventory().equals(this.statsInventory)) {

                        GamePlayer gamePlayer = this.gameManager.getPlayerManager().getPlayer(player.getUniqueId());
                        if (gamePlayer != null) {
                            this.updateMenu(gamePlayer);
                        }
                    } else {
                        this.stopAutoUpdate(player.getUniqueId());
                    }
                },
                UPDATE_INTERVAL,
                UPDATE_INTERVAL
        );

        this.viewerTasks.put(player.getUniqueId(), task);
    }

    private void stopAutoUpdate(UUID playerId) {
        BukkitTask task = this.viewerTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void updateMenu(GamePlayer gamePlayer) {
        this.statsInventory = Bukkit.createInventory(null, 27, Statics.c("&8Estadísticas"));

        ItemStack border = ItemBuilder.createItem(XMaterial.GRAY_STAINED_GLASS_PANE, 1, " ");
        for (int i = 0; i < 27; i++) {
            this.statsInventory.setItem(i, border);
        }

        this.animationCycle = (this.animationCycle + 1) % 3;

        int kills = gamePlayer.getPlayerStats().getKills();
        XMaterial killMaterial = this.animationCycle == 0 ? XMaterial.DIAMOND_SWORD :
                this.animationCycle == 1 ? XMaterial.IRON_SWORD : XMaterial.GOLDEN_SWORD;
        ItemStack killsItem = ItemBuilder.createItem(
                killMaterial,
                1,
                Statics.c("&bAsesinatos"),
                "",
                Statics.c("&7Tienes un total de &a" + kills + " &7asesinatos."),
                ""
        );
        this.statsInventory.setItem(10, killsItem);

        int deaths = gamePlayer.getPlayerStats().getDeaths();
        XMaterial deathMaterial = this.animationCycle == 0 ? XMaterial.BARRIER :
                this.animationCycle == 1 ? XMaterial.SKELETON_SKULL : XMaterial.REDSTONE_BLOCK;
        ItemStack deathsItem = ItemBuilder.createItem(
                deathMaterial,
                1,
                Statics.c("&bMuertes"),
                "",
                Statics.c("&7Tienes un total de &a" + deaths + " &7muertes."),
                ""
        );
        this.statsInventory.setItem(12, deathsItem);

        int cores = gamePlayer.getPlayerStats().getCores();
        XMaterial coreMaterial = this.animationCycle == 0 ? XMaterial.DIAMOND_BLOCK :
                this.animationCycle == 1 ? XMaterial.DIAMOND_PICKAXE : XMaterial.DIAMOND;
        ItemStack coresItem = ItemBuilder.createItem(
                coreMaterial,
                1,
                Statics.c("&bNúcleos destruídos"),
                "",
                Statics.c("&7Tienes un total de &a" + cores + " &7de destrucciones."),
                ""
        );
        this.statsInventory.setItem(14, coresItem);

        int wins = gamePlayer.getPlayerStats().getWins();
        XMaterial winMaterial = this.animationCycle == 0 ? XMaterial.FIREWORK_ROCKET :
                this.animationCycle == 1 ? XMaterial.GOLD_BLOCK : XMaterial.NETHER_STAR;
        ItemStack winsItem = ItemBuilder.createItem(
                winMaterial,
                1,
                Statics.c("&bPartidas ganadas"),
                "",
                Statics.c("&7Tienes un total de &a" + wins + " &7partidas ganadas."),
                ""
        );
        this.statsInventory.setItem(16, winsItem);

        int dominaciones = gamePlayer.getPlayerStats().getBDomination();
        XMaterial domMaterial = this.animationCycle == 0 ? XMaterial.GOLDEN_SWORD :
                this.animationCycle == 1 ? XMaterial.BLAZE_ROD : XMaterial.FIRE_CHARGE;
        ItemStack dominacionesItem = ItemBuilder.createItem(
                domMaterial,
                1,
                Statics.c("&bDominaciones"),
                "",
                Statics.c("&7Tienes un total de &a" + dominaciones + " &7dominaciones."),
                ""
        );
        this.statsInventory.setItem(22, dominacionesItem);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (this.statsInventory == null || e.getInventory() == null) {
            return;
        }

        if (!e.getInventory().equals(this.statsInventory)) {
            return;
        }

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        this.stopAutoUpdate(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (this.statsInventory == null || e.getInventory() == null) {
            return;
        }

        if (!e.getInventory().equals(this.statsInventory)) {
            return;
        }

        if (e.getPlayer() instanceof Player) {
            Player player = (Player) e.getPlayer();
            this.stopAutoUpdate(player.getUniqueId());
        }
    }
}