package vanta.rotation;

final class EaseOutCubic implements Rotator {
   public Rotation step(Rotation from, Rotation target, RotationConfig config, float deltaTicks) {
      float duration = config.scaled(40.0F, 0.0F);
      float remaining = duration <= 0.0F ? 0.0F : 1.0F - Math.min(1.0F, deltaTicks / duration);
      float fraction = 1.0F - remaining * remaining * remaining;
      return from.moved(from.yawTo(target) * fraction, from.pitchTo(target) * fraction);
   }
}
