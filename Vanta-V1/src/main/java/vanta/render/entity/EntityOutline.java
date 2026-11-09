package vanta.render.entity;

public record EntityOutline(int color, float width, float fill, int layers, float glow, float radius) {
   public EntityOutline withColor(int color) {
      return new EntityOutline(color, this.width, this.fill, this.layers, this.glow, this.radius);
   }

   public float reach() {
      return Math.max(this.width * (float)(2 * this.layers + 1), this.glow > 0.0F ? this.radius : 0.0F);
   }
}
