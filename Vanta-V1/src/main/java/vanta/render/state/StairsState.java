package vanta.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import vanta.render.Scissor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record StairsState(Matrix3x2fc pose, Row row, int color, ScreenRectangle scissorArea) implements GuiElementRenderState {
   public static final int FLUSH_INNER = 1;
   public static final int FLUSH_TOP = 2;
   public static final int NONE = -1;

   public StairsState(Matrix3x2fc pose, Row row, int color) {
      this(pose, row, color, Scissor.current());
   }

   public void buildVertices(VertexConsumer vertices) {
      float left = (this.row.edges() & 1) != 0 ? 0.0F : -1.0F;
      float right = (float)(Math.max(this.row.width(), Math.max(this.row.above(), this.row.below())) + this.row.radius() + 1);
      float top = this.row.first() ? -1.0F : 0.0F;
      float bottom = this.row.last() ? (float)(this.row.height() + 1) : (float)this.row.height();
      this.vertex(vertices, left, top);
      this.vertex(vertices, left, bottom);
      this.vertex(vertices, right, bottom);
      this.vertex(vertices, right, top);
   }

   private void vertex(VertexConsumer vertices, float localX, float localY) {
      float screenX = this.row.mirrored() ? (float)this.row.innerX() - localX : (float)this.row.innerX() + localX;
      vertices.addVertexWith2DPose(this.pose, screenX, (float)this.row.y() + localY).setColor(this.color).setUv(localX, localY).setUv1(this.row.radius(), this.row.edges()).setUv2(this.row.width(), this.row.height()).setLineWidth((float)(this.row.above() + 2048) * 4096.0F + (float)this.row.below() + 2048.0F);
   }

   public RenderPipeline pipeline() {
      return Pipelines.STAIRS;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }

   public ScreenRectangle bounds() {
      int reach = Math.max(this.row.width(), Math.max(this.row.above(), this.row.below())) + this.row.radius() + 1;
      int x = this.row.mirrored() ? this.row.innerX() - reach : this.row.innerX() - 1;
      return (new ScreenRectangle(x, this.row.y() - 1, reach + 1, this.row.height() + 2)).transformMaxBounds(this.pose);
   }

   public static record Row(int innerX, int y, int width, int height, int above, int below, int radius, int edges, boolean mirrored, boolean first, boolean last) {
   }
}
