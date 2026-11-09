package vanta.setting;

import java.util.Locale;
import vanta.ui.Units;

public enum Measure {
   NONE,
   DISTANCE,
   PERCENT,
   DEGREES,
   FRACTION,
   MILLIS,
   TICKS,
   TENTH_BLOCKS;

   public static final int NO_LIMIT = 1000000000;

   public int ceiling(int max) {
      int var10000;
      switch (this.ordinal()) {
         case 2 -> var10000 = Math.max(max, 100);
         case 3 -> var10000 = Math.max(max, 360);
         default -> var10000 = 1000000000;
      }

      return var10000;
   }

   public String plain(int value) {
      return this == FRACTION ? this.format(value) : (this == TENTH_BLOCKS ? String.format(Locale.ROOT, "%.1f", (double)value / (double)10.0F) : Integer.toString(value));
   }

   public Integer parse(String text) {
      String number = text.strip().replace(",", ".").replaceAll("[^0-9.-]", "");
      if (!number.isEmpty() && !number.equals("-") && !number.equals(".")) {
         try {
            double value = Double.parseDouble(number);
            if (this == FRACTION) {
               value *= (double)100.0F;
            } else if (this == TENTH_BLOCKS) {
               value *= (double)10.0F;
            }

            return (int)Math.max(-1000000000L, Math.min(1000000000L, Math.round(value)));
         } catch (NumberFormatException var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   public String format(int value) {
      String var10000;
      switch (this.ordinal()) {
         case 0 -> var10000 = Integer.toString(value);
         case 1 -> var10000 = Units.distance(value);
         case 2 -> var10000 = value + "%";
         case 3 -> var10000 = value + "°";
         case 4 -> var10000 = String.format(Locale.ROOT, "%.2f", (double)value / (double)100.0F);
         case 5 -> var10000 = value + "ms";
         case 6 -> var10000 = value + "t";
         case 7 -> var10000 = String.format(Locale.ROOT, "%.1fm", (double)value / (double)10.0F);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   // $FF: synthetic method
   private static Measure[] $values() {
      return new Measure[]{NONE, DISTANCE, PERCENT, DEGREES, FRACTION, MILLIS, TICKS, TENTH_BLOCKS};
   }
}
