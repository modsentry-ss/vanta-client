package vanta.render.entity;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import vanta.VantaCore;
import vanta.mixin.accessor.LevelRendererAccessor;
import vanta.render.post.KawaseBlur;
import vanta.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

public final class EntityOutlines {
   public static final EntityOutlines LEVEL = new EntityOutlines("level");
   public static final EntityOutlines PREVIEW = new EntityOutlines("preview");
   private static final EntityOutlines REAR = new EntityOutlines("rear");
   private static final Vector4fc CLEAR = new Vector4f();
   private static final BindGroupLayout CONFIG;
   private static final BindGroupLayout GLOW_SAMPLERS;
   private static final RenderPipeline SCAN;
   private static final RenderPipeline INK;
   private static final BlendFunction SCREEN;
   private static final RenderPipeline GLOW;
   private static final RenderPipeline WATER;
   private static EntityOutline level;
   private static Water water;
   private static final long START;
   private static EntityOutline hand;
   private static Area region;
   private final String name;
   private GpuBuffer config;
   private GpuBuffer waterConfig;
   private RenderTarget scratch;
   private KawaseBlur blur;
   private EntityOutline uploaded;

   public static boolean active() {
      return level != null;
   }

   public static void configure(EntityOutline outline) {
      level = outline;
      if (outline == null) {
         region = null;
         water = null;
      }

   }

   public static void configureWater(Water look) {
      water = look;
   }

   public static void include(int x0, int y0, int x1, int y1) {
      region = region == null ? new Area(x0, y0, x1, y1) : region.union(x0, y0, x1, y1);
   }

   public static void configureHand(EntityOutline outline) {
      hand = outline;
   }

   public static EntityOutline hand() {
      return hand;
   }

   public static RenderTarget mask() {
      return ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).client$getEntityOutlineTarget();
   }

   public static void render(RenderTarget mask, RenderTarget main) {
      LEVEL.composite(mask, main);
   }

   public static void renderRear(RenderTarget mask, RenderTarget image) {
      REAR.composite(mask, image);
   }

   public static void clearRegion() {
      region = null;
   }

   private void composite(RenderTarget mask, RenderTarget main) {
      Area area = region;
      region = null;
      if (area != null && level != null) {
         int margin = (int)Math.ceil((double)level.reach()) + 1;
         Area ink = area.pad(margin, margin, mask.width, mask.height);
         if (!ink.isEmpty() && water != null) {
            this.water(mask.getColorTextureView(), mask.width, mask.height, main.getColorTextureView(), main.getDepthTextureView(), level.withColor(0), water, ink, ink.pad(margin, margin, mask.width, mask.height));
         } else if (!ink.isEmpty()) {
            this.ink(mask.getColorTextureView(), mask.width, mask.height, main.getColorTextureView(), main.getDepthTextureView(), level.withColor(0), ink, ink.pad(margin, margin, mask.width, mask.height));
         }
      }

   }

   public void ink(GpuTextureView mask, int width, int height, GpuTextureView target, GpuTextureView depth, EntityOutline outline) {
      this.ink(mask, width, height, target, depth, outline, (Area)null, (Area)null);
   }

   private void ink(GpuTextureView mask, int width, int height, GpuTextureView target, GpuTextureView depth, EntityOutline outline, Area inkArea, Area readArea) {
      RenderSystem.assertOnRenderThread();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      this.upload(encoder, outline);
      if (outline.glow() > 0.0F) {
         this.glow(encoder, mask, width, height, target, depth, outline, inkArea, readArea);
      }

      this.scratch = this.target(this.scratch, "scratch", width, height);
      this.pass(encoder, "scan", SCAN, this.scratch.getColorTextureView(), (GpuTextureView)null, false, readArea, this.config, new Binding("InSampler", mask, nearest()));
      this.pass(encoder, "ink", INK, target, depth, false, inkArea, this.config, new Binding("InSampler", this.scratch.getColorTextureView(), nearest()));
   }

   private void water(GpuTextureView mask, int width, int height, GpuTextureView target, GpuTextureView depth, EntityOutline outline, Water look, Area inkArea, Area readArea) {
      RenderSystem.assertOnRenderThread();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      this.upload(encoder, outline);
      if (outline.glow() > 0.0F) {
         this.glow(encoder, mask, width, height, target, depth, outline, inkArea, readArea);
      }

      if (this.waterConfig == null) {
         this.waterConfig = this.buffer("water", 48);
      }

      float seconds = (float)((double)(System.nanoTime() - START) / (double)1.0E9F % (double)3600.0F);
      int tint = look.tint();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         encoder.writeToBuffer(this.waterConfig.slice(), Std140Builder.onStack(stack, 48).putFloat(seconds).putFloat(look.width()).putFloat(look.fill()).putFloat(look.shine()).putFloat(look.scale()).align(16).putVec4((float)Colors.red(tint) / 255.0F, (float)Colors.green(tint) / 255.0F, (float)Colors.blue(tint) / 255.0F, (float)Colors.alpha(tint) / 255.0F).get());
      } finally {
         stack.close();
      }

      this.pass(encoder, "water", WATER, target, depth, false, inkArea, this.waterConfig, new Binding("InSampler", mask, nearest()));
   }

   private void glow(CommandEncoder encoder, GpuTextureView mask, int width, int height, GpuTextureView target, GpuTextureView depth, EntityOutline outline, Area inkArea, Area readArea) {
      if (this.blur == null) {
         this.blur = new KawaseBlur("outline " + this.name);
      }

      ScreenRectangle half = readArea == null ? null : readArea.half().rectangle();
      GpuTextureView blurred = this.blur.blur(encoder, mask, width, height, outline.radius(), half);
      this.pass(encoder, "glow", GLOW, target, depth, false, inkArea, this.config, new Binding("InSampler", blurred, linear()), new Binding("MaskSampler", this.blur.downsampled(), linear()));
   }

   private void pass(CommandEncoder encoder, String label, RenderPipeline pipeline, GpuTextureView color, GpuTextureView depth, boolean clear, Area area, GpuBuffer uniform, Binding... textures) {
      RenderPass pass = encoder.createRenderPass(() -> "mace outline " + this.name + " " + label, color, clear ? Optional.of(CLEAR) : Optional.empty(), depth, OptionalDouble.empty());

      try {
         pass.setPipeline(pipeline);
         if (area != null) {
            pass.enableScissor(area.x0(), area.y0(), area.x1() - area.x0(), area.y1() - area.y0());
         }

         RenderSystem.bindDefaultUniforms(pass);

         for(Binding texture : textures) {
            pass.bindTexture(texture.name(), texture.view(), texture.sampler());
         }

         pass.setUniform("OutlineConfig", uniform.slice());
         pass.draw(3, 1, 0, 0);
      } catch (Throwable var16) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable var15) {
               var16.addSuppressed(var15);
            }
         }

         throw var16;
      }

      if (pass != null) {
         pass.close();
      }

   }

   private void upload(CommandEncoder encoder, EntityOutline outline) {
      if (this.config == null) {
         this.config = this.buffer("config", 48);
      }

      if (!outline.equals(this.uploaded)) {
         int color = outline.color();
         MemoryStack stack = MemoryStack.stackPush();

         try {
            encoder.writeToBuffer(this.config.slice(), Std140Builder.onStack(stack, 48).putFloat(outline.width()).align(16).putVec4((float)Colors.red(color) / 255.0F, (float)Colors.green(color) / 255.0F, (float)Colors.blue(color) / 255.0F, (float)Colors.alpha(color) / 255.0F).putFloat(outline.fill()).putFloat((float)outline.layers()).putFloat(outline.glow()).putFloat(0.0F).get());
         } catch (Throwable var8) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (stack != null) {
            stack.close();
         }

         this.uploaded = outline;
      }

   }

   private GpuBuffer buffer(String label, int size) {
      return RenderSystem.getDevice().createBuffer(() -> "mace outline " + this.name + " " + label, 136, (long)size);
   }

   private RenderTarget target(RenderTarget current, String label, int width, int height) {
      if (current != null && current.width == width && current.height == height) {
         return current;
      } else {
         if (current != null) {
            current.destroyBuffers();
         }

         return new TextureTarget("mace outline " + this.name + " " + label, width, height, false, GpuFormat.RGBA8_UNORM);
      }
   }

   private static GpuSampler nearest() {
      return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
   }

   private static GpuSampler linear() {
      return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
   }

   private static RenderPipeline pipeline(String fragmentShader, ColorTargetState color, BindGroupLayout samplers, BindGroupLayout uniforms) {
      return RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(VantaCore.id("pipeline/post/" + fragmentShader)).withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/screenquad")).withFragmentShader(VantaCore.id("post/" + fragmentShader)).withBindGroupLayout(samplers).withBindGroupLayout(uniforms).withColorTargetState(color).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build();
   }

   private EntityOutlines(String name) {
      this.name = name;
   }

   static {
      CONFIG = BindGroupLayout.builder().withUniform("OutlineConfig", UniformType.UNIFORM_BUFFER).build();
      GLOW_SAMPLERS = BindGroupLayout.builder().withSampler("InSampler").withSampler("MaskSampler").build();
      SCAN = pipeline("outline_scan", ColorTargetState.DEFAULT, BindGroupLayouts.IN_SAMPLER, CONFIG);
      INK = pipeline("outline", new ColorTargetState(BlendFunction.TRANSLUCENT), BindGroupLayouts.IN_SAMPLER, CONFIG);
      SCREEN = new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR);
      GLOW = pipeline("glow", new ColorTargetState(SCREEN), GLOW_SAMPLERS, CONFIG);
      WATER = pipeline("water", new ColorTargetState(BlendFunction.TRANSLUCENT), BindGroupLayouts.IN_SAMPLER, CONFIG);
      START = System.nanoTime();
   }

   public static record Water(float width, float fill, float shine, float scale, int tint) {
   }

   private static record Area(int x0, int y0, int x1, int y1) {
      Area union(int x0, int y0, int x1, int y1) {
         return new Area(Math.min(this.x0, x0), Math.min(this.y0, y0), Math.max(this.x1, x1), Math.max(this.y1, y1));
      }

      Area pad(int x, int y, int width, int height) {
         return new Area(Math.max(0, this.x0 - x), Math.max(0, this.y0 - y), Math.min(width, this.x1 + x), Math.min(height, this.y1 + y));
      }

      Area half() {
         return new Area(this.x0 / 2, this.y0 / 2, (this.x1 + 1) / 2, (this.y1 + 1) / 2);
      }

      ScreenRectangle rectangle() {
         return new ScreenRectangle(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0);
      }

      boolean isEmpty() {
         return this.x1 <= this.x0 || this.y1 <= this.y0;
      }
   }

   private static record Binding(String name, GpuTextureView view, GpuSampler sampler) {
   }
}
