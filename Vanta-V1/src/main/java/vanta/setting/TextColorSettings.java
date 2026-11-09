package vanta.setting;

import java.util.List;
import vanta.render.Text;
import vanta.ui.Theme;
import vanta.util.Colors;
import vanta.util.Time;

public final class TextColorSettings implements SettingGroup {
   private static final String[] MODES = new String[]{"Accent", "Solid", "Gradient"};
   private static final int ACCENT = 0;
   private final EnumSetting mode;
   private final ColorSetting color;

   public TextColorSettings() {
      this.mode = new EnumSetting("color", 0, MODES);
      this.color = new ColorSetting("textColor", 12115376, 12045544);
      this.color.visibleWhen(() -> (Integer)this.mode.get() != 0);
      this.color.gradientWhen(() -> (Integer)this.mode.get() == 2);
   }

   public List<Setting<?>> settings() {
      return List.of(this.mode, this.color);
   }

   public Text.ColorAt colorAt(float scale) {
      if ((Integer)this.mode.get() == 0) {
         return (x) -> Theme.ACCENT;
      } else {
         int primary = Colors.opaque((Integer)this.color.get());
         if ((Integer)this.mode.get() != 2) {
            return (x) -> primary;
         } else {
            int secondary = Colors.opaque(this.color.secondary());
            float phase = Time.seconds() / 2.0F;
            float length = 120.0F * scale;
            return (x) -> {
               float wave = (x / length - phase) % 1.0F;
               if (wave < 0.0F) {
                  ++wave;
               }

               return Colors.lerp(primary, secondary, wave < 0.5F ? wave * 2.0F : 2.0F - wave * 2.0F);
            };
         }
      }
   }
}
