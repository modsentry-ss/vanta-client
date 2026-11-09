package vanta.mixin.input;

import vanta.ui.click.ClickGui;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public abstract class ClickGuiKeyboardMixin {
   @Inject(
      method = {"keyPress"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mace$clickGuiKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
      if (handle == Minecraft.getInstance().getWindow().handle() && ClickGui.onKey(event, action)) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"charTyped"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mace$clickGuiChar(long handle, CharacterEvent event, CallbackInfo ci) {
      if (handle == Minecraft.getInstance().getWindow().handle() && ClickGui.onChar(event.codepoint())) {
         ci.cancel();
      }

   }
}
