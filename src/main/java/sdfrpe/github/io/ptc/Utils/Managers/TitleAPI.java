package sdfrpe.github.io.ptc.Utils.Managers;

import sdfrpe.github.io.ptc.Utils.ReflectionUtils;
import sdfrpe.github.io.ptc.Utils.Statics;
import org.bukkit.entity.Player;

public class TitleAPI {
   private String title = "";
   private String subTitle = "";
   private int fadeInTime = 10;
   private int showTime = 20;
   private int fadeOutTime = 10;

   public void send(Player player) {
      try {
         Object enumTitle = ReflectionUtils.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get(null);
         Object enumTitleText = ReflectionUtils.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null);
         Object enumSubtitle = ReflectionUtils.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null);

         Object timesPacket = ReflectionUtils.getNMSClass("PacketPlayOutTitle")
                 .getConstructor(ReflectionUtils.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], ReflectionUtils.getNMSClass("IChatBaseComponent"), int.class, int.class, int.class)
                 .newInstance(enumTitle, null, this.fadeInTime, this.showTime, this.fadeOutTime);
         ReflectionUtils.sendPacket(player, timesPacket);

         if (this.title != null && !this.title.isEmpty()) {
            Object chatTitle = ReflectionUtils.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0]
                    .getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + Statics.c(this.title) + "\"}");
            Object titlePacket = ReflectionUtils.getNMSClass("PacketPlayOutTitle")
                    .getConstructor(ReflectionUtils.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], ReflectionUtils.getNMSClass("IChatBaseComponent"))
                    .newInstance(enumTitleText, chatTitle);
            ReflectionUtils.sendPacket(player, titlePacket);
         }

         if (this.subTitle != null && !this.subTitle.isEmpty()) {
            Object chatSubtitle = ReflectionUtils.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0]
                    .getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + Statics.c(this.subTitle) + "\"}");
            Object subtitlePacket = ReflectionUtils.getNMSClass("PacketPlayOutTitle")
                    .getConstructor(ReflectionUtils.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], ReflectionUtils.getNMSClass("IChatBaseComponent"))
                    .newInstance(enumSubtitle, chatSubtitle);
            ReflectionUtils.sendPacket(player, subtitlePacket);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public String title() { return this.title; }
   public String subTitle() { return this.subTitle; }
   public TitleAPI title(String title) { this.title = title; return this; }
   public TitleAPI subTitle(String subTitle) { this.subTitle = subTitle; return this; }
   public TitleAPI fadeInTime(int fadeInTime) { this.fadeInTime = fadeInTime; return this; }
   public TitleAPI showTime(int showTime) { this.showTime = showTime; return this; }
   public TitleAPI fadeOutTime(int fadeOutTime) { this.fadeOutTime = fadeOutTime; return this; }
}