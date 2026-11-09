package vanta.visual.style;

import vanta.render.Gfx;
import vanta.render.Text;
import vanta.visual.Fx;

public final class TargetDraw {
   public static final String[] WEIGHTS = new String[]{"1 px", "2 px", "3 px"};
   public static final String[] HEALTH_COLORS = new String[]{"Stepped", "Smooth"};
   public static final String[] LABELS = new String[]{"Off", "Shadow", "Box"};
   public static final int LABEL_OFF = 0;
   public static final int LABEL_SHADOW = 1;
   public static final int LABEL_BOX = 2;
   public static final int GREEN = -11546018;
   public static final int YELLOW = -1649855;
   public static final int ORANGE = -1013204;
   public static final int RED = -1755077;
   public static final int BLACK = -16777216;
   public static final int BOX = -1946157056;
   public static final float TEXT = 5.5F;

   private TargetDraw() {
   }

   public static float px() {
      return Gfx.pixel();
   }

   public static float snap(float v) {
      float p = Gfx.pixel();
      return (float)Math.round(v / p) * p;
   }

   public static float weight(int index) {
      return (float)(Math.clamp((long)index, 0, WEIGHTS.length - 1) + 1) * Gfx.pixel();
   }

   public static float alphaOf(int color) {
      return (float)(color >>> 24) / 255.0F;
   }

   public static int outlineFor(int color) {
      return color & -16777216;
   }

   public static int health(float fraction, boolean smooth) {
      float f = Math.clamp(fraction, 0.0F, 1.0F);
      if (!smooth) {
         return f > 0.75F ? -11546018 : (f > 0.5F ? -1649855 : (f > 0.25F ? -1013204 : -1755077));
      } else if (f >= 0.75F) {
         return -11546018;
      } else if (f >= 0.5F) {
         return Fx.mix(-1649855, -11546018, (f - 0.5F) * 4.0F);
      } else {
         return f >= 0.25F ? Fx.mix(-1013204, -1649855, (f - 0.25F) * 4.0F) : Fx.mix(-1755077, -1013204, f * 4.0F);
      }
   }

   public static void rect(float x0, float y0, float x1, float y1, int color) {
      if (color >>> 24 != 0) {
         float p = Gfx.pixel();
         float l = (float)Math.round(Math.min(x0, x1) / p) * p;
         float r = (float)Math.round(Math.max(x0, x1) / p) * p;
         float t = (float)Math.round(Math.min(y0, y1) / p) * p;
         float b = (float)Math.round(Math.max(y0, y1) / p) * p;
         if (r > l && b > t) {
            Gfx.rect(l, t, r, b, color);
         }

      }
   }

   private static void frame(float l, float t, float r, float b, float w, int color) {
      if (!(r - l <= w * 2.0F) && !(b - t <= w * 2.0F)) {
         Gfx.rect(l, t, r, t + w, color);
         Gfx.rect(l, b - w, r, b, color);
         Gfx.rect(l, t + w, l + w, b - w, color);
         Gfx.rect(r - w, t + w, r, b - w, color);
      } else {
         Gfx.rect(l, t, r, b, color);
      }
   }

   private static void brackets(float l, float t, float r, float b, float w, float arm, int color) {
      float ax = Math.min(arm, (r - l) * 0.5F);
      float ay = Math.min(arm, (b - t) * 0.5F);
      Gfx.rect(l, t, l + ax, t + w, color);
      Gfx.rect(l, t + w, l + w, t + ay, color);
      Gfx.rect(r - ax, t, r, t + w, color);
      Gfx.rect(r - w, t + w, r, t + ay, color);
      Gfx.rect(l, b - w, l + ax, b, color);
      Gfx.rect(l, b - ay, l + w, b - w, color);
      Gfx.rect(r - ax, b - w, r, b, color);
      Gfx.rect(r - w, b - ay, r, b - w, color);
   }

   public static void box(float x0, float y0, float x1, float y1, float w, float edge, boolean corners, int color, int fill) {
      float p = Gfx.pixel();
      float l = (float)Math.round(x0 / p) * p;
      float t = (float)Math.round(y0 / p) * p;
      float r = (float)Math.round(x1 / p) * p;
      float b = (float)Math.round(y1 / p) * p;
      float least = w * 2.0F + p * 2.0F;
      if (r - l < least) {
         float mid = (float)Math.round((l + r) * 0.5F / p) * p;
         l = mid - (float)Math.round(least * 0.5F / p) * p;
         r = l + least;
      }

      if (b - t < least) {
         float mid = (float)Math.round((t + b) * 0.5F / p) * p;
         t = mid - (float)Math.round(least * 0.5F / p) * p;
         b = t + least;
      }

      if (fill >>> 24 != 0) {
         Gfx.rect(l + w, t + w, r - w, b - w, fill);
      }

      int black = outlineFor(color);
      float e = edge > 0.0F ? Math.max(p, (float)Math.round(edge / p) * p) : 0.0F;
      if (corners) {
         float arm = Math.max((float)Math.round(Math.min(r - l, b - t) * 0.25F / p) * p, w + p * 3.0F);
         if (e > 0.0F) {
            brackets(l - e, t - e, r + e, b + e, w + e * 2.0F, arm + e * 2.0F, black);
         }

         brackets(l, t, r, b, w, arm, color);
      } else {
         if (e > 0.0F) {
            frame(l - e, t - e, r + e, b + e, w + e * 2.0F, black);
         }

         frame(l, t, r, b, w, color);
      }

   }

   public static void healthBar(float x0, float y0, float x1, float y1, int side, float gap, float thick, float fraction, float trail, int color, float alpha, float[] out) {
      float p = Gfx.pixel();
      float l = (float)Math.round(x0 / p) * p;
      float t = (float)Math.round(y0 / p) * p;
      float r = (float)Math.round(x1 / p) * p;
      float b = (float)Math.round(y1 / p) * p;
      float g = (float)Math.round(gap / p) * p;
      float th = Math.max(p, (float)Math.round(thick / p) * p);
      boolean vertical = side == 0 || side == 1;
      float fl;
      float ft;
      float fr;
      float fb;
      switch (side) {
         case 0:
            fr = l - g - p;
            fl = fr - th;
            ft = t;
            fb = b;
            break;
         case 1:
            fl = r + g + p;
            fr = fl + th;
            ft = t;
            fb = b;
            break;
         case 2:
            fb = t - g - p;
            ft = fb - th;
            fl = l;
            fr = r;
            break;
         default:
            ft = b + g + p;
            fb = ft + th;
            fl = l;
            fr = r;
      }

      out[0] = fl - p;
      out[1] = ft - p;
      out[2] = fr + p;
      out[3] = fb + p;
      int black = Fx.alpha(-16777216, alpha);
      frame(fl - p, ft - p, fr + p, fb + p, p, black);
      float length = vertical ? fb - ft : fr - fl;
      float f = Math.clamp(fraction, 0.0F, 1.0F);
      float k = Math.clamp(Math.max(trail, f), 0.0F, 1.0F);
      float filled = (float)Math.round(length * f / p) * p;
      float trailed = (float)Math.round(length * k / p) * p;
      if (f > 0.0F && filled < p) {
         filled = p;
      }

      trailed = Math.max(trailed, filled);
      int track = Fx.alpha(-16777216, 0.55F * alpha);
      int lost = Fx.alpha(-1, 0.45F * alpha);
      int fill = Fx.alpha(color, alpha);
      if (vertical) {
         Gfx.rect(fl, ft, fr, fb - trailed, track);
         if (trailed > filled) {
            Gfx.rect(fl, fb - trailed, fr, fb - filled, lost);
         }

         if (filled > 0.0F) {
            Gfx.rect(fl, fb - filled, fr, fb, fill);
         }
      } else {
         Gfx.rect(fl + trailed, ft, fr, fb, track);
         if (trailed > filled) {
            Gfx.rect(fl + filled, ft, fl + trailed, fb, lost);
         }

         if (filled > 0.0F) {
            Gfx.rect(fl, ft, fl + filled, fb, fill);
         }
      }

   }

   public static void line(float x0, float y0, float x1, float y1, float w, boolean outline, int color) {
      if (outline) {
         Gfx.line(x0, y0, x1, y1, w + Gfx.pixel() * 2.0F, outlineFor(color));
      }

      Gfx.line(x0, y0, x1, y1, w, color);
   }

   public static void triangle(float ax, float ay, float bx, float by, float cx, float cy, boolean outline, int color) {
      if (color >>> 24 != 0) {
         if (outline) {
            float a = dist(bx, by, cx, cy);
            float b = dist(ax, ay, cx, cy);
            float c = dist(ax, ay, bx, by);
            float perimeter = a + b + c;
            if (perimeter > 1.0E-4F) {
               float ix = (a * ax + b * bx + c * cx) / perimeter;
               float iy = (a * ay + b * by + c * cy) / perimeter;
               float area = Math.abs((bx - ax) * (cy - ay) - (cx - ax) * (by - ay)) * 0.5F;
               float inradius = area * 2.0F / perimeter;
               if (inradius > 1.0E-4F) {
                  float s = (inradius + Gfx.pixel()) / inradius;
                  Gfx.fillConvex(new float[]{ix + (ax - ix) * s, iy + (ay - iy) * s, ix + (bx - ix) * s, iy + (by - iy) * s, ix + (cx - ix) * s, iy + (cy - iy) * s}, outlineFor(color));
               }
            }
         }

         Gfx.fillConvex(new float[]{ax, ay, bx, by, cx, cy}, color);
      }
   }

   private static float dist(float ax, float ay, float bx, float by) {
      float dx = bx - ax;
      float dy = by - ay;
      return (float)Math.sqrt((double)(dx * dx + dy * dy));
   }

   public static void square(float x, float y, float size, boolean outline, int color) {
      float p = Gfx.pixel();
      float s = Math.max(p, (float)Math.round(size / p) * p);
      float l = (float)Math.round((x - s * 0.5F) / p) * p;
      float t = (float)Math.round((y - s * 0.5F) / p) * p;
      if (outline) {
         Gfx.rect(l - p, t - p, l + s + p, t + s + p, outlineFor(color));
      }

      Gfx.rect(l, t, l + s, t + s, color);
   }

   public static float textWidth(String text) {
      return Fx.width(text, 5.5F);
   }

   public static void text(String text, float x, float centerY, int color, boolean shadow) {
      if (text != null && !text.isEmpty() && color >>> 24 != 0) {
         float p = Gfx.pixel();
         float baseline = (float)Math.round(Text.centeredBaseline(text, 5.5F, centerY) / p) * p;
         float left = (float)Math.round(x / p) * p;
         if (shadow) {
            Fx.queueText(text, left + p, baseline + p, 5.5F, Fx.alpha(-16777216, alphaOf(color) * 0.85F));
         }

         Fx.queueText(text, left, baseline, 5.5F, color);
      }
   }

   public static void label(String text, float x, float centerY, int color, int background, float pad) {
      if (background != 0) {
         if (background == 2) {
            float w = textWidth(text);
            float h = 3.96F;
            rect(x - pad, centerY - h * 0.5F - pad, x + w + pad, centerY + h * 0.5F + pad, Fx.alpha(-1946157056, alphaOf(color)));
         }

         text(text, x, centerY, color, background == 1);
      }
   }
}
