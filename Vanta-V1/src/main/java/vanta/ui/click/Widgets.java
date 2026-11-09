package vanta.ui.click;

import com.mojang.blaze3d.platform.InputConstants;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import vanta.config.StateStore;
import vanta.module.Module;
import vanta.render.Gfx;
import vanta.setting.BoolSetting;
import vanta.setting.ColorSetting;
import vanta.setting.EnumSetting;
import vanta.setting.IdSetSetting;
import vanta.setting.KeySetting;
import vanta.setting.MultiSetting;
import vanta.setting.PositionSetting;
import vanta.setting.RangeSetting;
import vanta.setting.Setting;
import vanta.setting.SliderSetting;
import vanta.setting.TextSetting;
import net.minecraft.client.input.KeyEvent;

final class Widgets {
   static final float LABEL = 6.9F;
   static final float VALUE = 6.3F;
   static final float LINE = 20.0F;
   static ColorSetting popup;
   static final Set<Setting<?>> RAW_IDS = Collections.newSetFromMap(new IdentityHashMap());
   static float popH;
   static float popS;
   static float popV;
   private static ColorSetting popFor;
   private static int popRgb = -1;
   static boolean hexEdit;
   static StringBuilder hexBuf = new StringBuilder();
   static long shakeAt;
   static String popupTitle;
   static float popupX;
   static float popupY;
   static Setting<?> dragging;
   static int dragKnob;
   static float dragX;
   static float dragW;
   private static final float DOUBLE_EVERY = 20.0F;
   private static float dragExtra;
   private static double dragMouseX;
   private static double dragPressX;
   private static boolean dragMoved;
   private static long dragFrameAt;
   static Setting<?> typing;
   static StringBuilder buffer = new StringBuilder();
   static Object listening;
   static final Set<ColorSetting> PICKERS = Collections.newSetFromMap(new IdentityHashMap());
   static int colorDrag;
   static float[] colorArea = new float[4];
   private static int lastDetent = Integer.MIN_VALUE;
   static final int WHITE = -920844;
   static final int CHIP = -13947340;
   static final int CHIP_ON = -12039084;
   static final int CHIP_BORDER = -11710119;
   static final int CHIP_DIM = -14736857;
   static final float POP_W = 92.0F;
   static final float POP_H = 96.0F;

   static void syncPicker() {
      if (popup == null) {
         popFor = null;
         hexEdit = false;
      } else {
         int rgb = (Integer)popup.get() & 16777215;
         if (popFor != popup || rgb != popRgb) {
            float[] h = hsv(rgb);
            boolean grey = h[1] < 0.001F || h[2] < 0.001F;
            if (popFor != popup || !grey) {
               popH = h[0];
            }

            if (popFor != popup || h[2] >= 0.001F) {
               popS = h[1];
            }

            popV = h[2];
            popFor = popup;
            popRgb = rgb;
         }

      }
   }

   private static void applyPicker() {
      int rgb = Color.HSBtoRGB(popH, popS, popV) & 16777215;
      popRgb = rgb;
      popup.set(rgb);
   }

   private Widgets() {
   }

   static int text() {
      return -3946804;
   }

   static int muted() {
      return -7499367;
   }

   static int accent() {
      return -16777216 | GuiPrefs.accent;
   }

   static int surface() {
      return 184549375;
   }

   static void pill(float x, float y, float w, float h, int fill, int border) {
      Gfx.roundedRect(x, y, w, h, h / 2.0F, Paint.a(fill, 1.0F));
      Gfx.roundedOutline(x, y, w, h, h / 2.0F, 0.55F, Paint.a(border, 1.0F));
   }

   static float height(Setting<?> s, float w) {
      if (!(s instanceof SliderSetting) && !(s instanceof RangeSetting)) {
         if (s instanceof MultiSetting) {
            MultiSetting m = (MultiSetting)s;
            return 13.0F + (float)chipRows(m.options(), w, m) * 13.0F + 2.0F;
         } else if (s instanceof ColorSetting) {
            ColorSetting c = (ColorSetting)s;
            return 20.0F;
         } else if (s instanceof TextSetting) {
            return 29.0F;
         } else if (s instanceof IdSetSetting) {
            IdSetSetting ids = (IdSetSetting)s;
            List<String> items = new ArrayList((Collection)ids.get());
            return 13.0F + (float)chipRows((String[])items.toArray(new String[0]), w, (MultiSetting)null) * 13.0F + 16.0F;
         } else {
            return 20.0F;
         }
      } else {
         return 27.0F;
      }
   }

   private static int chipRows(String[] options, float w, MultiSetting m) {
      if (options.length == 0) {
         return 0;
      } else {
         int rows = 1;
         float x = 0.0F;

         for(int i = 0; i < options.length; ++i) {
            if (m == null || m.isOptionVisible(i)) {
               float cw = Paint.width(options[i], 6.3F, (byte)0) + 10.0F + (m == null ? 8.0F : 0.0F);
               if (x > 0.0F && x + cw > w) {
                  ++rows;
                  x = 0.0F;
               }

               x += cw + 3.0F;
            }
         }

         return rows;
      }
   }

   static void draw(Setting<?> s, float x, float y, float w, double mx, double my) {
      boolean hover = Paint.in(mx, my, x, y, w, height(s, w));
      Anim.to(s, 1, hover ? 1.0F : 0.0F, 18.0F);
      String name = s.name();
      float cy = y + 10.0F;
      Objects.requireNonNull(s);
      byte var13 = 0;
      //$FF: var13->value
      //0->mace/setting/BoolSetting
      //1->mace/setting/SliderSetting
      //2->mace/setting/RangeSetting
      //3->mace/setting/EnumSetting
      //4->mace/setting/MultiSetting
      //5->mace/setting/ColorSetting
      //6->mace/setting/TextSetting
      //7->mace/setting/KeySetting
      //8->mace/setting/IdSetSetting
      //9->mace/setting/PositionSetting
      switch (s.getClass().getSimpleName()) {
         case "BoolSetting":
            BoolSetting b = (BoolSetting)s;
            Paint.text(Paint.fit(name, w - 26.0F, 6.9F, (byte)0), x + 2.0F, cy, 6.9F, text(), (byte)0);
            toggle(x + w - 17.0F, cy - 4.0F, b, (Boolean)b.get());
            break;
         case "SliderSetting":
            SliderSetting sl = (SliderSetting)s;
            Paint.text(Paint.fit(name, w * 0.62F, 6.9F, (byte)0), x + 2.0F, cy, 6.9F, text(), (byte)0);
            if (typing == sl) {
               String v = buffer.toString();
               float vw = Math.max(18.0F, Paint.width(v, 6.3F, (byte)0) + 8.0F);
               Gfx.roundedRect(x + w - vw, cy - 5.0F, vw, 10.0F, 2.5F, Paint.a(-14736857, 1.0F));
               Gfx.roundedOutline(x + w - vw, cy - 5.0F, vw, 10.0F, 2.5F, 0.55F, Paint.a(Paint.mix(-9801864, accent(), 0.7F), 1.0F));
               Paint.textRight(v, x + w - 4.0F, cy, 6.3F, -920844, (byte)0);
               float blink = 0.55F + 0.45F * (float)Math.cos((double)System.nanoTime() / (double)1.0E9F * Math.PI * (double)2.0F);
               Gfx.rect(x + w - 3.4F, cy - 3.2F, x + w - 2.8F, cy + 3.2F, Paint.a(accent(), blink));
            } else {
               Paint.textRight(sl.format((Integer)sl.get()), x + w - 2.0F, cy, 6.3F, (Integer)sl.get() > sl.max() ? accent() : muted(), (byte)0);
            }

            float t = frac((Integer)sl.get(), sl.min(), sl.max());
            float shown = Anim.to(sl, 2, t, 22.0F);
            track(x + 2.0F, y + 20.5F, w - 4.0F, 0.0F, shown, sl, dragging == sl);
            break;
         case "RangeSetting":
            RangeSetting r = (RangeSetting)s;
            Paint.text(Paint.fit(name, w * 0.5F, 6.9F, (byte)0), x + 2.0F, cy, 6.9F, text(), (byte)0);
            if (typing == r) {
               String v = buffer.toString();
               float vw = Math.max(18.0F, Paint.width(v, 6.3F, (byte)0) + 8.0F);
               Gfx.roundedRect(x + w - vw, cy - 5.0F, vw, 10.0F, 2.5F, Paint.a(-14736857, 1.0F));
               Gfx.roundedOutline(x + w - vw, cy - 5.0F, vw, 10.0F, 2.5F, 0.55F, Paint.a(Paint.mix(-9801864, accent(), 0.7F), 1.0F));
               Paint.textRight(v, x + w - 4.0F, cy, 6.3F, -920844, (byte)0);
               float blink = 0.55F + 0.45F * (float)Math.cos((double)System.nanoTime() / (double)1.0E9F * Math.PI * (double)2.0F);
               Gfx.rect(x + w - 3.4F, cy - 3.2F, x + w - 2.8F, cy + 3.2F, Paint.a(accent(), blink));
            } else {
               Paint.textRight(rangeText(r), x + w - 2.0F, cy, 6.3F, r.high() > r.max() ? accent() : muted(), (byte)0);
            }

            float lo = Anim.to(r, 2, frac(r.low(), r.min(), r.max()), 22.0F);
            float hi = Anim.to(r, 3, frac(r.high(), r.min(), r.max()), 22.0F);
            track(x + 2.0F, y + 20.5F, w - 4.0F, lo, hi, r, dragging == r);
            break;
         case "EnumSetting":
            EnumSetting e = (EnumSetting)s;
            Paint.text(Paint.fit(name, w * 0.48F, 6.9F, (byte)0), x + 2.0F, cy, 6.9F, text(), (byte)0);
            String value = Paint.fit(e.selected(), w * 0.46F, 6.3F, (byte)0);
            float vw = Paint.width(value, 6.3F, (byte)0) + 18.0F;
            float cx = x + w - vw;
            pill(cx, cy - 5.5F, vw, 11.0F, -13947340, Paint.mix(-11710119, accent(), 0.22F));
            float bump = Anim.to(e, 2, 0.0F, 14.0F);
            Paint.textCenter(value, cx + vw / 2.0F, cy - bump * 1.2F, 6.3F, Paint.mix(-920844, accent(), bump * 0.6F), (byte)0);
            Paint.chevron(cx + 4.8F, cy, 1.5F, 90.0F, 0.8F, Paint.a(-920844, 0.85F));
            Paint.chevron(cx + vw - 4.8F, cy, 1.5F, -90.0F, 0.8F, Paint.a(-920844, 0.85F));
            break;
         case "MultiSetting":
            MultiSetting m = (MultiSetting)s;
            Paint.text(name, x + 2.0F, y + 6.5F, 6.9F, text(), (byte)0);
            String[] options = m.options();
            float px = 0.0F;
            float py = y + 13.0F;

            for(int i = 0; i < options.length; ++i) {
               if (m.isOptionVisible(i)) {
                  float cw = Paint.width(options[i], 6.3F, (byte)0) + 10.0F;
                  if (px > 0.0F && px + cw > w) {
                     px = 0.0F;
                     py += 13.0F;
                  }

                  boolean on = m.has(i);
                  float t = Anim.to(m, 8 + i, on ? 1.0F : 0.0F, 16.0F);
                  float popT = Anim.spring(m, 40 + i * 2, on ? 1.0F : 0.0F, 300.0F, 14.0F);
                  float bump = (popT - t) * 1.6F;
                  pill(x + px - bump, py - bump * 0.5F, cw + bump * 2.0F, 11.0F + bump, Paint.mix(-14736857, Paint.mix(-12039084, accent(), 0.22F), t), Paint.mix(-13420996, Paint.mix(-10591638, accent(), 0.45F), t));
                  Paint.textCenter(options[i], x + px + cw / 2.0F, py + 5.5F, 6.3F, Paint.mix(muted(), -920844, t), (byte)0);
                  px += cw + 3.0F;
               }
            }
            break;
         case "ColorSetting":
            ColorSetting c = (ColorSetting)s;
            Paint.text(Paint.fit(name, w - 30.0F, 6.9F, (byte)0), x + 2.0F, cy, 6.9F, text(), (byte)0);
            int rgb = (Integer)c.get();
            Gfx.roundedRect(x + w - 11.0F, cy - 4.5F, 9.0F, 9.0F, 2.5F, Paint.rgba(rgb, 1.0F));
            Gfx.roundedOutline(x + w - 11.0F, cy - 4.5F, 9.0F, 9.0F, 2.5F, 0.55F, Paint.a(-11710119, 1.0F));
            break;
         case "TextSetting":
            TextSetting t = (TextSetting)s;
            Paint.text(name, x + 2.0F, cy, 6.9F, text(), (byte)0);
            field(t, (String)t.get(), "Type…", x + 2.0F, y + 15.5F, w - 4.0F);
            break;
         case "KeySetting":
            KeySetting k = (KeySetting)s;
            Paint.text(Paint.fit(name, w * 0.55F, 6.9F, (byte)0), x + 2.0F, cy, 6.9F, text(), (byte)0);
            keyChip(k, k.bound() ? ((InputConstants.Key)k.get()).getDisplayName().getString() : "None", x + w, cy);
            break;
         case "IdSetSetting":
            IdSetSetting ids = (IdSetSetting)s;
            Paint.text(name, x + 2.0F, y + 6.5F, 6.9F, text(), (byte)0);
            Paint.textRight("" + ((Set)ids.get()).size(), x + w - 2.0F, y + 6.5F, 6.3F, muted(), (byte)0);
            float px = 0.0F;
            float py = y + 13.0F;

            for(String id : (Set)ids.get()) {
               String label = short_(id);
               float cw = Paint.width(label, 6.3F, (byte)0) + 18.0F;
               if (px > 0.0F && px + cw > w) {
                  px = 0.0F;
                  py += 13.0F;
               }

               pill(x + px, py, cw, 11.0F, -12039084, -10591638);
               Paint.text(label, x + px + 5.0F, py + 5.5F, 6.3F, text(), (byte)0);
               float xx = x + px + cw - 6.5F;
               Gfx.line(xx - 1.6F, py + 3.9F, xx + 1.6F, py + 7.1F, 0.9F, Paint.a(muted(), 1.0F));
               Gfx.line(xx - 1.6F, py + 7.1F, xx + 1.6F, py + 3.9F, 0.9F, Paint.a(muted(), 1.0F));
               px += cw + 3.0F;
            }

            if (!((Set)ids.get()).isEmpty()) {
               py += 13.0F;
            }

            field(ids, typing == ids ? buffer.toString() : "", "Add id, Enter", x + 2.0F, py + 1.0F, w - 4.0F);
            break;
         case "PositionSetting":
            PositionSetting p = (PositionSetting)s;
            Paint.text(name, x + 2.0F, cy, 6.9F, text(), (byte)0);
            PositionSetting.Position pos = (PositionSetting.Position)p.get();
            int var10000 = Math.round(pos.offsetX());
            String v = var10000 + ", " + Math.round(pos.offsetY());
            float rw = Paint.width("Reset", 6.3F, (byte)0) + 10.0F;
            pill(x + w - rw, cy - 5.5F, rw, 11.0F, -13947340, -11710119);
            Paint.textCenter("Reset", x + w - rw / 2.0F, cy, 6.3F, text(), (byte)0);
            Paint.textRight(v, x + w - rw - 4.0F, cy, 6.3F, muted(), (byte)0);
            break;
         default:
            Paint.text(name, x + 2.0F, cy, 6.9F, text(), (byte)0);
            Paint.textRight(String.valueOf(s.get()), x + w - 2.0F, cy, 6.3F, muted(), (byte)0);
      }

   }

   static String short_(String id) {
      int colon = id.indexOf(58);
      String s = colon >= 0 ? id.substring(colon + 1) : id;
      return Paint.fit(s.replace('_', ' '), 70.0F, 6.3F, (byte)0);
   }

   static void toggle(float x, float y, Object key, boolean on) {
      float t = Anim.to(key, 3, on ? 1.0F : 0.0F, 16.0F);
      Gfx.roundedRect(x, y, 16.0F, 8.0F, 4.0F, Paint.a(Paint.mix(-13420996, accent(), t), 1.0F));
      float sp = Anim.spring(key, 12, on ? 1.0F : 0.0F, 260.0F, 20.0F);
      float kx = x + 4.0F + 8.0F * sp;
      if (t > 0.02F) {
         Gfx.glow(kx, y + 4.0F, 9.0F, Paint.a(accent() & 16777215 | 1426063360, t));
      }

      float squish = 1.0F + Math.abs(sp - (float)Math.round(sp)) * 0.0F;
      Gfx.roundedRect(kx - 3.1F * (1.0F + 0.25F * Math.min(1.0F, Math.abs(sp - (on ? 1.0F : 0.0F)) * 3.0F)), y + 0.9F, 6.2F * (1.0F + 0.25F * Math.min(1.0F, Math.abs(sp - (on ? 1.0F : 0.0F)) * 3.0F)), 6.2F * squish, 3.1F, Paint.a(-1, 1.0F));
   }

   static void keyChip(Object key, String label, float right, float cy) {
      boolean wait = listening == key;
      float pulse = wait ? 0.5F + 0.5F * (float)Math.sin((double)System.nanoTime() / (double)1.6E8F) : 0.0F;
      String shown = wait ? "Press a key…" : label;
      float kw = Math.max(22.0F, Paint.width(shown, 6.3F, (byte)0) + 10.0F);
      float t = Anim.to(key, 4, wait ? 1.0F : 0.0F, 16.0F);
      pill(right - kw, cy - 5.5F, kw, 11.0F, Paint.mix(-13947340, -12039084, t * (0.6F + 0.4F * pulse)), -11710119);
      Paint.textCenter(shown, right - kw / 2.0F, cy, 6.3F, -920844, (byte)0);
   }

   private static void track(float x, float y, float w, float lo, float hi, Object key, boolean active) {
      float h = 1.6F;
      Gfx.roundedRect(x, y - h / 2.0F, w, h, h / 2.0F, Paint.a(-13289410, 1.0F));
      float x0 = x + w * lo;
      float x1 = x + w * hi;
      Gfx.roundedRect(x0, y - h / 2.0F, Math.max(h, x1 - x0), h, h / 2.0F, Paint.a(Paint.mix(accent(), -2501392, 0.35F), 1.0F));
      float grow = Anim.to(key, 6, active ? 1.0F : 0.0F, 18.0F);
      float kw = 2.2F + 0.4F * grow;
      float kh = 6.0F + 1.0F * grow;
      if (key instanceof RangeSetting) {
         Gfx.roundedRect(x0 - kw / 2.0F, y - kh / 2.0F, kw, kh, 0.6F, Paint.a(-1, 1.0F));
      }

      Gfx.glow(x1, y, 6.0F + 3.0F * grow, Paint.a(accent() & 16777215 | 1073741824, 0.6F + 0.4F * grow));
      Gfx.roundedRect(x1 - kw / 2.0F, y - kh / 2.0F, kw, kh, 0.6F, Paint.a(-1, 1.0F));
   }

   private static void field(Setting<?> s, String value, String placeholder, float x, float y, float w) {
      boolean focus = typing == s;
      float t = Anim.to(s, 7, focus ? 1.0F : 0.0F, 16.0F);
      Gfx.roundedRect(x, y, w, 11.5F, 3.5F, Paint.a(-14736857, 1.0F));
      Gfx.roundedOutline(x, y, w, 11.5F, 3.5F, 0.55F, Paint.a(Paint.mix(-12960187, Paint.mix(-9801864, accent(), 0.7F), t), 1.0F));
      String shown = focus ? buffer.toString() : value;
      float cy = y + 5.75F;
      if (shown.isEmpty() && !focus) {
         Paint.text(placeholder, x + 4.0F, cy, 6.3F, Paint.a(muted(), 0.7F), (byte)0);
      } else {
         String fitted;
         for(fitted = shown; Paint.width(fitted, 6.3F, (byte)0) > w - 10.0F && fitted.length() > 1; fitted = fitted.substring(1)) {
         }

         Paint.text(fitted, x + 4.0F, cy, 6.3F, text(), (byte)0);
         if (focus) {
            float cx = x + 4.5F + Paint.width(fitted, 6.3F, (byte)0);
            float blink = 0.55F + 0.45F * (float)Math.cos((double)System.nanoTime() / (double)1.0E9F * Math.PI * (double)2.0F);
            Gfx.rect(cx, cy - 3.5F, cx + 0.7F, cy + 3.5F, Paint.a(Paint.mix(-920844, accent(), 0.6F), blink));
         }
      }

   }

   static void drawPopup(float open) {
      ColorSetting c = popup;
      float x = popupX;
      float y = popupY;
      float pop = Anim.outBack(open);
      Paint.flush();
      Paint.g.pose().pushMatrix();
      Paint.g.pose().translate(x, y + 10.0F);
      Paint.g.pose().scale(0.8F + 0.2F * pop, 0.8F + 0.2F * pop);
      Paint.g.pose().translate(-x, -(y + 10.0F));

      try {
         drawPopupBody(c, x, y, open);
      } finally {
         Paint.flush();
         Paint.g.pose().popMatrix();
      }

   }

   private static void drawPopupBody(ColorSetting c, float x, float y, float open) {
      open = Math.min(1.0F, open * 1.6F);
      float head = popupTitle == null ? 0.0F : 14.0F;
      Gfx.shadow(x, y + 2.0F, 92.0F, 96.0F + head, 3.0F, 12.0F, Paint.a(1610612736, open));
      Gfx.roundedRect(x, y, 92.0F, 96.0F + head, 3.0F, Paint.a(-182772189, open));
      Gfx.roundedOutline(x, y, 92.0F, 96.0F + head, 3.0F, 0.6F, Paint.a(-11907498, open));
      if (popupTitle != null) {
         Paint.text(popupTitle, x + 6.0F, y + 8.5F, 6.8F, Paint.a(-920844, open), (byte)0);
      }

      syncPicker();
      float[] hsv = new float[]{popH, popS, popV};
      float px = x + 5.0F;
      float py = y + 5.0F + head;
      float pw = 82.0F;
      float ph = 56.0F;
      int hue = -16777216 | Color.HSBtoRGB(hsv[0], 1.0F, 1.0F);
      Gfx.roundedGradient(px, py, pw, ph, 3.0F, Paint.a(-1, open), Paint.a(hue, open), Paint.a(hue, open), Paint.a(-1, open), true, true, true, true);
      Gfx.roundedGradient(px, py, pw, ph, 3.0F, 0, 0, Paint.a(-16777216, open), Paint.a(-16777216, open), true, true, true, true);
      float kx = px + hsv[1] * pw;
      float ky = py + (1.0F - hsv[2]) * ph;
      Gfx.circleOutline(kx, ky, 3.0F, 0.6F, Paint.a(accent(), open));
      Gfx.circleOutline(kx, ky, 2.3F, 0.8F, Paint.a(-1, open));
      float hy = py + ph + 5.0F;
      int seg = 6;

      for(int i = 0; i < seg; ++i) {
         int c0 = -16777216 | Color.HSBtoRGB((float)i / (float)seg, 1.0F, 1.0F);
         int c1 = -16777216 | Color.HSBtoRGB((float)(i + 1) / (float)seg, 1.0F, 1.0F);
         Gfx.gradientH(px + pw * (float)i / (float)seg, hy, px + pw * (float)(i + 1) / (float)seg, hy + 5.0F, Paint.a(c0, open), Paint.a(c1, open));
      }

      float hx = px + hsv[0] * pw;
      Gfx.rect(hx - 0.8F, hy - 1.0F, hx + 0.8F, hy + 6.0F, Paint.a(-1, open));
      float ay = hy + 9.0F;

      for(int i = 0; i < 12; ++i) {
         Gfx.rect(px + pw * (float)i / 12.0F, ay, px + pw * (float)(i + 1) / 12.0F, ay + 2.5F, Paint.a(i % 2 == 0 ? -12960444 : -14407892, open));
         Gfx.rect(px + pw * (float)i / 12.0F, ay + 2.5F, px + pw * (float)(i + 1) / 12.0F, ay + 5.0F, Paint.a(i % 2 == 0 ? -14407892 : -12960444, open));
      }

      Gfx.gradientH(px, ay, px + pw, ay + 5.0F, Paint.a((Integer)c.get() & 16777215, open), Paint.a(-16777216 | (Integer)c.get(), open));
      float ax = px + pw * (float)c.alpha() / 255.0F;
      Gfx.rect(Math.max(px, ax - 0.8F), ay - 1.0F, Math.min(px + pw, ax + 0.8F), ay + 6.0F, Paint.a(-1, open));
      float ry = ay + 12.0F;
      Gfx.roundedRect(px, ry - 3.5F, 7.0F, 7.0F, 1.5F, Paint.a(c.argb() | (c.alpha() < 40 ? 671088640 : 0), open));
      String hex = hexEdit ? "#" + String.valueOf(hexBuf) : String.format("#%06X", (Integer)c.get() & 16777215) + (c.alpha() < 255 ? String.format("%02X", c.alpha()) : "");
      float since = (float)(System.nanoTime() - shakeAt) / 1.0E9F;
      float shake = since < 0.35F ? (float)Math.sin((double)since * (double)60.0F) * 2.0F * (1.0F - since / 0.35F) : 0.0F;
      float fx = px + 10.0F + shake;
      float fw = pw - 10.0F;
      int fieldBorder = hexEdit ? Paint.mix(-9801864, accent(), 0.7F) : -12960187;
      if (since < 0.35F) {
         fieldBorder = -3122588;
      }

      Gfx.roundedRect(fx - 2.0F, ry - 4.5F, fw, 9.0F, 2.5F, Paint.a(-14736857, open));
      Gfx.roundedOutline(fx - 2.0F, ry - 4.5F, fw, 9.0F, 2.5F, 0.55F, Paint.a(fieldBorder, open));
      Paint.text(hex, fx, ry, 6.0F, Paint.a(-920844, open), (byte)0);
      if (hexEdit) {
         float cx = fx + Paint.width(hex, 6.0F, (byte)0) + 0.6F;
         float blink = 0.55F + 0.45F * (float)Math.cos((double)System.nanoTime() / (double)1.0E9F * Math.PI * (double)2.0F);
         Gfx.rect(cx, ry - 3.2F, cx + 0.6F, ry + 3.2F, Paint.a(Paint.mix(-920844, accent(), 0.6F), blink * open));
      }

      colorArea = new float[]{px, py, pw, ph, hy, ay};
   }

   static boolean popupClick(double mx, double my) {
      if (popup == null) {
         return false;
      } else {
         float head = popupTitle == null ? 0.0F : 14.0F;
         if (!Paint.in(mx, my, popupX, popupY, 92.0F, 96.0F + head)) {
            popup = null;
            GuiSounds.expand(false);
            return true;
         } else {
            float[] a = colorArea;
            if (a.length >= 6) {
               hexEdit = false;
               if (my < (double)(a[1] - 2.0F)) {
                  return true;
               }

               if (my > (double)(a[5] + 7.0F)) {
                  hexEdit = true;
                  String var10002 = String.format("%06X", (Integer)popup.get() & 16777215);
                  hexBuf = new StringBuilder(var10002 + (popup.alpha() < 255 ? String.format("%02X", popup.alpha()) : ""));
                  return true;
               }

               if (my < (double)(a[1] + a[3] + 2.5F)) {
                  colorDrag = 1;
               } else if (my < (double)(a[4] + 7.0F)) {
                  colorDrag = 2;
               } else {
                  if (!(my < (double)(a[5] + 7.0F))) {
                     return true;
                  }

                  colorDrag = 3;
               }

               dragging = popup;
               colorDragTo(mx, my);
            }

            return true;
         }
      }
   }

   static float[] hsv(int rgb) {
      return Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, (float[])null);
   }

   static String rangeText(RangeSetting r) {
      String var10000 = r.format(r.low());
      return var10000 + " - " + r.format(r.high());
   }

   static float frac(int v, int min, int max) {
      return max <= min ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float)(v - min) / (float)(max - min)));
   }

   static boolean click(Setting<?> s, float x, float y, float w, double mx, double my, int button) {
      float cy = y + 10.0F;
      if (typing != null && typing != s) {
         commit();
      }

      Objects.requireNonNull(s);
      byte var11 = 0;
      //$FF: var11->value
      //0->mace/setting/BoolSetting
      //1->mace/setting/SliderSetting
      //2->mace/setting/RangeSetting
      //3->mace/setting/EnumSetting
      //4->mace/setting/MultiSetting
      //5->mace/setting/ColorSetting
      //6->mace/setting/TextSetting
      //7->mace/setting/KeySetting
      //8->mace/setting/IdSetSetting
      //9->mace/setting/PositionSetting
      switch (s.getClass().getSimpleName()) {
         case "BoolSetting":
            BoolSetting b = (BoolSetting)s;
            b.set(!(Boolean)b.get());
            GuiSounds.toggle((Boolean)b.get());
            edited(s);
            return true;
         case "SliderSetting":
            SliderSetting sl = (SliderSetting)s;
            if (my < (double)(y + 20.0F) && mx > (double)(x + w - Math.max(24.0F, Paint.width(sl.format((Integer)sl.get()), 6.3F, (byte)0) + 6.0F))) {
               typing = sl;
               buffer = new StringBuilder(sl.typedText());
               GuiSounds.tap();
               return true;
            }

            startDrag(sl, x, w, mx, (Integer)sl.get() > sl.max());
            return true;
         case "RangeSetting":
            RangeSetting r = (RangeSetting)s;
            if (my < (double)(y + 20.0F) && mx > (double)(x + w - Math.max(24.0F, Paint.width(rangeText(r), 6.3F, (byte)0) + 6.0F))) {
               typing = r;
               buffer = new StringBuilder(r.low() == r.high() ? r.measure().plain(r.low()) : r.measure().plain(r.low()) + "-" + r.measure().plain(r.high()));
               GuiSounds.tap();
               return true;
            }

            float lo = x + 2.0F + (w - 4.0F) * frac(r.low(), r.min(), r.max());
            float hi = x + 2.0F + (w - 4.0F) * frac(r.high(), r.min(), r.max());
            dragKnob = !(Math.abs(mx - (double)lo) < Math.abs(mx - (double)hi)) && (Math.abs(mx - (double)lo) != Math.abs(mx - (double)hi) || !(mx < (double)lo)) ? 1 : 0;
            startDrag(r, x, w, mx, dragKnob == 1 && r.high() > r.max());
            return true;
         case "EnumSetting":
            EnumSetting e = (EnumSetting)s;
            int n = e.options().length;
            if (n == 0) {
               return true;
            }

            float vw = Paint.width(Paint.fit(e.selected(), w * 0.46F, 6.3F, (byte)0), 6.3F, (byte)0) + 18.0F;
            boolean back = button == 1 || mx < (double)(x + w - vw + 8.0F) && mx >= (double)(x + w - vw);
            e.set(Math.floorMod((Integer)e.get() + (back ? -1 : 1), n));
            Anim.set(e, 2, 1.0F);
            GuiSounds.tap();
            edited(s);
            return true;
         case "MultiSetting":
            MultiSetting m = (MultiSetting)s;
            String[] options = m.options();
            float px = 0.0F;
            float py = y + 13.0F;

            for(int i = 0; i < options.length; ++i) {
               if (m.isOptionVisible(i)) {
                  float cw = Paint.width(options[i], 6.3F, (byte)0) + 10.0F;
                  if (px > 0.0F && px + cw > w) {
                     px = 0.0F;
                     py += 13.0F;
                  }

                  if (Paint.in(mx, my, x + px, py, cw, 11.0F)) {
                     Set<Integer> next = new LinkedHashSet((Collection)m.get());
                     if (!next.remove(i)) {
                        next.add(i);
                     }

                     m.set(next);
                     GuiSounds.toggle(next.contains(i));
                     edited(s);
                     return true;
                  }

                  px += cw + 3.0F;
               }
            }

            return true;
         case "ColorSetting":
            ColorSetting c = (ColorSetting)s;
            if (popup == c) {
               popup = null;
            } else {
               popup = c;
               popupTitle = null;
               popupX = x + w + 14.0F;
               popupY = y - 4.0F;
            }

            GuiSounds.expand(popup == c);
            return true;
         case "TextSetting":
            TextSetting t = (TextSetting)s;
            typing = t;
            buffer = new StringBuilder((String)t.get());
            return true;
         case "KeySetting":
            KeySetting k = (KeySetting)s;
            listening = listening == k ? null : k;
            GuiSounds.tap();
            return true;
         case "IdSetSetting":
            IdSetSetting ids = (IdSetSetting)s;
            float px = 0.0F;
            float py = y + 13.0F;

            for(String id : new ArrayList((Collection)ids.get())) {
               float cw = Paint.width(short_(id), 6.3F, (byte)0) + 18.0F;
               if (px > 0.0F && px + cw > w) {
                  px = 0.0F;
                  py += 13.0F;
               }

               if (Paint.in(mx, my, x + px, py, cw, 11.0F)) {
                  Set<String> next = new LinkedHashSet((Collection)ids.get());
                  next.remove(id);
                  ((Setting)ids).set(next);
                  GuiSounds.toggle(false);
                  return true;
               }

               px += cw + 3.0F;
            }

            typing = ids;
            buffer = new StringBuilder();
            return true;
         case "PositionSetting":
            PositionSetting p = (PositionSetting)s;
            float rw = Paint.width("Reset", 6.3F, (byte)0) + 10.0F;
            if (Paint.in(mx, my, x + w - rw, cy - 5.5F, rw, 11.0F)) {
               p.reset();
               StateStore.changed();
               GuiSounds.tap();
            }

            return true;
         default:
            return false;
      }
   }

   private static void colorDragTo(double mx, double my) {
      Setting var5 = dragging;
      if (var5 instanceof ColorSetting c) {
         float[] var6 = colorArea;
         if (colorDrag == 3) {
            c.setAlpha(Math.round(clamp01((float)((mx - (double)var6[0]) / (double)var6[2])) * 255.0F));
         } else {
            syncPicker();
            if (colorDrag == 1) {
               popS = clamp01((float)((mx - (double)var6[0]) / (double)var6[2]));
               popV = 1.0F - clamp01((float)((my - (double)var6[1]) / (double)var6[3]));
            } else {
               popH = Math.min(0.999F, clamp01((float)((mx - (double)var6[0]) / (double)var6[2])));
            }

            applyPicker();
         }
      }
   }

   private static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   static void drag(double mx, double my) {
      if (dragging instanceof ColorSetting) {
         colorDragTo(mx, my);
      } else {
         drag(mx);
      }

   }

   private static void startDrag(Setting<?> s, float x, float w, double mx, boolean beyond) {
      dragging = s;
      dragX = x + 2.0F;
      dragW = Math.max(1.0F, w - 4.0F);
      dragPressX = mx;
      dragExtra = 0.0F;
      dragFrameAt = System.nanoTime();
      dragMoved = !beyond;
      drag(mx);
   }

   private static int slideValue(double mx, int min, int max, int limit) {
      double at = (mx - (double)dragX) / (double)dragW;
      if (!(at <= (double)1.0F) && limit > max) {
         double past = mx - (double)(dragX + dragW) + (double)dragExtra;
         double grown = (double)Math.max(1, max) * Math.pow((double)2.0F, past / (double)20.0F);
         return (int)Math.min((long)limit, Math.max((long)max, Math.round(grown)));
      } else {
         return min + (int)Math.round(Math.max((double)0.0F, Math.min((double)1.0F, at)) * (double)(max - min));
      }
   }

   static void dragFrame(float viewWidth) {
      long now = System.nanoTime();
      float dt = Math.min(0.1F, (float)(now - dragFrameAt) / 1.0E9F);
      dragFrameAt = now;
      if ((dragging instanceof SliderSetting || dragging instanceof RangeSetting) && dragMoved && dragMouseX >= (double)viewWidth - (double)3.0F && dragMouseX > (double)(dragX + dragW)) {
         dragExtra += dt * 45.0F;
         drag(dragMouseX);
      }

   }

   private static void drag(double mx) {
      dragMouseX = mx;
      if (!dragMoved) {
         if (Math.abs(mx - dragPressX) < (double)2.0F) {
            return;
         }

         dragMoved = true;
      }

      Setting v = dragging;
      if (v instanceof SliderSetting sl) {
         int v = slideValue(mx, sl.min(), sl.max(), sl.limit());
         if (v != (Integer)sl.get()) {
            sl.set(v);
            detent(v);
            edited(sl);
         }
      } else {
         v = dragging;
         if (v instanceof RangeSetting r) {
            int v = slideValue(mx, r.min(), r.max(), dragKnob == 1 ? r.limit() : r.max());
            if (dragKnob == 0) {
               if (v > r.high()) {
                  dragKnob = 1;
               }
            } else if (v < r.low()) {
               dragKnob = 0;
            }

            int before = dragKnob == 0 ? r.low() : r.high();
            if (dragKnob == 0) {
               r.setLow(v);
            } else {
               r.setHigh(v);
            }

            if (before != (dragKnob == 0 ? r.low() : r.high())) {
               detent(v);
               edited(r);
            }
         }
      }

   }

   private static void detent(int v) {
      if (v != lastDetent) {
         lastDetent = v;
         GuiSounds.detent();
      }

   }

   static void release() {
      dragging = null;
      colorDrag = 0;
   }

   private static void edited(Setting<?> s) {
      Runnable after = (Runnable)GuiPrefs.AFTER_EDIT.get(s);
      if (after != null) {
         after.run();
      }

   }

   static boolean resetOnRightClick(Setting<?> s) {
      if (!(s instanceof EnumSetting) && !(s instanceof MultiSetting) && !s.bound()) {
         s.reset();
         if (s instanceof ColorSetting) {
            ColorSetting c = (ColorSetting)s;
            c.setAlpha(255);
         }

         StateStore.changed();
         edited(s);
         GuiSounds.tap();
         return true;
      } else {
         return false;
      }
   }

   static boolean key(KeyEvent event) {
      int k = event.key();
      if (hexEdit && popup != null) {
         switch (k) {
            case 256:
               hexEdit = false;
               break;
            case 257:
            case 335:
               applyHex();
               break;
            case 259:
               if (!hexBuf.isEmpty()) {
                  hexBuf.deleteCharAt(hexBuf.length() - 1);
               }
         }

         return true;
      } else if (listening == null) {
         if (typing != null) {
            switch (k) {
               case 256:
                  typing = null;
                  break;
               case 257:
               case 335:
                  commit();
                  if (typing == null) {
                     return true;
                  }
                  break;
               case 259:
                  if (!buffer.isEmpty()) {
                     if ((event.modifiers() & 2) != 0) {
                        buffer.setLength(0);
                     } else {
                        buffer.deleteCharAt(buffer.length() - 1);
                     }
                  }
            }

            return true;
         } else {
            return false;
         }
      } else {
         InputConstants.Key key = k != 256 && k != 259 && k != 261 ? InputConstants.getKey(event) : InputConstants.UNKNOWN;
         bindTo(key);
         return true;
      }
   }

   static void bindTo(InputConstants.Key key) {
      Object target = listening;
      listening = null;
      if (target instanceof KeySetting ks) {
         ks.set(key);
      } else if (target instanceof Module module) {
         module.setKey(key);
      }

      if (key != InputConstants.UNKNOWN) {
         GuiSounds.bind();
      } else {
         GuiSounds.tap();
      }

   }

   static boolean applyHex() {
      String h = hexBuf.toString().replace("#", "").trim();
      if (h.matches("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")) {
         long v = Long.parseLong(h, 16);
         int rgb = (int)(h.length() == 8 ? v >>> 8 : v) & 16777215;
         popup.set(rgb);
         popup.setAlpha(h.length() == 8 ? (int)(v & 255L) : 255);
         popFor = null;
         syncPicker();
         hexEdit = false;
         GuiSounds.tap();
         return true;
      } else {
         shakeAt = System.nanoTime();
         hexBuf = new StringBuilder(String.format("%06X", (Integer)popup.get() & 16777215));
         hexEdit = false;
         return false;
      }
   }

   static boolean chr(int codepoint) {
      if (hexEdit && popup != null) {
         if (hexBuf.length() < 9 && codepoint != 35 && codepoint > 32) {
            hexBuf.appendCodePoint(codepoint);
         }

         return true;
      } else if (typing == null) {
         return false;
      } else {
         if (codepoint >= 32 && codepoint != 127) {
            Setting var3 = typing;
            int var10000;
            if (var3 instanceof TextSetting) {
               TextSetting t = (TextSetting)var3;
               var10000 = t.maxLength();
            } else {
               var10000 = 64;
            }

            int max = var10000;
            if (buffer.length() < max) {
               buffer.appendCodePoint(codepoint);
            }
         }

         return true;
      }
   }

   static void commit() {
      Setting<?> s = typing;
      typing = null;
      if (s instanceof SliderSetting sl) {
         Integer v = sl.parse(buffer.toString());
         if (v != null) {
            sl.set(v);
            edited(sl);
         } else {
            shakeAt = System.nanoTime();
         }
      } else if (s instanceof RangeSetting r) {
         String[] parts = buffer.toString().strip().split("(?<=[0-9.])\\s*(?:-|to)\\s*", 2);
         Integer low = r.measure().parse(parts[0]);
         Integer high = parts.length > 1 ? r.measure().parse(parts[1]) : low;
         if (low != null && high != null) {
            int a = Math.min(low, high);
            int b = Math.max(low, high);
            r.setHigh(Math.max(b, r.low()));
            r.setLow(a);
            r.setHigh(b);
            edited(r);
         } else {
            shakeAt = System.nanoTime();
         }
      } else if (s instanceof TextSetting t) {
         t.set(buffer.toString());
      } else if (s instanceof IdSetSetting ids) {
         boolean raw = RAW_IDS.contains(ids);
         String id = raw ? buffer.toString().trim() : buffer.toString().trim().toLowerCase(Locale.ROOT).replace(' ', '_');
         if (!id.isEmpty()) {
            if (!raw && !id.contains(":")) {
               id = "minecraft:" + id;
            }

            Set<String> next = new LinkedHashSet((Collection)ids.get());
            next.add(id);
            ((Setting)ids).set(next);
            GuiSounds.toggle(true);
            typing = ids;
         }
      }

      buffer = new StringBuilder();
   }

   static boolean busy() {
      return typing != null || listening != null || hexEdit && popup != null;
   }
}
