package sdfrpe.github.io.ptc.Utils.Inventories;

import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerUtils;
import sdfrpe.github.io.ptc.Utils.Interfaces.Callback;
import sdfrpe.github.io.ptc.Utils.Items.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Inventories {
   private PTC ptc;
   private Map<String, GInventory> inventoryList;

   public Inventories(PTC ptc) {
      this.ptc = ptc;
      this.inventoryList = Maps.newHashMap();
   }

   public void loadInventories() {
      ConfigurationSection lobby = this.ptc.getConfig("Settings").getConfigurationSection("Lobby");
      Set<String> lobbyKeys = lobby.getKeys(false);
      GInventory lobbyInv = new GInventory("Lobby");
      this.findKeys(lobbyKeys, lobby, gItem -> lobbyInv.getItems().add(gItem));
      this.inventoryList.put("Lobby", lobbyInv);
   }

   public void setLobby(Player player) {
      PlayerUtils.clean(player);
      GInventory inventory = this.inventoryList.getOrDefault("Lobby", null);
      if (inventory != null) {
         inventory.build(player);
      }
   }

   public void setInGame(Player player) {
      GamePlayer gamePlayer = this.ptc.getGameManager().getPlayerManager().getPlayer(player.getUniqueId());

      if (gamePlayer == null || gamePlayer.getArenaTeam() == null) {
         return;
      }

      PlayerUtils.clean(player);
      player.setGameMode(GameMode.SURVIVAL);

      gamePlayer.getArenaTeam().setInventory(gamePlayer);
   }

   public void giveSetup(Player player, String name) {
      PlayerUtils.clean(player);
      player.setGameMode(GameMode.CREATIVE);
      ItemStack yellow_spawn = ItemBuilder.createItem(XMaterial.YELLOW_WOOL, 1, String.format("&fArena :&a%s", name));
      ItemStack blue_spawn = ItemBuilder.createItem(XMaterial.BLUE_WOOL, 1, String.format("&fArena :&a%s", name));
      ItemStack red_spawn = ItemBuilder.createItem(XMaterial.RED_WOOL, 1, String.format("&fArena :&a%s", name));
      ItemStack green_spawn = ItemBuilder.createItem(XMaterial.LIME_WOOL, 1, String.format("&fArena :&a%s", name));
      ItemStack pigs_spawn = ItemBuilder.createItem(XMaterial.PINK_STAINED_GLASS, 1, String.format("&fPigs Spawn :&a%s", name));
      ItemStack center_spawn = ItemBuilder.createItem(XMaterial.BEACON, 1, String.format("&fCenter Spawn :&a%s", name));
      ItemStack time_config = ItemBuilder.createItem(XMaterial.CLOCK, 1, String.format("&fTime :&a%s", name), "&7Click derecho para cambiar", "&7Tiempo actual: &eDía");
      ItemStack health_config = ItemBuilder.createItem(XMaterial.GOLDEN_APPLE, 1, String.format("&fHealth :&a%s", name), "&7Click derecho para cambiar", "&7Corazones extra: &c0");
      ItemStack saveArena = ItemBuilder.createItem(XMaterial.PAPER, 1, String.format("&fSave Arena :&a%s", name));
      player.getInventory().addItem(new ItemStack[]{yellow_spawn, blue_spawn, red_spawn, green_spawn, pigs_spawn, center_spawn, time_config, health_config, saveArena});
   }

   public void findKeys(Set<String> keys, ConfigurationSection config, Callback<GItem> callback) {
      if (!keys.isEmpty()) {
         keys.forEach(item -> {
            String[] id = config.getString(item + ".item", "1:0").split(":");
            int slot = config.getInt(item + ".slot", 0);
            String name = config.getString(item + ".name", "No Name.");
            List<String> lore = config.getStringList(item + ".lore");
            String action = config.getString(item + ".action", "none");
            String need = config.getString(item + ".need", "none");
            Material material = Material.valueOf(id[0].toUpperCase());
            byte byteA = Byte.parseByte(id[1]);
            ItemStack stack = new ItemStack(material, 1, (short)byteA);
            ItemStack itemStack = ItemBuilder.createItem(stack, 1, name, lore.toArray(new String[0]));
            GItem gItem = new GItem(itemStack, slot, action, need);
            callback.done(gItem);
         });
      }
   }

   public PTC getPtc() {
      return this.ptc;
   }

   public Map<String, GInventory> getInventoryList() {
      return this.inventoryList;
   }
}