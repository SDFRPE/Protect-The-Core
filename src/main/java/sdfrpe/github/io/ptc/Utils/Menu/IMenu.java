package sdfrpe.github.io.ptc.Utils.Menu;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Events.Menu.ClickMenu;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Menu.utils.ordItems;
import java.util.Collection;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class IMenu implements Listener {
   private PTC main = PTC.getInstance();
   private Inventory inv;
   private String name;
   private Integer rows;
   private String command;
   private HashMap<Integer, ordItems> itemsList;

   public IMenu(String name, Integer rows, String command, HashMap<Integer, ordItems> items) {
      this.name = name;
      this.rows = rows;
      this.command = command;
      this.itemsList = items;
      Bukkit.getServer().getPluginManager().registerEvents(this, PTC.getInstance());
   }

   public void open(GamePlayer gamePlayer) {
      Player p = gamePlayer.getPlayer();
      if (this.inv == null) {
         this.inv = Bukkit.createInventory(p, this.rows, this.name);
      }

      this.addItems(p);
      p.openInventory(this.inv);
      gamePlayer.setMenu(this);
   }

   public void addItems(Player p) {
      this.itemsList.values().forEach((item) -> {
         this.inv.setItem(item.getSlot(), this.hideAttributes(item.getIcon().build(p)));
      });
   }

   public boolean hasPerm(Player player, String perm) {
      return perm == null || perm.length() == 0 || player.hasPermission(perm);
   }

   @EventHandler
   public void onClick(InventoryClickEvent e) {
      if (this.inv != null) {
         if (e.getInventory().equals(this.inv)) {
            if (e.getWhoClicked() instanceof Player) {
               if (e.getCurrentItem() != null) {
                  if (e.getAction() == InventoryAction.PICKUP_ALL) {
                     Player p = (Player)e.getWhoClicked();
                     GamePlayer gamePlayer = this.main.getGameManager().getPlayerManager().getPlayer(p.getUniqueId());
                     if (e.getInventory().equals(this.inv)) {
                        int slot = e.getSlot();
                        ordItems ordItems = (ordItems)this.itemsList.get(slot);
                        if (ordItems == null) {
                           return;
                        }

                        if (slot == ordItems.getSlot() && this.hasPerm(p, ordItems.getPermission())) {
                           Bukkit.getPluginManager().callEvent(new ClickMenu(gamePlayer, ordItems));
                        }
                     }

                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onLeft(InventoryCloseEvent e) {
      if (this.inv != null) {
         if (e.getInventory().equals(this.inv)) {
            if (e.getPlayer() instanceof Player) {
               Player p = (Player)e.getPlayer();
               GamePlayer gamePlayer = this.main.getGameManager().getPlayerManager().getPlayer(p.getUniqueId());
               if (e.getInventory().equals(this.inv)) {
                  gamePlayer.setMenu((IMenu)null);
               }

            }
         }
      }
   }

   @EventHandler
   public void onDragInv(InventoryDragEvent e) {
      if (this.inv != null) {
         if (e.getInventory().equals(this.inv)) {
            if (e.getWhoClicked() instanceof Player) {
               e.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onClickInv(InventoryClickEvent e) {
      if (this.inv != null) {
         if (e.getInventory().equals(this.inv)) {
            if (e.getWhoClicked() instanceof Player) {
               e.setCancelled(true);
            }
         }
      }
   }

   public ItemStack hideAttributes(ItemStack item) {
      if (item == null) {
         return null;
      } else {
         ItemMeta meta = item.getItemMeta();
         if (this.isNullOrEmpty(meta.getItemFlags())) {
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
         }

         return item;
      }
   }

   private boolean isNullOrEmpty(Collection<?> coll) {
      return coll == null || coll.isEmpty();
   }

   private String c(String c) {
      return ChatColor.translateAlternateColorCodes('&', c);
   }

   public PTC getMain() {
      return this.main;
   }

   public Inventory getInv() {
      return this.inv;
   }

   public String getName() {
      return this.name;
   }

   public Integer getRows() {
      return this.rows;
   }

   public String getCommand() {
      return this.command;
   }

   public HashMap<Integer, ordItems> getItemsList() {
      return this.itemsList;
   }

   public void setMain(PTC main) {
      this.main = main;
   }

   public void setInv(Inventory inv) {
      this.inv = inv;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setRows(Integer rows) {
      this.rows = rows;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public void setItemsList(HashMap<Integer, ordItems> itemsList) {
      this.itemsList = itemsList;
   }
}
