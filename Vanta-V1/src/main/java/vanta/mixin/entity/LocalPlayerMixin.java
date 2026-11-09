package vanta.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import vanta.VantaClient;
import vanta.rotation.Rotation;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerMixin {
   @Shadow
   private float yRotLast;
   @Shadow
   private float xRotLast;

   @ModifyExpressionValue(
      method = {"sendPosition"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"
)}
   )
   private float client$sentYaw(float yaw) {
      return VantaClient.ROTATIONS.sentYaw(yaw);
   }

   @ModifyExpressionValue(
      method = {"sendPosition"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"
)}
   )
   private float client$sentPitch(float pitch) {
      return VantaClient.ROTATIONS.sentPitch(pitch);
   }

   @Inject(
      method = {"sendPosition"},
      at = {@At("TAIL")}
   )
   private void client$sent(CallbackInfo info) {
      VantaClient.ROTATIONS.sent(new Rotation(this.yRotLast, this.xRotLast));
   }
}
