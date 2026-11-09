package vanta.ui.click;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

final class Anim {
   static float dt = 0.016666668F;
   private static long last = System.nanoTime();
   private static final Map<Object, float[]> VALUES = new IdentityHashMap();

   private Anim() {
   }

   static void frame() {
      long now = System.nanoTime();
      dt = Math.min(0.05F, Math.max(0.0F, (float)(now - last) / 1.0E9F));
      last = now;
   }

   static float approach(float current, float target, float speed) {
      float k = 1.0F - (float)Math.exp((double)(-dt * speed * GuiPrefs.animSpeed()));
      float next = current + (target - current) * k;
      return Math.abs(target - next) < 0.0015F ? target : next;
   }

   static float to(Object key, int slot, float target, float speed) {
      float[] v = (float[])VALUES.get(key);
      if (v == null || v.length <= slot) {
         float[] grown = new float[Math.max(slot + 1, 8)];
         Arrays.fill(grown, Float.NaN);
         if (v != null) {
            System.arraycopy(v, 0, grown, 0, v.length);
         }

         v = grown;
         VALUES.put(key, grown);
      }

      if (Float.isNaN(v[slot])) {
         v[slot] = target;
         return target;
      } else {
         v[slot] = approach(v[slot], target, speed);
         return v[slot];
      }
   }

   static float from(Object key, int slot, float start, float target, float speed) {
      float[] v = (float[])VALUES.get(key);
      if (v == null || v.length <= slot || Float.isNaN(v[slot])) {
         to(key, slot, start, speed);
      }

      return to(key, slot, target, speed);
   }

   static float spring(Object key, int slot, float target, float stiffness, float damping) {
      to(key, slot + 1, 0.0F, 1.0F);
      float[] v = (float[])VALUES.get(key);
      if (Float.isNaN(v[slot])) {
         v[slot] = target;
      }

      float k = stiffness * GuiPrefs.animSpeed() * GuiPrefs.animSpeed();
      float d = damping * GuiPrefs.animSpeed();
      float step = dt;
      int n = Math.max(1, (int)Math.ceil((double)(step / 0.008F)));
      float h = step / (float)n;

      for(int i = 0; i < n; ++i) {
         float acc = (target - v[slot]) * k - v[slot + 1] * d;
         v[slot + 1] += acc * h;
         v[slot] += v[slot + 1] * h;
      }

      if (Math.abs(target - v[slot]) < 5.0E-4F && Math.abs(v[slot + 1]) < 0.001F) {
         v[slot] = target;
         v[slot + 1] = 0.0F;
      }

      return v[slot];
   }

   static void set(Object key, int slot, float value) {
      to(key, slot, value, 1.0F);
      ((float[])VALUES.get(key))[slot] = value;
   }

   static float smooth(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return t * t * (3.0F - 2.0F * t);
   }

   static float outCubic(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   static float outBack(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      float c = 1.2F;
      float u = t - 1.0F;
      return 1.0F + (c + 1.0F) * u * u * u + c * u * u;
   }
}
