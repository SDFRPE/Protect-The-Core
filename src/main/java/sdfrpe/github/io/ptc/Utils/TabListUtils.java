package sdfrpe.github.io.ptc.Utils;

import org.bukkit.entity.Player;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TabListUtils {

    private static Class<?> chatComponentClass;
    private static Class<?> iChatBaseComponentClass;
    private static Class<?> packetClass;
    private static Constructor<?> packetConstructor;
    private static Method chatSerializerMethod;

    static {
        try {
            String version = Statics.VERSION;

            chatComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            iChatBaseComponentClass = chatComponentClass;
            packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerListHeaderFooter");

            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            chatSerializerMethod = chatSerializerClass.getMethod("a", String.class);

            packetConstructor = packetClass.getConstructor(iChatBaseComponentClass);

            Console.log("TabListUtils initialized successfully for version: " + version);
        } catch (Exception e) {
            Console.error("Failed to initialize TabListUtils: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void setHeaderFooter(Player player, String header, String footer) {
        try {
            String headerJson = "{\"text\":\"" + escapeJson(header) + "\"}";
            String footerJson = "{\"text\":\"" + escapeJson(footer) + "\"}";

            Object headerComponent = chatSerializerMethod.invoke(null, headerJson);
            Object footerComponent = chatSerializerMethod.invoke(null, footerJson);

            Object packet = packetConstructor.newInstance(headerComponent);

            Field footerField = packetClass.getDeclaredField("b");
            footerField.setAccessible(true);
            footerField.set(packet, footerComponent);

            ReflectionUtils.sendPacket(player, packet);

        } catch (Exception e) {
            Console.error("Error setting tab header/footer for " + player.getName() + ": " + e.getMessage());
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}