package vanta.render.post;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import vanta.VantaCore;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

public final class KawaseBlur {
   private static final Vector4fc CLEAR = new Vector4f();
   private static final BindGroupLayout CONFIG;
   private static final RenderPipeline PIPELINE;
   private final String name;
   private final GpuBuffer[] offsets = new GpuBuffer[9];
   private RenderTarget down;
   private RenderTarget first;
   private RenderTarget second;
   private float uploadedRadius = Float.NaN;
   private int passes;

   public KawaseBlur(String name) {
      this.name = name;
   }

   public static int passes(float radius) {
      return Math.clamp((long)((int)Math.ceil(Math.sqrt((double)radius))), 1, 8);
   }

   public static int reach(float radius) {
      int count = passes(radius);
      return 2 * (count * (count + 1) / 2 + 2) + 2;
   }

   public GpuTextureView blur(CommandEncoder encoder, GpuTextureView source, int width, int height, float radius, ScreenRectangle halfArea) {
      int halfWidth = (width + 1) / 2;
      int halfHeight = (height + 1) / 2;
      this.down = this.target(this.down, "down", halfWidth, halfHeight);
      this.first = this.target(this.first, "first", halfWidth, halfHeight);
      this.second = this.target(this.second, "second", halfWidth, halfHeight);
      this.upload(encoder, radius);
      this.pass(encoder, "down", this.down.getColorTextureView(), halfArea, this.offsets[0], source);
      GpuTextureView from = this.down.getColorTextureView();

      for(int i = 1; i <= this.passes; ++i) {
         RenderTarget to = (i & 1) == 1 ? this.first : this.second;
         this.pass(encoder, "pass " + i, to.getColorTextureView(), halfArea, this.offsets[i], from);
         from = to.getColorTextureView();
      }

      return from;
   }

   public GpuTextureView downsampled() {
      return this.down.getColorTextureView();
   }

   private void pass(CommandEncoder encoder, String label, GpuTextureView color, ScreenRectangle area, GpuBuffer offset, GpuTextureView source) {
      RenderPass pass = encoder.createRenderPass(() -> "mace kawase " + this.name + " " + label, color, Optional.of(CLEAR), (GpuTextureView)null, OptionalDouble.empty());

      try {
         pass.setPipeline(PIPELINE);
         if (area != null) {
            pass.enableScissor(area.left(), area.top(), area.width(), area.height());
         }

         RenderSystem.bindDefaultUniforms(pass);
         pass.bindTexture("InSampler", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.setUniform("KawaseConfig", offset.slice());
         pass.draw(3, 1, 0, 0);
      } catch (Throwable var11) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (pass != null) {
         pass.close();
      }

   }

   private void upload(CommandEncoder encoder, float radius) {
      if (radius != this.uploadedRadius) {
         this.passes = passes(radius);

         for(int i = 0; i <= this.passes; ++i) {
            if (this.offsets[i] == null) {
               this.offsets[i] = RenderSystem.getDevice().createBuffer(() -> "mace kawase " + this.name + " " + i, 136, 16L);
            }

            float offset = i == 0 ? 1.0F : (float)i - 0.5F;
            MemoryStack stack = MemoryStack.stackPush();

            try {
               encoder.writeToBuffer(this.offsets[i].slice(), Std140Builder.onStack(stack, 16).putVec2(offset, offset).putVec2(0.0F, 0.0F).get());
            } catch (Throwable var9) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (stack != null) {
               stack.close();
            }
         }

         this.uploadedRadius = radius;
      }

   }

   private RenderTarget target(RenderTarget current, String label, int width, int height) {
      if (current != null && current.width == width && current.height == height) {
         return current;
      } else {
         if (current != null) {
            current.destroyBuffers();
         }

         return new TextureTarget("mace kawase " + this.name + " " + label, width, height, false, GpuFormat.RGBA8_UNORM);
      }
   }

   static {
      CONFIG = BindGroupLayout.builder().withUniform("KawaseConfig", UniformType.UNIFORM_BUFFER).build();
      PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(VantaCore.id("pipeline/post/kawase")).withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/screenquad")).withFragmentShader(VantaCore.id("post/kawase")).withBindGroupLayout(BindGroupLayouts.IN_SAMPLER).withBindGroupLayout(CONFIG).withColorTargetState(ColorTargetState.DEFAULT).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build();
   }
}
