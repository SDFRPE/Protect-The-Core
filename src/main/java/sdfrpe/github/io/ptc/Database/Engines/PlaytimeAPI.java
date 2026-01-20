package sdfrpe.github.io.ptc.Database.Engines;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class PlaytimeAPI {
    private final String USER_AGENT = "Mozilla/5.0";
    private String BASE_URL;

    public static class HeartbeatResponse {
        private final boolean success;
        private final boolean needsRestart;

        public HeartbeatResponse(boolean success, boolean needsRestart) {
            this.success = success;
            this.needsRestart = needsRestart;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isNeedsRestart() {
            return needsRestart;
        }
    }

    public PlaytimeAPI() {
    }

    private String getBaseURL() {
        if (this.BASE_URL == null) {
            PTC ptc = PTC.getInstance();
            if (ptc != null && ptc.getGameManager() != null && ptc.getGameManager().getGlobalSettings() != null) {
                String databaseURL = ptc.getGameManager().getGlobalSettings().getDatabaseURL();
                this.BASE_URL = databaseURL.replace("/player/", "/playtime/");
            } else {
                this.BASE_URL = "http://127.0.0.1:25637/ptc/playtime/";
            }
        }
        return this.BASE_URL;
    }

    public boolean startSession(UUID uuid, String playerName, String serverName) {
        JsonObject response = startSessionRequest(uuid, playerName, serverName);
        if (response != null && response.has("success")) {
            return response.get("success").getAsBoolean();
        }
        return false;
    }

    private JsonObject startSessionRequest(UUID uuid, String playerName, String serverName) {
        try {
            URL obj = new URL(this.getBaseURL() + uuid.toString() + "/start");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setDoOutput(true);

            String jsonBody = "{\"playerName\":\"" + playerName + "\",\"serverName\":\"" + serverName + "\"}";
            DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
            dataOutputStream.writeBytes(jsonBody);
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonresponse = in.readLine();
                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonresponse);
                in.close();
                LogSystem.debug(LogCategory.DATABASE, "Playtime session started for:", uuid.toString());
                return root.getAsJsonObject();
            } else {
                LogSystem.debug(LogCategory.DATABASE, "Playtime start failed with code:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.DATABASE, "Error starting playtime session:", uuid.toString(), "-", e.getMessage());
            return null;
        }
    }

    public HeartbeatResponse sendHeartbeat(UUID uuid) {
        JsonObject response = sendHeartbeatRequest(uuid);

        if (response == null) {
            return null;
        }

        boolean success = response.has("success") && response.get("success").getAsBoolean();
        boolean needsRestart = response.has("needsRestart") && response.get("needsRestart").getAsBoolean();

        return new HeartbeatResponse(success, needsRestart);
    }

    private JsonObject sendHeartbeatRequest(UUID uuid) {
        try {
            URL obj = new URL(this.getBaseURL() + uuid.toString() + "/heartbeat");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setDoOutput(true);

            DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
            dataOutputStream.writeBytes("{}");
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonresponse = in.readLine();
                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonresponse);
                in.close();
                return root.getAsJsonObject();
            } else if (responseCode == 404) {
                LogSystem.debug(LogCategory.DATABASE, "Playtime session expired for:", uuid.toString());
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("needsRestart", true);
                return errorResponse;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean endSession(UUID uuid) {
        JsonObject response = endSessionRequest(uuid);
        if (response != null && response.has("success")) {
            return response.get("success").getAsBoolean();
        }
        return false;
    }

    private JsonObject endSessionRequest(UUID uuid) {
        try {
            URL obj = new URL(this.getBaseURL() + uuid.toString() + "/end");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setDoOutput(true);

            DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
            dataOutputStream.writeBytes("{}");
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonresponse = in.readLine();
                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonresponse);
                in.close();
                LogSystem.debug(LogCategory.DATABASE, "Playtime session ended for:", uuid.toString());
                return root.getAsJsonObject();
            } else {
                LogSystem.debug(LogCategory.DATABASE, "Playtime end failed with code:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.DATABASE, "Error ending playtime session:", uuid.toString(), "-", e.getMessage());
            return null;
        }
    }

    public JsonObject getPlaytime(UUID uuid) {
        try {
            URL obj = new URL(this.getBaseURL() + uuid.toString());
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonresponse = in.readLine();
                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonresponse);
                in.close();
                return root.getAsJsonObject();
            } else {
                return null;
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.DATABASE, "Error getting playtime:", uuid.toString(), "-", e.getMessage());
            return null;
        }
    }
}