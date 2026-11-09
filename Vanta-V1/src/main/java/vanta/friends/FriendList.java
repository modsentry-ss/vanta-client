package vanta.friends;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vanta.config.Storage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class FriendList {
   private static Data data;

   private static Data data() {
      if (data == null) {
         data = (Data)Storage.read(Storage.file("friends"), Data.class);
         if (data == null || data.names == null) {
            data = new Data();
         }
      }

      return data;
   }

   public static List<String> all() {
      return List.copyOf(data().names);
   }

   public static boolean contains(String name) {
      if (name == null) {
         return false;
      } else {
         String key = name.trim().toLowerCase(Locale.ROOT);

         for(String existing : data().names) {
            if (existing.toLowerCase(Locale.ROOT).equals(key)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean protects(Entity entity) {
      boolean var10000;
      if (entity instanceof Player player) {
         if (contains(player.getGameProfile().name())) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   public static void add(String name) {
      if (name != null && !name.isBlank() && !contains(name)) {
         data().names.add(name.trim());
         Storage.write(Storage.file("friends"), data());
      }

   }

   public static void remove(String name) {
      if (name != null && data().names.removeIf((existing) -> existing.equalsIgnoreCase(name.trim()))) {
         Storage.write(Storage.file("friends"), data());
      }

   }

   public static void toggle(String name) {
      if (contains(name)) {
         remove(name);
      } else {
         add(name);
      }

   }

   private FriendList() {
   }

   private static final class Data {
      List<String> names = new ArrayList();
   }
}
