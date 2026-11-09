package vanta.module.impl.render;

import vanta.event.events.PreTickEvent;
import vanta.module.Module;
import vanta.setting.EnumSetting;
import net.minecraft.client.CameraType;

public final class Perspective extends Module {
   private final EnumSetting view = (EnumSetting)this.add(new EnumSetting("view", 0, new String[]{"Back", "Front"}));
   private CameraType previous;

   public Perspective() {
      super("perspective");
   }

   public String info() {
      return this.view.selected();
   }

   private CameraType wanted() {
      return (Integer)this.view.get() == 1 ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK;
   }

   protected void onEnable() {
      this.previous = mc.options.getCameraType();
      mc.options.setCameraType(this.wanted());
      this.listen(PreTickEvent.class, (event) -> {
         if (mc.options.getCameraType() != this.wanted()) {
            mc.options.setCameraType(this.wanted());
         }

      });
   }

   protected void onDisable() {
      if (this.previous != null) {
         mc.options.setCameraType(this.previous);
         this.previous = null;
      }

   }
}
