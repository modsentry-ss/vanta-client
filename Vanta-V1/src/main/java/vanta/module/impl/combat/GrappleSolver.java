package vanta.module.impl.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

final class GrappleSolver {
   static final int MAX_TICKS = 60;
   private static final double GRAVITY = 0.03;
   private static final double DRAG = 0.99;
   private static final float DEG = ((float)Math.PI / 180F);
   private static final Vec3[] SPREADS = new Vec3[16];
   private static final double[] LEAD_WEIGHTS = new double[]{(double)0.25F, (double)0.5F, (double)0.25F};

   private GrappleSolver() {
   }

   static Vec3 velocity(float yaw, float pitch, Vec3 spread, Vec3 inherited) {
      return CatchSolver.windVelocity(yaw, pitch, spread, inherited);
   }

   private static double margin(int age) {
      return Math.max((double)0.0F, Math.min(0.3, (double)(age - 2) / (double)20.0F));
   }

   private static int hitTick(Shot s, Vec3 v0, int leadShift) {
      Vec3 position = s.spawn();
      Vec3 motion = v0;

      for(int j = 0; j < 60; ++j) {
         motion = motion.add((double)0.0F, -0.03, (double)0.0F).scale(0.99);
         Vec3 to = position.add(motion);
         if (s.target().at(s.lead() + leadShift + j).inflate(margin(j + 1)).clip(position, to).isPresent()) {
            return j;
         }

         position = to;
      }

      return -1;
   }

   static Plan plan(Shot s, float yaw, float pitch) {
      Vec3 v0 = velocity(yaw, pitch, Vec3.ZERO, s.inherited());
      int ticks = hitTick(s, v0, 0);
      if (ticks >= 0 && CatchSolver.PearlPath.of(s.level(), s.spawn(), v0).ticks() <= ticks) {
         ticks = -1;
      }

      double chance = (double)0.0F;

      for(int lead = -1; lead <= 1; ++lead) {
         int hits = 0;

         for(int i = 0; i < SPREADS.length; ++i) {
            Vec3 v = lead == 0 && i == 0 ? v0 : velocity(yaw, pitch, SPREADS[i], s.inherited());
            if (lead == 0 && i == 0) {
               if (ticks < 0) {
                  continue;
               }
            } else if (hitTick(s, v, lead) < 0) {
               continue;
            }

            ++hits;
         }

         chance += LEAD_WEIGHTS[lead + 1] * (double)hits / (double)SPREADS.length;
      }

      return new Plan(yaw, pitch, ticks, ticks >= 0 ? chance : (double)0.0F);
   }

   static Plan best(Shot s, float viewYaw) {
      List<Vec3> directions = new ArrayList();
      double previous = Double.NaN;
      Vec3[] candidate = new Vec3[61];
      double[] speedGap = new double[61];
      double d = (double)0.0F;
      double dn = (double)1.0F;

      for(int n = 1; n <= 60; ++n) {
         dn *= 0.99;
         d += dn;
         double g = 98.99999999999991 * ((double)n - d);
         Vec3 centre = s.target().at(s.lead() + n - 1).getCenter();
         Vec3 v0 = centre.subtract(s.spawn()).add((double)0.0F, 0.03 * g, (double)0.0F).scale((double)1.0F / d);
         Vec3 launch = v0.subtract(s.inherited());
         candidate[n] = launch.lengthSqr() > 1.0E-8 ? launch.normalize() : null;
         speedGap[n] = launch.length() - (double)1.5F;
         if (!Double.isNaN(previous) && Math.signum(previous) != Math.signum(speedGap[n])) {
            for(int k = Math.max(1, n - 2); k <= Math.min(60, n + 1); ++k) {
               if (k <= n && candidate[k] != null) {
                  directions.add(candidate[k]);
               }
            }
         }

         previous = speedGap[n];
      }

      for(int n = 1; n <= 3; ++n) {
         if (candidate[n] != null) {
            directions.add(candidate[n]);
         }
      }

      Plan best = null;

      for(Vec3 dir : directions) {
         float yaw = viewYaw + Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(-dir.x, dir.z)) - viewYaw);
         float pitch = (float)(-Math.toDegrees(Math.asin(Mth.clamp(dir.y, (double)-1.0F, (double)1.0F))));
         Plan plan = plan(s, yaw, pitch);
         if (plan.hits() && (best == null || plan.chance() > best.chance() + 1.0E-9 || Math.abs(plan.chance() - best.chance()) <= 1.0E-9 && plan.ticks() < best.ticks())) {
            best = plan;
         }
      }

      return best;
   }

   static {
      Random random = new Random(27153L);
      SPREADS[0] = Vec3.ZERO;

      for(int i = 1; i < SPREADS.length; ++i) {
         SPREADS[i] = new Vec3(0.0172275 * (random.nextDouble() - random.nextDouble()), 0.0172275 * (random.nextDouble() - random.nextDouble()), 0.0172275 * (random.nextDouble() - random.nextDouble()));
      }

   }

   static record TargetPath(AABB[] boxes) {
      static TargetPath of(Level level, AABB box, Vec3 velocity, boolean air, int ticks) {
         if (!air) {
            velocity = new Vec3(velocity.x, (double)0.0F, velocity.z);
         }

         AABB[] boxes = new AABB[ticks + 1];
         AABB at = box;

         for(int i = 0; i <= ticks; ++i) {
            boxes[i] = at;
            if (air) {
               velocity = new Vec3(velocity.x * 0.91, (velocity.y - 0.08) * 0.98, velocity.z * 0.91);
            }

            Vec3 move = velocity;
            if (air && !level.noCollision(at.move(velocity))) {
               Vec3 allowed = Entity.collideBoundingBox(CollisionContext.empty(), velocity, at, level, List.of());
               if (velocity.y < (double)0.0F && allowed.y > velocity.y + 1.0E-7) {
                  velocity = new Vec3(velocity.x, (double)0.0F, velocity.z);
                  air = false;
               }

               velocity = new Vec3(Math.abs(allowed.x - move.x) > 1.0E-7 ? (double)0.0F : velocity.x, velocity.y, Math.abs(allowed.z - move.z) > 1.0E-7 ? (double)0.0F : velocity.z);
               move = allowed;
            }

            at = at.move(move);
         }

         return new TargetPath(boxes);
      }

      AABB at(int tick) {
         return this.boxes[Mth.clamp(tick, 0, this.boxes.length - 1)];
      }
   }

   static record Shot(Level level, Vec3 spawn, Vec3 inherited, TargetPath target, int lead) {
   }

   static record Plan(float yaw, float pitch, int ticks, double chance) {
      boolean hits() {
         return this.ticks >= 0;
      }
   }
}
