package vanta.rotation;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Mth;

final class HumanFlick {
   private static final float JUMP = 8.0F;
   private static final float CORRECT = 2.5F;
   private static final float LAND = 0.3F;
   private static final float MAX_STEP = 170.0F;
   private Rotation goal;
   private Rotation last;
   private boolean flicking;
   private float errorYaw;
   private float errorPitch;
   private float overYaw;
   private float overPitch;
   private float carryYaw;
   private float carryPitch;
   private float bowYaw;
   private float bowPitch;
   private float duration;
   private float elapsed;
   private float warp;
   private float delay;
   private float lagYaw;
   private float lagPitch;
   private float lagShare;
   private float gain = 0.74F;

   void reset() {
      this.goal = null;
      this.last = null;
      this.flicking = false;
      this.lagYaw = 0.0F;
      this.lagPitch = 0.0F;
   }

   Rotation step(Rotation from, Rotation goal, float slow, boolean lag) {
      float speedYaw = this.last != null ? Mth.wrapDegrees(from.yaw() - this.last.yaw()) : 0.0F;
      float speedPitch = this.last != null ? from.pitch() - this.last.pitch() : 0.0F;
      Rotation previousGoal = this.goal;
      this.goal = goal;
      this.last = from;
      if (previousGoal == null) {
         return this.landed(this.plan(from, goal, 0.0F, 0.0F, slow, lag));
      } else {
         float movedYaw = Mth.wrapDegrees(goal.yaw() - previousGoal.yaw());
         float movedPitch = goal.pitch() - previousGoal.pitch();
         if (Mth.length(movedYaw, movedPitch) > 8.0F) {
            return this.landed(this.plan(from, goal, speedYaw, speedPitch, slow, lag));
         } else if (this.flicking) {
            this.lagYaw = this.lagYaw * 0.55F + movedYaw * this.lagShare;
            this.lagPitch = this.lagPitch * 0.55F + movedPitch * this.lagShare;
            return this.landed(this.flick(from, goal));
         } else {
            float yaw = from.yawTo(goal);
            float pitch = from.pitchTo(goal);
            float error = Mth.length(yaw, pitch);
            if (error > 2.5F * slow) {
               return this.landed(this.plan(from, goal, speedYaw, speedPitch, slow, lag));
            } else if (error < 0.3F) {
               return this.landed(goal);
            } else {
               ThreadLocalRandom random = ThreadLocalRandom.current();
               this.gain += (0.74F - this.gain) * 0.3F + (float)random.nextGaussian() * 0.07F;
               this.gain = Math.clamp(this.gain, 0.5F, 0.97F);
               float share = lag ? this.gain / slow : 1.0F;
               return from.moved(yaw * share, pitch * share);
            }
         }
      }
   }

   private Rotation landed(Rotation next) {
      Rotation from = this.last;
      float yaw = from.yawTo(next);
      float pitch = from.pitchTo(next);
      float length = Mth.length(yaw, pitch);
      float scale = length > 170.0F ? 170.0F / length : 1.0F;
      return from.moved(yaw * scale, pitch * scale);
   }

   private Rotation plan(Rotation from, Rotation goal, float speedYaw, float speedPitch, float slow, boolean lag) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      float yaw = from.yawTo(goal);
      float pitch = from.pitchTo(goal);
      float distance = Mth.length(yaw, pitch);
      float fitts = 0.8F + 0.5F * (float)(Math.log((double)1.0F + (double)distance / (double)10.0F) / Math.log((double)2.0F));
      float spread = Math.clamp((float)Math.exp(random.nextGaussian() * 0.14), 0.8F, 1.25F);
      this.duration = Math.clamp(fitts * spread * slow, 1.0F, 3.4F * slow);
      this.elapsed = 0.0F;
      double roll = random.nextDouble();
      float over;
      if (distance < 12.0F) {
         over = (float)random.nextGaussian() * 0.02F;
      } else if (roll < 0.33) {
         over = 0.02F + random.nextFloat() * 0.045F;
      } else if (roll < 0.58) {
         over = -(0.02F + random.nextFloat() * 0.035F);
      } else {
         over = (float)random.nextGaussian() * 0.012F;
      }

      over = Math.clamp(over, -4.0F / distance, 5.0F / distance);
      this.errorYaw = yaw;
      this.errorPitch = pitch;
      this.overYaw = yaw * over;
      this.overPitch = pitch * over;
      float bow = distance < 3.0F ? 0.0F : Math.clamp((float)random.nextGaussian() * 0.035F, -0.08F, 0.08F);
      this.bowYaw = -pitch * bow;
      this.bowPitch = yaw * bow;
      boolean sideways = Math.abs(yaw) >= Math.abs(pitch);
      this.delay = sideways ? -0.04F + random.nextFloat() * 0.17F : -0.1F + random.nextFloat() * 0.15F;
      this.warp = 0.55F + random.nextFloat() * 0.3F;
      this.carryYaw = speedYaw;
      this.carryPitch = speedPitch;
      this.lagYaw = 0.0F;
      this.lagPitch = 0.0F;
      this.lagShare = lag ? 0.08F + random.nextFloat() * 0.14F : 0.0F;
      this.flicking = true;
      return this.flick(from, goal);
   }

   private Rotation flick(Rotation from, Rotation goal) {
      ++this.elapsed;
      float t = Math.min(1.0F, this.elapsed / this.duration);
      if (t >= 1.0F) {
         this.flicking = false;
      }

      float yawDelay = this.delay < 0.0F ? -this.delay : 0.0F;
      float pitchDelay = Math.max(0.0F, this.delay);
      float yawDone = minimumJerk(this.warped(delayed(t, yawDelay)));
      float pitchDone = minimumJerk(this.warped(delayed(t, pitchDelay)));
      float bump = 16.0F * t * t * (1.0F - t) * (1.0F - t);
      float carry = this.duration * t * (1.0F - t) * (1.0F - t) * (1.0F - t);
      float restYaw = this.errorYaw - (this.errorYaw + this.overYaw) * yawDone + this.bowYaw * bump - this.carryYaw * carry + this.lagYaw;
      float restPitch = this.errorPitch - (this.errorPitch + this.overPitch) * pitchDone + this.bowPitch * bump - this.carryPitch * carry + this.lagPitch;
      return goal.moved(-restYaw, -restPitch);
   }

   private float warped(float t) {
      return (float)Math.pow((double)t, (double)this.warp);
   }

   private static float delayed(float t, float delay) {
      return delay <= 0.0F ? t : Math.clamp((t - delay) / (1.0F - delay), 0.0F, 1.0F);
   }

   private static float minimumJerk(float t) {
      return t * t * t * (10.0F - 15.0F * t + 6.0F * t * t);
   }
}
