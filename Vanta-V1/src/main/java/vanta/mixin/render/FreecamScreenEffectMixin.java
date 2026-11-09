package vanta.mixin.render;

import vanta.module.impl.render.Freecam;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ScreenEffectRenderer.class})
public abstract class FreecamScreenEffectMixin {
   @ModifyVariable(
      method = {"submit"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 0
   )
   private boolean client$freecamEffects(boolean isFirstPerson) {
      return isFirstPerson && Freecam.camera() == null;
   }
}
