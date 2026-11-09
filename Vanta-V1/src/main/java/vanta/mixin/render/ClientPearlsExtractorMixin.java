package vanta.mixin.render;

import vanta.util.ClientPearls;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LevelExtractor.class})
public abstract class ClientPearlsExtractorMixin {
   @Shadow
   public abstract boolean isEntityVisible(Entity var1, Frustum var2, double var3, double var5, double var7);

   @Inject(
      method = {"isEntityVisible"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$hideReal(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> info) {
      if (ClientPearls.hidden(entity)) {
         info.setReturnValue(false);
      }

   }

   @Inject(
      method = {"extractVisibleEntities"},
      at = {@At("TAIL")}
   )
   private void client$predictions(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo info) {
      ClientPearls.extract(output.entityRenderStates, frustum, camera.position(), deltaTracker.getGameTimeDeltaPartialTick(true), this::isEntityVisible);
   }
}
