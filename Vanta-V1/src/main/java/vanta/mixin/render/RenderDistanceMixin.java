package vanta.mixin.render;

import vanta.VantaClient;
import vanta.module.impl.render.esp.Esp;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class RenderDistanceMixin {
   @Inject(
      method = {"shouldRender(DDD)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$noRenderDistance(double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
      if (this instanceof LivingEntity && VantaClient.MODULES != null && VantaClient.MODULES.isEnabled(Esp.class)) {
         cir.setReturnValue(true);
      }

   }
}
