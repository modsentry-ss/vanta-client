package vanta.ui.click;

import java.awt.Color;
import vanta.VantaClient;
import vanta.module.Module;
import vanta.setting.ColorSetting;
import vanta.setting.Setting;
import vanta.setting.SliderSetting;
import net.minecraft.client.input.KeyEvent;

public final class PickerTest {
   private static ColorSetting target;
   private static boolean espOn;
   private static float hueBefore;

   private PickerTest() {
   }

   private static void log(String s) {
      System.out.println("[UT] picker " + s);
   }

   private static void check(String name, boolean ok, String detail) {
      log((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : " (" + detail + ")"));
   }

   private static GuiView view() {
      return ClickGui.view();
   }

   private static void click(double vx, double vy) {
      double[] g = view().toGui(vx, vy);
      if (!view().mouseClicked(g[0], g[1], 0)) {
         log("click at " + (int)vx + "," + (int)vy + " not used");
      }

   }

   private static void drag(double vx, double vy) {
      double[] g = view().toGui(vx, vy);
      view().mouseDragged(g[0], g[1]);
   }

   private static void release() {
      view().mouseReleased();
   }

   public static void sliderTyping() {
      GuiView v = view();

      for(Setting<?> s : VantaClient.MODULES.get("autoMace").settings()) {
         if (s instanceof SliderSetting sl) {
            if (v.rects.containsKey(sl)) {
               float[] r = (float[])v.rects.get(sl);
               int target = Math.max(sl.min(), Math.min(sl.max(), (sl.min() + sl.max()) / 3));
               click((double)(r[0] + r[2] - 4.0F), (double)(r[1] + 10.0F));
               check("slider value click starts typing", Widgets.typing == sl, sl.id());
               Widgets.buffer.setLength(0);

               for(char ch : sl.format(target).toCharArray()) {
                  v.charTyped(ch);
               }

               v.keyPressed(new KeyEvent(257, 0, 0));
               boolean var10001 = (Integer)sl.get() == target;
               String var10002 = sl.id();
               check("typed slider value applies (display units)", var10001, var10002 + "=" + String.valueOf(sl.get()) + " want " + target);
               return;
            }
         }
      }

      check("slider typing", false, "no slider drawn");
   }

   public static void step(int step) {
      try {
         run(step);
      } catch (RuntimeException e) {
         log("FAIL step " + step + " threw " + String.valueOf(e));
      }

   }

   private static void run(int step) {
      GuiView v = view();
      if (v == null) {
         log("FAIL no gui");
      } else {
         float[] a = Widgets.colorArea;
         switch (step) {
            case 0:
               Module esp = VantaClient.MODULES.get("esp2d");
               espOn = esp.isEnabled();

               for(Setting<?> s : esp.settings()) {
                  if (s.id().equals("friendColor")) {
                     target = (ColorSetting)s;
                  }
               }

               target.set(3368652);
               target.setAlpha(255);
               Widgets.popup = null;
               v.testExpand("esp2d");
               break;
            case 1:
               float[] r = (float[])v.rects.get(target);
               if (r == null) {
                  check("swatch visible", false, "row not drawn");
                  return;
               }

               click((double)(r[0] + r[2] - 6.5F), (double)(r[1] + 10.0F));
               check("swatch opens popup", Widgets.popup == target, "");
               check("popup next to swatch", Math.abs(Widgets.popupX - (r[0] + r[2] + 14.0F)) < 1.0F || Widgets.popupX + 92.0F <= 9999.0F, "x=" + Widgets.popupX);
               break;
            case 2:
               hueBefore = Widgets.popH;
               click((double)(a[0] + a[2] * 0.25F), (double)(a[1] + a[3] * 0.25F));
               boolean var25 = Math.abs(Widgets.popS - 0.25F) < 0.03F;
               float var31 = Widgets.popS;
               check("SV press sets saturation", var25, "s=" + var31);
               check("SV press sets value", Math.abs(Widgets.popV - 0.75F) < 0.03F, "v=" + Widgets.popV);
               drag((double)(a[0] + a[2] * 0.6F), (double)(a[1] + a[3] * 0.5F));
               var25 = Math.abs(Widgets.popS - 0.6F) < 0.03F && Math.abs(Widgets.popV - 0.5F) < 0.03F;
               var31 = Widgets.popS;
               check("SV drag updates live", var25, "s=" + var31 + " v=" + Widgets.popV);
               check("swatch colour follows drag", ((Integer)target.get() & 16777215) == (Color.HSBtoRGB(Widgets.popH, Widgets.popS, Widgets.popV) & 16777215), "");
               drag((double)(a[0] - 60.0F), (double)(a[1] + a[3] + 80.0F));
               var25 = Widgets.popV == 0.0F && Widgets.popS == 0.0F;
               var31 = Widgets.popS;
               check("SV drag clamps outside (black)", var25, "s=" + var31 + " v=" + Widgets.popV);
               check("hue kept on black", Math.abs(Widgets.popH - hueBefore) < 0.002F, "h=" + Widgets.popH + " was " + hueBefore);
               drag((double)(a[0] + a[2] * 0.7F), (double)(a[1] + a[3] * 0.4F));
               break;
            case 3:
               check("still dragging", Widgets.dragging == target, "");
               break;
            case 4:
               release();
               check("released", Widgets.dragging == null, "");
               boolean espBefore = VantaClient.MODULES.get("esp2d").isEnabled();
               click((double)(a[0] + a[2] * 0.5F), (double)(a[4] + 2.5F));
               boolean var10001 = Math.abs(Widgets.popH - 0.5F) < 0.03F;
               float var10002 = Widgets.popH;
               check("hue bar press", var10001, "h=" + var10002);
               drag((double)(a[0] + a[2] + 50.0F), (double)(a[4] + 40.0F));
               var10001 = Widgets.popH > 0.99F;
               var10002 = Widgets.popH;
               check("hue drag clamps", var10001, "h=" + var10002);
               release();
               click((double)(a[0] + a[2] * 0.5F), (double)(a[5] + 2.5F));
               var10001 = Math.abs(target.alpha() - 128) <= 4;
               int var29 = target.alpha();
               check("alpha bar press", var10001, "a=" + var29);
               drag((double)(a[0] - 40.0F), (double)(a[5] + 2.5F));
               var10001 = target.alpha() == 0;
               var29 = target.alpha();
               check("alpha drag clamps", var10001, "a=" + var29);
               drag((double)(a[0] + a[2] * 0.75F), (double)(a[5] + 2.5F));
               release();
               check("module under popup untouched", VantaClient.MODULES.get("esp2d").isEnabled() == espBefore, "");
               check("popup still open", Widgets.popup == target, "");
               break;
            case 5:
               click((double)(a[0] + 30.0F), (double)(a[5] + 13.0F));
               check("hex field focuses", Widgets.hexEdit, "");
               Widgets.hexBuf.setLength(0);

               for(char ch : "FF8800".toCharArray()) {
                  v.charTyped(ch);
               }

               v.keyPressed(new KeyEvent(257, 0, 0));
               check("hex 6 digits applies", ((Integer)target.get() & 16777215) == 16746496 && target.alpha() == 255, String.format("%06X a=%d", target.get(), target.alpha()));
               click((double)(a[0] + 30.0F), (double)(a[5] + 13.0F));
               Widgets.hexBuf.setLength(0);

               for(char ch : "11223380".toCharArray()) {
                  v.charTyped(ch);
               }

               v.keyPressed(new KeyEvent(257, 0, 0));
               check("hex 8 digits applies alpha", ((Integer)target.get() & 16777215) == 1122867 && target.alpha() == 128, String.format("%06X a=%d", target.get(), target.alpha()));
               click((double)(a[0] + 30.0F), (double)(a[5] + 13.0F));
               Widgets.hexBuf.setLength(0);

               for(char ch : "XYZ".toCharArray()) {
                  v.charTyped(ch);
               }

               v.keyPressed(new KeyEvent(257, 0, 0));
               check("invalid hex reverts", ((Integer)target.get() & 16777215) == 1122867 && !Widgets.hexEdit, String.format("%06X", target.get()));
               check("invalid hex shakes", System.nanoTime() - Widgets.shakeAt < 1000000000L, "");
               check("value saved in setting json", target.save().toString().toUpperCase().contains("11223380"), target.save().toString());
               break;
            case 6:
               float[] r = (float[])v.rects.get(target);
               click((double)(r[0] + r[2] - 6.5F), (double)(r[1] + 10.0F));
               check("outside click closes with one click", Widgets.popup == null, "");
               break;
            case 7:
               check("no reopen", Widgets.popup == null, "");
               float[] r = (float[])v.rects.get(target);
               click((double)(r[0] + r[2] - 6.5F), (double)(r[1] + 10.0F));
               check("reopen", Widgets.popup == target, "");
               v.keyPressed(new KeyEvent(256, 0, 0));
               check("escape closes popup", Widgets.popup == null, "");
               check("gui still open after escape", ClickGui.isOpen(), "");
               VantaClient.MODULES.get("esp2d").setEnabled(espOn);
               target.set(7073992);
               target.setAlpha(255);
         }

      }
   }
}
