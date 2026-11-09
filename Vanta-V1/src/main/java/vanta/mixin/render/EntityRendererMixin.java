package vanta.mixin.render;

import vanta.VantaClient;
import vanta.event.events.EntityRenderStateEvent;
import vanta.render.entity.EntityFill;
import vanta.render.entity.EntityOutline;
import vanta.render.entity.Filled;
import vanta.util.ClientPearls;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin {
   @Inject(
      method = {"createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"},
      at = {@At("RETURN")}
   )
   private void client$extracted(Entity entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> info) {
      EntityRenderState state = (EntityRenderState)info.getReturnValue();
      ((Filled)state).client$setFill((EntityFill)null);
      ((Filled)state).client$setOutline((EntityOutline)null);
      if (!ClientPearls.isPrediction(entity)) {
         VantaClient.EVENTS.post(new EntityRenderStateEvent(entity, state, partialTicks));
      }

   }
}
