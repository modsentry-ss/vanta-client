package vanta.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Consumer;
import vanta.render.entity.EntityFill;
import vanta.render.entity.EntityFills;
import vanta.render.entity.EntityOutline;
import vanta.render.entity.EntityOutlines;
import vanta.render.entity.Filled;
import vanta.render.entity.HandBounds;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SubmitNodeCollection.class})
public abstract class SubmitNodeCollectionMixin {
   @Inject(
      method = {"submitModel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$fillModel(Model model, Object state, PoseStack pose, RenderType type, int light, int overlay, int color, TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, CallbackInfo info) {
      if (!EntityFills.isFill(type)) {
         boolean hand = !(state instanceof Filled);
         SubmitNodeCollection collection = (SubmitNodeCollection)this;
         Identifier texture = EntityFills.texture(type);
         if (hand && texture != null) {
            client$outlineHand((fillx) -> {
               HandBounds.include(model, pose);
               collection.submitModel(model, state, pose, EntityFills.silhouette(texture), fillx.light(), fillx.visibleOverlay(), fillx.visible(), sprite, 0, crumbling);
            });
         }

         EntityFill fill = hand ? client$hand(pose) : ((Filled)state).client$fill();
         if (fill != null && texture != null) {
            if (fill.occluded() != 0) {
               collection.submitModel(model, state, pose, EntityFills.occluded(texture, fill.effect()), fill.light(), fill.occludedOverlay(), fill.occluded(), sprite, 0, crumbling);
            }

            RenderType visible = hand ? EntityFills.visibleHand(texture, fill.effect()) : EntityFills.visible(texture, fill.effect());
            collection.submitModel(model, state, pose, visible, fill.light(), fill.visibleOverlay(), fill.visible(), sprite, 0, crumbling);
            info.cancel();
         }
      }

   }

   @Inject(
      method = {"submitItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$fillItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, List<BakedQuad> quads, ItemStackRenderState.FoilType foil, CallbackInfo info) {
      if (!quads.isEmpty()) {
         Identifier texture = EntityFills.texture(((BakedQuad)quads.getFirst()).materialInfo().itemRenderType());
         if (texture != null) {
            SubmitNodeCollection collection = (SubmitNodeCollection)this;
            client$outlineHand((silhouette) -> {
               HandBounds.include(quads, pose);
               collection.submitCustomGeometry(pose, EntityFills.silhouette(texture), (entry, consumer) -> client$putQuads(entry, consumer, quads, silhouette));
            });
            EntityFill held = EntityFills.inHandPass() ? null : EntityFills.entity();
            if (held != null) {
               if (held.occluded() != 0) {
                  collection.submitCustomGeometry(pose, EntityFills.occluded(texture, held.effect()), (entry, consumer) -> client$putQuads(entry, consumer, quads, held.occluded(), held.occludedOverlay(), held.light()));
               }

               collection.submitCustomGeometry(pose, EntityFills.visible(texture, held.effect()), (entry, consumer) -> client$putQuads(entry, consumer, quads, held));
               info.cancel();
               return;
            }

            EntityFill fill = client$hand(pose);
            if (fill != null) {
               collection.submitCustomGeometry(pose, EntityFills.visibleHand(texture, fill.effect()), (entry, consumer) -> client$putQuads(entry, consumer, quads, fill));
               info.cancel();
            }
         }
      }

   }

   @Unique
   private static void client$putQuads(PoseStack.Pose entry, VertexConsumer consumer, List<BakedQuad> quads, EntityFill fill) {
      client$putQuads(entry, consumer, quads, fill.visible(), fill.visibleOverlay(), fill.light());
   }

   @Unique
   private static void client$putQuads(PoseStack.Pose entry, VertexConsumer consumer, List<BakedQuad> quads, int color, int overlay, int light) {
      QuadInstance instance = new QuadInstance();
      instance.setColor(color);
      instance.setLightCoords(light);
      instance.setOverlayCoords(overlay);

      for(BakedQuad quad : quads) {
         consumer.putBakedQuad(entry, quad, instance);
      }

   }

   @Unique
   private static void client$outlineHand(Consumer<EntityFill> submit) {
      EntityOutline outline = EntityOutlines.hand();
      if (outline != null && EntityFills.inHandPass()) {
         submit.accept(EntityFill.solid(outline.color(), 0));
      }

   }

   @Unique
   private static EntityFill client$hand(PoseStack pose) {
      EntityFill fill = EntityFills.hand();
      if (fill == null) {
         return null;
      } else {
         Vector3f pivot = pose.last().pose().getTranslation(new Vector3f());
         return fill.anchored(new Vec3(pivot));
      }
   }
}
