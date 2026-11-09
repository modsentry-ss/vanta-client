package vanta.ui.click;

import vanta.module.Category;
import vanta.render.Gfx;

final class Icons {
   private static final float W = 0.75F;

   private Icons() {
   }

   static void draw(Kind kind, float x, float y, float size, int color) {
      int c = Paint.a(color, 1.0F);
      float s = size / 12.0F;
      float cx = x + size / 2.0F;
      float cy = y + size / 2.0F;
      switch (kind.ordinal()) {
         case 0:
            sword(cx, cy, s, c, false);
            sword(cx, cy, s, c, true);
            break;
         case 1:
            Gfx.polyline(new float[]{cx - 4.0F * s, cy - 0.5F * s, cx, cy - 4.5F * s, cx + 4.0F * s, cy - 0.5F * s}, 0.75F, c, false);
            Gfx.polyline(new float[]{cx - 4.0F * s, cy + 3.5F * s, cx, cy - 0.5F * s, cx + 4.0F * s, cy + 3.5F * s}, 0.75F, c, false);
            break;
         case 2:
            star(cx - 0.8F * s, cy + 0.8F * s, 4.6F * s, c);
            star(cx + 3.6F * s, cy - 3.6F * s, 1.9F * s, c);
            star(cx + 3.8F * s, cy + 3.4F * s, 1.2F * s, c);
            break;
         case 3:
            Gfx.circleOutline(cx, cy - 2.4F * s, 2.4F * s, 0.75F, c);
            Gfx.arc(cx, cy + 5.6F * s, 4.6F * s, 0.75F, 180.0F, 360.0F, c);
            break;
         case 4:
            Gfx.circleOutline(cx, cy, 1.7F * s, 0.75F, c);
            int teeth = 8;
            float[] pts = new float[teeth * 4 * 2];
            float ro = 5.0F * s;
            float ri = 3.7F * s;

            for(int i = 0; i < teeth; ++i) {
               double base = (Math.PI * 2D) * (double)i / (double)teeth;
               double step = (Math.PI * 2D) / (double)teeth;
               double[] angles = new double[]{base - step * 0.18, base + step * 0.18, base + step * 0.32, base + step * 0.68};
               float[] radii = new float[]{ro, ro, ri, ri};

               for(int k = 0; k < 4; ++k) {
                  pts[(i * 4 + k) * 2] = cx + (float)Math.cos(angles[k]) * radii[k];
                  pts[(i * 4 + k) * 2 + 1] = cy + (float)Math.sin(angles[k]) * radii[k];
               }
            }

            Gfx.polyline(pts, 0.75F, c, true);
            break;
         case 5:
            Gfx.roundedOutline(cx - 5.0F * s, cy - 4.0F * s, 10.0F * s, 7.0F * s, 1.2F * s, 0.75F, c);
            Gfx.line(cx, cy + 3.0F * s, cx, cy + 4.6F * s, 0.75F, c);
            Gfx.line(cx - 2.6F * s, cy + 4.8F * s, cx + 2.6F * s, cy + 4.8F * s, 0.75F, c);
            break;
         case 6:
            Gfx.polyline(new float[]{cx - 3.8F * s, cy - 5.0F * s, cx + 1.6F * s, cy - 5.0F * s, cx + 3.8F * s, cy - 2.8F * s, cx + 3.8F * s, cy + 5.0F * s, cx - 3.8F * s, cy + 5.0F * s}, 0.75F, c, true);
            Gfx.line(cx - 2.0F * s, cy - 0.6F * s, cx + 2.0F * s, cy - 0.6F * s, 0.75F, c);
            Gfx.line(cx - 2.0F * s, cy + 1.6F * s, cx + 2.0F * s, cy + 1.6F * s, 0.75F, c);
            Gfx.line(cx - 2.0F * s, cy + 3.6F * s, cx + 0.6F * s, cy + 3.6F * s, 0.75F, c);
            break;
         case 7:
            float r = 5.0F * s;
            float a = 1.6F * s;
            Gfx.line(cx - r, cy, cx + r, cy, 0.75F, c);
            Gfx.line(cx, cy - r, cx, cy + r, 0.75F, c);
            Gfx.polyline(new float[]{cx - a, cy - r + a, cx, cy - r, cx + a, cy - r + a}, 0.75F, c, false);
            Gfx.polyline(new float[]{cx - a, cy + r - a, cx, cy + r, cx + a, cy + r - a}, 0.75F, c, false);
            Gfx.polyline(new float[]{cx - r + a, cy - a, cx - r, cy, cx - r + a, cy + a}, 0.75F, c, false);
            Gfx.polyline(new float[]{cx + r - a, cy - a, cx + r, cy, cx + r - a, cy + a}, 0.75F, c, false);
            break;
         case 8:
            star(cx + 0.8F * s, cy + 0.8F * s, 4.4F * s, c);
            star(cx - 3.6F * s, cy - 3.6F * s, 1.8F * s, c);
            break;
         case 9:
            Gfx.circleOutline(cx - 1.6F * s, cy - 2.0F * s, 2.2F * s, 0.75F, c);
            Gfx.arc(cx - 1.6F * s, cy + 5.6F * s, 4.2F * s, 0.75F, 180.0F, 360.0F, c);
            Gfx.circleOutline(cx + 2.9F * s, cy - 3.2F * s, 1.8F * s, 0.75F, c);
            Gfx.arc(cx + 2.9F * s, cy + 3.4F * s, 3.4F * s, 0.75F, 270.0F, 360.0F, c);
      }

   }

   private static void sword(float cx, float cy, float s, int c, boolean mirror) {
      float m = mirror ? -1.0F : 1.0F;
      float x0 = cx - 3.2F * s * m;
      float y0 = cy + 3.2F * s;
      float x1 = cx + 4.6F * s * m;
      float y1 = cy - 4.6F * s;
      Gfx.line(x0, y0, x1, y1, 0.75F, c);
      Gfx.line(x1, y1, x1 - 1.6F * s * m, y1, 0.75F, c);
      Gfx.line(x1, y1, x1, y1 + 1.6F * s, 0.75F, c);
      Gfx.line(x0 - 1.4F * s * m, y0 - 1.4F * s, x0 + 1.4F * s * m, y0 + 1.4F * s, 0.75F, c);
      Gfx.line(x0, y0, x0 - 1.8F * s * m, y0 + 1.8F * s, 0.75F, c);
   }

   private static void star(float cx, float cy, float r, int c) {
      float k = r * 0.26F;
      Gfx.polyline(new float[]{cx, cy - r, cx + k, cy - k, cx + r, cy, cx + k, cy + k, cx, cy + r, cx - k, cy + k, cx - r, cy, cx - k, cy - k}, 0.75F, c, true);
   }

   static Kind of(Category category) {
      Kind var10000;
      switch (category) {
         case COMBAT -> var10000 = Icons.Kind.SWORDS;
         case MOVEMENT -> var10000 = Icons.Kind.CHEVRONS_UP;
         case RENDER -> var10000 = Icons.Kind.SPARKLES;
         case PLAYER -> var10000 = Icons.Kind.PERSON;
         case HUD -> var10000 = Icons.Kind.MONITOR;
         case MISC -> var10000 = Icons.Kind.GEAR;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   static enum Kind {
      SWORDS,
      CHEVRONS_UP,
      SPARKLES,
      PERSON,
      GEAR,
      MONITOR,
      DOCUMENT,
      MOVE,
      SPARKLE,
      FRIENDS;

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{SWORDS, CHEVRONS_UP, SPARKLES, PERSON, GEAR, MONITOR, DOCUMENT, MOVE, SPARKLE, FRIENDS};
      }
   }
}
