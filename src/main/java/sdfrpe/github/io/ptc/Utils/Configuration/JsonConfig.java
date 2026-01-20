package sdfrpe.github.io.ptc.Utils.Configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class JsonConfig {
   public static void writeFile(File file, String data) {
      try (FileWriter myWriter = new FileWriter(file)) {
         myWriter.write(data);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static String readFile(File file) {
      StringBuilder builder = new StringBuilder();
      try (Scanner myReader = new Scanner(file)) {
         while (myReader.hasNextLine()) {
            builder.append(myReader.nextLine());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return builder.toString();
   }
}
