package vanta.render.state;

import com.mojang.blaze3d.vertex.VertexConsumer;
import vanta.render.Rect;
import vanta.util.Maths;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class Geometry {
   static final float PADDING = 1.0F;

   static ScreenRectangle bounds(Rect rect, Matrix3x2fc pose) {
      Rect outer = rect.expand(1.0F);
      return (new ScreenRectangle((int)Math.floor((double)outer.minX()), (int)Math.floor((double)outer.minY()), (int)Math.ceil((double)outer.width()) + 1, (int)Math.ceil((double)outer.height()) + 1)).transformMaxBounds(pose);
   }

   static void fill(VertexConsumer consumer, Matrix3x2fc pose, Rect rect, int color) {
      shape(consumer, pose, rect.minX(), rect.minY(), rect.width(), rect.height(), 0.0F, color);
   }

   static void border(VertexConsumer consumer, Matrix3x2fc pose, Rect rect, float width, int color) {
      Rect outer = rect.expand(width * 0.5F);
      shape(consumer, pose, outer.minX(), outer.minY(), outer.width(), outer.height(), width, color);
   }

   static void corners(VertexConsumer consumer, Matrix3x2fc pose, Rect rect, float width, float length, int color) {
      Rect outer = rect.expand(width * 0.5F);
      float reach = Math.min(length, Math.min(outer.width(), outer.height()) * 0.5F);
      float x0 = outer.minX();
      float y0 = outer.minY();
      float x1 = outer.maxX();
      float y1 = outer.maxY();
      shape(consumer, pose, x0, y0, reach, width, 0.0F, color);
      shape(consumer, pose, x0, y0, width, reach, 0.0F, color);
      shape(consumer, pose, x1 - reach, y0, reach, width, 0.0F, color);
      shape(consumer, pose, x1 - width, y0, width, reach, 0.0F, color);
      shape(consumer, pose, x0, y1 - width, reach, width, 0.0F, color);
      shape(consumer, pose, x0, y1 - reach, width, reach, 0.0F, color);
      shape(consumer, pose, x1 - reach, y1 - width, reach, width, 0.0F, color);
      shape(consumer, pose, x1 - width, y1 - reach, width, reach, 0.0F, color);
   }

   static void line(VertexConsumer consumer, Matrix3x2fc pose, float x0, float y0, float x1, float y1, float width, int color) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float length = Maths.length(dx, dy);
      if (!Maths.nearlyZero(length)) {
         Matrix3x2f along = (new Matrix3x2f(pose)).translate(x0, y0).rotate((float)Math.atan2((double)dy, (double)dx));
         shape(consumer, along, 0.0F, -width * 0.5F, length, width, 0.0F, color);
      }

   }

   private static void shape(VertexConsumer consumer, Matrix3x2fc pose, float x, float y, float width, float height, float border, int color) {
      if (!(width <= 0.0F) && !(height <= 0.0F)) {
         int sizeX = fixed(width);
         int sizeY = fixed(height);
         int ring = fixed(border);
         vertex(consumer, pose, x, y, -1.0F, -1.0F, sizeX, sizeY, ring, color);
         vertex(consumer, pose, x, y, -1.0F, height + 1.0F, sizeX, sizeY, ring, color);
         vertex(consumer, pose, x, y, width + 1.0F, height + 1.0F, sizeX, sizeY, ring, color);
         vertex(consumer, pose, x, y, width + 1.0F, -1.0F, sizeX, sizeY, ring, color);
      }

   }

   private static void vertex(VertexConsumer consumer, Matrix3x2fc pose, float x, float y, float localX, float localY, int sizeX, int sizeY, int ring, int color) {
      consumer.addVertexWith2DPose(pose, x + localX, y + localY).setColor(color).setUv(localX, localY).setUv1(ring, 0).setUv2(sizeX, sizeY);
   }

   private static int fixed(float value) {
      return Math.clamp((long)Math.round(value * 8.0F), 0, 32767);
   }

   private Geometry() {
   }
}
