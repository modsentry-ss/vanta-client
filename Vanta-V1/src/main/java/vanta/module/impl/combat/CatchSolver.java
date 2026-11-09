package vanta.module.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import vanta.rotation.Rotation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

final class CatchSolver {
   static final float SPEED = 1.5F;
   static final double SPREAD = 0.0172275;
   static final int HORIZON = 100;
   private static final double GRAVITY = 0.03;
   private static final float DRAG = 0.99F;
   private static final float DEG = ((float)Math.PI / 180F);
   private static final Vec3[] SPREADS = new Vec3[32];
   private static final int QUICK = 8;
   private static final double SERVER_MOVE = 0.01;
   private static final double LAG_BLUR = (double)10.0F;
   private static final int NO_DEFLECT_TICKS = 5;
   private static final double WIND_REACH;
   private static final double QUICK_LAG = 0.1;
   private static final int SCORED = 6;
   private static final double[] SHIFTS;
   private static final int MAX_WIND_TICKS = 50;
   private static final float STEEP = 80.0F;
   private static final float SPIN_PITCH = 60.0F;
   private static final float SPIN_FREE = 30.0F;
   static final double ALIKE = 0.04;

   private CatchSolver() {
   }

   static boolean landed(int tick, int landTick) {
      return tick > landTick;
   }

   static boolean serverMoved(Vec3 tickEnd, Vec3 now) {
      return tickEnd != null && now.distanceToSqr(tickEnd) > 1.0E-4;
   }

   static boolean tookYou(int tick, int movedTick) {
      return tick >= movedTick && (long)tick - (long)movedTick <= 1L;
   }

   static double[] lags(double driftMs) {
      double[] weights = new double[3];

      for(double x : new double[]{driftMs - (double)10.0F, driftMs, driftMs + (double)10.0F}) {
         double ticks = x / (double)50.0F;
         int whole = (int)Math.floor(ticks);
         double part = ticks - (double)whole;
         if (whole >= -1 && whole <= 1) {
            weights[whole + 1] += ((double)1.0F - part) / (double)3.0F;
         }

         if (whole + 1 >= -1 && whole + 1 <= 1) {
            weights[whole + 2] += part / (double)3.0F;
         }
      }

      double sum = weights[0] + weights[1] + weights[2];
      if (sum > (double)0.0F) {
         for(int i = 0; i < 3; ++i) {
            weights[i] /= sum;
         }
      }

      return weights;
   }

   static Vec3 windVelocity(float yaw, float pitch, Vec3 spread, Vec3 inherited) {
      float yRot = Mth.wrapDegrees(yaw) % 360.0F;
      float xRot = Mth.clamp(Mth.wrapDegrees(pitch), -90.0F, 90.0F) % 360.0F;
      float xd = -Mth.sin((double)(yRot * ((float)Math.PI / 180F))) * Mth.cos((double)(xRot * ((float)Math.PI / 180F)));
      float yd = -Mth.sin((double)(xRot * ((float)Math.PI / 180F)));
      float zd = Mth.cos((double)(yRot * ((float)Math.PI / 180F))) * Mth.cos((double)(xRot * ((float)Math.PI / 180F)));
      return (new Vec3((double)xd, (double)yd, (double)zd)).normalize().add(spread).scale((double)1.5F).add(inherited);
   }

   static Vec3 pearlVelocity(float yaw, float pitch, Vec3 inherited) {
      return windVelocity(yaw, pitch, Vec3.ZERO, inherited);
   }

   static AABB windBox(Vec3 position) {
      EntityDimensions size = EntityTypes.WIND_CHARGE.getDimensions();
      double half = (double)(size.width() / 2.0F);
      double bottom = position.y - (double)0.15F;
      return new AABB(position.x - half, bottom, position.z - half, position.x + half, bottom + (double)size.height(), position.z + half);
   }

   static AABB pearlBox(Vec3 position) {
      EntityDimensions size = EntityTypes.ENDER_PEARL.getDimensions();
      double half = (double)(size.width() / 2.0F);
      return new AABB(position.x - half, position.y, position.z - half, position.x + half, position.y + (double)size.height(), position.z + half);
   }

   private static float margin(int tickCount) {
      return Math.max(0.0F, Math.min(0.3F, (float)(tickCount - 2) / 20.0F));
   }

   private static boolean near(double cx, double cy, double cz, double radius, Vec3 from, double length) {
      double dx = from.x - cx;
      double dy = from.y - cy;
      double dz = from.z - cz;
      double reach = length + radius;
      return dx * dx + dy * dy + dz * dz <= reach * reach;
   }

   private static BlockHitResult clip(Level level, Vec3 from, Vec3 to) {
      return level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, CollisionContext.empty()));
   }

   static Outcome outcome(Throw t, int age, Vec3 velocity, boolean blocks) {
      PearlPath pearl = t.pearl();
      if (age < 0) {
         return CatchSolver.Outcome.NONE;
      } else {
         boolean left = false;
         Vec3 w = t.spawn();
         AABB yours = t.yourBox();
         Vec3 yourCentre = yours.getCenter();
         double yourReach = (double)0.5F * Math.sqrt(yours.getXsize() * yours.getXsize() + yours.getYsize() * yours.getYsize() + yours.getZsize() * yours.getZsize()) + Math.sqrt((double)3.0F) * 0.3 + 0.01;
         double windRise = (double)0.006249994F;

         for(int k = 0; age + k < pearl.ticks(); ++k) {
            int j = age + k;
            Vec3 from = pearl.start()[j];
            Vec3 step = pearl.move()[j];
            double length = step.length();
            Vec3 to = from.add(step);
            if (left && near(yourCentre.x, yourCentre.y, yourCentre.z, yourReach, from, length) && yours.inflate((double)margin(j + 1)).clip(from, to).isPresent()) {
               return CatchSolver.Outcome.NONE;
            }

            if (near(w.x, w.y + windRise, w.z, WIND_REACH, from, length) && windBox(w).inflate((double)margin(j + 1)).clip(from, to).isPresent()) {
               if (blocks && k > 0 && clip(t.level(), t.spawn(), w).getType() != Type.MISS) {
                  return CatchSolver.Outcome.NONE;
               }

               AABB at = t.you().makeBoundingBox(from).inflate((double)margin(k + 1));
               Vec3 end = w.add(k < 5 ? velocity : lookAt(t, velocity, k).scale(1.1));
               if (blocks) {
                  BlockHitResult block = clip(t.level(), w, end);
                  if (block.getType() != Type.MISS) {
                     end = block.getLocation();
                  }
               }

               boolean burst = !at.contains(w) && at.clip(w, end).isPresent();
               return new Outcome(k, from, burst);
            }

            w = w.add(velocity);
            left = left || !yours.intersects(pearlBox(to).expandTowards(step).inflate((double)1.0F));
         }

         return CatchSolver.Outcome.NONE;
      }
   }

   private static Vec3 lookAt(Throw t, Vec3 velocity, int k) {
      if (t.handBack() <= (double)0.0F) {
         return t.look();
      } else {
         Vec3 aim = velocity.normalize();
         double left = Math.pow(t.handBack(), (double)Math.max(0, k - 1));
         Vec3 look = t.look().add(aim.subtract(t.look()).scale(left));
         return look.lengthSqr() < 1.0E-8 ? aim : look.normalize();
      }
   }

   static Plan plan(Throw t, float yaw, float pitch, boolean burst) {
      Plan plan = plan(t, yaw, pitch, SPREADS.length, burst);
      return !plan.outcome().caught() && outcome(t, t.age(), windVelocity(yaw, pitch, Vec3.ZERO, t.inherited()), false).caught() ? new Plan(yaw, pitch, plan.outcome(), (double)0.0F) : plan;
   }

   private static Plan plan(Throw t, float yaw, float pitch, int samples, boolean burst) {
      Vec3 velocity = windVelocity(yaw, pitch, Vec3.ZERO, t.inherited());
      Outcome nominal = outcome(t, t.age(), velocity, true);
      boolean quick = samples < SPREADS.length;
      double chance = (double)0.0F;
      double weighed = (double)0.0F;

      for(int lag = -1; lag <= 1; ++lag) {
         if (!(t.lags()[lag + 1] <= (double)0.0F) && (!quick || !(t.lags()[lag + 1] < 0.1))) {
            weighed += t.lags()[lag + 1];
            int hits = 0;

            for(int i = 0; i < samples; ++i) {
               Outcome outcome = lag == 0 && i == 0 ? nominal : outcome(t, t.age() + lag, windVelocity(yaw, pitch, SPREADS[i], t.inherited()), false);
               if (outcome.caught() && (!burst || outcome.burst())) {
                  ++hits;
               }
            }

            chance += t.lags()[lag + 1] * (double)hits / (double)samples;
         }
      }

      return new Plan(yaw, pitch, nominal, quick && weighed > (double)0.0F ? chance / weighed : chance);
   }

   static Plan best(Throw t, Rotation from, boolean burstOnly) {
      return best(t, from, burstOnly, false);
   }

   static Plan best(Throw t, Rotation from, boolean burstOnly, boolean rough) {
      float fromYaw = from.yaw();
      double centre = windBox(Vec3.ZERO).getCenter().y;
      List<Plan> catching = new ArrayList();

      for(int k = 1; k <= 50 && t.age() + k < t.pearl().ticks(); ++k) {
         int j = t.age() + k;
         Vec3 b = t.pearl().move()[j];
         Vec3 along = b.lengthSqr() > 0.0025 ? b.normalize() : Vec3.ZERO;
         double reach = (double)(1.5F * (float)k);
         List<Vec3> targets = new ArrayList(6);

         for(double shift : SHIFTS) {
            if (shift == (double)0.0F || along != Vec3.ZERO) {
               Vec3 a = t.pearl().start()[j].subtract(t.spawn()).subtract(t.inherited().scale((double)k)).subtract((double)0.0F, centre, (double)0.0F).subtract(along.scale(shift));
               double qa = b.lengthSqr();
               double qb = (double)2.0F * a.dot(b);
               double qc = a.lengthSqr() - reach * reach;
               double disc = qb * qb - (double)4.0F * qa * qc;
               boolean found = false;
               if (qa > 1.0E-9 && disc >= (double)0.0F) {
                  double root = Math.sqrt(disc);

                  for(double s : new double[]{(-qb - root) / ((double)2.0F * qa), (-qb + root) / ((double)2.0F * qa)}) {
                     if (s >= (double)0.0F && s <= (double)1.0F) {
                        targets.add(a.add(b.scale(s)));
                        found = true;
                     }
                  }
               }

               if (!found && shift == (double)0.0F) {
                  double near = Math.abs(a.length() - reach);
                  double far = Math.abs(a.add(b).length() - reach);
                  if (Math.min(near, far) <= (double)1.0F) {
                     targets.add(near <= far ? a : a.add(b));
                  }
               }
            }
         }

         for(Vec3 target : targets) {
            Vec3 d = target.normalize();
            float yaw = fromYaw + Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(-d.x, d.z)) - fromYaw);
            float pitch = (float)(-Math.toDegrees(Math.asin(Mth.clamp(d.y, (double)-1.0F, (double)1.0F))));
            float[][] rotations = Math.abs(pitch) > 80.0F ? new float[][]{{yaw, pitch}, {fromYaw, pitch}, {fromYaw, Math.copySign(90.0F, pitch)}} : new float[][]{{yaw, pitch}};

            for(float[] rotation : rotations) {
               Outcome outcome = outcome(t, t.age(), windVelocity(rotation[0], rotation[1], Vec3.ZERO, t.inherited()), false);
               if (outcome.caught() && (!burstOnly || outcome.burst())) {
                  catching.add(new Plan(rotation[0], rotation[1], outcome, (double)0.0F));
               }
            }
         }
      }

      List<Plan> quick = new ArrayList(catching.size());

      for(Plan candidate : catching) {
         quick.add(plan(t, candidate.yaw(), candidate.pitch(), 8, burstOnly));
      }

      quick.sort(Comparator.comparingDouble((planx) -> planx.chance() - spinCost(planx, from, t.spin())).reversed().thenComparingInt((planx) -> planx.outcome().windTicks()));
      if (rough) {
         for(Plan plan : quick) {
            if (plan.outcome().caught()) {
               return plan;
            }
         }

         return null;
      } else {
         Plan best = null;

         for(int i = 0; i < Math.min(6, quick.size()); ++i) {
            Plan plan = plan(t, ((Plan)quick.get(i)).yaw(), ((Plan)quick.get(i)).pitch(), burstOnly);
            if (plan.outcome().caught() && (best == null || better(plan, best, from, t.spin()))) {
               best = plan;
            }
         }

         return best;
      }
   }

   static boolean better(Plan a, Plan b, Rotation from, double spin) {
      double scoreA = a.chance() - spinCost(a, from, spin);
      double scoreB = b.chance() - spinCost(b, from, spin);
      if (Math.abs(scoreA - scoreB) > 0.04) {
         return scoreA > scoreB;
      } else {
         float turnA = Math.abs(Mth.wrapDegrees(a.yaw() - from.yaw()));
         float turnB = Math.abs(Mth.wrapDegrees(b.yaw() - from.yaw()));
         if (Math.abs(turnA - turnB) > 20.0F) {
            return turnA < turnB;
         } else {
            return a.outcome().windTicks() < b.outcome().windTicks();
         }
      }
   }

   static double spinCost(Plan plan, Rotation from, double spin) {
      if (Math.min(Math.abs(plan.pitch()), Math.abs(from.pitch())) <= 60.0F) {
         return (double)0.0F;
      } else {
         float turn = Math.abs(Mth.wrapDegrees(plan.yaw() - from.yaw()));
         return spin * (double)Mth.clamp((turn - 30.0F) / 150.0F, 0.0F, 1.0F);
      }
   }

   static {
      Random random = new Random(24301L);
      SPREADS[0] = Vec3.ZERO;

      for(int i = 1; i < SPREADS.length; ++i) {
         SPREADS[i] = new Vec3(0.0172275 * (random.nextDouble() - random.nextDouble()), 0.0172275 * (random.nextDouble() - random.nextDouble()), 0.0172275 * (random.nextDouble() - random.nextDouble()));
      }

      WIND_REACH = Math.sqrt((double)3.0F) * 0.45625 + 0.01;
      SHIFTS = new double[]{(double)0.0F, -0.4, 0.4};
   }

   static record PearlPath(Vec3[] start, Vec3[] move, int ticks) {
      static PearlPath of(Level level, Vec3 spawn, Vec3 velocity) {
         Vec3[] start = new Vec3[100];
         Vec3[] move = new Vec3[100];
         Vec3 position = spawn;
         Vec3 motion = velocity;

         for(int j = 0; j < 100; ++j) {
            motion = motion.add((double)0.0F, -0.03, (double)0.0F).scale((double)0.99F);
            start[j] = position;
            Vec3 to = position.add(motion);
            BlockHitResult block = CatchSolver.clip(level, position, to);
            if (block.getType() != Type.MISS) {
               move[j] = block.getLocation().subtract(position);
               return new PearlPath(start, move, j + 1);
            }

            move[j] = motion;
            position = to;
         }

         return new PearlPath(start, move, 100);
      }
   }

   static record Outcome(int windTicks, Vec3 teleport, boolean burst) {
      static final Outcome NONE = new Outcome(-1, (Vec3)null, false);

      boolean caught() {
         return this.windTicks >= 0;
      }
   }

   static record Plan(float yaw, float pitch, Outcome outcome, double chance) {
   }

   static record Throw(Level level, PearlPath pearl, int age, Vec3 spawn, Vec3 inherited, EntityDimensions you, AABB yourBox, double[] lags, Vec3 look, double handBack, double spin) {
   }
}
