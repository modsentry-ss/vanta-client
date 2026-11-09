package vanta.render;

import vanta.util.Colors;

public final class Opacity {
   private static float current = 1.0F;

   public static void with(float alpha, Runnable draw) {
      float previous = current;
      current = previous * Math.clamp(alpha, 0.0F, 1.0F);

      try {
         draw.run();
      } finally {
         current = previous;
      }

   }

   public static int apply(int argb) {
      return current >= 1.0F ? argb : Colors.withAlpha(argb, current);
   }

   public static Style apply(Style style) {
      return current >= 1.0F ? style : new Style(apply(style.fill()), apply(style.stroke()), style.strokeWidth(), apply(style.outline()), style.outlineWidth(), style.edges());
   }

   private Opacity() {
   }
}
