package vanta.module.impl.render.esp;

import vanta.render.Gfx;

final class Halo {
   private static final float ARC_STEP = ((float)Math.PI / 8F);
   private static final int MAX_POINTS = 16;
   private static final float[] NX = new float[16];
   private static final float[] NY = new float[16];
   private static final int[] ORDER = new int[16];
   private static final int[] HULL = new int[32];

   private Halo() {
   }

   static void rect(float x0, float y0, float x1, float y1, float spread, int color, float[] scratch) {
      scratch[0] = x0;
      scratch[1] = y0;
      scratch[2] = x1;
      scratch[3] = y0;
      scratch[4] = x1;
      scratch[5] = y1;
      scratch[6] = x0;
      scratch[7] = y1;
      convex(scratch, 4, spread, color);
   }

   static void convex(float[] xy, int n, float spread, int color) {
      if (n >= 3 && n <= 16 && !(spread <= 0.0F) && color >>> 24 != 0) {
         float area = 0.0F;

         for(int i = 0; i < n; ++i) {
            int j = i + 1 == n ? 0 : i + 1;
            area += xy[i * 2] * xy[j * 2 + 1] - xy[j * 2] * xy[i * 2 + 1];
         }

         float sign = area >= 0.0F ? 1.0F : -1.0F;
         boolean any = false;

         for(int i = 0; i < n; ++i) {
            int j = i + 1 == n ? 0 : i + 1;
            float dx = xy[j * 2] - xy[i * 2];
            float dy = xy[j * 2 + 1] - xy[i * 2 + 1];
            float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
            if (len < 1.0E-4F) {
               NX[i] = i > 0 ? NX[i - 1] : 0.0F;
               NY[i] = i > 0 ? NY[i - 1] : -1.0F;
            } else {
               NX[i] = dy / len * sign;
               NY[i] = -dx / len * sign;
               any = true;
            }
         }

         if (any) {
            int clear = color & 16777215;

            for(int i = 0; i < n; ++i) {
               int j = i + 1 == n ? 0 : i + 1;
               float ax = xy[i * 2];
               float ay = xy[i * 2 + 1];
               float bx = xy[j * 2];
               float by = xy[j * 2 + 1];
               float nx = NX[i] * spread;
               float ny = NY[i] * spread;
               Gfx.quadColors(ax, ay, color, bx, by, color, bx + nx, by + ny, clear, ax + nx, ay + ny, clear);
               corner(bx, by, NX[i], NY[i], NX[j], NY[j], spread, color, clear);
            }

         }
      }
   }

   private static void corner(float x, float y, float ax, float ay, float bx, float by, float spread, int color, int clear) {
      float turn = (float)Math.atan2((double)(ax * by - ay * bx), (double)(ax * bx + ay * by));
      if (!(Math.abs(turn) < 0.001F)) {
         int steps = Math.max(1, (int)Math.ceil((double)(Math.abs(turn) / ((float)Math.PI / 8F))));
         float step = turn / (float)steps;
         float cs = (float)Math.cos((double)step);
         float sn = (float)Math.sin((double)step);
         float dx = ax;
         float dy = ay;

         for(int k = 0; k < steps; ++k) {
            float ex = k == steps - 1 ? bx : dx * cs - dy * sn;
            float ey = k == steps - 1 ? by : dx * sn + dy * cs;
            Gfx.triangleColors(x, y, color, x + dx * spread, y + dy * spread, clear, x + ex * spread, y + ey * spread, clear);
            dx = ex;
            dy = ey;
         }

      }
   }

   static int hull(float[] xy, int n, float[] out) {
      int[] order = ORDER;

      int j;
      for(int i = 0; i < n; order[j + 1] = i++) {
         j = i - 1;
         float x = xy[i * 2];

         for(float y = xy[i * 2 + 1]; j >= 0 && (xy[order[j] * 2] > x || xy[order[j] * 2] == x && xy[order[j] * 2 + 1] > y); --j) {
            order[j + 1] = order[j];
         }
      }

      int[] hull = HULL;
      j = 0;

      for(int i = 0; i < n; ++i) {
         while(j >= 2 && turn(xy, hull[j - 2], hull[j - 1], order[i]) <= 0.0F) {
            --j;
         }

         hull[j++] = order[i];
      }

      int i = n - 2;

      for(int lower = j + 1; i >= 0; --i) {
         while(j >= lower && turn(xy, hull[j - 2], hull[j - 1], order[i]) <= 0.0F) {
            --j;
         }

         hull[j++] = order[i];
      }

      i = Math.max(0, j - 1);

      for(int i = 0; i < i; ++i) {
         out[i * 2] = xy[hull[i] * 2];
         out[i * 2 + 1] = xy[hull[i] * 2 + 1];
      }

      return i;
   }

   private static float turn(float[] xy, int o, int a, int b) {
      return (xy[a * 2] - xy[o * 2]) * (xy[b * 2 + 1] - xy[o * 2 + 1]) - (xy[a * 2 + 1] - xy[o * 2 + 1]) * (xy[b * 2] - xy[o * 2]);
   }
}
