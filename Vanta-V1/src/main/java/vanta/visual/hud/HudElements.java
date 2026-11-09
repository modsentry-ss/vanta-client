package vanta.visual.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vanta.VantaClient;
import vanta.config.StateStore;
import vanta.module.HudModule;
import vanta.module.Module;
import vanta.setting.Setting;
import vanta.setting.SliderSetting;
import vanta.ui.DraggableHud;
import vanta.ui.click.HudEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

public final class HudElements {
   private static final Map<HudModule, DraggableHud> adapters = new HashMap();

   private HudElements() {
   }

   public static List<DraggableHud> all() {
      List<DraggableHud> elements = new ArrayList();

      for(Module module : VantaClient.MODULES.all()) {
         if (module instanceof HudModule hud) {
            elements.add((DraggableHud)adapters.computeIfAbsent(hud, Adapter::new));
         }
      }

      VanillaHud.init();
      elements.addAll(HudSlot.all());
      return elements;
   }

   public static boolean arranging() {
      Screen screen = Minecraft.getInstance().gui.screen();
      return screen instanceof ChatScreen || screen instanceof HudEditorScreen;
   }

   public static boolean editing() {
      return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
   }

   public static void save() {
      StateStore.save();
      HudSlot.saveAll();
   }

   private static final class Adapter implements DraggableHud {
      private final HudModule module;
      private final float[] bounds = new float[4];

      Adapter(HudModule module) {
         this.module = module;
      }

      public String getHudName() {
         return this.module.name();
      }

      public float[] getHudBounds() {
         if (!(this.module.width() <= 0.0F) && !(this.module.height() <= 0.0F)) {
            this.bounds[0] = this.module.x();
            this.bounds[1] = this.module.y();
            this.bounds[2] = this.module.width();
            this.bounds[3] = this.module.height();
            return this.bounds;
         } else {
            return null;
         }
      }

      public void setHudPosition(float x, float y) {
         Minecraft mc = Minecraft.getInstance();
         this.module.moveTo(x, y, (float)mc.getWindow().getGuiScaledWidth(), (float)mc.getWindow().getGuiScaledHeight());
      }

      public void resetHudPosition() {
         for(Setting<?> setting : this.module.settings()) {
            if (setting.id().equals("position")) {
               setting.reset();
            }
         }

      }

      public boolean isHudEnabled() {
         return this.module.isEnabled();
      }

      private SliderSetting scaleSetting() {
         for(Setting<?> setting : this.module.settings()) {
            if ((setting.id().equals("scale") || setting.id().equals("size")) && setting instanceof SliderSetting slider) {
               return slider;
            }
         }

         return null;
      }

      public boolean canScaleHud() {
         return this.scaleSetting() != null;
      }

      public float getHudScale() {
         SliderSetting slider = this.scaleSetting();
         return slider == null ? 1.0F : (float)(Integer)slider.get() / 100.0F;
      }

      public void setHudScale(float scale) {
         SliderSetting slider = this.scaleSetting();
         if (slider != null) {
            slider.set(Math.round(scale * 100.0F));
         }

      }
   }
}
