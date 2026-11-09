package vanta.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import vanta.VantaClient;
import vanta.util.Clicks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Inject(
      method = {"getViewVector"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$viewVector(float partialTick, CallbackInfoReturnable<Vec3> info) {
      Vec3 view = Clicks.view((Entity)this);
      if (view == null) {
         view = VantaClient.ROTATIONS.viewVector((Entity)this);
      }

      if (view != null) {
         info.setReturnValue(view);
      }

   }

   @ModifyExpressionValue(
      method = {"moveRelative"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/Entity;getYRot()F"
)}
   )
   private float client$movementYaw(float yaw) {
      return VantaClient.ROTATIONS.movementYaw((Entity)this, yaw);
   }
}
