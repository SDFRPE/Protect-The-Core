package sdfrpe.github.io.ptc.Hologram;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TopDataFetcher {
    private final String apiBaseUrl;

    public TopDataFetcher(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public List<TopEntry> fetchTop(TopType type, int limit) {
        List<TopEntry> entries = new ArrayList<>();

        try {
            String endpoint = buildEndpoint(type, limit);
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                reader.close();

                JsonParser parser = new JsonParser();
                JsonObject root = parser.parse(jsonBuilder.toString()).getAsJsonObject();

                if (!root.get("error").getAsBoolean()) {
                    if (type.isPlaytime()) {
                        entries = parsePlaytimeResponse(root, type);
                    } else if (type.isWeekly()) {
                        entries = parseWeeklyResponse(root, type);
                    } else {
                        entries = parseAllTimeResponse(root, type);
                    }

                    LogSystem.debug(LogCategory.API, "TOP data fetched:", type.name(), "-", entries.size() + " entries");
                } else {
                    LogSystem.warn(LogCategory.API, "API error fetching TOP:", type.name());
                }
            } else {
                LogSystem.warn(LogCategory.API, "API response code:", String.valueOf(responseCode), "for", type.name());
            }

            connection.disconnect();
        } catch (Exception e) {
            LogSystem.error(LogCategory.API, "Error fetching TOP data:", type.name(), e.getMessage());
        }

        return entries;
    }

    private String buildEndpoint(TopType type, int limit) {
        if (type.isPlaytime()) {
            if (type.isWeekly()) {
                return apiBaseUrl + "top/weekly/playtime?limit=" + limit;
            } else {
                return apiBaseUrl + "top/playtime?limit=" + limit;
            }
        } else if (type.isWeekly()) {
            return apiBaseUrl + "top/weekly/" + type.getFieldName() + "?limit=" + limit;
        } else {
            return apiBaseUrl + "top/" + type.getFieldName() + "?limit=" + limit;
        }
    }

    private List<TopEntry> parseAllTimeResponse(JsonObject root, TopType type) {
        List<TopEntry> entries = new ArrayList<>();
        JsonArray dataArray = root.getAsJsonArray("data");

        for (int i = 0; i < dataArray.size(); i++) {
            JsonObject playerData = dataArray.get(i).getAsJsonObject();

            String playerName = playerData.has("playerName") && !playerData.get("playerName").isJsonNull()
                    ? playerData.get("playerName").getAsString()
                    : "Desconocido";

            long value = playerData.has(type.getFieldName()) && !playerData.get(type.getFieldName()).isJsonNull()
                    ? playerData.get(type.getFieldName()).getAsLong()
                    : 0;

            String clanName = playerData.has("clanDisplayName") && !playerData.get("clanDisplayName").isJsonNull()
                    ? playerData.get("clanDisplayName").getAsString()
                    : null;

            entries.add(new TopEntry(i + 1, playerName, value, clanName));
        }

        return entries;
    }

    private List<TopEntry> parseWeeklyResponse(JsonObject root, TopType type) {
        List<TopEntry> entries = new ArrayList<>();
        JsonObject data = root.getAsJsonObject("data");
        JsonArray playersArray = data.getAsJsonArray("players");

        for (int i = 0; i < playersArray.size(); i++) {
            JsonObject playerData = playersArray.get(i).getAsJsonObject();

            String playerName = playerData.has("playerName") && !playerData.get("playerName").isJsonNull()
                    ? playerData.get("playerName").getAsString()
                    : "Desconocido";

            long value = playerData.has(type.getFieldName()) && !playerData.get(type.getFieldName()).isJsonNull()
                    ? playerData.get(type.getFieldName()).getAsLong()
                    : 0;

            String clanName = playerData.has("clanDisplayName") && !playerData.get("clanDisplayName").isJsonNull()
                    ? playerData.get("clanDisplayName").getAsString()
                    : null;

            entries.add(new TopEntry(i + 1, playerName, value, clanName));
        }

        return entries;
    }

    private List<TopEntry> parsePlaytimeResponse(JsonObject root, TopType type) {
        List<TopEntry> entries = new ArrayList<>();

        JsonArray playersArray;
        if (type.isWeekly()) {
            JsonObject data = root.getAsJsonObject("data");
            playersArray = data.getAsJsonArray("players");
        } else {
            playersArray = root.getAsJsonArray("data");
        }

        if (playersArray == null) {
            return entries;
        }

        for (int i = 0; i < playersArray.size(); i++) {
            JsonObject playerData = playersArray.get(i).getAsJsonObject();

            String playerName = playerData.has("playerName") && !playerData.get("playerName").isJsonNull()
                    ? playerData.get("playerName").getAsString()
                    : "Desconocido";

            long playtime = playerData.has("playtime") && !playerData.get("playtime").isJsonNull()
                    ? playerData.get("playtime").getAsLong()
                    : 0;

            String formattedPlaytime = playerData.has("playtimeFormatted") && !playerData.get("playtimeFormatted").isJsonNull()
                    ? playerData.get("playtimeFormatted").getAsString()
                    : formatPlaytime(playtime);

            String clanName = playerData.has("clanDisplayName") && !playerData.get("clanDisplayName").isJsonNull()
                    ? playerData.get("clanDisplayName").getAsString()
                    : null;

            entries.add(new TopEntry(i + 1, playerName, playtime, clanName, formattedPlaytime));
        }

        return entries;
    }

    private String formatPlaytime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}