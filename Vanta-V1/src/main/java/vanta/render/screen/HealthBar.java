package vanta.render.screen;

import vanta.render.Rect;
import vanta.render.Style;
import vanta.util.Colors;

public final class HealthBar {
   public static int color(float fraction) {
      float f = Math.clamp(fraction, 0.0F, 1.0F);
      return f < 0.5F ? Colors.lerp(-50384, -10742, f * 2.0F) : Colors.lerp(-10742, -13318311, (f - 0.5F) * 2.0F);
   }

   public static void collect(ScreenBuffer buffer, Rect box, Side side, float fraction, float clearance, float pixel) {
      float shortSide = Math.min(box.width(), box.height()) / pixel;
      float thickness = Math.clamp(shortSide * 0.1F, 0.6F, 2.0F) * pixel;
      float border = Math.min(1.0F, thickness / pixel * 0.5F) * pixel;
      float offset = Math.clamp(shortSide * 0.15F, 0.75F, 3.0F) * pixel + clearance;
      Rect var10000;
      switch (side.ordinal()) {
         case 0 -> var10000 = new Rect(box.minX() - offset - thickness, box.minY(), box.minX() - offset, box.maxY());
         case 1 -> var10000 = new Rect(box.maxX() + offset, box.minY(), box.maxX() + offset + thickness, box.maxY());
         case 2 -> var10000 = new Rect(box.minX(), box.minY() - offset - thickness, box.maxX(), box.minY() - offset);
         case 3 -> var10000 = new Rect(box.minX(), box.maxY() + offset, box.maxX(), box.maxY() + offset + thickness);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      Rect bar = var10000;
      float f = Math.clamp(fraction, 0.0F, 1.0F);
      switch (side.ordinal()) {
         case 0:
         case 1:
            var10000 = new Rect(bar.minX(), bar.maxY() - bar.height() * f, bar.maxX(), bar.maxY());
            break;
         case 2:
         case 3:
            var10000 = new Rect(bar.minX(), bar.minY(), bar.minX() + bar.width() * f, bar.maxY());
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      Rect filled = var10000;
      buffer.rect(bar.expand(border), Style.fill(-16777216));
      if (f > 0.0F) {
         buffer.rect(filled, Style.fill(color(f)));
      }

   }

   private HealthBar() {
   }

   public static enum Side {
      LEFT,
      RIGHT,
      TOP,
      BOTTOM;

      // $FF: synthetic method
      private static Side[] $values() {
         return new Side[]{LEFT, RIGHT, TOP, BOTTOM};
      }
   }
}
