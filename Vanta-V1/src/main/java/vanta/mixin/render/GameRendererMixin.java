package vanta.mixin.render;

import vanta.mixin.accessor.LevelRendererAccessor;
import vanta.render.entity.EntityOutlines;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"render(Lnet/minecraft/client/DeltaTracker;Z)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"
)}
   )
   private void client$inkOutline(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo info) {
      if (EntityOutlines.active()) {
         GameRenderer renderer = (GameRenderer)this;
         if (renderer.gameRenderState().levelRenderState.shouldShowEntityOutlines) {
            EntityOutlines.render(((LevelRendererAccessor)this.minecraft.levelRenderer).client$getEntityOutlineTarget(), renderer.mainRenderTarget());
         }
      }

   }
}
