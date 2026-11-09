package vanta.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vanta.render.Scissor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public record TextState(Matrix3x2fc pose, TextureSetup textureSetup, List<Quad> quads, RenderPipeline pipeline, int backdrop, ScreenRectangle bounds, ScreenRectangle scissorArea) implements GuiElementRenderState {
   private static final Map<GpuTextureView, TextureSetup> SETUPS = new HashMap();

   public static TextState of(GuiGraphicsExtractor graphics, Identifier atlas, List<Quad> quads) {
      return of(graphics, atlas, quads, Pipelines.TEXT, 0);
   }

   public static TextState of(GuiGraphicsExtractor graphics, Identifier atlas, List<Quad> quads, ScreenRectangle scissor) {
      TextState state = of(graphics, atlas, quads, Pipelines.TEXT, 0);
      ScreenRectangle bounds = scissor == null ? state.bounds : scissor.intersection(state.bounds);
      return bounds != null && bounds.width() > 0 && bounds.height() > 0 ? new TextState(state.pose, state.textureSetup, state.quads, state.pipeline, state.backdrop, bounds, scissor) : null;
   }

   public static TextState icon(GuiGraphicsExtractor graphics, Identifier atlas, List<Quad> quads, int backdrop) {
      return of(graphics, atlas, quads, Pipelines.ICON, backdrop);
   }

   private static TextState of(GuiGraphicsExtractor graphics, Identifier atlas, List<Quad> quads, RenderPipeline pipeline, int backdrop) {
      float left = Float.POSITIVE_INFINITY;
      float top = Float.POSITIVE_INFINITY;
      float right = Float.NEGATIVE_INFINITY;
      float bottom = Float.NEGATIVE_INFINITY;

      for(Quad quad : quads) {
         left = Math.min(left, quad.left);
         top = Math.min(top, quad.top);
         right = Math.max(right, quad.right);
         bottom = Math.max(bottom, quad.bottom);
      }

      Matrix3x2f pose = new Matrix3x2f(graphics.pose());
      ScreenRectangle bounds = (new ScreenRectangle((int)Math.floor((double)left), (int)Math.floor((double)top), (int)Math.ceil((double)right) - (int)Math.floor((double)left), (int)Math.ceil((double)bottom) - (int)Math.floor((double)top))).transformMaxBounds(pose);
      TextureSetup textureSetup = setup(Minecraft.getInstance().getTextureManager().getTexture(atlas).getTextureView());
      return new TextState(pose, textureSetup, List.copyOf(quads), pipeline, backdrop, bounds, Scissor.current());
   }

   public static TextureSetup setup(GpuTextureView view) {
      TextureSetup setup = (TextureSetup)SETUPS.get(view);
      if (setup == null) {
         SETUPS.keySet().removeIf(GpuTextureView::isClosed);
         setup = TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         SETUPS.put(view, setup);
      }

      return setup;
   }

   public void buildVertices(VertexConsumer vertices) {
      for(Quad quad : this.quads) {
         this.vertex(vertices, quad.left, quad.top, quad.u0, quad.v0, quad.leftColor);
         this.vertex(vertices, quad.left, quad.bottom, quad.u0, quad.v1, quad.leftColor);
         this.vertex(vertices, quad.right, quad.bottom, quad.u1, quad.v1, quad.rightColor);
         this.vertex(vertices, quad.right, quad.top, quad.u1, quad.v0, quad.rightColor);
      }

   }

   private void vertex(VertexConsumer vertices, float x, float y, float u, float v, int color) {
      vertices.addVertexWith2DPose(this.pose, x, y).setUv(u, v).setColor(color).setUv1(this.backdrop >> 16 & 255, this.backdrop >> 8 & 255).setUv2(this.backdrop & 255, 0);
   }

   public static record Quad(float left, float top, float right, float bottom, float u0, float v0, float u1, float v1, int leftColor, int rightColor) {
      public Quad(float left, float top, float right, float bottom, float u0, float v0, float u1, float v1, int color) {
         this(left, top, right, bottom, u0, v0, u1, v1, color, color);
      }
   }
}
