package sdfrpe.github.io.ptc.Utils;

import java.lang.reflect.Field;

public class PacketHelper {
   public static void setValue(Object obj, String name, Object value) {
      try {
         Field field = getValue(obj.getClass(), name);
         field.setAccessible(true);
         field.set(obj, value);
         field.setAccessible(false);
      } catch (Exception e) {
         throw new RuntimeException("Failed to set value for field: " + name, e);
      }
   }

   public static <T> T getValue(Object obj, String name, Class<T> type) {
      try {
         Field field = getValue(obj.getClass(), name);
         field.setAccessible(true);
         T value = type.cast(field.get(obj));
         field.setAccessible(false);
         return value;
      } catch (Exception e) {
         throw new RuntimeException("Failed to get value for field: " + name, e);
      }
   }

   public static Field getValue(Class<?> clazz, String name) throws NoSuchFieldException {
      try {
         Field field = clazz.getDeclaredField(name);
         return field;
      } catch (NoSuchFieldException e) {
         if (clazz.getSuperclass() == null) {
            throw e;
         } else {
            Console.debug(String.format("Trying to get(%s)", clazz.getSuperclass().getName()));
            return getValue(clazz.getSuperclass(), name);
         }
      }
   }

   public static Object getFieldValue(Object instance, String fieldName) throws Exception {
      Field field = instance.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(instance);
   }

   @SuppressWarnings("unchecked")
   public static <T> T getFieldValue(Field field, Object obj) {
      try {
         return (T) field.get(obj);
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }
}