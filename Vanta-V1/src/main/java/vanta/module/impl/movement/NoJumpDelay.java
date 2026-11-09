package vanta.module.impl.movement;

import vanta.event.events.PreTickEvent;
import vanta.mixin.extra.IAccessorLivingEntity;
import vanta.module.Module;
import vanta.setting.Measure;
import vanta.setting.SliderSetting;
import net.minecraft.client.player.LocalPlayer;

public final class NoJumpDelay extends Module {
   private final SliderSetting delay;

   public NoJumpDelay() {
      super("noJumpDelay");
      this.delay = (SliderSetting)this.add(new SliderSetting("delay", 3, 0, 8, Measure.TICKS));
      this.delay.describe();
   }

   public String info() {
      return String.valueOf(this.delay.get());
   }

   protected void onEnable() {
      this.listen(PreTickEvent.class, 100, (event) -> {
         LocalPlayer patt0$temp = event.client().player;
         if (patt0$temp instanceof IAccessorLivingEntity player) {
            player.frost$setNoJumpDelay(Math.min(player.frost$getNoJumpDelay(), (Integer)this.delay.get() + 1));
         }

      });
   }
}
