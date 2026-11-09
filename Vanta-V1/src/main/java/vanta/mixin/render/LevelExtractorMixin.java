package vanta.mixin.render;

import vanta.VantaClient;
import vanta.module.impl.render.esp.Esp;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {LevelExtractor.class},
   priority = 9999
)
public abstract class LevelExtractorMixin {
   @Redirect(
      method = {"isEntityVisible"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"
)}
   )
   private boolean client$sectionVisible(LevelRenderer renderer, BlockPos pos) {
      return VantaClient.MODULES != null && VantaClient.MODULES.isEnabled(Esp.class) ? true : renderer.isSectionCompiledAndVisible(pos);
   }
}
