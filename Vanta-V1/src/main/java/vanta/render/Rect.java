package vanta.render;

public record Rect(float minX, float minY, float maxX, float maxY) {
   public static Rect of(float x0, float y0, float x1, float y1) {
      return new Rect(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1));
   }

   public float width() {
      return this.maxX - this.minX;
   }

   public float height() {
      return this.maxY - this.minY;
   }

   public Rect expand(float amount) {
      return new Rect(this.minX - amount, this.minY - amount, this.maxX + amount, this.maxY + amount);
   }
}
