package vanta.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;

public final class Shields {
   private static final int STALE_TICKS = 3;

   private Shields() {
   }

   public static boolean covers(LivingEntity target, BlocksAttacks blocks, Vec3 from, Vec3 yourMotion, double slack) {
      float angle = 90.0F;
      if (blocks != null && !blocks.damageReductions().isEmpty()) {
         angle = 0.0F;

         for(BlocksAttacks.DamageReduction reduction : blocks.damageReductions()) {
            angle = Math.max(angle, reduction.horizontalBlockingAngle());
         }
      }

      Vec3 view = Vec3.directionFromRotation(0.0F, target.getYHeadRot());
      Vec3 to = from.subtract(target.position());
      Vec3 flat = new Vec3(to.x, (double)0.0F, to.z);
      double cos = flat.lengthSqr() < 1.0E-6 ? (double)0.0F : Mth.clamp(flat.normalize().dot(view), (double)-1.0F, (double)1.0F);
      double turning = (double)(Math.abs(Mth.wrapDegrees(target.getYHeadRot() - target.yHeadRotO)) * 3.0F);
      double moved = Math.toDegrees(Math.atan2(yourMotion.horizontalDistance() * (double)3.0F, Math.max(flat.length(), 0.05)));
      return Math.toDegrees(Math.acos(cos)) <= (double)angle + slack + Math.min((double)180.0F, turning) + moved;
   }
}
