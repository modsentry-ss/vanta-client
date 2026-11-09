package vanta.render.state;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Optional;
import vanta.VantaCore;
import vanta.render.font.Fonts;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;

public final class Pipelines {
   public static final VertexFormat SHAPE_FORMAT;
   public static final RenderPipeline RECT;
   public static final RenderPipeline HUE_BAR;
   public static final RenderPipeline SATURATION_VALUE;
   public static final RenderPipeline SHAPE;
   public static final RenderPipeline TEXT;
   public static final VertexFormat STAIRS_FORMAT;
   public static final RenderPipeline STAIRS;
   public static final RenderPipeline ICON;
   public static final RenderPipeline QUAD;

   private static RenderPipeline rect(String name, int mode) {
      return shape(name, "core/rect", "core/rect").withShaderDefine("RECT_MODE", mode).build();
   }

   private static RenderPipeline.Builder shape(String name, String vertexShader, String fragmentShader) {
      return builder(name, vertexShader, fragmentShader).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withVertexBinding(0, SHAPE_FORMAT);
   }

   private static RenderPipeline.Builder builder(String name, String vertexShader, String fragmentShader) {
      return RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(VantaCore.id("pipeline/" + name)).withVertexShader(VantaCore.id(vertexShader)).withFragmentShader(VantaCore.id(fragmentShader)).withPrimitiveTopology(PrimitiveTopology.QUADS).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withDepthStencilState(Optional.empty()).withCull(false);
   }

   private Pipelines() {
   }

   static {
      SHAPE_FORMAT = VertexFormat.builder(0).addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("Color", GpuFormat.RGBA8_UNORM).addAttribute("UV0", GpuFormat.RG32_FLOAT).addAttribute("UV1", GpuFormat.RG16_SINT).addAttribute("UV2", GpuFormat.RG16_SINT).build();
      RECT = rect("rect", 0);
      HUE_BAR = rect("hue_bar", 1);
      SATURATION_VALUE = rect("saturation_value", 2);
      SHAPE = shape("shape", "core/shape", "core/shape").build();
      TEXT = builder("text", "core/text", "core/text").withShaderDefine("MSDF_RANGE", Fonts.DISTANCE_RANGE).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).build();
      STAIRS_FORMAT = VertexFormat.builder(0).addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("Color", GpuFormat.RGBA8_UNORM).addAttribute("UV0", GpuFormat.RG32_FLOAT).addAttribute("UV1", GpuFormat.RG16_SINT).addAttribute("UV2", GpuFormat.RG16_SINT).addAttribute("LineWidth", GpuFormat.R32_FLOAT).build();
      STAIRS = builder("stairs", "core/stairs", "core/stairs").withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withVertexBinding(0, STAIRS_FORMAT).build();
      ICON = builder("icon", "core/icon", "core/icon").withShaderDefine("MSDF_RANGE", Fonts.DISTANCE_RANGE).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0).withVertexBinding(0, SHAPE_FORMAT).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)).build();
      QUAD = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.GUI_SNIPPET}).withLocation(VantaCore.id("pipeline/quad")).withCull(false).build();
   }
}
