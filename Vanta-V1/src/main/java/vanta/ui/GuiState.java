package vanta.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import vanta.VantaClient;
import vanta.config.StateStore;
import vanta.module.ModuleManager;
import vanta.ui.click.ClickGui;
import vanta.util.Clicks;
import vanta.util.ClientPearls;
import vanta.util.Lang;
import vanta.visual.hud.HudElements;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class GuiState {
   private static final List<CustomTheme> THEMES = new ArrayList();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static Data data = new Data();
   private static boolean loaded;
   private static boolean loading;
   private static boolean dirty;
   private static String lastModules;
   private static int moduleCheck;
   private static final LinkedHashSet<String> PINNED = new LinkedHashSet();
   private static boolean universalAccent = true;
   private static boolean previews = true;
   private static int backdrop = 60;
   private static boolean hideHud;
   private static boolean legitSilentSetting;
   private static InputConstants.Key silentKey;
   private static boolean silentToggle;
   private static boolean silentKeyWas;
   private static boolean silentKeyToggled;
   private static boolean silentSwitched;

   public static List<CustomTheme> customThemes() {
      return THEMES;
   }

   public static CustomTheme saveCustomTheme(String name, int[] colors, CustomTheme replacing) {
      CustomTheme theme = new CustomTheme(name, ((int[]) colors).clone());
      int at = replacing == null ? -1 : THEMES.indexOf(replacing);
      if (at >= 0) {
         THEMES.set(at, theme);
      } else {
         THEMES.add(theme);
      }

      changed();
      return theme;
   }

   public static void deleteCustomTheme(CustomTheme theme) {
      if (THEMES.remove(theme)) {
         changed();
      }

   }

   private GuiState() {
   }

   private static Path file() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("VantaClient").resolve("client.json");
   }

   public static void ensureLoaded() {
      if (!loaded) {
         loaded = true;
         loading = true;

         try {
            Path path = file();
            if (Files.exists(path, new LinkOption[0])) {
               Data read = (Data)GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Data.class);
               if (read != null) {
                  data = read;
               }
            }

            Lang.Language language = (Lang.Language)find(Lang.Language.ALL, data.language);
            if (language != null && language != Lang.current()) {
               Lang.set(language);
            }

            UiScale size = (UiScale)find(UiScale.ALL, data.size);
            if (size != null) {
               UiScale.set(size);
            }

            if (data.autoScale != null) {
               UiScale.setAuto(data.autoScale);
            }

            if (data.memorySaver != null) {
               MemorySaver.setEnabled(data.memorySaver);
            }

            Tooltips tooltips = (Tooltips)find(Tooltips.ALL, data.tooltips);
            if (tooltips != null) {
               Tooltips.set(tooltips);
            }

            if (data.clickSimulation != null) {
               Clicks.simulate = data.clickSimulation;
            }

            if (data.clientPearls != null) {
               ClientPearls.enabled = data.clientPearls;
            }

            if (data.clickHoldLow != null && data.clickHoldHigh != null) {
               Clicks.setHold(data.clickHoldLow, data.clickHoldHigh);
            }

            PINNED.clear();
            if (data.pinned != null) {
               PINNED.addAll(data.pinned);
            }

            if (data.universalAccent != null) {
               universalAccent = data.universalAccent;
            }

            if (data.previews != null) {
               previews = data.previews;
            }

            if (data.hideHud != null) {
               hideHud = data.hideHud;
            }

            if (data.backdrop != null) {
               backdrop = Math.clamp((long)data.backdrop, 0, 100);
            }

            if (data.legitSilent != null) {
               legitSilentSetting = data.legitSilent;
            }

            if (data.silentKey != null) {
               silentKey = ModuleManager.parseKey(data.silentKey);
            }

            if (data.silentToggle != null) {
               silentToggle = data.silentToggle;
            }

            THEMES.clear();
            if (data.themes != null) {
               for(SavedTheme saved : data.themes) {
                  if (saved != null && saved.name != null && saved.colors != null) {
                     int[] colors = new int[saved.colors.size()];

                     try {
                        for(int i = 0; i < colors.length; ++i) {
                           colors[i] = Integer.parseInt(((String)saved.colors.get(i)).replace("#", "").trim(), 16);
                        }

                        THEMES.add(new CustomTheme(saved.name, colors));
                     } catch (NumberFormatException var12) {
                     }
                  }
               }
            }
         } catch (Exception var13) {
            data = new Data();
         } finally {
            loading = false;
         }

      }
   }

   private static <E extends Enum<E>> E find(E[] values, String name) {
      if (name == null) {
         return null;
      } else {
         for(E value : values) {
            if (value.name().equals(name)) {
               return value;
            }
         }

         return null;
      }
   }

   public static boolean isPinned(String id) {
      return PINNED.contains(id);
   }

   public static void togglePinned(String id) {
      if (!PINNED.remove(id)) {
         PINNED.add(id);
      }

      changed();
   }

   public static Set<String> pinned() {
      return PINNED;
   }

   public static boolean universalAccent() {
      return universalAccent;
   }

   public static void setUniversalAccent(boolean on) {
      universalAccent = on;
      changed();
   }

   public static boolean previews() {
      return previews;
   }

   public static void setPreviews(boolean on) {
      previews = on;
      changed();
   }

   public static int backdrop() {
      return backdrop;
   }

   public static void setBackdrop(int percent) {
      backdrop = Math.clamp((long)percent, 0, 100);
      changed();
   }

   public static boolean hideHud() {
      return hideHud;
   }

   public static void setHideHud(boolean on) {
      hideHud = on;
      changed();
   }

   public static boolean hudHidden() {
      return (hideHud || ClickGui.isOpen()) && !HudElements.editing();
   }

   public static boolean legitSilentSetting() {
      return legitSilentSetting;
   }

   public static void setLegitSilent(boolean legit) {
      legitSilentSetting = legit;
      changed();
   }

   public static InputConstants.Key silentKey() {
      return silentKey;
   }

   public static boolean silentKeyBound() {
      return silentKey != null && silentKey != InputConstants.UNKNOWN;
   }

   public static void setSilentKey(InputConstants.Key key) {
      silentKey = key == null ? InputConstants.UNKNOWN : key;
      silentKeyToggled = false;
      changed();
   }

   public static boolean silentToggle() {
      return silentToggle;
   }

   public static void setSilentToggle(boolean toggle) {
      silentToggle = toggle;
      silentKeyToggled = false;
      changed();
   }

   public static boolean legitSilent() {
      return legitSilentSetting != silentSwitched;
   }

   public static void pollSilentKey(Minecraft client) {
      ensureLoaded();
      boolean down = false;
      if (silentKeyBound() && client.gui.screen() == null) {
         long window = client.getWindow().handle();
         boolean var10000;
         switch (silentKey.getType()) {
            case KEYSYM -> var10000 = GLFW.glfwGetKey(window, silentKey.getValue()) == 1;
            case MOUSE -> var10000 = GLFW.glfwGetMouseButton(window, silentKey.getValue()) == 1;
            default -> var10000 = false;
         }

         down = var10000;
      }

      if (!silentKeyBound()) {
         silentKeyToggled = false;
      } else if (down && !silentKeyWas && silentToggle) {
         silentKeyToggled = !silentKeyToggled;
      }

      boolean var4;
      label50: {
         label49: {
            silentKeyWas = down;
            if (silentKeyBound()) {
               if (silentToggle) {
                  if (silentKeyToggled) {
                     break label49;
                  }
               } else if (down) {
                  break label49;
               }
            }

            var4 = false;
            break label50;
         }

         var4 = true;
      }

      silentSwitched = var4;
   }

   public static void changed() {
      if (loaded && !loading) {
         dirty = true;
      }

   }

   public static void tick(boolean guiOpen) {
      if (loaded) {
         if (guiOpen && ++moduleCheck >= 10) {
            moduleCheck = 0;
            String modules = GSON.toJson(VantaClient.MODULES.snapshot());
            if (lastModules != null && !modules.equals(lastModules)) {
               StateStore.save();
            }

            lastModules = modules;
         } else if (!guiOpen) {
            lastModules = null;
         }

         if (dirty) {
            dirty = false;
            save();
         }

      }
   }

   private static void save() {
      data.language = Lang.current().name();
      data.size = UiScale.current().name();
      data.autoScale = UiScale.auto();
      data.memorySaver = MemorySaver.enabled();
      data.tooltips = Tooltips.current().name();
      data.clickSimulation = Clicks.simulate;
      data.clientPearls = ClientPearls.enabled;
      data.clickHoldLow = Clicks.holdLow;
      data.clickHoldHigh = Clicks.holdHigh;
      data.pinned = new ArrayList(PINNED);
      data.universalAccent = universalAccent;
      data.previews = previews;
      data.hideHud = hideHud;
      data.backdrop = backdrop;
      data.legitSilent = legitSilentSetting;
      data.silentKey = silentKey.getName();
      data.silentToggle = silentToggle;
      data.themes = new ArrayList();

      for(CustomTheme theme : THEMES) {
         SavedTheme saved = new SavedTheme();
         saved.name = theme.name();
         saved.colors = new ArrayList();

         for(int color : theme.colors()) {
            saved.colors.add(String.format("#%06X", color & 16777215));
         }

         data.themes.add(saved);
      }

      try {
         Path path = file();
         Files.createDirectories(path.getParent());
         Files.writeString(path, GSON.toJson(data), StandardCharsets.UTF_8);
      } catch (Exception var7) {
      }

      StateStore.save();
   }

   static {
      silentKey = InputConstants.UNKNOWN;
   }

   private static final class Data {
      String language;
      String size;
      Boolean autoScale;
      Boolean memorySaver;
      String tooltips;
      Boolean clickSimulation;
      Boolean clientPearls;
      Integer clickHoldLow;
      Integer clickHoldHigh;
      List<String> pinned;
      Boolean universalAccent;
      Boolean previews;
      Boolean hideHud;
      Integer backdrop;
      List<SavedTheme> themes;
      Boolean legitSilent;
      String silentKey;
      Boolean silentToggle;
   }

   private static final class SavedTheme {
      String name;
      List<String> colors;
   }

   public static record CustomTheme(String name, int[] colors) {
   }
}
