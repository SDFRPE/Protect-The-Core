package sdfrpe.github.io.ptc.Utils.Menu.Vanilla;

import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Items.ItemBuilder;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import sdfrpe.github.io.ptc.Utils.Enums.TimeOfDay;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ExtrasMenu implements Listener {
    private final GameManager gameManager;
    private Inventory extrasInventory;

    public ExtrasMenu(GameManager gameManager) {
        this.gameManager = gameManager;
        this.updateMenu();
    }

    public void open(Player player) {
        this.updateMenu();
        player.openInventory(this.extrasInventory);
    }

    public void updateMenu() {
        this.extrasInventory = Bukkit.createInventory(null, 27, "§8Extras");

        ItemStack border = ItemBuilder.createItem(XMaterial.GRAY_STAINED_GLASS_PANE, 1, " ");
        this.extrasInventory.setItem(0, border);
        this.extrasInventory.setItem(8, border);
        this.extrasInventory.setItem(9, border);
        this.extrasInventory.setItem(17, border);
        this.extrasInventory.setItem(18, border);
        this.extrasInventory.setItem(26, border);

        this.extrasInventory.setItem(1, this.createTimeItem(TimeOfDay.DAY));
        this.extrasInventory.setItem(2, this.createTimeItem(TimeOfDay.AFTERNOON));
        this.extrasInventory.setItem(3, this.createTimeItem(TimeOfDay.NIGHT));

        this.extrasInventory.setItem(10, this.createHealthItem(0));
        this.extrasInventory.setItem(11, this.createHealthItem(1));
        this.extrasInventory.setItem(12, this.createHealthItem(2));
        this.extrasInventory.setItem(13, this.createHealthItem(3));

        ItemStack closeItem = ItemBuilder.createItem(XMaterial.BARRIER, 1, "&cCerrar");
        this.extrasInventory.setItem(22, closeItem);

        LogSystem.debug(LogCategory.GAME, "Extras menu actualizado");
    }

    private ItemStack createTimeItem(TimeOfDay time) {
        int votes = this.gameManager.getGameVoteManager().getTimeVoteCount(time);
        XMaterial material;
        String name;
        String[] lore;

        switch (time) {
            case DAY:
                material = XMaterial.SUNFLOWER;
                name = "&e&lDÍA";
                lore = new String[]{"&76:00 AM", "&8" + votes + " votos", "", "&eClick para votar"};
                break;
            case AFTERNOON:
                material = XMaterial.ORANGE_TULIP;
                name = "&6&lATARDECER";
                lore = new String[]{"&76:00 PM", "&8" + votes + " votos", "", "&eClick para votar"};
                break;
            case NIGHT:
                material = XMaterial.BLUE_ORCHID;
                name = "&9&lNOCHE";
                lore = new String[]{"&712:00 AM", "&8" + votes + " votos", "", "&eClick para votar"};
                break;
            default:
                material = XMaterial.GLASS;
                name = "&fDesconocido";
                lore = new String[]{};
        }

        return ItemBuilder.createItem(material, 1, name, lore);
    }

    private ItemStack createHealthItem(int extraHearts) {
        int votes = this.gameManager.getGameVoteManager().getHealthVoteCount(extraHearts);
        XMaterial material;
        String name;
        String hearts;

        switch (extraHearts) {
            case 0:
                material = XMaterial.APPLE;
                name = "&c&lVIDA NORMAL";
                hearts = "&c❤❤❤❤❤";
                break;
            case 1:
                material = XMaterial.GOLDEN_APPLE;
                name = "&6&lVIDA +1";
                hearts = "&c❤❤❤❤❤&6❤";
                break;
            case 2:
                material = XMaterial.ENCHANTED_GOLDEN_APPLE;
                name = "&e&lVIDA +2";
                hearts = "&c❤❤❤❤❤&e❤❤";
                break;
            case 3:
                material = XMaterial.GOLDEN_CARROT;
                name = "&a&lVIDA +3";
                hearts = "&c❤❤❤❤❤&a❤❤❤";
                break;
            default:
                material = XMaterial.APPLE;
                name = "&fDesconocido";
                hearts = "";
        }

        String[] lore = new String[]{hearts, "&8" + votes + " votos", "", "&eClick para votar"};
        return ItemBuilder.createItem(material, extraHearts == 0 ? 1 : extraHearts, name, lore);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (this.extrasInventory == null || e.getInventory() == null) {
            return;
        }

        if (!e.getInventory().getName().equals(this.extrasInventory.getName())) {
            return;
        }

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
        GamePlayer gamePlayer = this.gameManager.getPlayerManager().getPlayer(player.getUniqueId());

        if (gamePlayer == null) {
            return;
        }

        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        Material itemType = clickedItem.getType();

        if (itemType == Material.DOUBLE_PLANT || itemType == Material.RED_ROSE) {
            TimeOfDay votedTime = null;
            String timeName = "";

            if (slot == 1) {
                votedTime = TimeOfDay.DAY;
                timeName = "Día";
            } else if (slot == 2) {
                votedTime = TimeOfDay.AFTERNOON;
                timeName = "Atardecer";
            } else if (slot == 3) {
                votedTime = TimeOfDay.NIGHT;
                timeName = "Noche";
            }

            if (votedTime != null) {
                this.gameManager.getGameVoteManager().voteTime(gamePlayer, votedTime);
                new TitleAPI()
                        .title(ChatColor.GREEN + "✓")
                        .subTitle(ChatColor.GOLD + timeName)
                        .send(player);
                player.closeInventory();
                LogSystem.debug(LogCategory.GAME, "Voto tiempo:", player.getName(), "->", timeName);
            }
        } else if (itemType == Material.APPLE || itemType == Material.GOLDEN_APPLE || itemType == Material.GOLDEN_CARROT) {
            int votedHearts = -1;

            if (slot == 10) votedHearts = 0;
            else if (slot == 11) votedHearts = 1;
            else if (slot == 12) votedHearts = 2;
            else if (slot == 13) votedHearts = 3;

            if (votedHearts >= 0) {
                this.gameManager.getGameVoteManager().voteHealth(gamePlayer, votedHearts);
                new TitleAPI()
                        .title(ChatColor.GREEN + "✓")
                        .subTitle(ChatColor.RED + "+" + votedHearts + " ❤")
                        .send(player);
                player.closeInventory();
                LogSystem.debug(LogCategory.GAME, "Voto vida:", player.getName(), "->", votedHearts + " extra");
            }
        }
    }
}