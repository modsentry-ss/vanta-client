package vanta.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class DrivenKeys {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final Map<KeyMapping, Integer> DRIVEN = new HashMap();

   private DrivenKeys() {
   }

   public static void drive(KeyMapping key, boolean down) {
      if (down) {
         DRIVEN.put(key, tick());
      } else {
         DRIVEN.remove(key);
      }

   }

   public static boolean driven(KeyMapping key) {
      Integer at = (Integer)DRIVEN.get(key);
      return at != null && tick() - at <= 1 && key.isDown();
   }

   private static int tick() {
      return mc.player == null ? 0 : mc.player.tickCount;
   }
}
