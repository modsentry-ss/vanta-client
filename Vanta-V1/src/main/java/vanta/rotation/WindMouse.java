package vanta.rotation;

import java.util.concurrent.ThreadLocalRandom;
import vanta.util.Maths;

final class WindMouse implements Rotator {
   private static final float DONE = 0.06666667F;
   private Rotation node;
   private Rotation next;
   private Rotation expected;
   private float phase;
   private float velocityYaw;
   private float velocityPitch;
   private float windYaw;
   private float windPitch;
   private float cap;
   private float pace = 0.4F;
   private boolean travelling;
   private boolean settled;

   public Rotation step(Rotation from, Rotation target, RotationConfig config, float deltaTicks) {
      if (config.smoothness() <= 0.0F) {
         this.rest();
         this.next = null;
         return from.moved(from.yawTo(target), from.pitchTo(target));
      } else {
         if (this.next == null) {
            this.node = from;
            this.next = this.advance(from, target, config);
            this.phase = 0.0F;
         } else {
            float driftYaw = this.expected.yawTo(from);
            float driftPitch = this.expected.pitchTo(from);
            if (driftYaw != 0.0F || driftPitch != 0.0F) {
               this.node = this.node.moved(driftYaw, driftPitch);
               this.next = this.next.moved(driftYaw, driftPitch);
            }
         }

         for(this.phase += deltaTicks; this.phase >= 1.0F; this.next = this.advance(this.node, target, config)) {
            --this.phase;
            this.node = this.next;
         }

         this.expected = lerp(this.node, this.next, this.phase);
         return this.expected;
      }
   }

   private Rotation advance(Rotation from, Rotation target, RotationConfig config) {
      float deltaYaw = from.yawTo(target);
      float deltaPitch = from.pitchTo(target);
      float distance = Maths.length(deltaYaw, deltaPitch);
      float base = config.scaled(1.0F, 14.0F);
      ThreadLocalRandom random = ThreadLocalRandom.current();
      if (!this.travelling || this.settled && distance >= base * 0.8F) {
         float reach = Math.min(1.0F, distance / 60.0F);
         this.pace = (0.4F + 0.6F * reach) * random.nextFloat(0.85F, 1.15F);
         this.cap = base * this.pace;
         this.travelling = true;
         this.settled = false;
      }

      float speed = base * this.pace;
      if (distance < speed * 0.06666667F) {
         this.rest();
         return from.moved(deltaYaw, deltaPitch);
      } else {
         if (distance >= speed * 0.8F) {
            float gust = Math.min(speed * 0.2F, distance) / 2.236068F;
            this.windYaw = this.windYaw / 1.7320508F + random.nextFloat(-1.0F, 1.0F) * gust;
            this.windPitch = this.windPitch / 1.7320508F + random.nextFloat(-1.0F, 1.0F) * gust;
         } else {
            this.windYaw /= 1.7320508F;
            this.windPitch /= 1.7320508F;
            if (this.cap < speed * 0.2F * 1.0001F) {
               this.cap = speed * (0.2F + 0.2F * random.nextFloat());
            } else {
               this.cap /= 2.236068F;
            }

            this.settled = true;
         }

         float gravity = speed * 0.6F;
         this.velocityYaw += this.windYaw + gravity * deltaYaw / distance;
         this.velocityPitch += this.windPitch + gravity * deltaPitch / distance;
         float magnitude = Maths.length(this.velocityYaw, this.velocityPitch);
         if (magnitude > this.cap) {
            float clipped = this.cap * (0.5F + 0.5F * random.nextFloat());
            this.velocityYaw *= clipped / magnitude;
            this.velocityPitch *= clipped / magnitude;
         }

         return from.moved(this.velocityYaw, this.velocityPitch);
      }
   }

   private void rest() {
      this.windYaw = 0.0F;
      this.windPitch = 0.0F;
      this.velocityYaw = 0.0F;
      this.velocityPitch = 0.0F;
      this.travelling = false;
      this.settled = false;
   }

   private static Rotation lerp(Rotation from, Rotation to, float phase) {
      return phase <= 0.0F ? from : new Rotation(from.yaw() + (to.yaw() - from.yaw()) * phase, from.pitch() + (to.pitch() - from.pitch()) * phase);
   }
}
