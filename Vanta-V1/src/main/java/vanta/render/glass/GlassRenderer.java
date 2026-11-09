package vanta.render.glass;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import vanta.VantaCore;
import vanta.render.post.KawaseBlur;
import vanta.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.lwjgl.system.MemoryStack;

public final class GlassRenderer {
   public static final int REGULAR = 0;
   public static final int CLEAR = 1;
   private static final BindGroupLayout SAMPLERS = BindGroupLayout.builder().withSampler("SharpSampler").withSampler("BlurSampler").build();
   private static final BindGroupLayout CONFIG;
   private static final RenderPipeline PIPELINE;
   private static final KawaseBlur BLUR_PASS;
   private static final List<Pane> PANES;
   private static final List<GpuBuffer> CONFIGS;
   private static GpuTexture sharp;
   private static GpuTextureView sharpView;

   public static void submit(Pane pane) {
      if (pane.width() > 0.0F && pane.height() > 0.0F && pane.opacity() > 0.0F) {
         PANES.add(pane);
      }

   }

   public static void render() {
      if (!PANES.isEmpty()) {
         RenderSystem.assertOnRenderThread();

         try {
            draw();
         } finally {
            PANES.clear();
         }
      }

   }

   private static void draw() {
      Minecraft minecraft = Minecraft.getInstance();
      RenderTarget main = minecraft.gameRenderer.mainRenderTarget();
      GpuTexture screen = main.getColorTexture();
      int width = screen.getWidth(0);
      int height = screen.getHeight(0);
      float scale = (float)minecraft.getWindow().getGuiScale();
      List<Region> regions = new ArrayList(PANES.size());
      Region union = null;
      boolean frosted = false;

      for(Pane pane : PANES) {
         Region region = GlassRenderer.Region.of(pane, scale, width, height);
         regions.add(region);
         if (!region.isEmpty()) {
            union = union == null ? region : union.union(region);
            frosted |= pane.variant() != 1;
         }
      }

      if (union != null) {
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         capture(encoder, screen, width, height);
         float blur = 1.5F * scale;
         GpuTextureView blurred = frosted ? BLUR_PASS.blur(encoder, sharpView, width, height, blur, union.halfPadded(KawaseBlur.reach(blur), width, height)) : sharpView;
         GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

         for(int i = 0; i < PANES.size(); ++i) {
            Region region = (Region)regions.get(i);
            if (!region.isEmpty()) {
               GpuBuffer config = config(encoder, i, (Pane)PANES.get(i), scale);
               RenderPass pass = encoder.createRenderPass(() -> "mace glass", main.getColorTextureView(), Optional.empty(), (GpuTextureView)null, OptionalDouble.empty());

               try {
                  pass.setPipeline(PIPELINE);
                  pass.enableScissor(region.x0(), region.y0(), region.x1() - region.x0(), region.y1() - region.y0());
                  RenderSystem.bindDefaultUniforms(pass);
                  pass.bindTexture("SharpSampler", sharpView, sampler);
                  pass.bindTexture("BlurSampler", blurred, sampler);
                  pass.setUniform("GlassConfig", config.slice());
                  pass.draw(3, 1, 0, 0);
               } catch (Throwable var20) {
                  if (pass != null) {
                     try {
                        pass.close();
                     } catch (Throwable var19) {
                        var20.addSuppressed(var19);
                     }
                  }

                  throw var20;
               }

               if (pass != null) {
                  pass.close();
               }
            }
         }
      }

   }

   private static void capture(CommandEncoder encoder, GpuTexture screen, int width, int height) {
      if (sharp == null || sharp.getWidth(0) != width || sharp.getHeight(0) != height || sharp.getFormat() != screen.getFormat()) {
         if (sharpView != null) {
            sharpView.close();
         }

         if (sharp != null) {
            sharp.close();
         }

         sharp = RenderSystem.getDevice().createTexture("mace glass backdrop", 5, screen.getFormat(), width, height, 1, 1);
         sharpView = RenderSystem.getDevice().createTextureView(sharp);
      }

      encoder.copyTextureToTexture(screen, sharp, 0, 0, 0, 0, 0, width, height);
   }

   private static GpuBuffer config(CommandEncoder encoder, int index, Pane pane, float scale) {
      while(CONFIGS.size() <= index) {
         int slot = CONFIGS.size();
         CONFIGS.add(RenderSystem.getDevice().createBuffer(() -> "mace glass config " + slot, 136, 64L));
      }

      GpuBuffer buffer = (GpuBuffer)CONFIGS.get(index);
      int tint = pane.tint();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         encoder.writeToBuffer(buffer.slice(), Std140Builder.onStack(stack, 64).putVec4(pane.x() * scale, pane.y() * scale, pane.width() * scale, pane.height() * scale).putVec4(pane.lightX(), pane.lightY(), pane.energy(), scale).putVec4((float)Colors.red(tint) / 255.0F, (float)Colors.green(tint) / 255.0F, (float)Colors.blue(tint) / 255.0F, (float)Colors.alpha(tint) / 255.0F).putVec4((float)pane.variant(), pane.opacity(), pane.radius(), 0.0F).get());
      } catch (Throwable var10) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }
         }

         throw var10;
      }

      if (stack != null) {
         stack.close();
      }

      return buffer;
   }

   private GlassRenderer() {
   }

   static {
      CONFIG = BindGroupLayout.builder().withUniform("GlassConfig", UniformType.UNIFORM_BUFFER).build();
      PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(VantaCore.id("pipeline/post/glass")).withVertexShader(VantaCore.id("post/glass")).withFragmentShader(VantaCore.id("post/glass")).withBindGroupLayout(SAMPLERS).withBindGroupLayout(CONFIG).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build();
      BLUR_PASS = new KawaseBlur("glass");
      PANES = new ArrayList();
      CONFIGS = new ArrayList();
   }

   public static record Pane(float x, float y, float width, float height, float radius, int variant, int tint, float lightX, float lightY, float energy, float opacity, ScreenRectangle clip) {
      float lift() {
         return Math.min(Math.min(this.width, this.height) * 0.5F, 14.0F);
      }
   }

   private static record Region(int x0, int y0, int x1, int y1) {
      static Region of(Pane pane, float scale, int width, int height) {
         float pad = pane.lift() * 0.8F + 1.5F + 1.0F;
         float left = pane.x() - pad;
         float top = pane.y() - pad;
         float right = pane.x() + pane.width() + pad;
         float bottom = pane.y() + pane.height() + pad + 1.5F;
         ScreenRectangle clip = pane.clip();
         if (clip != null) {
            left = Math.max(left, (float)clip.left());
            top = Math.max(top, (float)clip.top());
            right = Math.min(right, (float)(clip.left() + clip.width()));
            bottom = Math.min(bottom, (float)(clip.top() + clip.height()));
         }

         int x0 = Math.clamp((long)((int)Math.floor((double)(left * scale))), 0, width);
         int x1 = Math.clamp((long)((int)Math.ceil((double)(right * scale))), 0, width);
         int y0 = Math.clamp((long)(height - (int)Math.ceil((double)(bottom * scale))), 0, height);
         int y1 = Math.clamp((long)(height - (int)Math.floor((double)(top * scale))), 0, height);
         return new Region(x0, y0, x1, y1);
      }

      boolean isEmpty() {
         return this.x1 <= this.x0 || this.y1 <= this.y0;
      }

      Region union(Region other) {
         return new Region(Math.min(this.x0, other.x0), Math.min(this.y0, other.y0), Math.max(this.x1, other.x1), Math.max(this.y1, other.y1));
      }

      ScreenRectangle halfPadded(int pad, int width, int height) {
         int halfWidth = (width + 1) / 2;
         int halfHeight = (height + 1) / 2;
         int left = Math.max(0, (this.x0 - pad) / 2);
         int bottom = Math.max(0, (this.y0 - pad) / 2);
         int right = Math.min(halfWidth, (this.x1 + pad + 1) / 2);
         int top = Math.min(halfHeight, (this.y1 + pad + 1) / 2);
         return new ScreenRectangle(left, bottom, right - left, top - bottom);
      }
   }
}
