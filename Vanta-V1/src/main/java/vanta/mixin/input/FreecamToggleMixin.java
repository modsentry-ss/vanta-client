package vanta.mixin.input;

import java.util.function.BooleanSupplier;
import vanta.module.impl.render.Freecam;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ToggleKeyMapping.class})
public abstract class FreecamToggleMixin {
   @Shadow
   @Final
   private BooleanSupplier needsToggle;
   @Unique
   private boolean client$restoring;

   @Inject(
      method = {"shouldRestoreStateOnScreenClosed"},
      at = {@At("RETURN")}
   )
   private void client$restoring(CallbackInfoReturnable<Boolean> info) {
      this.client$restoring = info.getReturnValueZ();
   }

   @Inject(
      method = {"setDown"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$freecamToggle(boolean down, CallbackInfo info) {
      boolean restoring = this.client$restoring;
      this.client$restoring = false;
      if (down && !restoring && this.needsToggle.getAsBoolean() && Freecam.keepsToggle((KeyMapping)this)) {
         info.cancel();
      }

   }
}
