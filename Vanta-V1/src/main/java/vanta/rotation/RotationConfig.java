package vanta.rotation;

public record RotationConfig(float smoothness, Smoothing smoothing, boolean silent, boolean fixesMovement) {
   public static RotationConfig visible(float smoothness, Smoothing smoothing) {
      return new RotationConfig(smoothness, smoothing, false, false);
   }

   public static RotationConfig silent(float smoothness, Smoothing smoothing) {
      return new RotationConfig(smoothness, smoothing, true, false);
   }

   public RotationConfig fixingMovement() {
      return new RotationConfig(this.smoothness, this.smoothing, this.silent, true);
   }

   public float scaled(float slow, float fast) {
      float eager = (1.0F - this.smoothness) * (1.0F - this.smoothness);
      return slow + (fast - slow) * eager;
   }
}
