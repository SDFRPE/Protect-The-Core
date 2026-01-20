package sdfrpe.github.io.ptc.Utils.Menu.Vanilla;

import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Items.ItemBuilder;
import sdfrpe.github.io.ptc.Utils.Managers.GlobalTabManager;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;
import sdfrpe.github.io.ptc.Utils.PlayerTabUpdater;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class TeamSelectorMenu implements Listener {
   private final GameManager gameManager;
   private Inventory teamsInventory;

   public TeamSelectorMenu(GameManager gameManager) {
      this.gameManager = gameManager;
   }

   public void open(Player player) {
      player.openInventory(this.teamsInventory);
   }

   public void updateTeams() {
      this.teamsInventory = Bukkit.createInventory((InventoryHolder)null, InventoryType.HOPPER, "Elegir equipo");
      this.teamsInventory.clear();
      this.teamsInventory.addItem(new ItemStack[]{
              ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.RED, TeamColor.RED.getColoredName()),
              ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.BLUE, TeamColor.BLUE.getColoredName()),
              ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.GREEN, TeamColor.GREEN.getColoredName()),
              ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.ORANGE, TeamColor.YELLOW.getColoredName()),
              ItemBuilder.createArmor(Material.LEATHER_HELMET, Color.SILVER, TeamColor.SPECTATOR.getColoredName())
      });
   }

   @EventHandler
   public void inventoryClick(InventoryClickEvent e) {
      if (e.getInventory().getName().equals(this.teamsInventory.getName())) {
         e.setCancelled(true);

         if (e.getWhoClicked() instanceof Player) {
            if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
               if (e.getCurrentItem().getItemMeta().hasDisplayName()) {
                  Player player = (Player)e.getWhoClicked();
                  GamePlayer gamePlayer = this.gameManager.getPlayerManager().getPlayer(player.getUniqueId());

                  if (gamePlayer == null) {
                     LogSystem.error(LogCategory.PLAYER, "GamePlayer null para:", player.getName());
                     return;
                  }

                  ItemStack itemStack = e.getCurrentItem();
                  String teamName = itemStack.getItemMeta().getDisplayName();
                  TeamColor[] var6 = TeamColor.values();
                  int var7 = var6.length;

                  for(int var8 = 0; var8 < var7; ++var8) {
                     TeamColor value = var6[var8];

                     if (value.getColoredName().equalsIgnoreCase(teamName)) {
                        if (gamePlayer.getArenaTeam() != null) {
                           ArenaTeam oldTeam = gamePlayer.getArenaTeam();
                           oldTeam.getTeamPlayers().remove(gamePlayer);
                           LogSystem.debug(LogCategory.TEAM, "Removido", player.getName(), "de equipo", oldTeam.getColor().getName());
                        }

                        ArenaTeam arenaTeam = PTC.getInstance().getGameManager().getGameSettings().getTeamList().get(value);

                        if (arenaTeam == null) {
                           LogSystem.error(LogCategory.TEAM, "ArenaTeam null para color:", value.getName());
                           return;
                        }

                        arenaTeam.addPlayer(gamePlayer, true);

                        GlobalTabManager.getInstance().addPlayerToTeam(player, value);
                        giveTeamHelmet(player, value);

                        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
                           PlayerTabUpdater.updateAllPlayerTabs();
                        }, 2L);

                        new TitleAPI()
                                .showTime(60)
                                .title(arenaTeam.getColor().getColoredName())
                                .subTitle(String.format("%sCompañeros: %s", arenaTeam.getColor().getChatColor(), arenaTeam.countPlayers()))
                                .send(player);

                        LogSystem.debug(LogCategory.TEAM, "Jugador", player.getName(), "seleccionó equipo", value.getName());
                        break;
                     }
                  }

                  player.closeInventory();
               }
            }
         }
      }
   }

   private void giveTeamHelmet(Player player, TeamColor teamColor) {
      try {
         Color color = null;
         switch (teamColor) {
            case RED:
               color = Color.RED;
               break;
            case BLUE:
               color = Color.BLUE;
               break;
            case GREEN:
               color = Color.GREEN;
               break;
            case YELLOW:
               color = Color.ORANGE;
               break;
            case SPECTATOR:
               color = Color.GRAY;
               break;
            default:
               LogSystem.warn(LogCategory.TEAM, "Color de equipo no soportado:", teamColor.getName());
               return;
         }

         if (color != null) {
            ItemStack helmetInSlot = player.getInventory().getItem(1);

            if (helmetInSlot != null && helmetInSlot.getType() == Material.LEATHER_HELMET) {
               org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmetInSlot.getItemMeta();
               String originalName = meta.getDisplayName();
               meta.setColor(color);
               meta.setDisplayName(originalName);
               helmetInSlot.setItemMeta(meta);
            }

            ItemStack helmetForHead = ItemBuilder.createArmor(Material.LEATHER_HELMET, color);
            player.getInventory().setHelmet(helmetForHead);

            player.updateInventory();
            LogSystem.debug(LogCategory.TEAM, "Casco de", teamColor.getName(), "dado a", player.getName());
         }

      } catch (Exception e) {
         LogSystem.error(LogCategory.PLAYER, "Error dando casco:", player.getName(), e.getMessage());
      }
   }
}