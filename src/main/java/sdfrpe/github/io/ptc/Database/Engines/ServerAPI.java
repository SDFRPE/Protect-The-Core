package sdfrpe.github.io.ptc.Database.Engines;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Game.Server.ServerInfo;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ServerAPI {
    private final String USER_AGENT = "Mozilla/5.0";
    private String BASE_URL;
    private final Gson gson = new Gson();

    public ServerAPI() {
    }

    private String getBaseURL() {
        if (this.BASE_URL == null) {
            PTC ptc = PTC.getInstance();
            if (ptc != null && ptc.getGameManager() != null && ptc.getGameManager().getGlobalSettings() != null) {
                String databaseURL = ptc.getGameManager().getGlobalSettings().getDatabaseURL();
                this.BASE_URL = databaseURL.replace("/player/", "/server/");
            } else {
                this.BASE_URL = "http://127.0.0.1/ptc/server/";
            }
        }
        return this.BASE_URL;
    }

    public boolean updateServer(ServerInfo serverInfo) {
        try {
            String serverName = serverInfo.getServerName();
            URL obj = new URL(this.getBaseURL() + serverName);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setDoOutput(true);

            String jsonData = this.gson.toJson(serverInfo);
            DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
            dataOutputStream.writeBytes(jsonData);
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                in.close();
                LogSystem.debug(LogCategory.NETWORK, "Server actualizado:", serverName, "ModeCW:", String.valueOf(serverInfo.isModeCW()));
                return true;
            } else {
                LogSystem.error(LogCategory.NETWORK, "Error actualizando server:", serverName, "Code:", String.valueOf(responseCode));
                return false;
            }
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Fallo actualizando server:", e.getMessage());
            return false;
        }
    }

    public ServerInfo getServer(String serverName) {
        try {
            URL obj = new URL(this.getBaseURL() + serverName);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                in.close();

                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonResponse);
                JsonObject jsonObject = root.getAsJsonObject();

                if (!jsonObject.get("error").getAsBoolean()) {
                    JsonObject data = jsonObject.getAsJsonObject("data");
                    return this.gson.fromJson(data.toString(), ServerInfo.class);
                }
            }
            return null;
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error obteniendo info de server:", e.getMessage());
            return null;
        }
    }

    public List<ServerInfo> getAllServers() {
        try {
            URL obj = new URL(this.getBaseURL() + "all");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                in.close();

                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonResponse);
                JsonObject jsonObject = root.getAsJsonObject();

                if (!jsonObject.get("error").getAsBoolean()) {
                    JsonArray dataArray = jsonObject.getAsJsonArray("data");
                    List<ServerInfo> servers = new ArrayList<>();

                    for (JsonElement element : dataArray) {
                        ServerInfo server = this.gson.fromJson(element.toString(), ServerInfo.class);
                        if (server.isOnline()) {
                            servers.add(server);
                        }
                    }

                    LogSystem.debug(LogCategory.NETWORK, "Recuperados", servers.size() + " servidores activos");
                    return servers;
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error obteniendo todos los servers:", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<ServerInfo> getNormalServers() {
        try {
            URL obj = new URL(this.getBaseURL() + "normal");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                in.close();

                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonResponse);
                JsonObject jsonObject = root.getAsJsonObject();

                if (!jsonObject.get("error").getAsBoolean()) {
                    JsonArray dataArray = jsonObject.getAsJsonArray("data");
                    List<ServerInfo> servers = new ArrayList<>();

                    for (JsonElement element : dataArray) {
                        ServerInfo server = this.gson.fromJson(element.toString(), ServerInfo.class);
                        if (server.isOnline()) {
                            servers.add(server);
                        }
                    }

                    LogSystem.debug(LogCategory.NETWORK, "Recuperados", servers.size() + " servidores normales");
                    return servers;
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error obteniendo servidores normales:", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<ServerInfo> getCWServers() {
        try {
            URL obj = new URL(this.getBaseURL() + "cw");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                in.close();

                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonResponse);
                JsonObject jsonObject = root.getAsJsonObject();

                if (!jsonObject.get("error").getAsBoolean()) {
                    JsonArray dataArray = jsonObject.getAsJsonArray("data");
                    List<ServerInfo> servers = new ArrayList<>();

                    for (JsonElement element : dataArray) {
                        ServerInfo server = this.gson.fromJson(element.toString(), ServerInfo.class);
                        if (server.isOnline()) {
                            servers.add(server);
                        }
                    }

                    LogSystem.debug(LogCategory.NETWORK, "Recuperados", servers.size() + " servidores CW");
                    return servers;
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error obteniendo servidores CW:", e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean deleteServer(String serverName) {
        try {
            URL obj = new URL(this.getBaseURL() + serverName);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("DELETE");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                LogSystem.debug(LogCategory.NETWORK, "Server eliminado:", serverName);
                return true;
            }
            return false;
        } catch (Exception e) {
            LogSystem.error(LogCategory.NETWORK, "Error eliminando server:", e.getMessage());
            return false;
        }
    }
}