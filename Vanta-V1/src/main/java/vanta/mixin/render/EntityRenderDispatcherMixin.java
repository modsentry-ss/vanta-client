package vanta.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import vanta.render.entity.EntityFills;
import vanta.render.entity.Filled;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public abstract class EntityRenderDispatcherMixin {
   @Inject(
      method = {"submit"},
      at = {@At("HEAD")}
   )
   private <S extends EntityRenderState> void client$beginEntity(S state, CameraRenderState camera, double x, double y, double z, PoseStack pose, SubmitNodeCollector collector, CallbackInfo info) {
      EntityFills.beginEntity(((Filled)state).client$fill());
   }

   @Inject(
      method = {"submit"},
      at = {@At("RETURN")}
   )
   private <S extends EntityRenderState> void client$endEntity(S state, CameraRenderState camera, double x, double y, double z, PoseStack pose, SubmitNodeCollector collector, CallbackInfo info) {
      EntityFills.endEntity();
   }
}
