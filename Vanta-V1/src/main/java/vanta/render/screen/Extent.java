package vanta.render.screen;

import vanta.render.Point;
import vanta.render.Rect;

final class Extent {
   private float minX = Float.POSITIVE_INFINITY;
   private float minY = Float.POSITIVE_INFINITY;
   private float maxX = Float.NEGATIVE_INFINITY;
   private float maxY = Float.NEGATIVE_INFINITY;

   void add(Point point) {
      this.minX = Math.min(this.minX, point.x());
      this.minY = Math.min(this.minY, point.y());
      this.maxX = Math.max(this.maxX, point.x());
      this.maxY = Math.max(this.maxY, point.y());
   }

   Rect toRect() {
      return this.minX <= this.maxX ? new Rect(this.minX, this.minY, this.maxX, this.maxY) : null;
   }
}
