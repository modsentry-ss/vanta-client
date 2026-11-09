package vanta.render.world;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class TerrainDepth {
   private static final int MARGIN = 24;
   private static final double NEAR = (double)2.0F;
   private static final DepthTarget TARGET = new DepthTarget();
   static final OutputTarget OUTPUT = new OutputTarget("mace_terrain_depth", () -> TARGET.ready ? TARGET : null);
   private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
   private static final Vector4f CORNER = new Vector4f();
   public static long debugResets;
   public static long debugCaptures;
   public static long debugFull;

   private TerrainDepth() {
   }

   static void reset() {
      TARGET.ready = false;
      ++debugResets;
   }

   static void capture(CameraRenderState camera, List<WorldBuffer.Box> boxes) {
      if (!boxes.isEmpty()) {
         RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
         GpuTexture depth = main.getDepthTexture();
         if (depth != null && main.width > 0 && main.height > 0) {
            TARGET.fit(main.width, main.height);
            int[] area = area(camera, boxes, main.width, main.height);
            ++debugCaptures;
            if (area != null && area[2] == main.width && area[3] == main.height) {
               ++debugFull;
            }

            if (area != null) {
               RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(depth, TARGET.getDepthTexture(), 0, area[0], area[1], area[0], area[1], area[2], area[3]);
            }

            TARGET.ready = true;
         }
      }
   }

   private static int[] area(CameraRenderState camera, List<WorldBuffer.Box> boxes, int width, int height) {
      VIEW_PROJECTION.set(camera.projectionMatrix).mul(camera.viewRotationMatrix);
      Vec3 at = camera.pos;
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;

      for(WorldBuffer.Box box : boxes) {
         AABB bounds = box.bounds();
         int behind = 0;
         double boxMinX = Double.POSITIVE_INFINITY;
         double boxMinY = Double.POSITIVE_INFINITY;
         double boxMaxX = Double.NEGATIVE_INFINITY;
         double boxMaxY = Double.NEGATIVE_INFINITY;

         for(int corner = 0; corner < 8; ++corner) {
            double x = ((corner & 1) == 0 ? bounds.minX : bounds.maxX) - at.x;
            double y = ((corner & 2) == 0 ? bounds.minY : bounds.maxY) - at.y;
            double z = ((corner & 4) == 0 ? bounds.minZ : bounds.maxZ) - at.z;
            VIEW_PROJECTION.transform(CORNER.set((float)x, (float)y, (float)z, 1.0F));
            if (CORNER.w <= 0.05F) {
               ++behind;
            } else {
               double sx = ((double)(CORNER.x / CORNER.w) * (double)0.5F + (double)0.5F) * (double)width;
               double sy = ((double)(CORNER.y / CORNER.w) * (double)0.5F + (double)0.5F) * (double)height;
               boxMinX = Math.min(boxMinX, sx);
               boxMinY = Math.min(boxMinY, sy);
               boxMaxX = Math.max(boxMaxX, sx);
               boxMaxY = Math.max(boxMaxY, sy);
            }
         }

         if (behind != 8) {
            if (behind > 0 && bounds.inflate((double)2.0F).contains(at)) {
               return new int[]{0, 0, width, height};
            }

            minX = Math.min(minX, boxMinX);
            minY = Math.min(minY, boxMinY);
            maxX = Math.max(maxX, boxMaxX);
            maxY = Math.max(maxY, boxMaxY);
         }
      }

      if (minX > maxX) {
         return null;
      } else {
         int x0 = (int)Math.max((double)0.0F, Math.floor(minX) - (double)24.0F);
         int y0 = (int)Math.max((double)0.0F, Math.floor(minY) - (double)24.0F);
         int x1 = (int)Math.min((double)width, Math.ceil(maxX) + (double)24.0F);
         int y1 = (int)Math.min((double)height, Math.ceil(maxY) + (double)24.0F);
         return x1 > x0 && y1 > y0 ? new int[]{x0, y0, x1 - x0, y1 - y0} : null;
      }
   }

   private static final class DepthTarget extends RenderTarget {
      private boolean ready;

      private DepthTarget() {
         super("Mace terrain depth", true, GpuFormat.RGBA8_UNORM);
      }

      private void fit(int width, int height) {
         if (this.depthTexture == null || this.width != width || this.height != height) {
            this.destroyBuffers();
            GpuDevice device = RenderSystem.getDevice();
            this.width = width;
            this.height = height;
            this.depthTexture = device.createTexture(() -> this.label + " / Depth", 15, GpuFormat.D32_FLOAT, width, height, 1, 1);
            this.depthTextureView = device.createTextureView(this.depthTexture);
         }
      }

      private static RenderTarget main() {
         return Minecraft.getInstance().gameRenderer.mainRenderTarget();
      }

      public GpuTexture getColorTexture() {
         return main().getColorTexture();
      }

      public GpuTextureView getColorTextureView() {
         return main().getColorTextureView();
      }
   }
}
