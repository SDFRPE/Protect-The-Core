package sdfrpe.github.io.ptc.Listeners.General;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Events.Menu.ClickMenu;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Tasks.InGame.ArenaTask;
import sdfrpe.github.io.ptc.Utils.EnchantmentValidator;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.VIPDiscountHelper;
import sdfrpe.github.io.ptc.Utils.Menu.IMenu;
import sdfrpe.github.io.ptc.Utils.Menu.utils.ordItems;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class onMenu implements Listener {
   private final PTC main = PTC.getInstance();

   @EventHandler
   public void onClick(ClickMenu event) {
      String cmd = event.getOrdItems().getCommand();
      if (cmd != null && !cmd.equals("")) {
         if (cmd.contains(";")) {
            String[] array = cmd.split(";");
            String[] array2 = array;
            int length = array.length;

            for(int i = 0; i < length; ++i) {
               String sub = array2[i];
               if (sub.startsWith(" ")) {
                  sub = sub.substring(1);
               }

               this.parseCommand(event.getPtcPlayer(), sub, event.getOrdItems());
            }
         } else {
            this.parseCommand(event.getPtcPlayer(), cmd, event.getOrdItems());
         }
      }
   }

   private void parseCommand(GamePlayer gamePlayer, String cmd, ordItems ordItems) {
      if (cmd != null && !cmd.equals("")) {
         Player p = gamePlayer.getPlayer();
         if (cmd.contains("%player%")) {
            cmd = cmd.replace("%player%", gamePlayer.getName());
         }

         String kit;
         if (cmd.startsWith("console:")) {
            kit = cmd.substring(8);
            if (kit.startsWith(" ")) {
               kit = kit.substring(1);
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), kit);
         } else if (cmd.startsWith("player:")) {
            kit = cmd.substring(7);
            if (kit.startsWith(" ")) {
               kit = kit.substring(1);
            }

            p.chat(kit);
         } else if (cmd.startsWith("open:")) {
            kit = cmd.substring(5);
            if (kit.startsWith(" ")) {
               kit = kit.substring(1);
            }

            IMenu menu = (IMenu)this.main.getMenuManager().getMenuFileName().get(kit);
            if (menu == null) {
               return;
            }

            Bukkit.getScheduler().runTaskLater(this.main, () -> {
               menu.open(gamePlayer);
            }, 2L);
         } else if (cmd.startsWith("server:")) {
            kit = cmd.substring(7);
            if (kit.startsWith(" ")) {
               kit = kit.substring(1);
            }

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            try {
               out.writeUTF("Connect");
               out.writeUTF(kit);
               p.sendPluginMessage(this.main, "BungeeCord", b.toByteArray());
            } catch (Exception var15) {
               LogSystem.error(LogCategory.NETWORK, "Error conectando al servidor:", kit, var15.getMessage());
            }
         } else if (cmd.startsWith("msg:")) {
            kit = cmd.substring(4);
            if (kit.startsWith(" ")) {
               kit = kit.substring(1);
            }

            p.sendMessage(Statics.c(kit));
         } else if (cmd.startsWith("addcores:")) {
            kit = cmd.substring(9);
            if (kit.startsWith(" ")) {
               kit = kit.substring(1);
            }

            String[] parts = kit.split(":");
            if (parts.length != 2) {
               p.sendMessage(Statics.c("&cError: Formato incorrecto de comando addcores"));
               return;
            }

            try {
               int originalPrice = Integer.parseInt(parts[0]);
               int coresToAdd = Integer.parseInt(parts[1]);
               int discountPercent = VIPDiscountHelper.getDiscountPercentage(p);
               int price = VIPDiscountHelper.calculateDiscountedPrice(originalPrice, discountPercent);
               int balance = gamePlayer.getCoins();

               if (balance >= price) {
                  ArenaTeam team = gamePlayer.getArenaTeam();
                  if (team != null) {
                     gamePlayer.removeCoins(price);
                     team.setCores(team.getCores() + coresToAdd);

                     p.sendMessage(Statics.c("&a¡Compraste " + coresToAdd + " destrucciones para tu equipo!"));
                     p.sendMessage(Statics.c("&7Total de cores: &e" + team.getCores()));

                     Bukkit.broadcastMessage(Statics.c(String.format("&6%s &ecompró &a%d destrucciones &epara el equipo %s%s",
                             p.getName(), coresToAdd, team.getColor().getChatColor(), team.getColor().getName())));

                     LogSystem.info(LogCategory.GAME, "Compra de cores:", p.getName(), "compró", coresToAdd + " cores", "para", team.getColor().getName(), "por", price + " coins");

                     for (GamePlayer teamPlayer : team.getTeamPlayers()) {
                        if (teamPlayer.getPlayer() != null) {
                           teamPlayer.createPBoard();
                        }
                     }
                  } else {
                     p.sendMessage(Statics.c("&cNo tienes equipo asignado."));
                  }
               } else {
                  p.sendMessage(Statics.c("&cNo tienes suficientes coins. Necesitas: &6" + price + " &c(Tienes: &6" + balance + "&c)"));
               }
            } catch (NumberFormatException e) {
               p.sendMessage(Statics.c("&cError: Precio o cantidad inválidos"));
               LogSystem.error(LogCategory.GAME, "Formato addcores inválido:", kit);
            }
         } else if (cmd.startsWith("addtime:")) {
            kit = cmd.substring(8);
            if (kit.startsWith(" ")) {
               kit = kit.substring(1);
            }

            String[] parts = kit.split(":");
            if (parts.length != 2) {
               p.sendMessage(Statics.c("&cError: Formato incorrecto de comando addtime"));
               return;
            }

            try {
               int originalPrice = Integer.parseInt(parts[0]);
               int secondsToAdd = Integer.parseInt(parts[1]);
               int discountPercent = VIPDiscountHelper.getDiscountPercentage(p);
               int price = VIPDiscountHelper.calculateDiscountedPrice(originalPrice, discountPercent);
               int balance = gamePlayer.getCoins();

               if (balance >= price) {
                  ArenaTask arenaTask = ArenaTask.getInstance();
                  if (arenaTask != null) {
                     gamePlayer.removeCoins(price);
                     arenaTask.addTime(secondsToAdd);

                     int minutes = secondsToAdd / 60;
                     p.sendMessage(Statics.c("&a¡Compraste " + minutes + " minutos de tiempo extra!"));

                     Bukkit.broadcastMessage(Statics.c(String.format("&6%s &ecompró &a%d minutos &ede tiempo extra",
                             p.getName(), minutes)));

                     LogSystem.info(LogCategory.GAME, "Compra de tiempo:", p.getName(), "compró", secondsToAdd + " segundos", "por", price + " coins");
                  } else {
                     p.sendMessage(Statics.c("&cNo hay partida en curso."));
                  }
               } else {
                  p.sendMessage(Statics.c("&cNo tienes suficientes coins. Necesitas: &6" + price + " &c(Tienes: &6" + balance + "&c)"));
               }
            } catch (NumberFormatException e) {
               p.sendMessage(Statics.c("&cError: Precio o tiempo inválidos"));
               LogSystem.error(LogCategory.GAME, "Formato addtime inválido:", kit);
            }
         } else {
            ItemStack itemStack;
            int originalPrice;
            int price;
            int balance;
            int discountPercent;
            if (cmd.startsWith("buy:")) {
               kit = cmd.substring(4);
               if (kit.startsWith(" ")) {
                  kit = kit.substring(1);
               }

               originalPrice = Integer.parseInt(kit);
               discountPercent = VIPDiscountHelper.getDiscountPercentage(p);
               price = VIPDiscountHelper.calculateDiscountedPrice(originalPrice, discountPercent);
               balance = gamePlayer.getCoins();
               if (balance >= price) {
                  gamePlayer.removeCoins(price);
                  itemStack = ordItems.getIcon().build(p);
                  if (itemStack.hasItemMeta()) {
                     ItemMeta im = itemStack.getItemMeta();
                     if (im.hasLore()) {
                        im.setLore((List)null);
                     }

                     if (im.hasDisplayName()) {
                        im.setDisplayName((String)null);
                     }

                     itemStack.setItemMeta(im);
                  }

                  p.getInventory().addItem(new ItemStack[]{itemStack});
               } else {
                  p.sendMessage(Statics.c("&cNo tienes suficientes coins. Necesitas: &6" + price + " &c(Tienes: &6" + balance + "&c)"));
               }
            } else if (cmd.startsWith("enchant:")) {
               kit = cmd.substring(8);
               if (kit.startsWith(" ")) {
                  kit = kit.substring(1);
               }

               originalPrice = Integer.parseInt(kit);
               discountPercent = VIPDiscountHelper.getDiscountPercentage(p);
               price = VIPDiscountHelper.calculateDiscountedPrice(originalPrice, discountPercent);
               balance = gamePlayer.getCoins();

               itemStack = p.getItemInHand();

               if (itemStack == null || itemStack.getType() == Material.AIR) {
                  p.sendMessage(Statics.c("&cDebes tener un item en la mano para encantarlo."));
                  return;
               }

               Map<Enchantment, Integer> enchants = ordItems.getIcon().getEnchantments();

               for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                  Enchantment ench = entry.getKey();
                  if (!EnchantmentValidator.canEnchant(itemStack, ench)) {
                     String requiredType = EnchantmentValidator.getRequiredItemType(ench);
                     p.sendMessage(Statics.c("&cEste encantamiento solo funciona en: &e" + requiredType));
                     return;
                  }

                  int currentLevel = itemStack.getEnchantmentLevel(ench);
                  if (currentLevel >= ench.getMaxLevel()) {
                     p.sendMessage(Statics.c("&cEste item ya tiene el nivel máximo de este encantamiento. &7(Nivel " + ench.getMaxLevel() + ")"));
                     return;
                  }
               }

               if (balance >= price) {
                  gamePlayer.removeCoins(price);
                  LogSystem.debug(LogCategory.GAME, "Encantamientos a agregar:", enchants.toString());

                  for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                     Enchantment ench = entry.getKey();
                     Integer level = entry.getValue();
                     LogSystem.debug(LogCategory.GAME, "Agregando encantamiento:", ench.toString(), "nivel", level.toString());

                     if (itemStack.containsEnchantment(ench)) {
                        int currentLevel = itemStack.getEnchantmentLevel(ench);
                        int newLevel = level + currentLevel;
                        if (newLevel > ench.getMaxLevel()) {
                           newLevel = ench.getMaxLevel();
                        }
                        if (newLevel > currentLevel) {
                           itemStack.addUnsafeEnchantment(ench, newLevel);
                        }
                     } else {
                        int newLevel = level;
                        if (newLevel > ench.getMaxLevel()) {
                           newLevel = ench.getMaxLevel();
                        }
                        itemStack.addUnsafeEnchantment(ench, newLevel);
                     }
                  }

                  p.updateInventory();
                  p.sendMessage(Statics.c("&a¡Item encantado correctamente!"));
               } else {
                  p.sendMessage(Statics.c("&cNo tienes suficientes coins. Necesitas: &6" + price + " &c(Tienes: &6" + balance + "&c)"));
               }
            } else if (cmd.startsWith("kit:")) {
               kit = cmd.substring(4);
               if (kit.startsWith(" ")) {
                  kit = kit.substring(1);
               }

               originalPrice = Integer.parseInt(kit);
               discountPercent = VIPDiscountHelper.getDiscountPercentage(p);
               price = VIPDiscountHelper.calculateDiscountedPrice(originalPrice, discountPercent);
               balance = gamePlayer.getCoins();
               if (balance >= price) {
                  if (ordItems.getMaterialList() != null) {
                     gamePlayer.removeCoins(price);
                     ordItems.getMaterialList().forEach((mat) -> {
                        p.getInventory().addItem(new ItemStack[]{new ItemStack(Material.valueOf(mat.toUpperCase()))});
                     });
                  } else {
                     LogSystem.error(LogCategory.GAME, "Kit null en menú");
                  }
               } else {
                  p.sendMessage(Statics.c("&cNo tienes suficientes coins. Necesitas: &6" + price + " &c(Tienes: &6" + balance + "&c)"));
               }
            }
         }
      }
   }

   @EventHandler(
           priority = EventPriority.HIGH,
           ignoreCancelled = true
   )
   public void handleCommandEvent(PlayerCommandPreprocessEvent e) {
      String cmd = e.getMessage();
      cmd = cmd.substring(1);
      if (cmd.length() != 0) {
         String file = (String)this.main.getMenuManager().getMenuCommand().get(cmd.toLowerCase());
         if (file != null) {
            e.setCancelled(true);
            Player p = e.getPlayer();
            this.main.getMenuManager().openInventory(file, this.main.getGameManager().getPlayerManager().getPlayer(p.getUniqueId()));
         }
      }
   }
}