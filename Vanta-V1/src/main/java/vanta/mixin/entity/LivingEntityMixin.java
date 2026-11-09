package vanta.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import vanta.VantaClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin {
   @ModifyExpressionValue(
      method = {"jumpFromGround"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
)}
   )
   private float client$jumpYaw(float yaw) {
      return VantaClient.ROTATIONS.movementYaw((Entity)this, yaw);
   }

   @ModifyExpressionValue(
      method = {"updateFallFlyingMovement"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"
)}
   )
   private float client$glidePitch(float pitch) {
      return VantaClient.ROTATIONS.movementPitch((Entity)this, pitch);
   }

   @ModifyExpressionValue(
      method = {"updateFallFlyingMovement"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
)}
   )
   private Vec3 client$glideLook(Vec3 look) {
      return VantaClient.ROTATIONS.movementLook((Entity)this, look);
   }
}
