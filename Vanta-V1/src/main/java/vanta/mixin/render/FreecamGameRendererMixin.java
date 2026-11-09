package vanta.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import vanta.module.impl.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public abstract class FreecamGameRendererMixin {
   @Inject(
      method = {"renderItemInHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$freecamHand(CameraRenderState cameraState, float partialTicks, Matrix4fc modelViewMatrix, CallbackInfo info) {
      Freecam freecam = Freecam.camera();
      if (freecam != null && freecam.hidesHand()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"shouldRenderBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$freecamOutline(CallbackInfoReturnable<Boolean> info) {
      if (Freecam.blocksClicks()) {
         info.setReturnValue(false);
      }

   }

   @WrapOperation(
      method = {"shouldRenderBlockOutline"},
      require = 0,
      at = {@At(
   value = "FIELD",
   target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;"
)}
   )
   private HitResult client$freecamOutlineHit(Minecraft client, Operation<HitResult> original) {
      return Freecam.outlineHit((HitResult)original.call(new Object[]{client}));
   }
}
