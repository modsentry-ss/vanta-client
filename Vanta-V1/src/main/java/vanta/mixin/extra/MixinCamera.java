package vanta.mixin.extra;

import vanta.VantaClient;
import vanta.module.impl.render.FreeLook;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {Camera.class},
   priority = 9999
)
public abstract class MixinCamera {
   private static FreeLook frost$freeLook(Entity entity) {
      if (!(entity instanceof LocalPlayer)) {
         return null;
      } else {
         FreeLook freeLook = (FreeLook)VantaClient.MODULES.get(FreeLook.class);
         return freeLook != null && freeLook.isFreeLooking() ? freeLook : null;
      }
   }

   @Redirect(
      method = {"alignWithEntity"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"
)}
   )
   private float frost$cameraYaw(Entity entity, float partialTicks) {
      FreeLook freeLook = frost$freeLook(entity);
      return freeLook != null ? freeLook.getCameraYaw() : entity.getViewYRot(partialTicks);
   }

   @Redirect(
      method = {"alignWithEntity"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"
)}
   )
   private float frost$cameraPitch(Entity entity, float partialTicks) {
      FreeLook freeLook = frost$freeLook(entity);
      return freeLook != null ? Mth.clamp(freeLook.getCameraPitch(), -90.0F, 90.0F) : entity.getViewXRot(partialTicks);
   }
}
