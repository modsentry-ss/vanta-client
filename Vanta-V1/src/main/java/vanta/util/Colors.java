package vanta.util;

public final class Colors {
   public static int alpha(int argb) {
      return argb >>> 24;
   }

   public static int red(int argb) {
      return argb >> 16 & 255;
   }

   public static int green(int argb) {
      return argb >> 8 & 255;
   }

   public static int blue(int argb) {
      return argb & 255;
   }

   public static int rgb(int argb) {
      return argb & 16777215;
   }

   public static boolean transparent(int argb) {
      return alpha(argb) == 0;
   }

   public static int opaque(int rgb) {
      return -16777216 | rgb(rgb);
   }

   public static int lerp(int from, int to, float t) {
      t = Math.clamp(t, 0.0F, 1.0F);
      int a = channel(alpha(from), alpha(to), t);
      int r = channel(red(from), red(to), t);
      int g = channel(green(from), green(to), t);
      int b = channel(blue(from), blue(to), t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int withAlpha(int argb, float alpha) {
      int scaled = Math.round((float)alpha(argb) * Math.clamp(alpha, 0.0F, 1.0F));
      return scaled << 24 | rgb(argb);
   }

   public static int darken(int rgb, float amount) {
      int r = Math.round((float)red(rgb) * (1.0F - amount));
      int g = Math.round((float)green(rgb) * (1.0F - amount));
      int b = Math.round((float)blue(rgb) * (1.0F - amount));
      return r << 16 | g << 8 | b;
   }

   public static String hex(int rgb) {
      return String.format("#%06X", rgb(rgb));
   }

   private static int channel(int from, int to, float t) {
      return Math.round((float)from + (float)(to - from) * t);
   }

   private Colors() {
   }
}
