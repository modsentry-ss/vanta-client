package vanta.ui.click;

import vanta.VantaClient;
import vanta.setting.RangeSetting;
import vanta.setting.Setting;
import vanta.setting.SliderSetting;
import net.minecraft.client.input.KeyEvent;

public final class SliderDriver {
   private SliderDriver() {
   }

   private static void check(String name, boolean ok, String detail) {
      System.out.println("[UT] slider " + (ok ? "PASS " : "FAIL ") + name + " (" + detail + ")");
   }

   private static Setting<?> setting(String module, String id) {
      for(Setting<?> s : VantaClient.MODULES.get(module).settings()) {
         if (s.id().equals(id)) {
            return s;
         }
      }

      throw new IllegalArgumentException(module + "." + id);
   }

   private static float[] rect(Setting<?> s) {
      return (float[])ClickGui.view().rects.get(s);
   }

   private static double[] gui(float vx, float vy) {
      return ClickGui.view().toGui((double)vx, (double)vy);
   }

   private static void drag(float x0, float x1, float y) {
      GuiView v = ClickGui.view();
      double[] a = gui(x0, y);
      double[] b = gui(x1, y);
      v.mouseClicked(a[0], a[1], 0);

      for(int i = 1; i <= 12; ++i) {
         v.mouseDragged(a[0] + (b[0] - a[0]) * (double)i / (double)12.0F, a[1]);
      }

      v.mouseReleased();
   }

   private static void click(float x, float y) {
      GuiView v = ClickGui.view();
      double[] a = gui(x, y);
      v.mouseClicked(a[0], a[1], 0);
      v.mouseReleased();
   }

   private static void type(String text) {
      ClickGui.onKey(new KeyEvent(259, 0, 2), 1);

      for(char c : text.toCharArray()) {
         ClickGui.onChar(c);
      }

      ClickGui.onKey(new KeyEvent(257, 0, 0), 1);
   }

   private static float trackY(float[] r) {
      return r[1] + 20.5F;
   }

   public static void sliderPastEnd() {
      SliderSetting s = (SliderSetting)setting("autoMace", "aimSpeed");
      float[] r = rect(s);
      if (r == null) {
         check("aim speed row drawn", false, "no rect");
      } else {
         s.set(50);
         float end = r[0] + 2.0F + (r[2] - 4.0F);
         drag(r[0] + r[2] / 2.0F, end + 60.0F, trackY(r));
         boolean var10001 = (Integer)s.get() == 800;
         String var10002 = String.valueOf(s.get());
         check("drag 60 past the end of an uncapped % slider", var10001, "aimSpeed=" + var10002 + " (max " + s.max() + ", expect 100*2^3=800)");
         int before = (Integer)s.get();
         click(r[0] + r[2] / 2.0F, trackY(r));
         var10001 = (Integer)s.get() == before;
         var10002 = String.valueOf(s.get());
         check("a click without moving keeps the over-max value", var10001, "aimSpeed=" + var10002);
         drag(r[0] + r[2] / 2.0F, r[0] + 2.0F + (r[2] - 4.0F) * 0.25F, trackY(r));
         check("dragging back inside the track works again", (Integer)s.get() == 25, "aimSpeed=" + String.valueOf(s.get()));
      }
   }

   public static void sliderTyping() {
      SliderSetting s = (SliderSetting)setting("autoMace", "aimSpeed");
      float[] r = rect(s);
      click(r[0] + r[2] - 4.0F, r[1] + 10.0F);
      boolean var10001 = Widgets.typing == s;
      String var10002 = String.valueOf(Widgets.typing);
      check("clicking the value starts typing", var10001, "typing=" + var10002 + " buffer='" + String.valueOf(Widgets.buffer) + "'");
      type("250");
      check("typed 250 on a 0-100 % slider", (Integer)s.get() == 250, "aimSpeed=" + String.valueOf(s.get()));
      SliderSetting fall = (SliderSetting)setting("autoMace", "minFall");
      fall.set(15);
      float[] f = rect(fall);
      click(f[0] + f[2] - 4.0F, f[1] + 10.0F);
      String pre = Widgets.buffer.toString();
      ClickGui.onKey(new KeyEvent(257, 0, 0), 1);
      check("block slider prefill is in blocks and Enter keeps it", pre.equals("1.5") && (Integer)fall.get() == 15, "prefill='" + pre + "' minFall=" + String.valueOf(fall.get()));
      click(f[0] + f[2] - 4.0F, f[1] + 10.0F);
      type("45");
      check("typed 45 blocks past the 30 block end", (Integer)fall.get() == 450, "minFall=" + String.valueOf(fall.get()));
   }

   public static void rangeTyping() {
      RangeSetting r = (RangeSetting)setting("autoMace", "stunDelay");
      float[] b = rect(r);
      if (b == null) {
         check("stun delay row drawn", false, "no rect");
      } else {
         click(b[0] + b[2] - 4.0F, b[1] + 10.0F);
         boolean var10001 = Widgets.typing == r;
         String var10002 = String.valueOf(Widgets.typing);
         check("clicking the range values starts typing", var10001, "typing=" + var10002 + " buffer='" + String.valueOf(Widgets.buffer) + "'");
         type("3-15");
         var10001 = r.low() == 3 && r.high() == 15;
         int var7 = r.low();
         check("typed 3-15 on a 1-10 tick range", var10001, "stunDelay=" + var7 + "-" + r.high());
         click(b[0] + b[2] - 4.0F, b[1] + 10.0F);
         type("4");
         var10001 = r.low() == 4 && r.high() == 4;
         var7 = r.low();
         check("one number sets both ends", var10001, "stunDelay=" + var7 + "-" + r.high());
         float end = b[0] + 2.0F + (b[2] - 4.0F);
         float hi = b[0] + 2.0F + (b[2] - 4.0F) * Widgets.frac(r.high(), r.min(), r.max());
         drag(hi + 1.0F, end + 40.0F, trackY(b));
         var10001 = r.high() == 40 && r.low() == 4;
         var7 = r.low();
         check("drag the high knob 40 past the end", var10001, "stunDelay=" + var7 + "-" + r.high() + " (expect 10*2^2=40)");
      }
   }

   public static void scrollDown() {
      float[] r = rect(setting("autoMace", "minFall"));
      double[] g = gui(r[0] + r[2] / 2.0F, r[1] + 10.0F);
      ClickGui.view().mouseScrolled(g[0], g[1], (double)-8.0F);
   }

   public static void cappedPastEnd() {
      SliderSetting s = (SliderSetting)setting("autoMace", "aimFov");
      float[] r = rect(s);
      if (r == null) {
         check("aim fov row drawn", false, "no rect");
      } else {
         float end = r[0] + 2.0F + (r[2] - 4.0F);
         drag(r[0] + r[2] / 2.0F, end + 60.0F, trackY(r));
         boolean var10001 = (Integer)s.get() == s.max();
         String var10002 = String.valueOf(s.get());
         check("capped slider stops at its end", var10001, "aimFov=" + var10002 + " shown " + s.format((Integer)s.get()));
         s.set(70);
      }
   }
}
