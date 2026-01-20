package sdfrpe.github.io.ptc.Utils.Managers;

import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.Game.GameManager;
import sdfrpe.github.io.ptc.Game.Arena.Arena;
import sdfrpe.github.io.ptc.Game.Settings.ArenaSettings;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import sdfrpe.github.io.ptc.Utils.Configuration.JsonConfig;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ArenaManager {
   private GameManager gameManager;
   private Map<String, Arena> arenas;
   private Random random;

   public ArenaManager(GameManager gameManager) {
      this.gameManager = gameManager;
      this.arenas = Maps.newConcurrentMap();
      this.random = new Random();
   }

   public void loadArenas() {
      LogSystem.debug(LogCategory.GAME, "Cargando arenas...");
      boolean ignored = this.gameManager.getArenasFolder().mkdir();
      File[] files = this.gameManager.getArenasFolder().listFiles();

      if (files != null && files.length > 0) {
         for (File file : files) {
            String arenaJson = JsonConfig.readFile(file);
            ArenaSettings arenaSettings = this.gameManager.getGson().fromJson(arenaJson, ArenaSettings.class);
            Arena arena = new Arena(arenaSettings);
            this.arenas.put(arena.getArenaSettings().getName(), arena);
            LogSystem.debug(LogCategory.GAME, "Arena cargada:", arena.getArenaSettings().getName());
         }

         this.clearAllVotes();
         LogSystem.info(LogCategory.GAME, "Arenas cargadas:", this.arenas.size() + " arenas");
      } else {
         LogSystem.warn(LogCategory.GAME, "No hay arenas creadas - Usa /ptc create <nombre>");
      }
   }

   public void vote(GamePlayer gamePlayer, String arenaName) {
      if (gamePlayer == null) {
         LogSystem.warn(LogCategory.GAME, "Intento de voto con GamePlayer null");
         return;
      }

      for (Arena arena : this.arenas.values()) {
         if (arena.getArenaSettings().getName().equalsIgnoreCase(arenaName)) {
            arena.getVotes().removeIf(voter -> voter != null && voter.getUuid().equals(gamePlayer.getUuid()));

            if (!arena.getVotes().contains(gamePlayer)) {
               arena.getVotes().add(gamePlayer);
               LogSystem.debug(LogCategory.GAME, "Voto registrado:", gamePlayer.getName(), "->", arenaName);
            }
         } else {
            arena.getVotes().removeIf(voter -> voter != null && voter.getUuid().equals(gamePlayer.getUuid()));
         }
      }
   }

   public void removePlayerVotes(GamePlayer gamePlayer) {
      if (gamePlayer == null) {
         LogSystem.error(LogCategory.GAME, "No se pueden remover votos: GamePlayer es null");
         return;
      }

      int totalRemoved = 0;
      UUID playerUUID = gamePlayer.getUuid();

      for (Arena arena : this.arenas.values()) {
         Iterator<GamePlayer> iterator = arena.getVotes().iterator();
         while (iterator.hasNext()) {
            GamePlayer voter = iterator.next();
            if (voter == null || voter.getUuid().equals(playerUUID)) {
               iterator.remove();
               totalRemoved++;
            }
         }
      }

      if (totalRemoved > 0) {
         LogSystem.debug(LogCategory.GAME, "Votos removidos para:", gamePlayer.getName(), "de", totalRemoved + " arenas");
      }
   }

   public void clearAllVotes() {
      for (Arena arena : this.arenas.values()) {
         arena.getVotes().clear();
      }
      LogSystem.debug(LogCategory.GAME, "Todos los votos limpiados");
   }

   public Arena getMostVotedArena() {
      if (this.arenas.isEmpty()) {
         return null;
      }

      Arena mostVoted = null;
      int maxVotes = 0;

      for (Arena arena : this.arenas.values()) {
         int voteCount = arena.getVotes().size();
         if (voteCount > maxVotes) {
            maxVotes = voteCount;
            mostVoted = arena;
         }
      }

      if (mostVoted != null && maxVotes > 0) {
         LogSystem.debug(LogCategory.GAME, "Arena más votada:", mostVoted.getArenaSettings().getName(), "con", maxVotes + " votos");
      }

      return mostVoted;
   }

   public Set<Entry<String, Integer>> getVotes() {
      return this.arenas.values().stream()
              .map(arena -> new AbstractMap.SimpleEntry<>(arena.getArenaSettings().getName(), arena.getVotes().size()))
              .collect(Collectors.toSet());
   }

   public String getRandomArenaKey() {
      if (this.arenas.isEmpty()) {
         LogSystem.warn(LogCategory.GAME, "No hay arenas disponibles para seleccionar");
         return null;
      }

      List<String> arenaKeys = new ArrayList<>(this.arenas.keySet());
      String randomKey = arenaKeys.get(random.nextInt(arenaKeys.size()));

      LogSystem.debug(LogCategory.GAME, "Arena aleatoria seleccionada:", randomKey);
      return randomKey;
   }

   public List<String> getArenaNames() {
      return this.arenas.values().stream()
              .map(arena -> arena.getArenaSettings().getName())
              .collect(Collectors.toList());
   }

   public int countArenas() {
      return this.arenas.size();
   }

   public Arena getArena(String key) {
      return this.arenas.getOrDefault(key, null);
   }

   public Map<String, Arena> getArenas() {
      return this.arenas;
   }
}