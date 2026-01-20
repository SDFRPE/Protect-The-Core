package sdfrpe.github.io.ptc.Database.Structures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Database.IDatabase;
import sdfrpe.github.io.ptc.Database.Engines.GameAPI;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Player.PlayerStats;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameLoader implements IDatabase {
    private final GameAPI gameAPI = new GameAPI();
    private final Gson gson = new Gson();
    private final ConcurrentHashMap<UUID, AtomicBoolean> savingPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> saveRetries = new ConcurrentHashMap<>();
    private static final int MAX_SAVE_RETRIES = 3;
    private static final long SAVE_RETRY_DELAY = 1000L;

    public void loadPlayer(UUID uuid) {
        try {
            JsonObject response = this.gameAPI.GET(uuid);
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(uuid);

            if (gamePlayer == null) {
                LogSystem.error(LogCategory.DATABASE, "GamePlayer null para UUID:", uuid.toString());
                return;
            }

            LogSystem.info(LogCategory.DATABASE, "Cargando jugador:", uuid.toString());

            if (response == null) {
                LogSystem.debug(LogCategory.DATABASE, "Sin respuesta de API para", uuid.toString(), "- Creando nuevo");
                gamePlayer.setPlayerStats(new PlayerStats());
                return;
            }

            if (response.get("code") != null && response.get("code").getAsInt() == 200) {
                PlayerStats playerStats = this.gson.fromJson(response.get("data").getAsJsonObject(), PlayerStats.class);
                playerStats.checkMultiplierExpiration();
                LogSystem.info(LogCategory.DATABASE, "Datos cargados correctamente (XP:", String.valueOf(playerStats.getPlayerLevels().getTotalExp()) + ")");
                gamePlayer.setPlayerStats(playerStats);

                JsonObject dataObj = response.get("data").getAsJsonObject();
                boolean needsUpdate = false;
                String reason = "";

                if (!dataObj.has("playerName")) {
                    needsUpdate = true;
                    reason = "campo playerName no existe";
                } else {
                    String savedName = dataObj.get("playerName").getAsString();
                    if (savedName == null || savedName.trim().isEmpty()) {
                        needsUpdate = true;
                        reason = "playerName es null o vacío";
                    } else if (savedName.equals("Desconocido")) {
                        needsUpdate = true;
                        reason = "playerName es 'Desconocido'";
                    }
                }

                if (needsUpdate) {
                    LogSystem.warn(LogCategory.DATABASE, "Actualizando playerName:", gamePlayer.getName(), "- Razón:", reason);
                    Bukkit.getScheduler().runTaskAsynchronously(PTC.getInstance(), () -> {
                        savePlayer(uuid);
                    });
                }

            } else if (response.get("code").getAsInt() == 403) {
                LogSystem.debug(LogCategory.DATABASE, "Jugador no existe en DB:", uuid.toString(), "- Creando nuevo");
                gamePlayer.setPlayerStats(new PlayerStats());
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.DATABASE, "Error cargando jugador", uuid.toString(), e.getMessage());
            e.printStackTrace();
        }
    }

    public PlayerStats loadPlayerSync(UUID uuid) {
        try {
            JsonObject response = this.gameAPI.GET(uuid);

            if (response == null) {
                LogSystem.debug(LogCategory.DATABASE, "Sin respuesta de API para", uuid.toString(), "- Creando nuevo");
                return new PlayerStats();
            }

            if (response.get("code") != null && response.get("code").getAsInt() == 200) {
                PlayerStats playerStats = this.gson.fromJson(response.get("data").getAsJsonObject(), PlayerStats.class);
                playerStats.checkMultiplierExpiration();
                LogSystem.info(LogCategory.DATABASE, "Datos cargados correctamente (XP:", String.valueOf(playerStats.getPlayerLevels().getTotalExp()) + ")");

                JsonObject dataObj = response.get("data").getAsJsonObject();
                boolean needsUpdate = false;
                String reason = "";

                GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(uuid);
                String playerName = gamePlayer != null ? gamePlayer.getName() : null;

                if (playerName == null) {
                    Player bukkitPlayer = Bukkit.getPlayer(uuid);
                    if (bukkitPlayer != null) {
                        playerName = bukkitPlayer.getName();
                    }
                }

                if (!dataObj.has("playerName")) {
                    needsUpdate = true;
                    reason = "campo playerName no existe";
                } else {
                    String savedName = dataObj.get("playerName").getAsString();
                    if (savedName == null || savedName.trim().isEmpty()) {
                        needsUpdate = true;
                        reason = "playerName es null o vacío";
                    } else if (savedName.equals("Desconocido")) {
                        needsUpdate = true;
                        reason = "playerName es 'Desconocido'";
                    }
                }

                if (needsUpdate && playerName != null) {
                    LogSystem.warn(LogCategory.DATABASE, "Actualizando playerName:", playerName, "- Razón:", reason);
                    try {
                        JsonObject dataToSave = this.gson.toJsonTree(playerStats).getAsJsonObject();
                        dataToSave.addProperty("playerName", playerName);
                        String toSave = this.gson.toJson(dataToSave);
                        JsonObject saveResponse = this.gameAPI.POST(uuid, toSave);
                        if (saveResponse != null) {
                            LogSystem.info(LogCategory.DATABASE, "PlayerName actualizado exitosamente:", playerName);
                        }
                    } catch (Exception e) {
                        LogSystem.error(LogCategory.DATABASE, "Error actualizando playerName:", e.getMessage());
                    }
                }

                return playerStats;
            } else if (response.get("code").getAsInt() == 403) {
                LogSystem.debug(LogCategory.DATABASE, "Jugador no existe en DB:", uuid.toString(), "- Creando nuevo");
                return new PlayerStats();
            }

            return new PlayerStats();
        } catch (Exception e) {
            LogSystem.error(LogCategory.DATABASE, "Error cargando datos síncronos", uuid.toString(), e.getMessage());
            e.printStackTrace();
            return new PlayerStats();
        }
    }

    public PlayerStats loadPlayerDataSync(UUID uuid) {
        return loadPlayerSync(uuid);
    }

    public void savePlayer(UUID uuid) {
        AtomicBoolean saving = savingPlayers.computeIfAbsent(uuid, k -> new AtomicBoolean(false));

        if (!saving.compareAndSet(false, true)) {
            int retries = saveRetries.getOrDefault(uuid, 0);
            if (retries < MAX_SAVE_RETRIES) {
                saveRetries.put(uuid, retries + 1);
                Bukkit.getScheduler().runTaskLaterAsynchronously(PTC.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        savePlayer(uuid);
                    }
                }, SAVE_RETRY_DELAY / 50);
                LogSystem.debug(LogCategory.DATABASE, "Reintentando guardar jugador (intento " + (retries + 1) + "):", uuid.toString());
                return;
            } else {
                LogSystem.warn(LogCategory.DATABASE, "No se pudo guardar jugador después de", MAX_SAVE_RETRIES + " intentos:", uuid.toString());
                saveRetries.remove(uuid);
                return;
            }
        }

        try {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(uuid);

            if (gamePlayer == null) {
                LogSystem.debug(LogCategory.DATABASE, "No se puede guardar jugador null:", uuid.toString());
                return;
            }

            String playerName = gamePlayer.getName();

            if (playerName == null || playerName.trim().isEmpty() || playerName.equals("Desconocido")) {
                LogSystem.warn(LogCategory.DATABASE, "Nombre inválido detectado para UUID:", uuid.toString(), "- Nombre:", playerName);

                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    playerName = player.getName();
                    gamePlayer.setName(playerName);
                    LogSystem.info(LogCategory.DATABASE, "Nombre corregido a:", playerName, "antes de guardar");
                } else {
                    LogSystem.error(LogCategory.DATABASE, "GUARDADO CANCELADO - No se puede guardar jugador sin nombre válido:", uuid.toString());

                    int retries = saveRetries.getOrDefault(uuid, 0);
                    if (retries < MAX_SAVE_RETRIES) {
                        saveRetries.put(uuid, retries + 1);
                        LogSystem.debug(LogCategory.DATABASE, "Se reintentará guardar cuando el nombre esté disponible (intento", String.valueOf(retries + 1), "de", String.valueOf(MAX_SAVE_RETRIES) + ")");
                    } else {
                        LogSystem.error(LogCategory.DATABASE, "ADVERTENCIA: Se alcanzó el máximo de reintentos para", uuid.toString());
                        saveRetries.remove(uuid);
                    }

                    return;
                }
            }

            Player player = gamePlayer.getPlayer();
            if (player != null && player.isOnline()) {
                gamePlayer.getPlayerStats().setExp(player);
            }

            gamePlayer.getPlayerStats().checkMultiplierExpiration();

            JsonObject dataToSave = this.gson.toJsonTree(gamePlayer.getPlayerStats()).getAsJsonObject();
            dataToSave.addProperty("playerName", gamePlayer.getName());
            String toSave = this.gson.toJson(dataToSave);

            JsonObject response = this.gameAPI.POST(uuid, toSave);

            if (response == null) {
                LogSystem.warn(LogCategory.DATABASE, "No se pudo guardar datos para", uuid.toString(), "- Solo en memoria");
                return;
            }

            LogSystem.info(LogCategory.DATABASE, "Guardando jugador:", uuid.toString(), "(XP:", String.valueOf(gamePlayer.getPlayerStats().getPlayerLevels().getTotalExp()) + ")");
            saveRetries.remove(uuid);

        } catch (Exception e) {
            LogSystem.error(LogCategory.DATABASE, "Error guardando jugador", uuid.toString(), e.getMessage());
            e.printStackTrace();
        } finally {
            saving.set(false);
        }
    }

    public void savePlayerSync(UUID uuid) {
        try {
            GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(uuid);

            if (gamePlayer == null) {
                LogSystem.debug(LogCategory.DATABASE, "No se puede guardar jugador null:", uuid.toString());
                return;
            }

            String playerName = gamePlayer.getName();

            if (playerName == null || playerName.trim().isEmpty() || playerName.equals("Desconocido")) {
                LogSystem.warn(LogCategory.DATABASE, "Nombre inválido en guardado síncrono:", uuid.toString());

                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    playerName = player.getName();
                    gamePlayer.setName(playerName);
                    LogSystem.info(LogCategory.DATABASE, "Nombre corregido a:", playerName);
                } else {
                    LogSystem.error(LogCategory.DATABASE, "GUARDADO CANCELADO - No se puede guardar sin nombre válido:", uuid.toString());
                    return;
                }
            }

            Player player = gamePlayer.getPlayer();
            if (player != null && player.isOnline()) {
                gamePlayer.getPlayerStats().setExp(player);
            }

            gamePlayer.getPlayerStats().checkMultiplierExpiration();

            JsonObject dataToSave = this.gson.toJsonTree(gamePlayer.getPlayerStats()).getAsJsonObject();
            dataToSave.addProperty("playerName", gamePlayer.getName());
            String toSave = this.gson.toJson(dataToSave);

            JsonObject response = this.gameAPI.POST(uuid, toSave);

            if (response == null) {
                LogSystem.warn(LogCategory.DATABASE, "No se pudo guardar datos para", uuid.toString());
                return;
            }

            LogSystem.info(LogCategory.DATABASE, "Jugador guardado síncronamente:", uuid.toString(), "(XP:", String.valueOf(gamePlayer.getPlayerStats().getPlayerLevels().getTotalExp()) + ")");

        } catch (Exception e) {
            LogSystem.error(LogCategory.DATABASE, "Error guardando jugador", uuid.toString(), e.getMessage());
        }
    }

    public int saveAllPlayersSync() {
        int savedPlayers = 0;
        int errors = 0;

        for (GamePlayer gamePlayer : PTC.getInstance().getGameManager().getPlayerManager().getPlayerMap().values()) {
            try {
                savePlayerSync(gamePlayer.getUuid());
                savedPlayers++;
            } catch (Exception e) {
                errors++;
                LogSystem.error(LogCategory.DATABASE, "Error guardando jugador:", e.getMessage());
            }
        }

        if (errors > 0) {
            LogSystem.warn(LogCategory.DATABASE, "Guardados " + savedPlayers + " jugadores con " + errors + " errores");
        } else {
            LogSystem.info(LogCategory.DATABASE, "Guardados " + savedPlayers + " jugadores exitosamente");
        }

        return savedPlayers;
    }

    public void clearRetries(UUID uuid) {
        saveRetries.remove(uuid);
        savingPlayers.remove(uuid);
    }
}