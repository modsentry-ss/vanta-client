package vanta.visual.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vanta.render.Gfx;
import vanta.render.Opacity;
import vanta.render.Text;
import vanta.render.font.Font;
import vanta.render.font.Fonts;
import vanta.render.state.Submit;
import vanta.render.state.TextState;
import vanta.ui.Theme;
import vanta.visual.Fx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;

public final class HudKit {
   public static final float RADIUS = 7.0F;
   public static final float RADIUS_SMALL = 3.5F;
   public static final float PAD = 6.0F;
   public static final float TITLE = 8.5F;
   public static final float BODY = 7.5F;
   public static final float SMALL = 6.5F;
   public static final float CARD_ALPHA = 0.9F;
   public static boolean LIGHT;
   public static int FILL_TOP = -434430944;
   public static int FILL_BOTTOM = -435286254;
   public static int RIM_TOP = 703788021;
   public static int RIM_BOTTOM = 267580405;
   public static int TEXT = -855051;
   public static int MUTED = -5921110;
   public static int FAINT = -9342088;
   public static int TRACK = 569570293;
   public static int WELL = 250803189;
   public static int ACCENT = -6301454;
   public static int ACCENT_DEEP = -8408336;
   public static int ACCENT_BRIGHT = -3741192;
   public static int ON_ACCENT = -15724528;
   public static int SHADOW_AMBIENT = 1493172224;
   public static int SHADOW_CONTACT = 1191182336;
   public static final int DANGER = -45730;
   private static int syncedBack = -1;
   private static int syncedText = -1;
   private static int syncedAccent = -1;
   private static int themeBack = -15329510;
   private static int themeFillTop = -15000544;
   private static int themeFillBottom = -15855854;
   private static int themeText = -855051;
   private static int themeAccent = -6301454;
   private static int themeAccentDeep = -8408336;
   private static int themeAccentBright = -3741192;
   private static int customAccent = -1;
   private static boolean customAccentLight;
   private static int customDeep;
   private static int customBright;
   public static final int ROUNDED = 0;
   public static final int SQUARED = 1;
   public static final int CUT = 2;
   public static final int BG_THEME = 0;
   public static final int BG_FLAT = 1;
   public static final int BG_CUSTOM = 2;
   public static final int BG_GRADIENT = 3;
   public static final int BORDER_HAIRLINE = 0;
   public static final int BORDER_NONE = 1;
   public static final int BORDER_SOLID = 2;
   public static final int BORDER_ACCENT = 3;
   public static final int SHADOW_SOFT = 0;
   public static final int SHADOW_HARD = 1;
   public static final int SHADOW_GLOW = 2;
   public static final int SHADOW_NONE = 3;
   public static final int BAR_NONE = 0;
   public static final int BAR_LEFT = 1;
   public static final int BAR_TOP = 2;
   public static final int BAR_BOTTOM = 3;
   public static final int BAR_RIGHT = 4;
   public static final Look DEFAULT = new Look();
   private static Look look;
   private static float elementScale;
   private static final float[][] QUARTERS;
   private static final float[] PX;
   private static final float[] PY;
   private static final float[] NX;
   private static final float[] NY;
   private static final float[] IX;
   private static final float[] IY;
   private static int points;
   private static boolean gradientX;
   private static float g0;
   private static float gSpan;
   private static int c0;
   private static int c1;
   public static final Font SEMIBOLD;
   public static final Font MEDIUM;
   private static final float CENTER_SEMIBOLD;
   private static final float CENTER_MEDIUM;
   private static final Map<String, Layout> SEMIBOLD_LAYOUTS;
   private static final Map<String, Layout> MEDIUM_LAYOUTS;
   private static final List<Line> LINES;
   private static int lines;
   private static final List<TextState.Quad> SEMIBOLD_QUADS;
   private static final List<TextState.Quad> MEDIUM_QUADS;

   private HudKit() {
   }

   public static void sync() {
      sync(DEFAULT);
   }

   public static void sync(Look look) {
      int back = Theme.get(Theme.Slot.HUD_BACKGROUND) & 16777215;
      int text = Theme.get(Theme.Slot.HUD_TEXT) & 16777215;
      int accent = Theme.accentRgb() & 16777215;
      if (back != syncedBack || text != syncedText || accent != syncedAccent) {
         syncedBack = back;
         syncedText = text;
         syncedAccent = accent;
         boolean light = luminance(back) > 0.5F;
         LIGHT = light;
         int backOpaque = -16777216 | back;
         int textOpaque = -16777216 | text;
         themeBack = backOpaque;
         themeText = textOpaque;
         if (light) {
            themeFillTop = Fx.mix(backOpaque, -1, 0.65F);
            themeFillBottom = backOpaque;
            SHADOW_AMBIENT = 637534208;
            SHADOW_CONTACT = 469762048;
         } else {
            themeFillTop = Fx.mix(backOpaque, textOpaque, 0.06F);
            themeFillBottom = Fx.mix(backOpaque, -16777216, 0.12F);
            SHADOW_AMBIENT = 1493172224;
            SHADOW_CONTACT = 1191182336;
         }

         themeAccent = -16777216 | accent;
         themeAccentDeep = -16777216 | vivid(accent, 0.035F, 1.18F, light ? 0.86F : 0.9F);
         themeAccentBright = -16777216 | vivid(accent, -0.02F, 0.92F, light ? 1.12F : 1.16F);
         ON_ACCENT = Theme.ON_ACCENT;
      }

      HudKit.look = look == null ? DEFAULT : look;
      apply(HudKit.look);
   }

   private static void apply(Look look) {
      boolean light = LIGHT;
      float opacity = Math.clamp(look.opacity, 0.0F, 1.0F);
      float alpha = opacity <= 0.0F ? 0.0F : Math.min(1.0F, opacity + (light ? 0.04F : 0.0F));
      int back;
      switch (look.background) {
         case 1:
            FILL_TOP = Fx.alpha(themeBack, alpha);
            FILL_BOTTOM = FILL_TOP;
            back = themeBack;
            break;
         case 2:
            int c = -16777216 | look.backgroundFrom;
            FILL_TOP = Fx.alpha(Fx.mix(c, -1, 0.05F), alpha);
            FILL_BOTTOM = Fx.alpha(Fx.mix(c, -16777216, 0.12F), alpha);
            back = c;
            break;
         case 3:
            FILL_TOP = Fx.alpha(-16777216 | look.backgroundFrom, alpha);
            FILL_BOTTOM = Fx.alpha(-16777216 | look.backgroundTo, alpha);
            back = Fx.mix(-16777216 | look.backgroundFrom, -16777216 | look.backgroundTo, 0.5F);
            break;
         default:
            FILL_TOP = Fx.alpha(themeFillTop, alpha);
            FILL_BOTTOM = Fx.alpha(themeFillBottom, alpha);
            back = themeBack;
      }

      int text = look.text >= 0 ? -16777216 | look.text : themeText;
      int rgb = text & 16777215;
      TEXT = text;
      MUTED = Fx.mix(text, back, 0.34F);
      FAINT = Fx.mix(text, back, 0.56F);
      TRACK = (light ? 469762048 : 553648128) | rgb;
      WELL = (light ? 268435456 : 234881024) | rgb;
      RIM_TOP = (light ? 436207616 : 687865856) | rgb;
      RIM_BOTTOM = (light ? 721420288 : 251658240) | rgb;
      if (look.accent >= 0) {
         int accent = look.accent & 16777215;
         if (accent != customAccent || light != customAccentLight) {
            customAccent = accent;
            customAccentLight = light;
            customDeep = -16777216 | vivid(accent, 0.035F, 1.18F, light ? 0.86F : 0.9F);
            customBright = -16777216 | vivid(accent, -0.02F, 0.92F, light ? 1.12F : 1.16F);
         }

         ACCENT = -16777216 | accent;
         ACCENT_DEEP = customDeep;
         ACCENT_BRIGHT = customBright;
      } else {
         ACCENT = themeAccent;
         ACCENT_DEEP = themeAccentDeep;
         ACCENT_BRIGHT = themeAccentBright;
      }

   }

   public static Look look() {
      return look;
   }

   public static boolean squared() {
      return look.shape == 1;
   }

   public static float corner(float radius) {
      return look.shape == 1 ? 0.0F : radius * look.corners;
   }

   private static int vivid(int rgb, float hue, float saturation, float brightness) {
      float[] hsb = Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, (float[])null);
      float s = Math.clamp(hsb[1] * saturation, 0.0F, 1.0F);
      float b = Math.clamp(hsb[2] * brightness, 0.0F, 1.0F);
      if (brightness > 1.0F && hsb[2] * brightness > 1.0F) {
         s *= Math.max(0.0F, 1.0F - (hsb[2] * brightness - 1.0F) * 2.0F);
      }

      return Color.HSBtoRGB(hsb[0] + hue, s, b) & 16777215;
   }

   private static float luminance(int rgb) {
      return (0.2126F * (float)(rgb >> 16 & 255) + 0.7152F * (float)(rgb >> 8 & 255) + 0.0722F * (float)(rgb & 255)) / 255.0F;
   }

   public static int accentAt(float t, float alpha) {
      return Fx.alpha(Fx.mix(ACCENT_DEEP, ACCENT_BRIGHT, t), alpha);
   }

   public static int deeper(int color) {
      int r = color >> 16 & 255;
      int g = color >> 8 & 255;
      int b = color & 255;
      float mean = (float)(r + g + b) / 3.0F;
      return color & -16777216 | deep(r, mean) << 16 | deep(g, mean) << 8 | deep(b, mean);
   }

   private static int deep(int channel, float mean) {
      return Math.clamp((long)Math.round((((float)channel - mean) * 1.2F + mean) * 0.84F), 0, 255);
   }

   public static int brighter(int color) {
      return color & -16777216 | Fx.mix(color | -16777216, -1, 0.2F) & 16777215;
   }

   public static void shadow(float x, float y, float w, float h, float radius, float s, float alpha) {
      elementScale = s;
      if (!(alpha <= 0.01F) && !(w <= 0.0F) && !(h <= 0.0F)) {
         Look look = HudKit.look;
         float k = alpha * Math.clamp(look.shadowStrength, 0.0F, 2.0F);
         float behind = Math.min(1.0F, Math.clamp(look.opacity, 0.0F, 1.0F) / 0.9F);
         float r = shadowCorner(radius, w, h);
         switch (look.shadow) {
            case 1:
               float p = Gfx.pixel();
               float o = Math.max(p, (float)Math.round(1.5F * s / p) * p);
               fill(x + o, y + o, w, h, radius, Fx.alpha(-16777216, Math.min(1.0F, (LIGHT ? 0.22F : 0.5F) * k * behind)));
               break;
            case 2:
               Gfx.shadow(x, y, w, h, r, 7.0F * s, Fx.alpha(ACCENT, Math.min(1.0F, 0.42F * k)));
            case 3:
               break;
            default:
               float soft = k * behind;
               Gfx.shadow(x, y + 2.5F * s, w, h, r, 9.0F * s, Fx.alpha(SHADOW_AMBIENT, Math.min(1.0F, soft)));
               Gfx.shadow(x, y + 0.7F * s, w, h, r, 2.2F * s, Fx.alpha(SHADOW_CONTACT, Math.min(1.0F, soft)));
         }

      }
   }

   private static float shadowCorner(float radius, float w, float h) {
      float r = Math.min(corner(radius), Math.min(w, h) * 0.5F);
      return look.shape == 2 ? r * 0.45F : r;
   }

   public static void halo(float x, float y, float w, float h, float radius, float softness, int color) {
      if (color >>> 24 != 0 && w > 0.0F && h > 0.0F) {
         Gfx.shadow(x, y, w, h, shadowCorner(radius, w, h), softness, color);
      }

   }

   public static void card(float x, float y, float w, float h, float radius, float alpha) {
      if (!(alpha <= 0.0F)) {
         Look look = HudKit.look;
         if (look.background == 3) {
            fillH(x, y, w, h, radius, Fx.alpha(FILL_TOP, alpha), Fx.alpha(FILL_BOTTOM, alpha));
         } else {
            fillV(x, y, w, h, radius, Fx.alpha(FILL_TOP, alpha), Fx.alpha(FILL_BOTTOM, alpha));
         }

         if (look.accentBar != 0) {
            accentBar(x, y, w, h, radius, alpha);
         }

         switch (look.border) {
            case 1 -> { }
            case 2 -> ring(x, y, w, h, radius, (float)look.borderWidth * Gfx.pixel(), Fx.alpha(-16777216 | look.borderColor, alpha));
            case 3 -> ring(x, y, w, h, radius, (float)look.borderWidth * Gfx.pixel(), Fx.alpha(ACCENT, alpha));
            default -> rim(x, y, w, h, radius, Fx.alpha(RIM_TOP, alpha), Fx.alpha(RIM_BOTTOM, alpha));
         }

      }
   }

   private static void accentBar(float x, float y, float w, float h, float radius, float alpha) {
      float p = Gfx.pixel();
      float t = Math.max(p, (float)Math.round(2.0F * elementScale / p) * p);
      float r = Math.min(corner(radius), Math.min(w, h) * 0.5F);
      float across = (float)Math.round(r * 0.25F / p) * p;
      float along = r * 0.6F;
      float round = look.shape == 0 ? t * 0.5F : 0.0F;
      int from = Fx.alpha(ACCENT_BRIGHT, alpha);
      int to = Fx.alpha(ACCENT_DEEP, alpha);
      switch (look.accentBar) {
         case 1:
            fillV(x + across, y + along, t, h - along * 2.0F, round, from, to);
            break;
         case 2:
            fillH(x + along, y + across, w - along * 2.0F, t, round, to, from);
            break;
         case 3:
         default:
            fillH(x + along, y + h - across - t, w - along * 2.0F, t, round, to, from);
            break;
         case 4:
            fillV(x + w - across - t, y + along, t, h - along * 2.0F, round, from, to);
      }

   }

   public static void ring(float x, float y, float w, float h, float radius, float width, int color) {
      if (!(w <= 0.0F) && !(h <= 0.0F) && !(width <= 0.0F) && color >>> 24 != 0) {
         float r = Math.max(0.0F, Math.min(corner(radius), Math.min(w, h) * 0.5F));
         contour(x, y, w, h, r);
         float half = Gfx.pixel() * 0.5F;
         int clear = color & 16777215;
         float in = Math.min(width, Math.min(w, h) * 0.5F);
         float along = Math.min(r, in);
         float past = Math.max(0.0F, in - r);
         int per = points / 4;

         for(int i = 0; i < points; ++i) {
            int k = i / per;
            IX[i] = PX[i] - NX[i] * along + (k != 0 && k != 3 ? -past : past);
            IY[i] = PY[i] - NY[i] * along + (k < 2 ? past : -past);
         }

         for(int i = 0; i < points; ++i) {
            int j = i + 1 == points ? 0 : i + 1;
            float aix = PX[i] - NX[i] * half;
            float aiy = PY[i] - NY[i] * half;
            float bix = PX[j] - NX[j] * half;
            float biy = PY[j] - NY[j] * half;
            Gfx.quadColors(aix, aiy, color, bix, biy, color, IX[j], IY[j], color, IX[i], IY[i], color);
            Gfx.quadColors(aix, aiy, color, bix, biy, color, PX[j] + NX[j] * half, PY[j] + NY[j] * half, clear, PX[i] + NX[i] * half, PY[i] + NY[i] * half, clear);
         }

      }
   }

   public static void pill(float x, float y, float w, float h, float alpha) {
      card(x, y, w, h, h * 0.5F, alpha);
   }

   public static void raised(float x, float y, float w, float h, float radius, float s, float alpha) {
      elementScale = s;
      shadow(x, y, w, h, radius, s, alpha);
      card(x, y, w, h, radius, alpha);
   }

   public static void well(float x, float y, float w, float h, float radius, float strength) {
      int alpha = Math.clamp((long)Math.round((float)(WELL >>> 24) * strength), 0, 255);
      fill(x, y, w, h, radius, alpha << 24 | WELL & 16777215);
   }

   public static void bar(float x, float y, float w, float h, float fraction, float trail, int from, int to, int trailColor, float alpha) {
      if (!(w <= 0.0F) && !(h <= 0.0F) && !(alpha <= 0.0F)) {
         float r = h * 0.5F;
         fill(x, y, w, h, r, Fx.alpha(TRACK, alpha));
         fraction = Math.clamp(fraction, 0.0F, 1.0F);
         trail = Math.clamp(trail, 0.0F, 1.0F);
         if (trail > fraction + 0.002F) {
            fill(x, y, Math.max(h, w * trail), h, r, Fx.alpha(trailColor, alpha));
         }

         if (fraction > 0.0F) {
            fillH(x, y, Math.max(h, w * fraction), h, r, Fx.alpha(from, alpha), Fx.alpha(to, alpha));
         }

      }
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

   private static void point(float x, float y, float nx, float ny) {
      PX[points] = x;
      PY[points] = y;
      NX[points] = nx;
      NY[points] = ny;
      ++points;
   }

   private static void contour(float x, float y, float w, float h, float r) {
      points = 0;
      if (r <= 0.0F) {
         point(x, y, -1.0F, -1.0F);
         point(x + w, y, 1.0F, -1.0F);
         point(x + w, y + h, 1.0F, 1.0F);
         point(x, y + h, -1.0F, 1.0F);
      } else {
         int n = look.shape == 2 ? 1 : quarterSegments(r);
         float[] q = quarter(n);

         for(int i = 0; i <= n; ++i) {
            float c = q[i * 2];
            float sn = q[i * 2 + 1];
            point(x + r - c * r, y + r - sn * r, -c, -sn);
         }

         for(int i = 0; i <= n; ++i) {
            float c = q[i * 2];
            float sn = q[i * 2 + 1];
            point(x + w - r + sn * r, y + r - c * r, sn, -c);
         }

         for(int i = 0; i <= n; ++i) {
            float c = q[i * 2];
            float sn = q[i * 2 + 1];
            point(x + w - r + c * r, y + h - r + sn * r, c, sn);
         }

         for(int i = 0; i <= n; ++i) {
            float c = q[i * 2];
            float sn = q[i * 2 + 1];
            point(x + r - sn * r, y + h - r + c * r, -sn, c);
         }

      }
   }

   private static int colorAt(float x, float y) {
      if (c0 == c1) {
         return c0;
      } else {
         float t = ((gradientX ? x : y) - g0) / gSpan;
         return Gfx.lerpColor(c0, c1, Math.clamp(t, 0.0F, 1.0F));
      }
   }

   public static void fill(float x, float y, float w, float h, float radius, int color) {
      fillV(x, y, w, h, radius, color, color);
   }

   public static void fillV(float x, float y, float w, float h, float radius, int top, int bottom) {
      gradientX = false;
      g0 = y;
      gSpan = Math.max(h, 1.0E-4F);
      fillContour(x, y, w, h, radius, top, bottom);
   }

   public static void fillH(float x, float y, float w, float h, float radius, int left, int right) {
      gradientX = true;
      g0 = x;
      gSpan = Math.max(w, 1.0E-4F);
      fillContour(x, y, w, h, radius, left, right);
   }

   private static void fillContour(float x, float y, float w, float h, float radius, int from, int to) {
      if (!(w <= 0.0F) && !(h <= 0.0F) && (from | to) >>> 24 != 0) {
         c0 = from;
         c1 = to;
         contour(x, y, w, h, Math.max(0.0F, Math.min(corner(radius), Math.min(w, h) * 0.5F)));
         float half = Gfx.pixel() * 0.5F;
         float cx = x + w * 0.5F;
         float cy = y + h * 0.5F;
         int center = colorAt(cx, cy);

         for(int i = 0; i < points; ++i) {
            int j = i + 1 == points ? 0 : i + 1;
            float aix = PX[i] - NX[i] * half;
            float aiy = PY[i] - NY[i] * half;
            float bix = PX[j] - NX[j] * half;
            float biy = PY[j] - NY[j] * half;
            float aox = PX[i] + NX[i] * half;
            float aoy = PY[i] + NY[i] * half;
            float box = PX[j] + NX[j] * half;
            float boy = PY[j] + NY[j] * half;
            int ai = colorAt(aix, aiy);
            int bi = colorAt(bix, biy);
            Gfx.triangleColors(cx, cy, center, aix, aiy, ai, bix, biy, bi);
            Gfx.quadColors(aix, aiy, ai, bix, biy, bi, box, boy, colorAt(box, boy) & 16777215, aox, aoy, colorAt(aox, aoy) & 16777215);
         }

      }
   }

   public static void rim(float x, float y, float w, float h, float radius, int top, int bottom) {
      if (!(w <= 0.0F) && !(h <= 0.0F) && (top | bottom) >>> 24 != 0) {
         gradientX = false;
         g0 = y;
         gSpan = Math.max(h, 1.0E-4F);
         c0 = top;
         c1 = bottom;
         contour(x, y, w, h, Math.max(0.0F, Math.min(corner(radius), Math.min(w, h) * 0.5F)));
         float p = Gfx.pixel();

         for(int i = 0; i < points; ++i) {
            int j = i + 1 == points ? 0 : i + 1;
            int ci = colorAt(PX[i], PY[i]);
            int cj = colorAt(PX[j], PY[j]);
            int clearI = ci & 16777215;
            int clearJ = cj & 16777215;
            Gfx.quadColors(PX[i], PY[i], ci, PX[j], PY[j], cj, PX[j] + NX[j] * p, PY[j] + NY[j] * p, clearJ, PX[i] + NX[i] * p, PY[i] + NY[i] * p, clearI);
            Gfx.quadColors(PX[i] - NX[i] * p, PY[i] - NY[i] * p, clearI, PX[j] - NX[j] * p, PY[j] - NY[j] * p, clearJ, PX[j], PY[j], cj, PX[i], PY[i], ci);
         }

      }
   }

   public static void rim(float x, float y, float w, float h, float radius, int color) {
      rim(x, y, w, h, radius, color, color);
   }

   private static float center(Font font) {
      Font.Bounds cap = font.glyph(72).planeBounds();
      return -(cap.top() + cap.bottom()) * 0.5F;
   }

   private static Layout layout(String text, Font font) {
      Map<String, Layout> cache = font == MEDIUM ? MEDIUM_LAYOUTS : SEMIBOLD_LAYOUTS;
      Layout layout = (Layout)cache.get(text);
      if (layout == null) {
         if (cache.size() > 2048) {
            cache.clear();
         }

         float[] glyphs = new float[text.length() * 8];
         int n = 0;
         float cursor = 0.0F;
         int previous = -1;

         Font.Glyph glyph;
         for(int i = 0; i < text.length(); previous = glyph.unicode()) {
            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);
            glyph = font.glyph(codepoint);
            cursor += font.kerning(previous, glyph.unicode());
            Font.Bounds plane = glyph.planeBounds();
            if (plane != null && glyph.atlasBounds() != null) {
               Font.Bounds uv = font.uv(glyph);
               glyphs[n++] = cursor + plane.left();
               glyphs[n++] = plane.top();
               glyphs[n++] = cursor + plane.right();
               glyphs[n++] = plane.bottom();
               glyphs[n++] = uv.left();
               glyphs[n++] = uv.top();
               glyphs[n++] = uv.right();
               glyphs[n++] = uv.bottom();
            }

            cursor += glyph.advance();
         }

         layout = new Layout(cursor, n == glyphs.length ? glyphs : Arrays.copyOf(glyphs, n));
         cache.put(text, layout);
      }

      return layout;
   }

   public static float width(String text, float size, boolean medium) {
      return text != null && !text.isEmpty() ? layout(text, medium ? MEDIUM : SEMIBOLD).width * size : 0.0F;
   }

   public static float width(String text, float size) {
      return width(text, size, false);
   }

   public static String fit(String text, float width, float size, boolean medium) {
      if (width(text, size, medium) <= width) {
         return text;
      } else {
         int low = 0;
         int high = text.length();

         while(low < high) {
            int mid = low + high + 1 >>> 1;
            if (width(text.substring(0, mid).stripTrailing() + "…", size, medium) <= width) {
               low = mid;
            } else {
               high = mid - 1;
            }
         }

         return low == 0 ? "…" : text.substring(0, low).stripTrailing() + "…";
      }
   }

   public static void text(String text, float x, float centerY, float size, int color, boolean medium) {
      queue(text, x, centerY, size, color, (Text.ColorAt)null, medium);
   }

   public static void text(String text, float x, float centerY, float size, int color) {
      queue(text, x, centerY, size, color, (Text.ColorAt)null, false);
   }

   public static void textRight(String text, float right, float centerY, float size, int color, boolean medium) {
      queue(text, right - width(text, size, medium), centerY, size, color, (Text.ColorAt)null, medium);
   }

   public static void textCentered(String text, float centerX, float centerY, float size, int color, boolean medium) {
      queue(text, centerX - width(text, size, medium) * 0.5F, centerY, size, color, (Text.ColorAt)null, medium);
   }

   public static void text(String text, float x, float centerY, float size, Text.ColorAt colorAt, float alpha) {
      queue(text, x, centerY, size, Fx.alpha(-1, alpha), colorAt, false);
   }

   private static void queue(String text, float x, float centerY, float size, int color, Text.ColorAt colorAt, boolean medium) {
      if (text != null && !text.isEmpty() && size > 0.0F && Gfx.active()) {
         float fade = Gfx.alphaScale();
         if (fade < 1.0F) {
            color = Fx.alpha(color, fade);
         }

         color = Opacity.apply(color);
         if (color >>> 24 >= 2) {
            Layout layout = layout(text, medium ? MEDIUM : SEMIBOLD);
            float baseline = centerY + (medium ? CENTER_MEDIUM : CENTER_SEMIBOLD) * size;
            Matrix3x2f m = Gfx.pose();
            float gx = m.m00() * x + m.m10() * baseline + m.m20();
            float gy = m.m01() * x + m.m11() * baseline + m.m21();
            if (look.textShadow) {
               float p = Gfx.pixel();
               float o = Math.max(p, (float)Math.round(size * m.m11() * 0.08F / p) * p);
               int shadow = Math.round((float)(color >>> 24) * (LIGHT ? 0.3F : 0.62F)) << 24;
               line(layout, medium, gx + o, gy + o, m, size, shadow, (Text.ColorAt)null, x);
            }

            line(layout, medium, gx, gy, m, size, color, colorAt, x);
         }
      }
   }

   private static void line(Layout layout, boolean medium, float x, float baseline, Matrix3x2f m, float size, int color, Text.ColorAt colorAt, float localX) {
      if (lines == LINES.size()) {
         LINES.add(new Line());
      }

      Line line = (Line)LINES.get(lines++);
      line.layout = layout;
      line.medium = medium;
      line.x = x;
      line.baseline = baseline;
      line.scaleX = m.m00();
      line.scaleY = m.m11();
      line.size = size;
      line.color = color;
      line.colorAt = colorAt;
      line.localX = localX;
   }

   public static void flushText() {
      if (lines != 0) {
         Gfx.flush();
         GuiGraphicsExtractor g = Gfx.graphics();
         float guiScale = (float)Math.max((double)1.0F, (double)Minecraft.getInstance().getWindow().getGuiScale());
         int count = lines;
         lines = 0;

         for(int i = 0; i < count; ++i) {
            Line line = (Line)LINES.get(i);
            float sx = line.size * line.scaleX;
            float sy = line.size * line.scaleY;
            float x = line.x;
            float y = (float)Math.round(line.baseline * guiScale) / guiScale;
            float[] gl = line.layout.glyphs;
            List<TextState.Quad> quads = line.medium ? MEDIUM_QUADS : SEMIBOLD_QUADS;
            Text.ColorAt colorAt = line.colorAt;
            int alpha = line.color >>> 24;

            for(int k = 0; k < gl.length; k += 8) {
               int left = line.color;
               int right = line.color;
               if (colorAt != null) {
                  left = Fx.alpha(colorAt.at(line.localX + gl[k] * line.size), (float)alpha / 255.0F);
                  right = Fx.alpha(colorAt.at(line.localX + gl[k + 2] * line.size), (float)alpha / 255.0F);
               }

               quads.add(new TextState.Quad(x + gl[k] * sx, y + gl[k + 1] * sy, x + gl[k + 2] * sx, y + gl[k + 3] * sy, gl[k + 4], gl[k + 5], gl[k + 6], gl[k + 7], left, right));
            }

            line.layout = null;
            line.colorAt = null;
         }

         g.pose().pushMatrix();
         g.pose().identity();

         try {
            submit(g, SEMIBOLD, SEMIBOLD_QUADS);
            submit(g, MEDIUM, MEDIUM_QUADS);
         } finally {
            g.pose().popMatrix();
         }

      }
   }

   private static void submit(GuiGraphicsExtractor g, Font face, List<TextState.Quad> quads) {
      if (!quads.isEmpty()) {
         try {
            TextState state = TextState.of(g, face.texture(), quads, g.scissorStack.peek());
            if (state != null) {
               Submit.submit(g, state);
            }
         } finally {
            quads.clear();
         }

      }
   }

   public static void discardText() {
      for(int i = 0; i < lines; ++i) {
         ((Line)LINES.get(i)).layout = null;
         ((Line)LINES.get(i)).colorAt = null;
      }

      lines = 0;
      SEMIBOLD_QUADS.clear();
      MEDIUM_QUADS.clear();
   }

   public static float lively(Spring spring, float target, float dt) {
      return spring.update(target, 300.0F, 25.0F, dt);
   }

   public static float smooth(Spring spring, float target, float dt) {
      return spring.update(target, 220.0F, 30.0F, dt);
   }

   public static float approach(float current, float target, float rate, float dt) {
      return !look.animate ? target : current + (target - current) * (1.0F - (float)Math.exp((double)(-rate * dt)));
   }

   static {
      look = DEFAULT;
      elementScale = 1.0F;
      QUARTERS = new float[41][];
      PX = new float[164];
      PY = new float[164];
      NX = new float[164];
      NY = new float[164];
      IX = new float[164];
      IY = new float[164];
      SEMIBOLD = Fonts.INTER_SEMIBOLD;
      MEDIUM = Fonts.INTER_MEDIUM;
      CENTER_SEMIBOLD = center(SEMIBOLD);
      CENTER_MEDIUM = center(MEDIUM);
      SEMIBOLD_LAYOUTS = new HashMap();
      MEDIUM_LAYOUTS = new HashMap();
      LINES = new ArrayList();
      SEMIBOLD_QUADS = new ArrayList();
      MEDIUM_QUADS = new ArrayList();
   }

   public static final class Look {
      public int shape = 0;
      public float corners = 1.0F;
      public int background = 0;
      public int backgroundFrom = 1776672;
      public int backgroundTo = 2764613;
      public float opacity = 0.9F;
      public int border = 0;
      public int borderColor = 16777215;
      public int borderWidth = 1;
      public int shadow = 0;
      public float shadowStrength = 1.0F;
      public int accentBar = 0;
      public int accent = -1;
      public int text = -1;
      public boolean textShadow;
      public boolean animate = true;
   }

   private static final class Layout {
      final float width;
      final float[] glyphs;

      Layout(float width, float[] glyphs) {
         this.width = width;
         this.glyphs = glyphs;
      }
   }

   private static final class Line {
      Layout layout;
      boolean medium;
      float x;
      float baseline;
      float scaleX;
      float scaleY;
      float size;
      int color;
      Text.ColorAt colorAt;
      float localX;
   }

   public static final class Spring {
      public float value;
      public float velocity;

      public Spring(float value) {
         this.value = value;
      }

      public float update(float target, float stiffness, float damping, float dt) {
         if (!HudKit.look.animate) {
            this.snap(target);
            return target;
         } else if (dt <= 0.0F) {
            return this.value;
         } else {
            int steps = Math.min(16, Math.max(1, (int)Math.ceil((double)(dt / 0.008F))));
            float h = dt / (float)steps;

            for(int i = 0; i < steps; ++i) {
               float force = stiffness * (target - this.value) - damping * this.velocity;
               this.velocity += force * h;
               this.value += this.velocity * h;
            }

            float scale = Math.max(1.0F, Math.abs(target));
            if (Math.abs(target - this.value) < 5.0E-4F * scale && Math.abs(this.velocity) < 0.005F * scale) {
               this.value = target;
               this.velocity = 0.0F;
            }

            return this.value;
         }
      }

      public void snap(float value) {
         this.value = value;
         this.velocity = 0.0F;
      }
   }

   public static final class Clock {
      private long last;

      public float tick() {
         long now = System.nanoTime();
         long last = this.last;
         this.last = now;
         return last == 0L ? 0.0F : Math.clamp((float)(now - last) / 1.0E9F, 0.0F, 0.1F);
      }
   }
}
