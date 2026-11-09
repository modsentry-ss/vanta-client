package vanta.render.world;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import vanta.VantaCore;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class WorldRenderTypes {
   private static final DepthStencilState SEE_THROUGH;
   private static final RenderPipeline FILL_PIPELINE;
   private static final RenderPipeline LINES_PIPELINE;
   private static final DepthStencilState DEPTH_TESTED;
   private static final RenderPipeline FILL_DEPTH_PIPELINE;
   private static final RenderPipeline LINES_DEPTH_PIPELINE;
   public static final RenderType FILL_DEPTH;
   public static final RenderType LINES_DEPTH;
   public static final RenderType FILL;
   public static final RenderType LINES;

   private WorldRenderTypes() {
   }

   static {
      SEE_THROUGH = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
      FILL_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET}).withLocation(VantaCore.id("pipeline/world_fill")).withDepthStencilState(SEE_THROUGH).withCull(false).build();
      LINES_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.LINES_SNIPPET}).withLocation(VantaCore.id("pipeline/world_lines")).withDepthStencilState(SEE_THROUGH).build();
      DEPTH_TESTED = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false);
      FILL_DEPTH_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET}).withLocation(VantaCore.id("pipeline/world_fill_depth")).withDepthStencilState(DEPTH_TESTED).withCull(false).build();
      LINES_DEPTH_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.LINES_SNIPPET}).withLocation(VantaCore.id("pipeline/world_lines_depth")).withDepthStencilState(DEPTH_TESTED).build();
      FILL_DEPTH = RenderType.create("vanta:world_fill_depth", RenderSetup.builder(FILL_DEPTH_PIPELINE).sortOnUpload().setOutputTarget(TerrainDepth.OUTPUT).createRenderSetup());
      LINES_DEPTH = RenderType.create("vanta:world_lines_depth", RenderSetup.builder(LINES_DEPTH_PIPELINE).setOutputTarget(TerrainDepth.OUTPUT).createRenderSetup());
      FILL = RenderType.create("vanta:world_fill", RenderSetup.builder(FILL_PIPELINE).sortOnUpload().createRenderSetup());
      LINES = RenderType.create("vanta:world_lines", RenderSetup.builder(LINES_PIPELINE).createRenderSetup());
   }
}
