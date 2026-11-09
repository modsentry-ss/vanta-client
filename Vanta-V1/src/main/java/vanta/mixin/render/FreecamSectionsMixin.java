package vanta.mixin.render;

import vanta.module.impl.render.Freecam;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({LevelRenderer.class})
public abstract class FreecamSectionsMixin {
   @ModifyArg(
      method = {"repositionCamera"},
      require = 0,
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/ViewArea;repositionCamera(Lnet/minecraft/core/SectionPos;)Z"
)}
   )
   private SectionPos client$freecamGrid(SectionPos camera) {
      return Freecam.gridCenter(camera);
   }
}
