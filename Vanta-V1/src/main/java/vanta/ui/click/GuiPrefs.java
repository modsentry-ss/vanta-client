package vanta.ui.click;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vanta.module.Category;
import vanta.module.ModuleManager;
import vanta.setting.BoolSetting;
import vanta.setting.ColorSetting;
import vanta.setting.KeySetting;
import vanta.setting.Measure;
import vanta.setting.RangeSetting;
import vanta.setting.Setting;
import vanta.setting.SliderSetting;
import vanta.ui.GuiState;
import vanta.ui.Theme;
import vanta.util.Clicks;
import vanta.util.ClientPearls;
import net.minecraft.client.Minecraft;

final class GuiPrefs {
   static InputConstants.Key key = InputConstants.getKey("key.keyboard.right.shift");
   static int scale = 100;
   static int anim = 100;
   static boolean blur = false;
   static int dim = 0;
   static int accent = 10851577;
   static String activeConfig = "";
   static boolean sounds = true;
   static int volume = 100;
   static boolean hoverSounds = false;
   static final Map<String, float[]> PANELS = new LinkedHashMap();
   static float[] plate;
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static boolean loaded;
   private static boolean dirty;
   private static List<Section> sections;
   static final ColorSetting ACCENT = (ColorSetting)(new ColorSetting("guiAccent", 10851577)).bind(() -> accent, (v) -> {
      setAccent(v);
      changed();
   }, "Accent");
   static final Map<Setting<?>, Runnable> AFTER_EDIT = new IdentityHashMap();

   private GuiPrefs() {
   }

   static float animSpeed() {
      return Math.max(0.25F, (float)anim / 100.0F);
   }

   private static Path file() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("VantaClient").resolve("clickgui.json");
   }

   static void load() {
      if (!loaded) {
         loaded = true;

         try {
            Path path = file();
            if (!Files.exists(path, new LinkOption[0])) {
               return;
            }

            Data d = (Data)GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Data.class);
            if (d == null) {
               return;
            }

            if (d.key != null) {
               key = ModuleManager.parseKey(d.key);
            }

            if (d.scale != null) {
               scale = Math.clamp((long)d.scale, 60, 160);
            }

            if (d.anim != null) {
               anim = Math.clamp((long)d.anim, 25, 300);
            }

            if (d.blur != null) {
               blur = d.blur;
            }

            if (d.dim != null) {
               dim = Math.clamp((long)d.dim, 0, 100);
            }

            if (d.accent != null) {
               accent = d.accent & 16777215;
            }

            if (d.activeConfig != null) {
               activeConfig = d.activeConfig;
            }

            if (d.sounds != null) {
               sounds = d.sounds;
            }

            if (d.volume != null) {
               volume = Math.clamp((long)d.volume, 0, 100);
            }

            if (d.hoverSounds != null) {
               hoverSounds = d.hoverSounds;
            }

            if (d.panels != null) {
               PANELS.putAll(d.panels);
            }

            if (d.plate != null && d.plate.length >= 2) {
               plate = new float[]{Math.clamp(d.plate[0], 0.0F, 1.0F), Math.clamp(d.plate[1], 0.0F, 1.0F)};
            }
         } catch (Exception var2) {
         }

         setAccent(accent);
      }
   }

   static void changed() {
      dirty = true;
   }

   static void saveIfDirty() {
      if (dirty) {
         save();
      }

   }

   static void save() {
      dirty = false;
      Data d = new Data();
      d.key = key.getName();
      d.scale = scale;
      d.anim = anim;
      d.blur = blur;
      d.dim = dim;
      d.accent = accent;
      d.activeConfig = activeConfig;
      d.sounds = sounds;
      d.volume = volume;
      d.hoverSounds = hoverSounds;
      d.panels = PANELS;
      d.plate = plate;

      try {
         Path path = file();
         Files.createDirectories(path.getParent());
         Files.writeString(path, GSON.toJson(d), StandardCharsets.UTF_8);
      } catch (Exception var2) {
      }

   }

   static boolean bound() {
      return key != null && key != InputConstants.UNKNOWN;
   }

   static String keyName() {
      return bound() ? key.getDisplayName().getString() : "None";
   }

   static void setAccent(int rgb) {
      accent = rgb & 16777215;
      if ((Theme.accentRgb() & 16777215) != accent) {
         Theme.setAccent(accent);
      }

   }

   static List<Section> sections() {
      if (sections != null) {
         return sections;
      } else {
         List<Section> out = new ArrayList();
         List<Setting<?>> gui = new ArrayList();
         gui.add((new KeySetting("guiKey")).bind(() -> key, (k) -> {
            key = k;
            changed();
         }, "GUI key"));
         gui.add(ACCENT);
         gui.add((new SliderSetting("guiScale", 100, 60, 160, Measure.PERCENT)).bind(() -> scale, (v) -> {
            scale = v;
            changed();
         }, "GUI scale (applies on reopen)"));
         gui.add((new SliderSetting("animSpeed", 100, 25, 300, Measure.PERCENT)).bind(() -> anim, (v) -> {
            anim = v;
            changed();
         }, "Animation speed"));
         gui.add((new BoolSetting("blur", false)).bind(() -> blur, (v) -> {
            blur = v;
            changed();
         }, "Blur background"));
         gui.add((new SliderSetting("dim", 0, 0, 100, Measure.PERCENT)).bind(() -> dim, (v) -> {
            dim = v;
            changed();
         }, "Background dim"));
         out.add(new Section("Interface", gui));
         List<Setting<?>> sound = new ArrayList();
         sound.add((new BoolSetting("sounds", true)).bind(() -> sounds, (v) -> {
            sounds = v;
            changed();
         }, "UI sounds"));
         SliderSetting vol = new SliderSetting("volume", 100, 0, 100, Measure.PERCENT);
         vol.bind(() -> volume, (v) -> {
            volume = v;
            changed();
         }, "Volume");
         vol.visibleWhen(() -> sounds);
         sound.add(vol);
         BoolSetting hover = new BoolSetting("hoverSounds", false);
         hover.bind(() -> hoverSounds, (v) -> {
            hoverSounds = v;
            changed();
         }, "Hover ticks");
         hover.visibleWhen(() -> sounds);
         sound.add(hover);
         out.add(new Section("Sounds", sound));
         List<Setting<?>> client = new ArrayList();
         client.add((new BoolSetting("clickSimulation", true)).bind(() -> Clicks.simulate, (v) -> {
            Clicks.simulate = v;
            GuiState.changed();
         }, "Click simulation"));
         RangeSetting hold = new RangeSetting("clickHold", Clicks.holdLow, Clicks.holdHigh, 15, 200, Measure.MILLIS);
         hold.visibleWhen(() -> Clicks.simulate);
         AFTER_EDIT.put(hold, (Runnable)() -> {
            Clicks.setHold(hold.low(), hold.high());
            GuiState.changed();
         });
         client.add(hold);
         client.add((new BoolSetting("clientPearls", false)).bind(() -> ClientPearls.enabled, (v) -> {
            ClientPearls.setEnabled(v);
            GuiState.changed();
         }, "Client pearls"));
         client.add((new BoolSetting("legitSilent", false)).bind(GuiState::legitSilentSetting, GuiState::setLegitSilent, "Legit silent aim"));
         client.add((new KeySetting("silentKey")).bind(GuiState::silentKey, GuiState::setSilentKey, "Silent aim key"));
         client.add((new BoolSetting("silentToggle", false)).bind(GuiState::silentToggle, GuiState::setSilentToggle, "Silent key toggles"));
         client.add((new BoolSetting("hideHud", false)).bind(GuiState::hideHud, GuiState::setHideHud, "Hide client HUD"));
         out.add(new Section("Client", client));
         sections = out;
         return out;
      }
   }

   static float[] panel(Category category) {
      return (float[])PANELS.get(category.id());
   }

   static void panel(Category category, float x, float y, boolean collapsed) {
      PANELS.put(category.id(), new float[]{x, y, collapsed ? 1.0F : 0.0F});
      changed();
   }

   private static final class Data {
      String key;
      Integer scale;
      Integer anim;
      Boolean blur;
      Integer dim;
      Integer accent;
      String activeConfig;
      Boolean sounds;
      Integer volume;
      Boolean hoverSounds;
      Map<String, float[]> panels;
      float[] plate;
   }

   static record Section(String title, List<Setting<?>> settings) {
   }
}
