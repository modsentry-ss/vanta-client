package vanta.mixin.input;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import vanta.util.Clicks;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public abstract class KeyboardHandlerMixin {
   @Inject(
      method = {"keyPress"},
      at = {@At("HEAD")}
   )
   private void client$key(long handle, int action, KeyEvent event, CallbackInfo callback) {
      if (handle == Minecraft.getInstance().getWindow().handle()) {
         Clicks.input(InputConstants.getKey(event), action);
      }

   }

   @WrapOperation(
      method = {"keyPress"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V"
)}
   )
   private void client$keySet(InputConstants.Key key, boolean down, Operation<Void> original) {
      Clicks.guard(key, () -> original.call(new Object[]{key, down}));
   }

   @WrapOperation(
      method = {"keyPress"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"
)}
   )
   private void client$keyClick(InputConstants.Key key, Operation<Void> original) {
      Clicks.guard(key, () -> original.call(new Object[]{key}));
   }
}
