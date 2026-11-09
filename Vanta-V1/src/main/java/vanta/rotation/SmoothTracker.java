package vanta.rotation;

import vanta.util.Humanize;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class SmoothTracker {
   public static final RotationConfig CONFIG;
   private static final float FAST_TAU = 0.4F;
   private static final float SLOW_TAU = 8.0F;
   private static final float MAX_FRAME_TICKS = 2.0F;
   private static final float MAX_FOLLOW = 12.0F;
   private static final float INSTANT = 0.1F;
   private final float rampScale;
   private final float fadeScale;
   private float strength;
   private float lastStrength;
   private Rotation lastMotion;
   private double lastFrame = (double)-1.0F;
   private float speedJitter;
   private float noiseDegrees;
   private float speedFactor = 1.0F;
   private int seed = Humanize.seed();
   private static final double NOISE_PERIOD = (double)7.0F;
   private static final double SPEED_PERIOD = (double)13.0F;

   public SmoothTracker(float rampScale, float fadeScale) {
      this.rampScale = rampScale;
      this.fadeScale = fadeScale;
   }

   public void reset() {
      this.strength = 0.0F;
      this.lastStrength = 0.0F;
      this.lastMotion = null;
      this.lastFrame = (double)-1.0F;
      this.seed = Humanize.seed();
      this.speedFactor = 1.0F + this.speedJitter * Humanize.signed();
   }

   public void humanize(float speedJitter, float noiseDegrees) {
      float jitter = Mth.clamp(speedJitter / 100.0F, 0.0F, 0.9F);
      if (jitter != this.speedJitter) {
         this.speedJitter = jitter;
         this.speedFactor = 1.0F + jitter * Humanize.signed();
      }

      this.noiseDegrees = Math.max(0.0F, noiseDegrees);
   }

   public void rampUp(float speed) {
      float ticks = tau(speed) * this.rampScale;
      this.lastStrength = this.strength;
      this.strength = ticks < 0.1F ? 1.0F : Math.min(1.0F, this.strength + 1.0F / ticks);
   }

   public boolean fadeOut(float speed) {
      float ticks = tau(speed) * this.fadeScale;
      this.lastStrength = this.strength;
      this.strength = ticks < 0.1F ? 0.0F : Math.max(0.0F, this.strength - 1.0F / ticks);
      if (this.strength <= 0.0F) {
         this.reset();
         return false;
      } else {
         return true;
      }
   }

   public Rotation step(Rotation view, Rotation goal, Rotation motion, float speed, float partialTick) {
      Minecraft client = Minecraft.getInstance();
      double now = (double)((float)(client.player != null ? client.player.tickCount : 0) + partialTick);
      float dt = this.lastFrame < (double)0.0F ? 0.0F : (float)Mth.clamp(now - this.lastFrame, (double)0.0F, (double)2.0F);
      float followYaw = 0.0F;
      float followPitch = 0.0F;
      if (motion != null && this.lastMotion != null) {
         followYaw = Mth.clamp(this.lastMotion.yawTo(motion), -12.0F, 12.0F);
         followPitch = Mth.clamp(motion.pitch() - this.lastMotion.pitch(), -12.0F, 12.0F);
      }

      this.lastMotion = motion;
      this.lastFrame = now;
      if (this.speedJitter > 0.0F) {
         speed *= Math.max(0.1F, this.speedFactor * (1.0F + 0.5F * this.speedJitter * Humanize.noise(this.seed, now / (double)13.0F)));
      }

      if (this.noiseDegrees > 0.0F) {
         goal = goal.moved(this.noiseDegrees * Humanize.noise(this.seed + 1, now / (double)7.0F), 0.6F * this.noiseDegrees * Humanize.noise(this.seed + 2, now / (double)7.0F));
      }

      float close = 1.0F - (float)Math.exp((double)(-dt / tau(speed)));
      float strength = Mth.lerp(partialTick, this.lastStrength, this.strength);
      float eased = strength >= 1.0F ? 1.0F : strength * strength * (3.0F - 2.0F * strength);
      float yaw = (followYaw + (view.yawTo(goal) - followYaw) * close) * eased;
      float pitch = (followPitch + (view.pitchTo(goal) - followPitch) * close) * eased;
      return view.moved(yaw, pitch);
   }

   public static float speedFor(float tau) {
      if (tau >= 8.0F) {
         return 0.0F;
      } else {
         return tau >= 0.4F ? (8.0F - tau) / 7.6F * 100.0F : 40.0F / Math.max(0.01F, tau);
      }
   }

   public static float tau(float speed) {
      speed = Math.max(0.0F, speed);
      return speed <= 100.0F ? 8.0F + -7.6F * speed / 100.0F : 40.0F / speed;
   }

   static {
      CONFIG = RotationConfig.visible(0.0F, Smoothing.EASE_OUT_CUBIC);
   }
}
