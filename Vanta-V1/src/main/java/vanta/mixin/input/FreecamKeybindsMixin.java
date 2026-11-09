package vanta.mixin.input;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import vanta.module.impl.render.Freecam;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public abstract class FreecamKeybindsMixin {
   @Shadow
   @Final
   public Options options;

   @Inject(
      method = {"handleKeybinds"},
      at = {@At("HEAD")}
   )
   private void client$freecamClicks(CallbackInfo info) {
      if (Freecam.on() != null) {
         while(this.options.keyTogglePerspective.consumeClick()) {
         }
      }

      Freecam freecam = Freecam.controls();
      if (freecam != null) {
         freecam.clicks();
      }

   }

   @WrapOperation(
      method = {"handleKeybinds"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/KeyMapping;isDown()Z"
)}
   )
   private boolean client$freecamHeld(KeyMapping key, Operation<Boolean> original) {
      boolean down = (Boolean)original.call(new Object[]{key});
      if (key == this.options.keyAttack || key == this.options.keyUse) {
         Freecam freecam = Freecam.controls();
         if (freecam != null) {
            return freecam.held(key, down);
         }
      }

      return down;
   }
}
