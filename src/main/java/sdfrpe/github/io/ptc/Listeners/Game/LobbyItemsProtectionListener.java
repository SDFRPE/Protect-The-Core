package sdfrpe.github.io.ptc.Listeners.Game;

import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class LobbyItemsProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();

        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();

            if (isLobbyItem(displayName)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§cNo puedes colocar items del lobby.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();

        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();

            if (isLobbyItem(displayName)) {
                e.setCancelled(true);
                LogSystem.debug(LogCategory.GAME, "Bloqueado intento de tirar item de lobby:", e.getPlayer().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (Statics.gameStatus != GameStatus.LOBBY &&
                Statics.gameStatus != GameStatus.STARTING) {
            return;
        }

        ItemStack clickedItem = e.getCurrentItem();

        if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {
            if (clickedItem != null && hasLobbyItemName(clickedItem)) {
                e.setCancelled(true);
                LogSystem.debug(LogCategory.GAME, "Bloqueado shift+click en item de lobby:", e.getWhoClicked().getName());
                return;
            }
        }

        if (e.getClick() == ClickType.DROP ||
                e.getClick() == ClickType.CONTROL_DROP ||
                e.getClick() == ClickType.WINDOW_BORDER_LEFT ||
                e.getClick() == ClickType.WINDOW_BORDER_RIGHT) {
            if (clickedItem != null && hasLobbyItemName(clickedItem)) {
                e.setCancelled(true);
                LogSystem.debug(LogCategory.GAME, "Bloqueado arrastre de item de lobby:", e.getWhoClicked().getName());
                return;
            }
        }

        if (e.getClick() == ClickType.NUMBER_KEY) {
            if (clickedItem != null && hasLobbyItemName(clickedItem)) {
                e.setCancelled(true);
                LogSystem.debug(LogCategory.GAME, "Bloqueado tecla numérica en item de lobby:", e.getWhoClicked().getName());
                return;
            }
        }
    }

    public static void cleanLobbyItemsFromAllPlayers() {
        int itemsRemoved = 0;
        int playersAffected = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            int playerItemsRemoved = 0;

            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack item = player.getInventory().getItem(slot);

                if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = item.getItemMeta().getDisplayName();

                    if (isLobbyItemStatic(displayName)) {
                        player.getInventory().setItem(slot, null);
                        playerItemsRemoved++;
                        itemsRemoved++;
                    }
                }
            }

            if (playerItemsRemoved > 0) {
                playersAffected++;
                player.updateInventory();
                LogSystem.debug(LogCategory.GAME, "Limpiados", playerItemsRemoved + " items de lobby de:", player.getName());
            }
        }

        if (itemsRemoved > 0) {
            LogSystem.info(LogCategory.GAME, "Limpieza de items del lobby completada:",
                    itemsRemoved + " items removidos de", playersAffected + " jugadores");
        }
    }

    private boolean isLobbyItem(String displayName) {
        return displayName.contains("Votar") ||
                displayName.contains("Equipo") ||
                displayName.contains("Extras") ||
                displayName.contains("Estadísticas") ||
                displayName.contains("Salir") ||
                displayName.contains("Leave");
    }

    private static boolean isLobbyItemStatic(String displayName) {
        return displayName.contains("Votar") ||
                displayName.contains("Equipo") ||
                displayName.contains("Extras") ||
                displayName.contains("Estadísticas") ||
                displayName.contains("Salir") ||
                displayName.contains("Leave");
    }

    private boolean hasLobbyItemName(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return isLobbyItem(item.getItemMeta().getDisplayName());
    }
}