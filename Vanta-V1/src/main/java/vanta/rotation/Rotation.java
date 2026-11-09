package vanta.rotation;

import vanta.util.Maths;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record Rotation(float yaw, float pitch) {
   public static Rotation of(Entity entity) {
      return new Rotation(entity.getYRot(), entity.getXRot());
   }

   public static Rotation toward(Vec3 from, Vec3 to) {
      Vec3 delta = to.subtract(from);
      float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
      return new Rotation(yaw, pitch);
   }

   public Vec3 direction() {
      return Vec3.directionFromRotation(this.pitch, this.yaw);
   }

   public float yawTo(Rotation target) {
      return Mth.wrapDegrees(target.yaw - this.yaw);
   }

   public float pitchTo(Rotation target) {
      return Mth.clamp(target.pitch, -90.0F, 90.0F) - this.pitch;
   }

   public float distanceTo(Rotation target) {
      return Maths.length(this.yawTo(target), this.pitchTo(target));
   }

   public Rotation moved(float deltaYaw, float deltaPitch) {
      return new Rotation(this.yaw + deltaYaw, Mth.clamp(this.pitch + deltaPitch, -90.0F, 90.0F));
   }
}
