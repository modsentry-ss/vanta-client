package vanta.visual;

import java.awt.Color;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import vanta.render.Gfx;
import vanta.render.Text;
import vanta.ui.Theme;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public final class Fx {
   public static final float RADIUS = 3.0F;
   public static final float PAD = 5.0F;
   public static final float GAP = 3.0F;
   public static final float ROW = 12.0F;
   public static final float TEXT_L = 8.0F;
   public static final float TEXT_M = 7.0F;
   public static final float TEXT_S = 6.0F;
   public static int PANEL = -871362283;
   public static int BORDER = 452984831;
   public static int HOVER = 318767103;
   public static int TRACK = 788529151;
   public static int TEXT = -855051;
   public static int TEXT_DIM = -6644056;
   private static float[] ribbonNx = new float[64];
   private static float[] ribbonNy = new float[64];
   private static final float[][] QUARTERS = new float[41][];
   private static float fanX;
   private static float fanY;
   private static int fanColor;
   private static float halfPixel;
   private static boolean contourStarted;
   private static float firstX;
   private static float firstY;
   private static float firstNx;
   private static float firstNy;
   private static float lastX;
   private static float lastY;
   private static float lastNx;
   private static float lastNy;
   private static final String[] NUMBERS = new String[1000];
   private static final int QUEUE_FLOATS = 9;
   private static String[] queuedText = new String[32];
   private static float[] queuedGeom = new float[288];
   private static int[] queuedColor = new int[32];
   private static int queued;
   private static final int INFINITY_POINTS = 40;
   private static final float[] INFINITY_X = new float[40];
   private static final float[] INFINITY_Y = new float[40];

   private Fx() {
   }

   public static void themeChanged(int hudBackground, int hudText) {
      PANEL = -872415232 | hudBackground & 16777215;
      BORDER = 436207616 | hudText & 16777215;
      HOVER = 301989888 | hudText & 16777215;
      TRACK = 771751936 | hudText & 16777215;
      TEXT = -16777216 | hudText;
      int dim = -16777216;

      for(int shift = 0; shift <= 16; shift += 8) {
         int a = hudText >> shift & 255;
         int b = hudBackground >> shift & 255;
         dim |= Math.round((float)a + (float)(b - a) * 0.4F) << shift;
      }

      TEXT_DIM = dim;
   }

   public static int accent() {
      return -16777216 | Theme.ACCENT;
   }

   public static int alpha(int argb, float alpha) {
      int a = Math.round((float)(argb >>> 24) * Math.clamp(alpha, 0.0F, 1.0F));
      return a << 24 | argb & 16777215;
   }

   public static int withAlpha(int rgb, int alpha) {
      return Math.clamp((long)alpha, 0, 255) << 24 | rgb & 16777215;
   }

   public static int mix(int a, int b, float t) {
      return Gfx.lerpColor(a, b, Math.clamp(t, 0.0F, 1.0F));
   }

   public static float smooth(float t) {
      t = Math.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   public static int lighten(int rgb, float amount) {
      return mix(rgb | -16777216, -1, amount) & 16777215 | rgb & -16777216;
   }

   public static int darken(int rgb, float amount) {
      return mix(rgb | -16777216, -16777216, amount) & 16777215 | rgb & -16777216;
   }

   public static int shiftHue(int rgb, float amount) {
      float[] hsb = Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, (float[])null);
      return Color.HSBtoRGB(hsb[0] + amount, hsb[1], hsb[2]) & 16777215;
   }

   public static int health(float fraction) {
      fraction = Math.clamp(fraction, 0.0F, 1.0F);
      return fraction > 0.5F ? mix(-16059, -12723068, (fraction - 0.5F) * 2.0F) : mix(-45730, -16059, fraction * 2.0F);
   }

   public static void glow(float cx, float cy, float radius, int color) {
      Gfx.glow(cx, cy, radius, color);
   }

   /** @deprecated */
   @Deprecated
   public static void orb(float cx, float cy, float radius, int color, float glowScale) {
      dot(cx, cy, radius, color);
   }

   public static void dot(float cx, float cy, float radius, int color) {
      if (radius > 0.0F) {
         round(cx - radius, cy - radius, radius * 2.0F, radius * 2.0F, radius, color);
      }

   }

   public static void ring(float cx, float cy, float radius, float width, IntUnaryOperator colorAt) {
      arc(cx, cy, radius, width, 0.0F, 360.0F, colorAt, 0);
   }

   /** @deprecated */
   @Deprecated
   public static void ring(float cx, float cy, float radius, float width, float glow, IntUnaryOperator colorAt) {
      arc(cx, cy, radius, width, 0.0F, 360.0F, colorAt, 0);
   }

   public static void arcRing(float cx, float cy, float radius, float width, float startDeg, float sweepDeg, IntUnaryOperator colorAt) {
      arc(cx, cy, radius, width, startDeg, sweepDeg, colorAt, 0);
   }

   public static void arcRing(float cx, float cy, float radius, float width, float startDeg, float sweepDeg, int color) {
      arc(cx, cy, radius, width, startDeg, sweepDeg, (IntUnaryOperator)null, color);
   }

   /** @deprecated */
   @Deprecated
   public static void arcRing(float cx, float cy, float radius, float width, float glow, float startDeg, float sweepDeg, IntUnaryOperator colorAt) {
      arc(cx, cy, radius, width, startDeg, sweepDeg, colorAt, 0);
   }

   private static void arc(float cx, float cy, float radius, float width, float startDeg, float sweepDeg, IntUnaryOperator colorAt, int solid) {
      if (!(radius <= 0.0F) && !(width <= 0.0F) && sweepDeg != 0.0F && (colorAt != null || solid >>> 24 != 0)) {
         float px = Gfx.pixel();
         float physical = radius / px;
         int segments = Math.clamp((long)((int)(Math.abs(sweepDeg) / 360.0F * Math.max(20.0F, (float)Math.sqrt((double)physical) * 4.5F))), 6, 96);
         float half = Math.max(width, px) * 0.5F;
         float inner = Math.max(half - px * 0.5F, 0.0F);
         float outer = half + px * 0.5F;
         double a0 = Math.toRadians((double)startDeg);
         float c0x = (float)Math.cos(a0);
         float s0 = (float)Math.sin(a0);
         int col0 = colorAt == null ? solid : colorAt.applyAsInt(Math.round((startDeg / 360.0F % 1.0F + 1.0F) % 1.0F * 1000.0F));

         for(int i = 0; i < segments; ++i) {
            float deg1 = startDeg + sweepDeg * (float)(i + 1) / (float)segments;
            double a1 = Math.toRadians((double)deg1);
            float c1x = (float)Math.cos(a1);
            float s1 = (float)Math.sin(a1);
            int col1 = colorAt == null ? solid : colorAt.applyAsInt(Math.round((deg1 / 360.0F % 1.0F + 1.0F) % 1.0F * 1000.0F));
            band(cx, cy, c0x, s0, c1x, s1, radius - inner, radius + inner, col0, col1, col0, col1);
            band(cx, cy, c0x, s0, c1x, s1, radius + inner, radius + outer, col0, col1, col0 & 16777215, col1 & 16777215);
            band(cx, cy, c0x, s0, c1x, s1, radius - outer, radius - inner, col0 & 16777215, col1 & 16777215, col0, col1);
            c0x = c1x;
            s0 = s1;
            col0 = col1;
         }

      }
   }

   private static void band(float cx, float cy, float c0, float s0, float c1, float s1, float r0, float r1, int in0, int in1, int out0, int out1) {
      Gfx.quadColors(cx + c0 * r0, cy + s0 * r0, in0, cx + c1 * r0, cy + s1 * r0, in1, cx + c1 * r1, cy + s1 * r1, out1, cx + c0 * r1, cy + s0 * r1, out0);
   }

   /** @deprecated */
   @Deprecated
   public static void ribbon(float[] xy, float[] widths, int[] colors, float glow) {
      ribbon(xy, widths, colors, Math.min(xy.length / 2, Math.min(widths.length, colors.length)));
   }

   public static void ribbon(float[] xy, float[] widths, int[] colors, int n) {
      if (n >= 2) {
         if (ribbonNx.length < n) {
            ribbonNx = new float[Math.max(n, ribbonNx.length * 2)];
            ribbonNy = new float[ribbonNx.length];
         }

         float px = Gfx.pixel();
         float[] nx = ribbonNx;
         float[] ny = ribbonNy;

         for(int i = 0; i < n; ++i) {
            int a = Math.max(0, i - 1);
            int b = Math.min(n - 1, i + 1);
            float dx = xy[b * 2] - xy[a * 2];
            float dy = xy[b * 2 + 1] - xy[a * 2 + 1];
            float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
            if (len < 1.0E-4F) {
               nx[i] = i > 0 ? nx[i - 1] : 0.0F;
               ny[i] = i > 0 ? ny[i - 1] : 1.0F;
            } else {
               nx[i] = -dy / len;
               ny[i] = dx / len;
            }
         }

         for(int i = 0; i + 1 < n; ++i) {
            int j = i + 1;
            float hi = Math.max(widths[i], px) * 0.5F;
            float hj = Math.max(widths[j], px) * 0.5F;
            float ii = Math.max(hi - px * 0.5F, 0.0F);
            float ij = Math.max(hj - px * 0.5F, 0.0F);
            float oi = hi + px * 0.5F;
            float oj = hj + px * 0.5F;
            int ci = colors[i];
            int cj = colors[j];
            strip(xy, nx, ny, i, j, -ii, -ij, ii, ij, ci, cj, ci, cj);
            strip(xy, nx, ny, i, j, ii, ij, oi, oj, ci, cj, ci & 16777215, cj & 16777215);
            strip(xy, nx, ny, i, j, -oi, -oj, -ii, -ij, ci & 16777215, cj & 16777215, ci, cj);
         }

      }
   }

   private static void strip(float[] xy, float[] nx, float[] ny, int i, int j, float a0, float a1, float b0, float b1, int ca0, int ca1, int cb0, int cb1) {
      float xi = xy[i * 2];
      float yi = xy[i * 2 + 1];
      float xj = xy[j * 2];
      float yj = xy[j * 2 + 1];
      Gfx.quadColors(xi + nx[i] * a0, yi + ny[i] * a0, ca0, xj + nx[j] * a1, yj + ny[j] * a1, ca1, xj + nx[j] * b1, yj + ny[j] * b1, cb1, xi + nx[i] * b0, yi + ny[i] * b0, cb0);
   }

   public static void line(float x0, float y0, float x1, float y1, float width, int color) {
      Gfx.line(x0, y0, x1, y1, Math.max(width, Gfx.pixel()), color);
   }

   public static void panel(float x, float y, float w, float h, float alpha) {
      card(x, y, w, h, 3.0F, alpha);
   }

   public static void panel(float x, float y, float w, float h, float radius, int fill, int border) {
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         round(x, y, w, h, radius, fill);
         if (border >>> 24 != 0) {
            Gfx.roundedOutline(x, y, w, h, radius, Gfx.pixel(), border);
         }

      }
   }

   public static void card(float x, float y, float w, float h, float radius, float alpha) {
      if (!(alpha <= 0.0F)) {
         round(x, y, w, h, radius, alpha(PANEL, alpha));
      }
   }

   /** @deprecated */
   @Deprecated
   public static void card(float x, float y, float w, float h, float radius, Palette palette, float alpha) {
      if (!(alpha <= 0.0F)) {
         panel(x, y, w, h, radius, alpha(PANEL, alpha), alpha(BORDER, alpha));
      }
   }

   /** @deprecated */
   @Deprecated
   public static void accentLine(float x, float y, float w, float thickness, Palette palette, float alpha) {
   }

   public static void hairline(float x, float y, float w, int color) {
      Gfx.rectWH(x, y, w, Gfx.pixel(), color);
   }

   public static void bar(float x, float y, float w, float h, float fraction, float ghost, Palette palette, int fill, float alpha) {
      float r = Math.min(h * 0.5F, 3.0F);
      round(x, y, w, h, r, alpha(TRACK, alpha));
      fraction = Math.clamp(fraction, 0.0F, 1.0F);
      ghost = Math.clamp(ghost, 0.0F, 1.0F);
      int color = alpha(fill != 0 ? fill | -16777216 : (palette != null ? palette.primary() : accent()), alpha);
      if (ghost > fraction) {
         round(x, y, Math.max(h, w * ghost), h, r, alpha(color, 0.35F));
      }

      if (fraction > 0.0F) {
         round(x, y, Math.max(h, w * fraction), h, r, color);
      }

   }

   public static void toggle(float x, float y, float w, float h, float on, int accent) {
      on = Math.clamp(on, 0.0F, 1.0F);
      float r = h * 0.5F;
      Gfx.roundedRect(x, y, w, h, r, mix(TRACK, accent, on));
      float knob = r - Math.max(1.0F, h * 0.15F);
      Gfx.circle(x + r + (w - h) * on, y + r, knob, -1);
   }

   public static void check(float x, float y, float size, float on, int accent) {
      on = Math.clamp(on, 0.0F, 1.0F);
      float r = Math.min(2.25F, size * 0.3F);
      Gfx.roundedRect(x, y, size, size, r, mix(TRACK, accent, on));
      if (on > 0.05F) {
         int mark = alpha(-15724267, on);
         float lw = Math.max(1.0F, size * 0.14F);
         Gfx.line(x + size * 0.24F, y + size * 0.52F, x + size * 0.42F, y + size * 0.7F, lw, mark);
         Gfx.line(x + size * 0.42F, y + size * 0.7F, x + size * 0.76F, y + size * 0.32F, lw, mark);
      }

   }

   public static void slider(float x, float centerY, float w, float fraction, int accent) {
      fraction = Math.clamp(fraction, 0.0F, 1.0F);
      float th = 2.0F;
      Gfx.roundedRect(x, centerY - th * 0.5F, w, th, th * 0.5F, TRACK);
      float end = x + w * fraction;
      if (end > x) {
         Gfx.roundedRect(x, centerY - th * 0.5F, end - x, th, th * 0.5F, accent);
      }

      Gfx.circle(end, centerY, 3.0F, -1);
   }

   private static float[] quarter(int n) {
      float[] table = QUARTERS[n];
      if (table == null) {
         table = new float[(n + 1) * 2];

         for(int i = 0; i <= n; ++i) {
            double a = (Math.PI / 2D) * (double)i / (double)n;
            table[i * 2] = (float)Math.cos(a);
            table[i * 2 + 1] = (float)Math.sin(a);
         }

         QUARTERS[n] = table;
      }

      return table;
   }

   private static int quarterSegments(float radius) {
      float physical = radius / Gfx.pixel();
      double step = (double)2.0F * Math.acos(Math.max((double)0.0F, (double)1.0F - 0.2 / (double)Math.max(0.2F, physical)));
      int full = Math.max(12, Math.min(160, (int)Math.ceil((Math.PI * 2D) / step)));
      return Math.clamp((long)((int)Math.ceil((double)((float)full / 4.0F))), 2, QUARTERS.length - 1);
   }

   public static void round(float x, float y, float w, float h, float radius, int color) {
      if (!(w <= 0.0F) && !(h <= 0.0F) && color >>> 24 != 0) {
         float r = Math.max(0.0F, Math.min(radius, Math.min(w, h) * 0.5F));
         if (r <= 0.0F) {
            Gfx.rectWH(x, y, w, h, color);
         } else {
            int n = quarterSegments(r);
            float[] q = quarter(n);
            contourStart(x + w * 0.5F, y + h * 0.5F, color);

            for(int i = 0; i <= n; ++i) {
               float c = q[i * 2];
               float sn = q[i * 2 + 1];
               contourPoint(x + r - c * r, y + r - sn * r, -c, -sn);
            }

            for(int i = 0; i <= n; ++i) {
               float c = q[i * 2];
               float sn = q[i * 2 + 1];
               contourPoint(x + w - r + sn * r, y + r - c * r, sn, -c);
            }

            for(int i = 0; i <= n; ++i) {
               float c = q[i * 2];
               float sn = q[i * 2 + 1];
               contourPoint(x + w - r + c * r, y + h - r + sn * r, c, sn);
            }

            for(int i = 0; i <= n; ++i) {
               float c = q[i * 2];
               float sn = q[i * 2 + 1];
               contourPoint(x + r - sn * r, y + h - r + c * r, -sn, c);
            }

            contourEnd();
         }
      }
   }

   public static void triangle(float x0, float y0, float x1, float y1, float x2, float y2, int color) {
      if (color >>> 24 != 0) {
         float sign = x0 * y1 - x1 * y0 + (x1 * y2 - x2 * y1) + (x2 * y0 - x0 * y2) >= 0.0F ? 1.0F : -1.0F;
         contourStart((x0 + x1 + x2) / 3.0F, (y0 + y1 + y2) / 3.0F, color);
         corner(x2, y2, x0, y0, x1, y1, sign);
         corner(x0, y0, x1, y1, x2, y2, sign);
         corner(x1, y1, x2, y2, x0, y0, sign);
         contourEnd();
      }
   }

   private static void corner(float ax, float ay, float bx, float by, float cx, float cy, float sign) {
      float dx0 = bx - ax;
      float dy0 = by - ay;
      float dx1 = cx - bx;
      float dy1 = cy - by;
      float l0 = (float)Math.sqrt((double)(dx0 * dx0 + dy0 * dy0));
      float l1 = (float)Math.sqrt((double)(dx1 * dx1 + dy1 * dy1));
      float n0x = l0 < 1.0E-6F ? 0.0F : dy0 / l0 * sign;
      float n0y = l0 < 1.0E-6F ? 0.0F : -dx0 / l0 * sign;
      float n1x = l1 < 1.0E-6F ? 0.0F : dy1 / l1 * sign;
      float n1y = l1 < 1.0E-6F ? 0.0F : -dx1 / l1 * sign;
      float mx = n0x + n1x;
      float my = n0y + n1y;
      float dot = mx * n0x + my * n0y;
      if (dot < 0.001F) {
         mx = n0x;
         my = n0y;
         dot = 1.0F;
      }

      float miter = Math.min(1.0F / dot, 4.0F);
      contourPoint(bx, by, mx * miter, my * miter);
   }

   private static void contourStart(float centerX, float centerY, int color) {
      fanX = centerX;
      fanY = centerY;
      fanColor = color;
      halfPixel = Gfx.pixel() * 0.5F;
      contourStarted = false;
   }

   private static void contourPoint(float x, float y, float nx, float ny) {
      if (!contourStarted) {
         contourStarted = true;
         firstX = x;
         firstY = y;
         firstNx = nx;
         firstNy = ny;
      } else {
         contourEdge(lastX, lastY, lastNx, lastNy, x, y, nx, ny);
      }

      lastX = x;
      lastY = y;
      lastNx = nx;
      lastNy = ny;
   }

   private static void contourEnd() {
      if (contourStarted) {
         contourEdge(lastX, lastY, lastNx, lastNy, firstX, firstY, firstNx, firstNy);
      }

   }

   private static void contourEdge(float ax, float ay, float anx, float any, float bx, float by, float bnx, float bny) {
      float half = halfPixel;
      int color = fanColor;
      int clear = color & 16777215;
      float aix = ax - anx * half;
      float aiy = ay - any * half;
      float bix = bx - bnx * half;
      float biy = by - bny * half;
      Gfx.triangleColors(fanX, fanY, color, aix, aiy, color, bix, biy, color);
      Gfx.quadColors(aix, aiy, color, bix, biy, color, bx + bnx * half, by + bny * half, clear, ax + anx * half, ay + any * half, clear);
   }

   public static void text(String text, float x, float baseline, float size, int color) {
      Gfx.flush();
      Text.draw(Gfx.graphics(), text, x, baseline, size, color);
   }

   public static void label(String text, float x, float centerY, float size, int color) {
      Gfx.flush();
      Text.draw(Gfx.graphics(), text, x, Text.centeredBaseline(text, size, centerY), size, color);
   }

   public static void labelShadowed(String text, float x, float centerY, float size, int color) {
      Gfx.flush();
      float baseline = Text.centeredBaseline(text, size, centerY);
      Text.drawShadowed(Gfx.graphics(), text, x, baseline, size, color, alpha(-16777216, (float)(color >>> 24) / 255.0F * 0.5F), size * 0.07F, size * 0.07F);
   }

   public static float width(String text, float size) {
      return Text.width(text, size);
   }

   public static String number(int value) {
      if (value >= 0 && value < NUMBERS.length) {
         String text = NUMBERS[value];
         if (text == null) {
            text = Integer.toString(value);
            NUMBERS[value] = text;
         }

         return text;
      } else {
         return Integer.toString(value);
      }
   }

   public static void queueLabel(String text, float x, float centerY, float size, int color) {
      if (text != null && !text.isEmpty()) {
         queueText(text, x, Text.centeredBaseline(text, size, centerY), size, color);
      }
   }

   public static void queueText(String text, float x, float baseline, float size, int color) {
      if (text != null && !text.isEmpty() && color >>> 24 != 0) {
         if (queued == queuedText.length) {
            int grown = queued * 2;
            queuedText = (String[])Arrays.copyOf(queuedText, grown);
            queuedGeom = Arrays.copyOf(queuedGeom, grown * 9);
            queuedColor = Arrays.copyOf(queuedColor, grown);
         }

         Matrix3x2f m = Gfx.pose();
         int o = queued * 9;
         queuedGeom[o] = x;
         queuedGeom[o + 1] = baseline;
         queuedGeom[o + 2] = size;
         queuedGeom[o + 3] = m.m00();
         queuedGeom[o + 4] = m.m01();
         queuedGeom[o + 5] = m.m10();
         queuedGeom[o + 6] = m.m11();
         queuedGeom[o + 7] = m.m20();
         queuedGeom[o + 8] = m.m21();
         queuedText[queued] = text;
         queuedColor[queued] = color;
         ++queued;
      }
   }

   public static void discardText() {
      for(int i = 0; i < queued; ++i) {
         queuedText[i] = null;
      }

      queued = 0;
   }

   public static void flushText() {
      if (queued != 0) {
         Gfx.flush();
         Matrix3x2fStack pose = Gfx.pose();
         int count = queued;
         queued = 0;

         for(int i = 0; i < count; ++i) {
            int o = i * 9;
            pose.pushMatrix();
            pose.set(queuedGeom[o + 3], queuedGeom[o + 4], queuedGeom[o + 5], queuedGeom[o + 6], queuedGeom[o + 7], queuedGeom[o + 8]);

            try {
               Text.draw(Gfx.graphics(), queuedText[i], queuedGeom[o], queuedGeom[o + 1], queuedGeom[o + 2], queuedColor[i]);
            } finally {
               pose.popMatrix();
               queuedText[i] = null;
            }
         }

      }
   }

   public static void heart(float cx, float cy, float size, int color) {
      float r = size * 0.27F;
      dot(cx - r * 0.95F, cy - size * 0.12F, r, color);
      dot(cx + r * 0.95F, cy - size * 0.12F, r, color);
      triangle(cx - r * 1.9F, cy - size * 0.06F, cx + r * 1.9F, cy - size * 0.06F, cx, cy + size * 0.42F, color);
   }

   public static void infinity(float cx, float cy, float size, float width, int color) {
      if (!(size <= 0.0F) && color >>> 24 != 0) {
         for(int i = 0; i < 40; ++i) {
            int j = (i + 1) % 40;
            Gfx.line(cx + INFINITY_X[i] * size, cy + INFINITY_Y[i] * size, cx + INFINITY_X[j] * size, cy + INFINITY_Y[j] * size, Math.max(width, Gfx.pixel()), color);
         }

      }
   }

   public static void shield(float cx, float cy, float size, int color) {
      float w = size * 0.42F;
      Gfx.fillConvex(new float[]{cx - w, cy - size * 0.4F, cx + w, cy - size * 0.4F, cx + w, cy + size * 0.02F, cx + w * 0.55F, cy + size * 0.3F, cx, cy + size * 0.46F, cx - w * 0.55F, cy + size * 0.3F, cx - w, cy + size * 0.02F}, color);
   }

   static {
      for(int i = 0; i < 40; ++i) {
         double t = (Math.PI * 2D) * (double)i / (double)40.0F;
         INFINITY_X[i] = (float)(Math.cos(t) * (double)0.5F);
         INFINITY_Y[i] = (float)(Math.sin(t) * Math.cos(t) * 0.42);
      }

   }
}
