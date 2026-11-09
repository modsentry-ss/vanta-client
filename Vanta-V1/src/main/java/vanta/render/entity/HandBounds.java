package vanta.render.entity;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import vanta.mixin.accessor.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class HandBounds {
   private static final float NEAR = 1.0E-4F;
   private static final Matrix4f clip = new Matrix4f();
   private static final Vector4f point = new Vector4f();
   private static float minX;
   private static float minY;
   private static float maxX;
   private static float maxY;
   private static boolean any;
   private static boolean behind;

   public static void begin() {
      ((GameRendererAccessor)Minecraft.getInstance().gameRenderer).client$getHudProjection().getMatrix(clip).mul(RenderSystem.getModelViewStack());
      minY = Float.POSITIVE_INFINITY;
      minX = Float.POSITIVE_INFINITY;
      maxY = Float.NEGATIVE_INFINITY;
      maxX = Float.NEGATIVE_INFINITY;
      any = false;
      behind = false;
   }

   public static void include(Model<?> model, PoseStack pose) {
      model.root().getExtentsForGui(pose, (corner) -> add(clip, corner.x(), corner.y(), corner.z()));
   }

   public static void include(List<BakedQuad> quads, PoseStack pose) {
      Matrix4f local = (new Matrix4f(clip)).mul(pose.last().pose());

      for(BakedQuad quad : quads) {
         for(int i = 0; i < 4; ++i) {
            Vector3fc position = quad.position(i);
            add(local, position.x(), position.y(), position.z());
         }
      }

   }

   public static void end(EntityOutline outline) {
      if (any) {
         RenderTarget mask = EntityOutlines.mask();
         if (behind) {
            EntityOutlines.include(0, 0, mask.width, mask.height);
         } else {
            int slack = (int)Math.ceil((double)(2.0F * outline.reach())) + 2;
            EntityOutlines.include((int)Math.floor((double)((minX * 0.5F + 0.5F) * (float)mask.width)) - slack, (int)Math.floor((double)((minY * 0.5F + 0.5F) * (float)mask.height)) - slack, (int)Math.ceil((double)((maxX * 0.5F + 0.5F) * (float)mask.width)) + slack, (int)Math.ceil((double)((maxY * 0.5F + 0.5F) * (float)mask.height)) + slack);
         }
      }

   }

   private static void add(Matrix4f transform, float x, float y, float z) {
      transform.transform(point.set(x, y, z, 1.0F));
      any = true;
      if (point.w <= 1.0E-4F) {
         behind = true;
      } else {
         float ndcX = point.x / point.w;
         float ndcY = point.y / point.w;
         minX = Math.min(minX, ndcX);
         minY = Math.min(minY, ndcY);
         maxX = Math.max(maxX, ndcX);
         maxY = Math.max(maxY, ndcY);
      }

   }

   private HandBounds() {
   }
}
