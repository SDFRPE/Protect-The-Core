package sdfrpe.github.io.ptc.Listeners.Lobby;

import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LobbyInventoryProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }

        if (Statics.gameStatus == GameStatus.LOBBY ||
                Statics.gameStatus == GameStatus.STARTING) {

            if (e.getClickedInventory() != null &&
                    e.getClickedInventory().getType() == InventoryType.PLAYER) {

                ItemStack clickedItem = e.getCurrentItem();

                if (isLobbyItem(clickedItem)) {
                    if (e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean isLobbyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = item.getItemMeta().getDisplayName();

        return displayName.contains("Votar") ||
                displayName.contains("Equipo") ||
                displayName.contains("Extras") ||
                displayName.contains("Estadísticas") ||
                displayName.contains("Salir") ||
                displayName.contains("Leave");
    }
}