package vanta.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import vanta.module.impl.render.Freecam;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Hud.class})
public abstract class FreecamHudMixin {
   @ModifyExpressionValue(
      method = {"extractCameraOverlays"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"
)}
   )
   private boolean client$freecamWorn(boolean firstPerson) {
      return firstPerson && Freecam.camera() == null;
   }

   @ModifyExpressionValue(
      method = {"extractCameraOverlays"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/player/LocalPlayer;getTicksFrozen()I"
)}
   )
   private int client$freecamFrost(int ticksFrozen) {
      return Freecam.camera() != null ? 0 : ticksFrozen;
   }
}
