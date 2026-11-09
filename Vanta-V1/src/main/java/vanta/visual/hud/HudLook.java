package vanta.visual.hud;

import java.util.ArrayList;
import java.util.List;
import vanta.setting.BoolSetting;
import vanta.setting.ColorSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.Setting;
import vanta.setting.SettingGroup;
import vanta.setting.SliderSetting;

public final class HudLook implements SettingGroup {
   public static final String[] SHAPES = new String[]{"Rounded", "Squared", "Cut"};
   public static final String[] BACKGROUNDS = new String[]{"Theme", "Flat", "Custom", "Gradient"};
   public static final String[] BORDERS = new String[]{"Hairline", "None", "Solid", "Accent"};
   public static final String[] SHADOWS = new String[]{"Soft", "Hard", "Glow", "None"};
   public static final String[] BARS = new String[]{"None", "Left", "Top", "Bottom", "Right"};
   public static final String[] TONES = new String[]{"Theme", "Custom"};
   private final EnumSetting shape;
   private final SliderSetting corners;
   private final EnumSetting background;
   private final ColorSetting backgroundColor;
   private final SliderSetting backgroundOpacity;
   private final EnumSetting border;
   private final ColorSetting borderColor;
   private final SliderSetting borderWidth;
   private final EnumSetting shadow;
   private final SliderSetting shadowStrength;
   private final EnumSetting accentBar;
   private final EnumSetting accent;
   private final ColorSetting accentColor;
   private final EnumSetting textTone;
   private final ColorSetting textToneColor;
   private final BoolSetting textShadow;
   private final BoolSetting animations;
   private final List<Setting<?>> settings;
   private final boolean ownAccent;
   private final boolean ownText;
   private final HudKit.Look look;

   public HudLook() {
      this(true, true);
   }

   public HudLook(boolean accent, boolean text) {
      this.shape = new EnumSetting("shape", 0, SHAPES);
      this.corners = new SliderSetting("corners", 100, 0, 200, Measure.PERCENT);
      this.background = new EnumSetting("background", 0, BACKGROUNDS);
      this.backgroundColor = new ColorSetting("backgroundColor", 1776672, 2764613);
      this.backgroundOpacity = new SliderSetting("backgroundOpacity", Math.round(90.0F), 0, 100, Measure.PERCENT);
      this.border = new EnumSetting("border", 0, BORDERS);
      this.borderColor = new ColorSetting("borderColor", 16777215);
      this.borderWidth = (new SliderSetting("borderWidth", 1, 1, 6)).withFormat((value) -> value + " px");
      this.shadow = new EnumSetting("shadow", 0, SHADOWS);
      this.shadowStrength = new SliderSetting("shadowStrength", 100, 0, 200, Measure.PERCENT);
      this.accentBar = new EnumSetting("accentBar", 0, BARS);
      this.accent = new EnumSetting("accent", 0, TONES);
      this.accentColor = new ColorSetting("accentColor", 10475762);
      this.textTone = new EnumSetting("textTone", 0, TONES);
      this.textToneColor = new ColorSetting("textToneColor", 15922165);
      this.textShadow = new BoolSetting("textShadow", false);
      this.animations = new BoolSetting("animations", true);
      this.settings = new ArrayList();
      this.look = new HudKit.Look();
      this.ownAccent = accent;
      this.ownText = text;
      this.corners.visibleWhen(() -> (Integer)this.shape.get() != 1);
      this.backgroundColor.visibleWhen(() -> (Integer)this.background.get() == 2 || (Integer)this.background.get() == 3);
      this.backgroundColor.gradientWhen(() -> (Integer)this.background.get() == 3);
      this.borderColor.visibleWhen(() -> (Integer)this.border.get() == 2);
      this.borderWidth.visibleWhen(() -> (Integer)this.border.get() == 2 || (Integer)this.border.get() == 3);
      this.shadowStrength.visibleWhen(() -> (Integer)this.shadow.get() != 3);
      this.accentColor.visibleWhen(() -> (Integer)this.accent.get() == 1);
      this.textToneColor.visibleWhen(() -> (Integer)this.textTone.get() == 1);

      for(Setting<?> described : List.of(this.shape, this.corners, this.background, this.backgroundOpacity, this.border, this.shadow, this.accentBar, this.accent, this.textTone, this.animations)) {
         described.describe();
      }

      this.settings.addAll(List.of(this.shape, this.corners, this.background, this.backgroundColor, this.backgroundOpacity, this.border, this.borderColor, this.borderWidth, this.shadow, this.shadowStrength, this.accentBar));
      if (accent) {
         this.settings.add(this.accent);
         this.settings.add(this.accentColor);
      }

      if (text) {
         this.settings.add(this.textTone);
         this.settings.add(this.textToneColor);
      }

      this.settings.add(this.textShadow);
      this.settings.add(this.animations);
   }

   public List<Setting<?>> settings() {
      return this.settings;
   }

   public boolean squared() {
      return (Integer)this.shape.get() == 1;
   }

   public HudKit.Look look() {
      HudKit.Look look = this.look;
      look.shape = (Integer)this.shape.get();
      look.corners = (float)(Integer)this.corners.get() / 100.0F;
      look.background = (Integer)this.background.get();
      look.backgroundFrom = (Integer)this.backgroundColor.get() & 16777215;
      look.backgroundTo = this.backgroundColor.secondary() & 16777215;
      look.opacity = (float)(Integer)this.backgroundOpacity.get() / 100.0F;
      look.border = (Integer)this.border.get();
      look.borderColor = (Integer)this.borderColor.get() & 16777215;
      look.borderWidth = (Integer)this.borderWidth.get();
      look.shadow = (Integer)this.shadow.get();
      look.shadowStrength = (float)(Integer)this.shadowStrength.get() / 100.0F;
      look.accentBar = (Integer)this.accentBar.get();
      look.accent = this.ownAccent && (Integer)this.accent.get() == 1 ? (Integer)this.accentColor.get() & 16777215 : -1;
      look.text = this.ownText && (Integer)this.textTone.get() == 1 ? (Integer)this.textToneColor.get() & 16777215 : -1;
      look.textShadow = (Boolean)this.textShadow.get();
      look.animate = (Boolean)this.animations.get();
      return look;
   }
}
