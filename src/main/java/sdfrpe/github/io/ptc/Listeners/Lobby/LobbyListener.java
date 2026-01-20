package sdfrpe.github.io.ptc.Listeners.Lobby;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Events.Player.ItemInteractEvent;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Statics;
import sdfrpe.github.io.ptc.Utils.Enums.Action;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.Inventories.GInventory;
import sdfrpe.github.io.ptc.Utils.Inventories.GItem;
import sdfrpe.github.io.ptc.Utils.Managers.BarAPI;
import sdfrpe.github.io.ptc.Utils.Managers.GlobalTabManager;
import sdfrpe.github.io.ptc.Utils.Managers.TitleAPI;

import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LobbyListener implements Listener {
    @EventHandler
    public void cancelDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerJoin(PlayerJoinEvent e) {
        e.setJoinMessage((String) null);
        Player player = e.getPlayer();
        PTC.getInstance().getInventories().setLobby(player);

        if (PTC.getInstance().getGameManager().getGlobalSettings().getLobbyLocation() != null) {
            player.teleport(PTC.getInstance().getGameManager().getGlobalSettings().getLobbyLocation().getLocation());
        } else {
            LogSystem.error(LogCategory.CORE, "Lobby location no configurada - Usa /ptc setLobby");
            player.sendMessage(Statics.c("&cLa ubicación del lobby no está configurada. Por favor, contacta con un administrador."));
        }

        GlobalTabManager.getInstance().applyToPlayer(player);
        GlobalTabManager.getInstance().addPlayerToTeam(player, TeamColor.LOBBY);

        PTC.getInstance().getGameManager().getScoreManager().addLobby(player);

        BarAPI.broadcastActionbar(Statics.c(String.format("&b%s &ase ha unido.", e.getPlayer().getName())));

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(player.getUniqueId());
            if (gamePlayer != null) {
                gamePlayer.updateTabList();
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerQuit(PlayerQuitEvent e) {
        e.setQuitMessage((String) null);
        BarAPI.broadcastActionbar(Statics.c(String.format("&b%s &cse ha ido.", e.getPlayer().getName())));
    }

    @EventHandler
    public void playerDropItem(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void playerFoodLevel(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            player.setFoodLevel(20);
            player.setExhaustion(20.0F);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void itemInteract(ItemInteractEvent e) {
        Player player = e.getPlayer();
        e.setCancelled(true);
        boolean doAction = e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK);
        if (doAction) {
            Iterator var4 = PTC.getInstance().getInventories().getInventoryList().entrySet().iterator();

            while (var4.hasNext()) {
                Entry<String, GInventory> entry = (Entry) var4.next();
                GInventory gInventory = (GInventory) entry.getValue();
                GItem gItem = gInventory.findItem(e.getItemStack());
                if (gItem != null) {
                    if (gItem.getAction().equalsIgnoreCase("none") && gItem.getAction().toLowerCase().contains("none")) {
                        break;
                    }

                    String[] args = gItem.getAction().split(":", 2);
                    if (args.length < 2) {
                        return;
                    }

                    String action = args[0];
                    String value = args[1];
                    this.doAction(e.getPlayer(), action, value);
                    break;
                }
            }
        }
    }

    private void doAction(Player p, String action, String value) {
        if (action.equalsIgnoreCase("voteMenu")) {
            PTC.getInstance().getGameManager().getVoteMenu().open(p);
        } else if (action.equalsIgnoreCase("teamsMenu")) {
            if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
                p.sendMessage(ChatColor.RED + "═══════════════════════════════════");
                p.sendMessage(ChatColor.YELLOW + "  MODO GUERRA DE CLANES");
                p.sendMessage(ChatColor.GRAY + "");
                p.sendMessage(ChatColor.WHITE + "Los equipos se asignan");
                p.sendMessage(ChatColor.WHITE + "automáticamente según tu clan.");
                p.sendMessage(ChatColor.GRAY + "");
                p.sendMessage(ChatColor.RED + "═══════════════════════════════════");
                LogSystem.debug(LogCategory.TEAM, "Jugador intentó abrir menú de equipos en modo CW:", p.getName());
                return;
            }
            PTC.getInstance().getGameManager().getTeamSelectorMenu().open(p);
        } else if (action.equalsIgnoreCase("extrasMenu")) {
            if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
                p.sendMessage(ChatColor.RED + "═══════════════════════════════════");
                p.sendMessage(ChatColor.YELLOW + "  MODO GUERRA DE CLANES");
                p.sendMessage(ChatColor.GRAY + "");
                p.sendMessage(ChatColor.WHITE + "El tiempo y vida están");
                p.sendMessage(ChatColor.WHITE + "predefinidos para las guerras.");
                p.sendMessage(ChatColor.GRAY + "");
                p.sendMessage(ChatColor.RED + "═══════════════════════════════════");
                LogSystem.debug(LogCategory.GAME, "Jugador intentó abrir menú extras en modo CW:", p.getName());
                return;
            }
            PTC.getInstance().getGameManager().getExtrasMenu().open(p);
        } else if (action.equalsIgnoreCase("statisticsMenu")) {
            PTC.getInstance().getGameManager().getStatisticsMenu().open(p);
        } else if (action.equalsIgnoreCase("leave")) {
            String lobbyServerName = PTC.getInstance().getGameManager().getGlobalSettings().getLobbyServerName();

            if (lobbyServerName == null || lobbyServerName.isEmpty()) {
                p.sendMessage(ChatColor.RED + "El servidor de lobby no está configurado.");
                return;
            }

            try {
                p.sendMessage(ChatColor.YELLOW + "Enviándote al lobby...");

                new TitleAPI()
                        .title(ChatColor.GREEN + "Conectando...")
                        .subTitle(ChatColor.YELLOW + "Enviándote al lobby")
                        .fadeInTime(10)
                        .showTime(40)
                        .fadeOutTime(10)
                        .send(p);

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(lobbyServerName);

                p.sendPluginMessage(PTC.getInstance(), "BungeeCord", out.toByteArray());

                LogSystem.info(LogCategory.NETWORK, "Jugador enviado a lobby:", p.getName());
            } catch (Exception ex) {
                LogSystem.error(LogCategory.NETWORK, "Error enviando a lobby:", p.getName(), ex.getMessage());
                p.sendMessage(ChatColor.RED + "Error al intentar conectarte al lobby.");
            }
        }
    }
}