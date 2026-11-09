package vanta.mixin.render;

import vanta.VantaClient;
import vanta.event.events.HandRenderEvent;
import vanta.render.entity.EntityFills;
import vanta.render.entity.EntityOutlines;
import vanta.render.entity.HandBounds;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Inject(
      method = {"submitHandsWithItems"},
      at = {@At("HEAD")}
   )
   private void client$beginHand(CallbackInfo info) {
      EntityFills.beginHand(((HandRenderEvent)VantaClient.EVENTS.post(new HandRenderEvent())).fill());
      if (EntityOutlines.hand() != null) {
         HandBounds.begin();
      }

   }

   @Inject(
      method = {"submitHandsWithItems"},
      at = {@At("RETURN")}
   )
   private void client$endHand(CallbackInfo info) {
      if (EntityOutlines.hand() != null) {
         HandBounds.end(EntityOutlines.hand());
      }

      EntityFills.endHand();
   }
}
