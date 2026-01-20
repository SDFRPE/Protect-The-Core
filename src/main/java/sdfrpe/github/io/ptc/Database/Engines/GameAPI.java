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

public class GameAPI {
   private final String USER_AGENT = "Mozilla/5.0";
   private final String URL;

   public GameAPI() {
      PTC ptc = PTC.getInstance();
      this.URL = ptc.getGameManager().getGlobalSettings().getDatabaseURL();
   }

   public JsonObject GET(UUID uuid) {
      try {
         URL obj = new URL(this.URL + uuid.toString());
         HttpURLConnection con = (HttpURLConnection)obj.openConnection();
         con.setRequestMethod("GET");
         con.setRequestProperty("User-Agent", "Mozilla/5.0");
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
            LogSystem.debug(LogCategory.DATABASE, "GET exitoso para UUID:", uuid.toString());
            return root.getAsJsonObject();
         } else {
            LogSystem.debug(LogCategory.DATABASE, "GET falló con código:", String.valueOf(responseCode));
            return null;
         }
      } catch (Exception e) {
         LogSystem.error(LogCategory.DATABASE, "Error en GET para UUID:", uuid.toString(), "-", e.getMessage());
         return null;
      }
   }

   public JsonObject POST(UUID uuid, String jsonObject) {
      try {
         URL obj = new URL(this.URL + uuid.toString());
         HttpURLConnection con = (HttpURLConnection)obj.openConnection();
         con.setRequestMethod("POST");
         con.setRequestProperty("User-Agent", "Mozilla/5.0");
         con.setRequestProperty("Content-type", "application/json");
         con.setConnectTimeout(5000);
         con.setReadTimeout(5000);
         con.setDoOutput(true);

         DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
         dataOutputStream.writeBytes(jsonObject);
         dataOutputStream.flush();
         dataOutputStream.close();

         int responseCode = con.getResponseCode();

         if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String jsonresponse = in.readLine();
            JsonParser jsonParser = new JsonParser();
            JsonElement root = jsonParser.parse(jsonresponse);
            in.close();
            LogSystem.debug(LogCategory.DATABASE, "POST exitoso para UUID:", uuid.toString());
            return root.getAsJsonObject();
         } else {
            LogSystem.debug(LogCategory.DATABASE, "POST falló con código:", String.valueOf(responseCode));
            return null;
         }
      } catch (Exception e) {
         LogSystem.error(LogCategory.DATABASE, "Error en POST para UUID:", uuid.toString(), "-", e.getMessage());
         return null;
      }
   }
}