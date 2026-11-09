package vanta.ui.click;

import com.mojang.blaze3d.platform.InputConstants.Type;
import vanta.VantaClient;
import vanta.config.StateStore;
import vanta.mixin.accessor.ClickGuiCreativeAccessor;
import vanta.setting.ColorSetting;
import vanta.ui.GuiState;
import vanta.ui.MemorySaver;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

public final class ClickGui {
   private static GuiView view;
   private static boolean finishing;
   private static long openedAt;
   private static boolean glassOnly;

   private ClickGui() {
   }

   public static void init() {
      ScreenEvents.AFTER_INIT.register((ScreenEvents.AfterInit)(mc, screen, width, height) -> attach(screen));
      ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(mc) -> {
         GuiState.ensureLoaded();
         GuiState.tick(view != null);
         if (view == null && mc.gui.screen() instanceof Host) {
            mc.gui.setScreen((Screen)null);
         }

         if (MemorySaver.enabled()) {
            MemorySaver.setEnabled(false);
         }

      });
   }

   static void requestGlassOnly(boolean on) {
      glassOnly = on;
   }

   public static boolean takeGlassOnly() {
      boolean on = glassOnly;
      glassOnly = false;
      return on;
   }

   static GuiView view() {
      return view;
   }

   public static boolean isOpen() {
      return view != null && !view.closing();
   }

   public static void open() {
      Minecraft mc = Minecraft.getInstance();
      GuiPrefs.load();
      GuiState.ensureLoaded();
      if (view != null) {
         view.reopen();
      } else {
         openedAt = System.currentTimeMillis();
         view = new GuiView();
         GuiSounds.open();
         if (mc.gui.screen() == null) {
            mc.gui.setScreen(new Host());
         }

      }
   }

   public static void close() {
      if (view != null) {
         view.close();
      }

   }

   public static void toggle() {
      if (isOpen()) {
         close();
      } else {
         open();
      }

   }

   static void finish() {
      if (view != null && !finishing) {
         finishing = true;

         try {
            view = null;
            Widgets.typing = null;
            Widgets.listening = null;
            Widgets.popup = null;
            Widgets.release();
            GuiPrefs.save();
            StateStore.changed();
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.screen() instanceof Host) {
               mc.gui.setScreen((Screen)null);
            }
         } finally {
            finishing = false;
         }

      }
   }

   static void openHudEditor() {
      Minecraft mc = Minecraft.getInstance();
      Screen previous = mc.gui.screen() instanceof Host ? null : mc.gui.screen();
      finishing = true;

      try {
         view = null;
         Widgets.typing = null;
         Widgets.listening = null;
         GuiPrefs.save();
      } finally {
         finishing = false;
      }

      GuiSounds.expand(true);
      mc.gui.setScreen(new HudEditorScreen(previous));
   }

   public static Screen replacementFor(Screen screen) {
      Minecraft mc = Minecraft.getInstance();
      return (Screen)(screen == null && view != null && !view.closing() && !finishing && mc.level != null && mc.player != null && !mc.player.isDeadOrDying() ? new Host() : screen);
   }

   private static void attach(Screen screen) {
      if (!(screen instanceof HudEditorScreen)) {
         ScreenEvents.afterExtract(screen).register((ScreenEvents.AfterExtract)(s, graphics, mouseX, mouseY, delta) -> {
            if (view != null) {
               try {
                  view.render(graphics, mouseX, mouseY);
               } catch (RuntimeException error) {
                  LoggerFactory.getLogger("vanta").error("Click GUI render failed", error);
               }
            }

         });
         ScreenMouseEvents.allowMouseClick(screen).register((ScreenMouseEvents.AllowMouseClick)(s, event) -> {
            if (view == null) {
               return true;
            } else {
               return !view.mouseClicked(event.x(), event.y(), event.button()) && !view.covers(event.x(), event.y()) && !(s instanceof Host);
            }
         });
         ScreenMouseEvents.allowMouseRelease(screen).register((ScreenMouseEvents.AllowMouseRelease)(s, event) -> view == null || !view.mouseReleased());
         ScreenMouseEvents.allowMouseDrag(screen).register((ScreenMouseEvents.AllowMouseDrag)(s, event, dx, dy) -> view == null || !view.mouseDragged(event.x(), event.y()));
         ScreenMouseEvents.allowMouseScroll(screen).register((ScreenMouseEvents.AllowMouseScroll)(s, x, y, h, v) -> view == null || !view.mouseScrolled(x, y, v) && !view.covers(x, y));
      }
   }

   public static boolean onKey(KeyEvent event, int action) {
      Minecraft mc = Minecraft.getInstance();
      if (view != null && !view.closing()) {
         if (action == 0) {
            return false;
         } else {
            return System.currentTimeMillis() - openedAt < 150L && isBind(event) ? true : view.keyPressed(event);
         }
      } else if (action == 1 && isBind(event) && mc.gui.overlay() == null) {
         Screen var4 = mc.gui.screen();
         if (var4 instanceof HudEditorScreen) {
            HudEditorScreen editor = (HudEditorScreen)var4;
            editor.onClose();
            return true;
         } else if (typingInScreen(mc.gui.screen())) {
            return false;
         } else {
            open();
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean onChar(int codepoint) {
      return view != null && !view.closing() && view.charTyped(codepoint);
   }

   private static boolean isBind(KeyEvent event) {
      GuiPrefs.load();
      return GuiPrefs.bound() && GuiPrefs.key.getType() == Type.KEYSYM && GuiPrefs.key.getValue() == event.key();
   }

   private static boolean typingInScreen(Screen screen) {
      if (screen == null) {
         return false;
      } else if (!(screen instanceof ChatScreen) && !(screen instanceof AbstractSignEditScreen) && !(screen instanceof BookEditScreen)) {
         return screen instanceof CreativeModeInventoryScreen && ((ClickGuiCreativeAccessor)screen).mace$searchBox().isFocused() ? true : focusedText(screen);
      } else {
         return true;
      }
   }

   private static boolean focusedText(ContainerEventHandler parent) {
      GuiEventListener focused = parent.getFocused();
      if (focused instanceof EditBox box) {
         if (box.isFocused()) {
            return true;
         }
      }

      if (!(focused instanceof MultiLineEditBox)) {
         if (focused instanceof ContainerEventHandler) {
            ContainerEventHandler child = (ContainerEventHandler)focused;
            if (child != parent) {
               return focusedText(child);
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static void testExpand(String id) {
      if (view != null) {
         view.testExpand(id);
      }

   }

   public static void testSearch(String text) {
      if (view != null) {
         view.testSearch(text);
      }

   }

   public static void testTab(int tab) {
      if (view != null) {
         view.testTab(tab);
      }

   }

   public static void testColorPopup(String module, String setting) {
      VantaClient.MODULES.find(module).ifPresent((m) -> m.settings().stream().filter((s) -> s.id().equals(setting)).findFirst().ifPresent((s) -> {
            Widgets.popup = (ColorSetting)s;
            Widgets.popupTitle = null;
            Widgets.popupX = 425.0F;
            Widgets.popupY = 225.0F;
         }));
      if (VantaClient.MODULES.find(module).isEmpty()) {
         Widgets.popup = null;
      }

   }

   public static void testAccent(boolean on) {
      Widgets.popup = on ? GuiPrefs.ACCENT : null;
      Widgets.popupTitle = "Accent";
      Widgets.popupX = 560.0F;
      Widgets.popupY = 38.0F;
   }

   public static void testConfigs() {
      if (view != null) {
         view.testConfigs();
      }

   }

   public static void testAccentColor(int rgb) {
      GuiPrefs.setAccent(rgb);
   }

   public static void testAlpha() {
      VantaClient.MODULES.find("esp2d").ifPresent((m) -> m.settings().stream().filter((s) -> s.id().equals("friendColor")).findFirst().ifPresent((s) -> ((ColorSetting)s).setAlpha(120)));
   }

   public static void testResetLayout() {
      if (view != null) {
         view.resetLayout();
      }

   }

   public static float[] testDragPlate(float dx, float dy) {
      return view != null ? view.testDragPlate(dx, dy) : null;
   }

   public static float testScale(int percent) {
      if (percent > 0) {
         GuiPrefs.scale = percent;
         GuiPrefs.changed();
      }

      return view != null ? view.testScale() : -1.0F;
   }

   public static float[] testPlatePref() {
      return GuiPrefs.plate;
   }

   public static void drawPlateOnHud(GuiGraphicsExtractor g) {
      if (view == null) {
         GuiView.drawPlateOnHud(g);
      }

   }

   public static final class Host extends Screen {
      public Host() {
         super(Component.literal("VantaClient"));
      }

      public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      }

      public boolean isPauseScreen() {
         return false;
      }

      public boolean shouldCloseOnEsc() {
         return false;
      }
   }
}
