package sdfrpe.github.io.ptc.Utils.Menu;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Console;
import sdfrpe.github.io.ptc.Utils.Menu.utils.Icon;
import sdfrpe.github.io.ptc.Utils.Menu.utils.listEnchants;
import sdfrpe.github.io.ptc.Utils.Menu.utils.ordItems;
import com.cryptomorin.xseries.XMaterial;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

public class IMenuManager {
   private PTC main;
   private HashMap<String, IMenu> menuFileName;
   private HashMap<String, String> menuCommand;

   public IMenuManager(PTC main) {
      this.main = main;
      this.menuFileName = new HashMap();
      this.menuCommand = new HashMap();
   }

   public void loadFiles() {
      File menuFolder = new File(this.main.getDataFolder(), "Menu");
      if (menuFolder.mkdir()) {
         Console.log("Created Menu folder.");
      }

      File file = new File(this.main.getDataFolder(), "Menu");
      File[] files = file.listFiles();
      Bukkit.getScheduler().runTaskAsynchronously(this.main, () -> {
         if (files != null && files.length > 0) {
            File[] var2 = files;
            int var3 = files.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               File menuFile = var2[var4];
               if (menuFile != null) {
                  String fileName = menuFile.getName();
                  if (fileName.endsWith(".yml")) {
                     this.loadMenus(menuFile);
                  }
               }
            }
         }

         Console.info("&eLoaded menus: &a" + this.getMenuFileName().size());
      });
   }

   public void reloadMenu() {
      this.menuFileName.clear();
      this.menuCommand.clear();
      this.loadFiles();
   }

   private void loadMenus(File menuFile) {
      String fileName = menuFile.getName();
      FileConfiguration config = YamlConfiguration.loadConfiguration(menuFile);
      Set<String> menuNodes = config.getKeys(false);
      String menuName = config.getString("menu-settings.name");
      int menuRows = config.getInt("menu-settings.rows");
      String menuCommand = config.getString("menu-settings.command");

      final Material materialview;
      final short dataView;
      final List<String> loreView;
      final boolean enableIcon;

      if (config.getString("menu-settings.permissionView.ICON-ITEM") != null) {
         materialview = config.isInt("menu-settings.permissionView.ICON-ITEM") ? Material.getMaterial(config.getInt("menu-settings.permissionView.ICON-ITEM")) : Material.getMaterial(config.getString("menu-settings.permissionView.ICON-ITEM"));
         dataView = Short.parseShort(config.getString("menu-settings.permissionView.DATA-VALUE"));
         loreView = config.getStringList("menu-settings.permissionView.DESCRIPTION");
         enableIcon = config.getBoolean("menu-settings.permissionView.ICON-VIEW");
      } else {
         materialview = null;
         dataView = 0;
         loreView = null;
         enableIcon = false;
      }

      if (menuName != null && !menuName.isEmpty()) {
         if (menuRows == 0) {
            Console.error("The menu§2 " + fileName + " §ecan't load: It was not found 'row', check your menu-settings");
         } else {
            menuName = "§r" + menuName.replace("&", "§");
            if (menuName.length() > 48) {
               menuName = "§rError, name too long!";
            }

            HashMap<Integer, ordItems> items = new HashMap();
            menuNodes.forEach((s) -> {
               ConfigurationSection confSection = config.getConfigurationSection(s);
               if (!s.equals("menu-settings")) {
                  String command = confSection.getString("ACTION");
                  String name = confSection.getString("NAME");
                  String permission = confSection.getString("PERMISSION");
                  List<String> description = confSection.getStringList("DESCRIPTION");
                  List<String> materialList = confSection.getStringList("KIT");
                  String skullplayer = confSection.getString("SKULL-OWNER");
                  int amount = confSection.getInt("ICON-AMOUNT");
                  short data = (short)confSection.getInt("DATA-VALUE");
                  int slotX = confSection.getInt("POSITION-X");
                  int slotY = confSection.getInt("POSITION-Y");
                  List<String> enchants = confSection.getStringList("ENCHANTMENTS");
                  HashMap<Enchantment, Integer> enchantments = new HashMap();
                  if (!enchants.isEmpty()) {
                     Iterator var21 = enchants.iterator();

                     while(var21.hasNext()) {
                        String enchanter = (String)var21.next();
                        String[] enchanting = enchanter.split(":");
                        enchantments.put(listEnchants.getEnchantmentFromString(enchanting[0]), Integer.parseInt(enchanting[1]));
                     }
                  }

                  short durability = (short)confSection.getInt("DURABILITY");
                  Material material = confSection.isInt("ICON-ITEM") ? Material.getMaterial(confSection.getInt("ICON-ITEM")) : Material.getMaterial(confSection.getString("ICON-ITEM"));
                  Icon icon = (new Icon(XMaterial.matchXMaterial(material), amount, data)).setName(name).setLore(description).setSkull(skullplayer).addDamage(durability).addEnchantment(enchantments).addPermissionView(enableIcon, permission, materialview, dataView, loreView);
                  items.put(this.getRelativePosition(slotX, slotY), new ordItems(icon, materialList, this.getRelativePosition(slotX, slotY), command, permission));
               }
            });
            IMenu menu = new IMenu(menuName, menuRows * 9, menuCommand, items);
            this.menuFileName.put(fileName, menu);
            if (!menuCommand.isEmpty()) {
               this.menuCommand.put(menuCommand.toLowerCase(), fileName);
            }
         }
      } else {
         Console.error("The menu§2 " + fileName + " §ecan't load: It was not found 'name', check your menu-settings", menuName);
      }
   }

   public void openInventory(String inv, GamePlayer gamePlayer) {
      if (this.menuFileName.containsKey(inv)) {
         ((IMenu)this.menuFileName.get(inv)).open(gamePlayer);
      }
   }

   public int getRelativePosition(int x, int y) {
      --x;
      --y;
      if (x < 0) {
         x = 0;
      }

      if (y < 0) {
         y = 0;
      }

      int r = y * 9 + x;
      if (r < 0) {
         r = 0;
      }

      return r;
   }

   public PTC getMain() {
      return this.main;
   }

   public HashMap<String, IMenu> getMenuFileName() {
      return this.menuFileName;
   }

   public HashMap<String, String> getMenuCommand() {
      return this.menuCommand;
   }

   public void setMain(PTC main) {
      this.main = main;
   }

   public void setMenuFileName(HashMap<String, IMenu> menuFileName) {
      this.menuFileName = menuFileName;
   }

   public void setMenuCommand(HashMap<String, String> menuCommand) {
      this.menuCommand = menuCommand;
   }
}