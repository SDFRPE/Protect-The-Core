package sdfrpe.github.io.ptc.Listeners;

import com.google.common.collect.Lists;
import sdfrpe.github.io.ptc.Listeners.Game.*;
import sdfrpe.github.io.ptc.Listeners.General.*;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Commands.Commands;
import sdfrpe.github.io.ptc.Commands.cmds.*;
import sdfrpe.github.io.ptc.Listeners.Lobby.LobbyListener;
import sdfrpe.github.io.ptc.Listeners.Lobby.LobbyModeListener;
import sdfrpe.github.io.ptc.Listeners.Setup.SetupListener;
import sdfrpe.github.io.ptc.Listeners.Vanilla.PlayerListener;
import sdfrpe.github.io.ptc.Utils.Console;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class ListenerManager {
    private final PTC plugin;
    private final List<Listener> listeners;

    public ListenerManager(PTC plugin) {
        this.plugin = plugin;
        this.listeners = Lists.newArrayList();
    }

    public void init() {
        Commands commandExecutor = new Commands(PTC.getInstance());

        this.registerCommands();

        if (this.plugin.getGameManager().getGlobalSettings().isLobbyMode()) {
            Console.log("§eRegistering Lobby Mode listeners...");
            this.register(new LobbyModeListener());
            this.register(new PlayerListener());
            this.register(new DBLoadListener());
            this.register(new PlayerCleanupListener());
            this.register(new TNTDamageListener());
            this.register(new onMenu());
            this.register(new PlaytimeListener());

            PTC.getInstance().getCommand("ptc").setExecutor(commandExecutor);
            PTC.getInstance().getCommand("admin").setExecutor(commandExecutor);

            Console.log("§aLobby Mode listeners registered (Menu + Data loading only).");
            Console.log("§aPlaytimeListener registered - Playtime tracking ACTIVE.");
            Console.log("§eScoreboard, Chat, and TAB modifications DISABLED in Lobby Mode.");
            return;
        }

        if (this.plugin.getGameManager().getGlobalSettings().isConfiguring()) {
            this.register(new SetupListener());
            PTC.getInstance().getCommand("ptc").setExecutor(commandExecutor);
            PTC.getInstance().getCommand("admin").setExecutor(commandExecutor);
        } else {
            PTC.getInstance().getCommand("ptc").setExecutor(commandExecutor);
            PTC.getInstance().getCommand("admin").setExecutor(commandExecutor);

            this.register(
                    new ChatListener(),
                    new PlayerListener(),
                    new DBLoadListener(),
                    new LobbyListener(),
                    new onMenu(),
                    new LobbyItemsProtectionListener(),
                    new ExperienceListener(),
                    new ClanWarIntegrationListener(),
                    new SpectatorProtectionListener(),
                    new BossBarCleanupListener(),
                    new BossBarWorldChangeListener(),
                    new PlayerCleanupListener(),
                    new TNTDamageListener(),
                    new BossBarRespawnListener(),
                    new PlaytimeListener(),
                    this.plugin.getGameManager().getVoteMenu(),
                    this.plugin.getGameManager().getTeamSelectorMenu()
            );

            if (this.plugin.getGameManager().getGlobalSettings().isModeCW()) {
                this.register(new ClanWarAccessListener());
                Console.log("§aClanWarAccessListener registered - Restricción de acceso activa.");
            }

            Console.log("§aChatListener registered - Chat system enabled for LOBBY and ARENA.");
            Console.log("§aExperienceListener registered - XP sync active from server start.");
            Console.log("§aClanWarIntegrationListener registered - CW mode support enabled.");
            Console.log("§aSpectatorProtectionListener registered - Spectator protection ACTIVE.");
            Console.log("§aBossBarCleanupListener registered - BossBar cleanup on quit.");
            Console.log("§aBossBarWorldChangeListener registered - BossBar persistence across worlds.");
            Console.log("§aTNTDamageListener registered - TNT damage normalization ACTIVE.");
            Console.log("§aBossBarRespawnListener registered - BossBar respawn restoration ACTIVE.");
            Console.log("§aPlaytimeListener registered - Playtime tracking ACTIVE.");
        }
    }

    private void registerCommands() {
        Console.log("§eRegistering commands...");

        PTC.getInstance().getCommand("coins").setExecutor(new CoinsCommand(plugin));
        PTC.getInstance().getCommand("msg").setExecutor(new MsgCommand(plugin));
        PTC.getInstance().getCommand("feed").setExecutor(new FeedCommand(plugin));
        PTC.getInstance().getCommand("tienda").setExecutor(new TiendaCommand(plugin));
        PTC.getInstance().getCommand("encantar").setExecutor(new EncantarCommand(plugin));
        PTC.getInstance().getCommand("mochila").setExecutor(new MochilaCommand(plugin));
        PTC.getInstance().getCommand("mesa").setExecutor(new MesaCommand(plugin));
        PTC.getInstance().getCommand("kill").setExecutor(new KillCommand(plugin));
        PTC.getInstance().getCommand("salir").setExecutor(new SalirCommand(plugin));
        PTC.getInstance().getCommand("team").setExecutor(new TeamCommand(plugin));
        PTC.getInstance().getCommand("multiplier").setExecutor(new MultiplierCommand(plugin));
        PTC.getInstance().getCommand("playtime").setExecutor(new PlaytimeCommand(plugin));

        Console.log("§aAll commands registered successfully:");
        Console.log("  §7- /coins (money, balance, bal)");
        Console.log("  §7- /msg (message, whisper, tell, w)");
        Console.log("  §7- /feed (comida) §6[VIP]");
        Console.log("  §7- /tienda (shop) §6[VIP]");
        Console.log("  §7- /encantar (enchant, encantamientos) §6[VIP]");
        Console.log("  §7- /mochila (echest, enderchest) §6[VIP]");
        Console.log("  §7- /mesa (craft, workbench)");
        Console.log("  §7- /kill (suicide, suicidio)");
        Console.log("  §7- /salir (leave, lobby, hub)");
        Console.log("  §7- /team (equipo, teams) §c[ADMIN]");
        Console.log("  §7- /multiplier (multi, mult) §c[ADMIN]");
        Console.log("  §7- /playtime (tiempo, jugado)");
    }

    public void register(Listener... listeners) {
        Listener[] var2 = listeners;
        int var3 = listeners.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            Listener listener = var2[var4];
            Console.debug(String.format("Loading %s", listener.getClass().getSimpleName()));
            this.listeners.add(listener);
            Bukkit.getPluginManager().registerEvents(listener, this.plugin);
        }
    }

    public void unregister(Class<?> className) {
        Console.debug(String.format("Unloading %s", className.getSimpleName()));
        this.listeners.stream().filter((listener) -> {
            return listener.getClass().getName().equals(className.getName());
        }).forEachOrdered(HandlerList::unregisterAll);
    }
}