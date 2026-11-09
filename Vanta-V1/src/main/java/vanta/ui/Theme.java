package vanta.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import vanta.setting.ColorSetting;
import vanta.util.Colors;
import vanta.visual.Fx;

public final class Theme {
   private static final int[] rgb = new int[Theme.Slot.values().length];
   public static int ACCENT;
   public static int ON_ACCENT;
   public static int TEXT;
   public static int MUTED;
   public static int DIM;
   public static int SIDEBAR;
   public static int MAIN;
   public static int ROW;
   public static int OVERLAY;
   public static int FIELD;
   public static int CONTROL;
   public static int CONTROL_ACTIVE;
   public static int TRACK;
   public static int POPUP;
   public static int POPUP_BORDER;
   public static int HIGHLIGHT;
   public static int UNCHECKED;
   public static int TOGGLE_OFF;
   public static int TOGGLE_OFF_BORDER;
   public static int THUMB_OFF;
   private static boolean light;

   private Theme() {
   }

   private static void set(int index, int value) {
      int color = Colors.rgb(value);
      if (rgb[index] != color) {
         rgb[index] = color;
         recompute();
      }

      GuiState.changed();
   }

   public static void update() {
   }

   public static int get(Slot slot) {
      return rgb[slot.ordinal()];
   }

   public static void set(Slot slot, int value) {
      slot.setting().set(Colors.rgb(value));
   }

   public static int[] colors() {
      return (int[])rgb.clone();
   }

   public static void apply(int[] colors) {
      for(int i = 0; i < rgb.length && i < colors.length; ++i) {
         rgb[i] = colors[i] & 16777215;
      }

      recompute();
      GuiState.changed();
   }

   public static boolean showing(int[] colors) {
      for(int i = 0; i < rgb.length && i < colors.length; ++i) {
         if ((rgb[i] & 16777215) != (colors[i] & 16777215)) {
            return false;
         }
      }

      return true;
   }

   public static void applyPreset(Preset preset) {
      for(Slot slot : Theme.Slot.ALL) {
         rgb[slot.ordinal()] = preset.color(slot);
      }

      recompute();
      GuiState.changed();
   }

   public static int accentRgb() {
      return rgb[Theme.Slot.ACCENT.ordinal()];
   }

   public static void setAccent(int value) {
      set(Theme.Slot.ACCENT, value);
   }

   public static boolean isLight() {
      return light;
   }

   public static Map<String, String> save() {
      Map<String, String> out = new HashMap();

      for(Slot slot : Theme.Slot.ALL) {
         out.put(slot.id(), Colors.hex(rgb[slot.ordinal()]));
      }

      return out;
   }

   public static void load(Map<String, String> saved) {
      for(Slot slot : Theme.Slot.ALL) {
         String value = (String)saved.get(slot.id());
         if (value != null) {
            try {
               rgb[slot.ordinal()] = Colors.rgb(Integer.parseInt(value.replace("#", "").trim(), 16));
            } catch (NumberFormatException var7) {
            }
         }
      }

      recompute();
   }

   public static void loadLegacy(String theme, int accent) {
      Preset var10000;
      switch (theme == null ? "" : theme.toUpperCase(Locale.ROOT)) {
         case "DARK" -> var10000 = Theme.Preset.DARK;
         case "LIGHT" -> var10000 = Theme.Preset.LIGHT;
         case "SAKURA" -> var10000 = Theme.Preset.SAKURA;
         default -> var10000 = Theme.Preset.DEFAULT;
      }

      Preset preset = var10000;

      for(Slot slot : Theme.Slot.ALL) {
         rgb[slot.ordinal()] = preset.color(slot);
      }

      if (accent != 0 && (preset == Theme.Preset.DARK || preset == Theme.Preset.LIGHT)) {
         rgb[Theme.Slot.ACCENT.ordinal()] = preset == Theme.Preset.LIGHT ? Colors.rgb(Colors.darken(accent, 0.28F)) : Colors.rgb(accent);
      }

      recompute();
   }

   private static int mix(int a, int b, float t) {
      return Colors.lerp(-16777216 | a, -16777216 | b, t);
   }

   private static float luminance(int rgb) {
      return (0.2126F * (float)(rgb >> 16 & 255) + 0.7152F * (float)(rgb >> 8 & 255) + 0.0722F * (float)(rgb & 255)) / 255.0F;
   }

   private static void recompute() {
      int accent = rgb[Theme.Slot.ACCENT.ordinal()];
      int back = rgb[Theme.Slot.BACKGROUND.ordinal()];
      int surface = rgb[Theme.Slot.SURFACE.ordinal()];
      int text = rgb[Theme.Slot.TEXT.ordinal()];
      int muted = rgb[Theme.Slot.MUTED.ordinal()];
      int outline = rgb[Theme.Slot.OUTLINE.ordinal()];
      light = luminance(back) > 0.5F;
      ACCENT = -16777216 | accent;
      ON_ACCENT = luminance(accent) > 0.55F ? -15724528 : -1;
      TEXT = -16777216 | text;
      MUTED = -16777216 | muted;
      DIM = mix(muted, text, 0.2F);
      MAIN = -16777216 | back;
      SIDEBAR = mix(back, surface, 0.35F);
      ROW = -16777216 | surface;
      OVERLAY = mix(back, surface, 0.5F);
      FIELD = mix(back, surface, 0.25F);
      CONTROL = mix(surface, text, 0.06F);
      CONTROL_ACTIVE = mix(surface, text, 0.16F);
      TRACK = mix(surface, text, 0.12F);
      POPUP = mix(back, surface, 0.7F);
      POPUP_BORDER = -16777216 | outline;
      HIGHLIGHT = mix(surface, text, 0.05F);
      UNCHECKED = mix(surface, text, 0.22F);
      TOGGLE_OFF = mix(back, surface, 0.5F);
      TOGGLE_OFF_BORDER = -16777216 | outline;
      THUMB_OFF = mix(surface, text, 0.3F);
      Fx.themeChanged(rgb[Theme.Slot.HUD_BACKGROUND.ordinal()], rgb[Theme.Slot.HUD_TEXT.ordinal()]);
   }

   static {
      for(Slot slot : Theme.Slot.ALL) {
         rgb[slot.ordinal()] = Theme.Preset.DEFAULT.color(slot);
      }

      recompute();
   }

   public static enum Slot {
      ACCENT("accent", "Accent", "The one colour that shows state: on, selected, filled, focused"),
      BACKGROUND("background", "Background", "Panels and cards of the GUIs"),
      SURFACE("surface", "Surface", "Rows, fields, buttons and popups on the panels"),
      TEXT("text", "Text", "Names, values and titles"),
      MUTED("muted", "Muted text", "Hints, counts and secondary text"),
      OUTLINE("outline", "Outline", "Edges of panels and the lines between parts"),
      HUD_BACKGROUND("hudBackground", "HUD background", "The panels of HUD elements (watermark, module list, ...)"),
      HUD_TEXT("hudText", "HUD text", "Text and edges of HUD elements");

      public static final Slot[] ALL = values();
      private final String id;
      private final String label;
      private final String help;
      private final ColorSetting setting;

      private Slot(String id, String label, String help) {
         this.id = id;
         this.label = label;
         this.help = help;
         int index = this.ordinal();
         ColorSetting color = new ColorSetting(id, Theme.Preset.DEFAULT.colors[index]);
         color.bind(() -> Theme.rgb[index], (value) -> Theme.set(index, value), label);
         this.setting = color;
      }

      public String id() {
         return this.id;
      }

      public String label() {
         return this.label;
      }

      public String help() {
         return this.help;
      }

      public ColorSetting setting() {
         return this.setting;
      }

      public int rgb() {
         return Theme.rgb[this.ordinal()];
      }

      // $FF: synthetic method
      private static Slot[] $values() {
         return new Slot[]{ACCENT, BACKGROUND, SURFACE, TEXT, MUTED, OUTLINE, HUD_BACKGROUND, HUD_TEXT};
      }
   }

   public static enum Preset {
      DEFAULT("Default", new int[]{10475762, 1184536, 1710882, 16053752, 9343907, 2829363, 1052949, 15922165}),
      DARK("Dark", new int[]{12115376, 986895, 1842204, 15395562, 8750469, 2763306, 1052688, 15395562}),
      LIGHT("Light", new int[]{3112896, 15987958, 16777215, 1382172, 7041152, 14146272, 15921906, 1315860}),
      SAKURA("Sakura", new int[]{16752325, 1838100, 3020066, 16771827, 13212594, 5056064, 1706774, 16771827}),
      FROST("Frost", new int[]{10475762, 594717, 1188399, 15398652, 9417671, 2771552, 462874, 15398652}),
      CRIMSON("Crimson", new int[]{15222111, 1315862, 1973793, 15987701, 9276822, 2894896, 986897, 15987701}),
      MIDNIGHT("Midnight", new int[]{8162559, 855834, 1448491, 15659263, 9081013, 2501445, 724246, 15659263}),
      OCEAN("Ocean", new int[]{3065062, 662050, 1124402, 15268092, 8825526, 1982026, 529691, 15268092}),
      EMERALD("Emerald", new int[]{3462041, 792338, 1254428, 15399153, 8824984, 2046510, 594702, 15399153}),
      SUNSET("Sunset", new int[]{16747084, 1708301, 2759187, 16773608, 11901578, 4532509, 1379594, 16773608}),
      AMETHYST("Amethyst", new int[]{11893759, 1248796, 1971756, 16051967, 10654904, 3351880, 985879, 16051967}),
      NEON("Neon", new int[]{16727717, 920084, 1577506, 16248575, 10259120, 3023424, 657168, 16248575}),
      MOCHA("Mocha", new int[]{13935475, 1380364, 2234900, 16116963, 11048842, 3812643, 1117450, 16116963}),
      MONO("Mono", new int[]{15921906, 921102, 1710618, 15921906, 9079434, 2763306, 723723, 15921906}),
      PAPER("Paper", new int[]{14706234, 16184044, 16777215, 2038294, 8024167, 14538187, 16052200, 2038294}),
      ROSE("Rose", new int[]{16033464, 1577492, 2431775, 16510962, 11573921, 4008753, 1248784, 16510962});

      public static final Preset[] ALL = values();
      private final String label;
      private final int[] colors;

      private Preset(String label, int... colors) {
         this.label = label;
         this.colors = colors;
      }

      public String label() {
         return this.label;
      }

      public int color(Slot slot) {
         return this.colors[slot.ordinal()];
      }

      public int[] colors() {
         return (int[])this.colors.clone();
      }

      // $FF: synthetic method
      private static Preset[] $values() {
         return new Preset[]{DEFAULT, DARK, LIGHT, SAKURA, FROST, CRIMSON, MIDNIGHT, OCEAN, EMERALD, SUNSET, AMETHYST, NEON, MOCHA, MONO, PAPER, ROSE};
      }
   }
}
