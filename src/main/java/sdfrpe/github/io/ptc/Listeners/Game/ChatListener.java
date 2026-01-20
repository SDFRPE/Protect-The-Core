package sdfrpe.github.io.ptc.Listeners.Game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Arena.ArenaTeam;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.ClanChatUtils;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Enums.GameStatus;
import sdfrpe.github.io.ptc.Utils.Enums.TeamColor;
import sdfrpe.github.io.ptc.Utils.RankUtils;
import sdfrpe.github.io.ptc.Utils.Statics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChatListener implements Listener {

    private static final Map<UUID, UUID> lastMessages = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();

        GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(sender.getUniqueId());

        if (gamePlayer == null) {
            return;
        }

        if (Statics.gameStatus == GameStatus.LOBBY || Statics.gameStatus == GameStatus.STARTING) {
            handleLobbyChat(event, sender, message, gamePlayer);
        } else {
            handleArenaChat(event, sender, message, gamePlayer);
        }
    }

    private void handleLobbyChat(AsyncPlayerChatEvent event, Player sender, String message, GamePlayer gamePlayer) {
        event.setCancelled(true);

        if (message.startsWith("!")) {
            sender.sendMessage(ChatColor.RED + "El chat global solo está disponible durante la partida.");
            sender.sendMessage(ChatColor.GRAY + "Usa el chat normal para hablar con todos en el lobby.");
            return;
        }

        ChatColor playerColor = ChatColor.AQUA;
        String teamName = "";

        if (gamePlayer.getArenaTeam() != null) {
            TeamColor teamColor = gamePlayer.getArenaTeam().getColor();

            if (teamColor != TeamColor.LOBBY && teamColor != TeamColor.SPECTATOR) {
                playerColor = teamColor.getChatColor();
                teamName = "<" + capitalizeFirst(teamColor.getName()) + "> ";
            }
        }

        String prefix = RankUtils.getChatPrefix(sender);
        String clanDisplay = ClanChatUtils.getClanDisplayForChat(sender);

        String formattedMessage = playerColor + teamName + prefix + clanDisplay + playerColor + sender.getName() + ChatColor.WHITE + ": " + message;

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(formattedMessage);
        }

        LogSystem.debug(LogCategory.PLAYER, "Chat lobby:", sender.getName());
    }

    private void handleArenaChat(AsyncPlayerChatEvent event, Player sender, String message, GamePlayer gamePlayer) {
        if (gamePlayer.getArenaTeam() == null) {
            sender.sendMessage(ChatColor.RED + "No estás en un equipo.");
            event.setCancelled(true);
            return;
        }

        ArenaTeam senderTeam = gamePlayer.getArenaTeam();

        event.setCancelled(true);

        if (message.startsWith("!")) {
            handleGlobalChat(sender, message, senderTeam);
        } else {
            handleTeamChat(sender, message, senderTeam);
        }
    }

    private void handleGlobalChat(Player sender, String message, ArenaTeam senderTeam) {
        String globalMessage = message.substring(1).trim();

        if (globalMessage.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Escribe un mensaje después del '!'");
            return;
        }

        String teamTag = "<" + capitalizeFirst(senderTeam.getColor().getName()) + ">";
        String prefix = RankUtils.getChatPrefix(sender);
        String clanDisplay = ClanChatUtils.getClanDisplayForChat(sender);
        ChatColor teamColor = senderTeam.getColor().getChatColor();

        String formattedMessage = ChatColor.GRAY + "[GLOBAL] " +
                teamColor + teamTag + " " +
                prefix + clanDisplay + teamColor + sender.getName() +
                ChatColor.WHITE + ": " + globalMessage;

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(formattedMessage);
        }

        LogSystem.debug(LogCategory.PLAYER, "Chat global:", sender.getName());
    }

    private void handleTeamChat(Player sender, String message, ArenaTeam senderTeam) {
        String teamTag = "<" + capitalizeFirst(senderTeam.getColor().getName()) + ">";
        String prefix = RankUtils.getChatPrefix(sender);
        String clanDisplay = ClanChatUtils.getClanDisplayForChat(sender);
        ChatColor teamColor = senderTeam.getColor().getChatColor();

        String formattedMessage = teamColor + teamTag + " " +
                prefix + clanDisplay + teamColor + sender.getName() +
                ChatColor.WHITE + ": " + message;

        Set<UUID> sentPlayers = new HashSet<>();

        for (GamePlayer teamMember : senderTeam.getTeamPlayers()) {
            Player teamPlayer = teamMember.getPlayer();
            if (teamPlayer != null && teamPlayer.isOnline() && !sentPlayers.contains(teamPlayer.getUniqueId())) {
                teamPlayer.sendMessage(formattedMessage);
                sentPlayers.add(teamPlayer.getUniqueId());
            }
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("ptc.spy") && !sentPlayers.contains(onlinePlayer.getUniqueId())) {
                GamePlayer spyGamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(onlinePlayer.getUniqueId());

                if (spyGamePlayer == null || spyGamePlayer.getArenaTeam() != senderTeam) {
                    String spyMessage = ChatColor.GOLD + "[SPY] " +
                            teamColor + teamTag + " " +
                            prefix + clanDisplay + teamColor + sender.getName() +
                            ChatColor.WHITE + ": " + message;
                    onlinePlayer.sendMessage(spyMessage);
                    sentPlayers.add(onlinePlayer.getUniqueId());
                }
            }
        }

        LogSystem.debug(LogCategory.PLAYER, "Chat equipo:", sender.getName(), "-", senderTeam.getColor().getName());
    }

    public static void registerMessage(UUID sender, UUID recipient) {
        lastMessages.put(sender, recipient);
        lastMessages.put(recipient, sender);
    }

    public static UUID getLastRecipient(UUID player) {
        return lastMessages.get(player);
    }

    public static void sendPrivateMessage(Player sender, Player recipient, String message) {
        GamePlayer senderGamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(sender.getUniqueId());
        GamePlayer recipientGamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(recipient.getUniqueId());

        if (senderGamePlayer == null || recipientGamePlayer == null) {
            sender.sendMessage(ChatColor.RED + "Error al enviar el mensaje.");
            return;
        }

        ChatColor senderColor = ChatColor.AQUA;
        ChatColor recipientColor = ChatColor.AQUA;

        if (senderGamePlayer.getArenaTeam() != null) {
            TeamColor teamColor = senderGamePlayer.getArenaTeam().getColor();
            if (teamColor != TeamColor.LOBBY && teamColor != TeamColor.SPECTATOR) {
                senderColor = teamColor.getChatColor();
            }
        }

        if (recipientGamePlayer.getArenaTeam() != null) {
            TeamColor teamColor = recipientGamePlayer.getArenaTeam().getColor();
            if (teamColor != TeamColor.LOBBY && teamColor != TeamColor.SPECTATOR) {
                recipientColor = teamColor.getChatColor();
            }
        }

        String senderPrefix = RankUtils.getChatPrefix(sender);
        String recipientPrefix = RankUtils.getChatPrefix(recipient);
        String senderClanDisplay = ClanChatUtils.getClanDisplayForChat(sender);
        String recipientClanDisplay = ClanChatUtils.getClanDisplayForChat(recipient);

        String senderMessage = ChatColor.GRAY + "[TÚ -> " +
                recipientPrefix + recipientClanDisplay + recipientColor + recipient.getName() +
                ChatColor.GRAY + "] " + ChatColor.WHITE + message;
        sender.sendMessage(senderMessage);

        String recipientMessage = ChatColor.GRAY + "[" +
                senderPrefix + senderClanDisplay + senderColor + sender.getName() +
                ChatColor.GRAY + " -> TÚ] " + ChatColor.WHITE + message;
        recipient.sendMessage(recipientMessage);

        registerMessage(sender.getUniqueId(), recipient.getUniqueId());

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("ptc.spy") && !onlinePlayer.equals(sender) && !onlinePlayer.equals(recipient)) {

                String spyMessage = ChatColor.GOLD + "[SPY] [MSG] " +
                        senderPrefix + senderClanDisplay + senderColor + sender.getName() +
                        ChatColor.GRAY + " -> " +
                        recipientPrefix + recipientClanDisplay + recipientColor + recipient.getName() +
                        ChatColor.WHITE + ": " + message;
                onlinePlayer.sendMessage(spyMessage);
            }
        }

        LogSystem.debug(LogCategory.PLAYER, "Mensaje privado:", sender.getName(), "->", recipient.getName());
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}