package vanta.util;

import java.util.concurrent.ThreadLocalRandom;
import vanta.setting.RangeSetting;

public final class Humanize {
   private Humanize() {
   }

   public static int ticks(RangeSetting range) {
      return between(range.low(), range.high());
   }

   public static int between(int low, int high) {
      return high <= low ? low : ThreadLocalRandom.current().nextInt(low, high + 1);
   }

   public static boolean chance(int percent) {
      return percent >= 100 || percent > 0 && ThreadLocalRandom.current().nextInt(100) < percent;
   }

   public static float signed() {
      return ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F;
   }

   public static int seed() {
      return ThreadLocalRandom.current().nextInt();
   }

   public static float noise(int seed, double t) {
      double floor = Math.floor(t);
      long cell = (long)floor;
      float a = hash(seed, cell);
      float b = hash(seed, cell + 1L);
      float f = (float)(t - floor);
      float eased = f * f * (3.0F - 2.0F * f);
      return a + (b - a) * eased;
   }

   private static float hash(int seed, long cell) {
      long h = cell * -7046029254386353131L + (long)seed * -4417276706812531889L;
      h ^= h >>> 31;
      h *= -4658895280553007687L;
      h ^= h >>> 29;
      return (float)((double)(h >>> 11) * (double)1.110223E-16F * (double)2.0F - (double)1.0F);
   }
}
