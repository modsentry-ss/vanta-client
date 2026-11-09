package vanta.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import vanta.render.Scissor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record RectState(Matrix3x2fc pose, int x, int y, int width, int height, int radius, int shape, int color, int bottomColor, RenderPipeline pipeline, ScreenRectangle scissorArea) implements GuiElementRenderState {
   public RectState(Matrix3x2fc pose, int x, int y, int width, int height, int radius, int shape, int color, int bottomColor, RenderPipeline pipeline) {
      this(pose, x, y, width, height, radius, shape, color, bottomColor, pipeline, Scissor.current());
   }

   public void buildVertices(VertexConsumer vertices) {
      this.vertex(vertices, -1.0F, -1.0F, this.color);
      this.vertex(vertices, -1.0F, (float)(this.height + 1), this.bottomColor);
      this.vertex(vertices, (float)(this.width + 1), (float)(this.height + 1), this.bottomColor);
      this.vertex(vertices, (float)(this.width + 1), -1.0F, this.color);
   }

   private void vertex(VertexConsumer vertices, float localX, float localY, int vertexColor) {
      vertices.addVertexWith2DPose(this.pose, (float)this.x + localX, (float)this.y + localY).setColor(vertexColor).setUv(localX, localY).setUv1(this.radius, this.shape).setUv2(this.width, this.height);
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   public ScreenRectangle bounds() {
      return (new ScreenRectangle(this.x - 1, this.y - 1, this.width + 2, this.height + 2)).transformMaxBounds(this.pose);
   }
}
