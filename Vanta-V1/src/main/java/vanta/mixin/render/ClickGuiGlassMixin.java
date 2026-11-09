package vanta.mixin.render;

import vanta.render.glass.GlassRenderer;
import vanta.ui.click.ClickGui;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({GuiRenderer.class})
public abstract class ClickGuiGlassMixin {
   @Redirect(
      method = {"draw"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"
)}
   )
   private void mace$glassAtBlur(GameRenderer renderer) {
      if (!ClickGui.takeGlassOnly()) {
         renderer.processBlurEffect();
      }

      GlassRenderer.render();
   }
}
