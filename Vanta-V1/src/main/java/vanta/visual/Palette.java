package vanta.visual;

import java.util.List;
import vanta.setting.ColorSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Setting;
import vanta.setting.SettingGroup;
import vanta.setting.SliderSetting;
import vanta.ui.GuiState;
import vanta.ui.Theme;
import vanta.util.Time;

public final class Palette implements SettingGroup {
   public static final String[] NAMES = new String[]{"Client", "Aurora", "Sunset", "Ocean", "Sakura", "Mint", "Blaze", "Galaxy", "Frost", "Custom"};
   private static final int CLIENT = 0;
   private static final int CUSTOM = 9;
   private static final int[][] STOPS = new int[][]{null, {8346352, 2930301, 53759}, {16736109, 16761713}, {50943, 29439}, {16740312, 16758497, 11766015}, {4450683, 3733975}, {16266752, 16372771}, {9317858, 4849888, 51711}, {15136511, 10475762, 4886722}, null};
   private final EnumSetting palette;
   private final ColorSetting custom;
   private final SliderSetting speed;
   private static int[] clientStops;
   private static int clientAccent;
   private final int[] customStops = new int[2];

   public Palette(int defaultPalette) {
      this.palette = new EnumSetting("palette", defaultPalette, NAMES);
      this.custom = new ColorSetting("customColors", 12115120, 8346352);
      this.custom.gradientWhen(() -> true);
      this.custom.onChange(() -> {
         if ((Integer)this.palette.get() != 9) {
            this.palette.set(9);
         }

      });
      this.speed = new SliderSetting("flow", 0, 0, 100);
      this.palette.visibleWhen(() -> !GuiState.universalAccent());
      this.custom.visibleWhen(() -> !GuiState.universalAccent());
   }

   public List<Setting<?>> settings() {
      return List.of(this.palette, this.custom, this.speed);
   }

   public int at(float t) {
      float flow = Time.seconds() * (float)(Integer)this.speed.get() / 100.0F * 0.35F;
      return -16777216 | this.still(t + flow);
   }

   public int at(float t, float alpha) {
      return Fx.alpha(this.at(t), alpha);
   }

   public int primary() {
      return -16777216 | this.still(0.0F);
   }

   public int secondary() {
      return -16777216 | this.still(0.5F);
   }

   private int still(float t) {
      int index = GuiState.universalAccent() ? 0 : (Integer)this.palette.get();
      t -= (float)Math.floor((double)t);
      int[] stops;
      if (index == 0) {
         int accent = Theme.ACCENT & 16777215;
         if (accent != clientAccent || clientStops == null) {
            clientStops = new int[]{accent, Fx.shiftHue(accent, 0.08F), Fx.lighten(accent, 0.35F)};
            clientAccent = accent;
         }

         stops = clientStops;
      } else if (index == 9) {
         stops = this.customStops;
         stops[0] = (Integer)this.custom.get();
         stops[1] = this.custom.secondary();
      } else {
         stops = STOPS[Math.clamp((long)index, 0, STOPS.length - 1)];
      }

      float x = t < 0.5F ? t * 2.0F : 2.0F - t * 2.0F;
      float scaled = x * (float)(stops.length - 1);
      int i = Math.min((int)scaled, stops.length - 2);
      int color = Fx.mix(stops[i], stops[i + 1], Fx.smooth(scaled - (float)i));
      return color & 16777215;
   }
}
