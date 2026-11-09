package vanta.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import vanta.module.impl.render.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({LevelExtractor.class})
public abstract class FreecamExtractorMixin {
   @ModifyArg(
      method = {"extract"},
      require = 0,
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/SectionUpdateTracker;repositionCamera(Lnet/minecraft/core/SectionPos;)V"
)}
   )
   private SectionPos client$freecamGrid(SectionPos camera) {
      return Freecam.gridCenter(camera);
   }

   @WrapOperation(
      method = {"extractBlockOutline"},
      require = 0,
      at = {@At(
   value = "FIELD",
   target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;"
)}
   )
   private HitResult client$freecamOutline(Minecraft client, Operation<HitResult> original) {
      return Freecam.outlineHit((HitResult)original.call(new Object[]{client}));
   }
}
