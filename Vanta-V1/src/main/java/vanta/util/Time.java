package vanta.util;

public final class Time {
   private static final float NANOS_PER_SECOND = 1.0E9F;
   private static final long ORIGIN = System.nanoTime();
   public static final long TICK_MILLIS = 50L;
   private static long ticks;

   public static float seconds() {
      return (float)(System.nanoTime() - ORIGIN) / 1.0E9F;
   }

   public static void tick() {
      ++ticks;
   }

   public static long tickMillis() {
      return ticks * 50L;
   }

   public static boolean blink(long millis) {
      return (System.nanoTime() - ORIGIN) / (millis * 1000000L) % 2L == 0L;
   }

   private Time() {
   }

   public static final class Stopwatch {
      private long startedAt = System.nanoTime();

      public void reset() {
         this.startedAt = System.nanoTime();
      }

      public long elapsedMillis() {
         return (System.nanoTime() - this.startedAt) / 1000000L;
      }

      public boolean elapsed(long millis) {
         return this.elapsedMillis() >= millis;
      }
   }

   public static final class Ticker {
      private long last = -4611686018427387904L;
      private long due = -4611686018427387904L;

      public void mark() {
         this.last = Time.tickMillis();
         this.due = this.last;
      }

      public void pace(long delayMillis) {
         long now = Time.tickMillis();
         this.last = now;
         this.due = this.due + delayMillis > now ? this.due + delayMillis : now + delayMillis;
      }

      public boolean ready() {
         return Time.tickMillis() >= this.due;
      }

      public boolean elapsed(long millis) {
         return Time.tickMillis() - this.last >= millis;
      }
   }
}
