package vanta.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import vanta.render.Rect;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record FadedTextureState(Matrix3x2fc pose, TextureSetup textureSetup, float u0, float fade, int color, ScreenRectangle scissorArea) implements GuiElementRenderState {
   public void buildVertices(VertexConsumer vertices) {
      int clear = this.color & 16777215;
      this.quad(vertices, 0.0F, this.fade, clear, this.color);
      this.quad(vertices, this.fade, 1.0F, this.color, this.color);
   }

   private void quad(VertexConsumer vertices, float x0, float x1, int left, int right) {
      float left0 = this.u(x0);
      float right0 = this.u(x1);
      vertices.addVertexWith2DPose(this.pose, x0, 0.0F).setUv(left0, 0.0F).setColor(left);
      vertices.addVertexWith2DPose(this.pose, x0, 1.0F).setUv(left0, 1.0F).setColor(left);
      vertices.addVertexWith2DPose(this.pose, x1, 1.0F).setUv(right0, 1.0F).setColor(right);
      vertices.addVertexWith2DPose(this.pose, x1, 0.0F).setUv(right0, 0.0F).setColor(right);
   }

   private float u(float x) {
      return this.u0 + x * (1.0F - this.u0);
   }

   public RenderPipeline pipeline() {
      return RenderPipelines.GUI_TEXTURED;
   }

   public ScreenRectangle bounds() {
      return Geometry.bounds(new Rect(0.0F, 0.0F, 1.0F, 1.0F), this.pose);
   }
}
