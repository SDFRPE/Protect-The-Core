package sdfrpe.github.io.ptc.Utils.Menu.Vanilla;

import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Items.ItemBuilder;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import com.cryptomorin.xseries.XMaterial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class VoteMenu implements Listener {
   private GameManager gameManager;
   private Inventory voteInventory;

   public VoteMenu(GameManager gameManager) {
      this.gameManager = gameManager;
   }

   public void open(Player player) {
      player.openInventory(this.voteInventory);
   }

   public void updateArenas() {
      int arenaCount = this.gameManager.getArenaManager().countArenas();
      LogSystem.debug(LogCategory.GAME, "Actualizando menú de votación con", arenaCount + " arenas");

      if (arenaCount == 0) {
         LogSystem.error(LogCategory.GAME, "No hay arenas - Usa /ptc create <nombre>");
         this.voteInventory = Bukkit.createInventory((InventoryHolder)null, 9, "Votar");
         return;
      }

      List<String> arenaNames = new ArrayList<>(this.gameManager.getArenaManager().getArenaNames());
      Collections.sort(arenaNames, String.CASE_INSENSITIVE_ORDER);

      int totalSlots = arenaCount + 1;
      int rows = (int) Math.ceil(totalSlots / 9.0);
      rows = Math.max(1, Math.min(6, rows));

      this.voteInventory = Bukkit.createInventory((InventoryHolder)null, 9 * rows, "Votar");
      this.voteInventory.clear();

      for (String arenaName : arenaNames) {
         String name = ChatColor.AQUA + arenaName;
         this.voteInventory.addItem(new ItemStack[]{ItemBuilder.hideAttributes(ItemBuilder.createItem((XMaterial)XMaterial.FILLED_MAP, 1, name))});
      }

      this.voteInventory.addItem(new ItemStack[]{ItemBuilder.createItem((XMaterial)XMaterial.BARRIER, 1, "&cCerrar")});
      LogSystem.debug(LogCategory.GAME, "Menú de votación actualizado:", arenaCount + " arenas en", rows + " filas");
   }

   @EventHandler
   public void inventoryClick(InventoryClickEvent e) {
      if (e.getInventory().getName().equals(this.voteInventory.getName())) {
         e.setCancelled(true);

         if (e.getWhoClicked() instanceof Player) {
            if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
               if (e.getCurrentItem().getItemMeta().hasDisplayName()) {
                  Player player = (Player)e.getWhoClicked();
                  ItemStack itemStack = e.getCurrentItem();
                  String displayName = Statics.strip(itemStack.getItemMeta().getDisplayName());

                  if (displayName.equalsIgnoreCase("Cerrar") || itemStack.getType() == Material.BARRIER) {
                     player.closeInventory();
                     return;
                  }

                  GamePlayer gamePlayer = this.gameManager.getPlayerManager().getPlayer(player.getUniqueId());
                  if (gamePlayer != null) {
                     this.gameManager.getArenaManager().vote(gamePlayer, displayName);
                     (new TitleAPI()).title(ChatColor.GREEN + "VOTASTE POR").subTitle(ChatColor.AQUA + displayName).send(player);
                     player.closeInventory();
                     LogSystem.debug(LogCategory.GAME, "Voto registrado:", player.getName(), "->", displayName);
                  }
               }
            }
         }
      }
   }
}