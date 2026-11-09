package vanta.ui;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

public enum UiScale {
   PERCENT_100(1.0F),
   PERCENT_125(1.25F),
   PERCENT_150(1.5F),
   PERCENT_200(2.0F);

   public static final UiScale[] ALL = values();
   public static final float REFERENCE_HEIGHT = 1152.0F;
   private static UiScale current = PERCENT_100;
   private static boolean auto = true;
   private final float factor;

   public String displayName() {
      return Math.round(this.factor * 100.0F) + "%";
   }

   public static UiScale current() {
      return current;
   }

   public static void set(UiScale scale) {
      current = scale;
      GuiState.changed();
   }

   public static boolean auto() {
      return auto;
   }

   public static void setAuto(boolean on) {
      auto = on;
      GuiState.changed();
   }

   public static float fit() {
      if (!auto) {
         return 1.0F;
      } else {
         Minecraft mc = Minecraft.getInstance();
         Window window = mc.getWindow();
         if (window != null && window.getHeight() > 0) {
            float fit = (float)window.getHeight() / 1152.0F;
            if (mc.options != null && (Integer)mc.options.guiScale().get() == 0 && (double)window.getGuiScale() > (double)0.0F) {
               fit *= 2.0F / (float)window.getGuiScale();
            }

            return Math.clamp(fit, 0.5F, 3.0F);
         } else {
            return 1.0F;
         }
      }
   }

   private UiScale(final float factor) {
      this.factor = factor;
   }

   public float factor() {
      return this.factor * fit();
   }

   // $FF: synthetic method
   private static UiScale[] $values() {
      return new UiScale[]{PERCENT_100, PERCENT_125, PERCENT_150, PERCENT_200};
   }
}
