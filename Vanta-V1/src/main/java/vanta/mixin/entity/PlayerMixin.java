package vanta.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import vanta.VantaClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Player.class})
public abstract class PlayerMixin {
   @ModifyExpressionValue(
      method = {"travel"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
)}
   )
   private Vec3 client$swimLook(Vec3 look) {
      return VantaClient.ROTATIONS.movementLook((Entity)this, look);
   }
}
