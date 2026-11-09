package vanta.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import vanta.VantaClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ClientPearls {
   public static boolean enabled = false;
   private static final int MATCH_WINDOW = 40;
   private static final double BLEND = 0.7;
   private static final double PEARL_GRAVITY = 0.03;
   private static final float SHOOT_POWER = 1.5F;
   private static final List<Shot> shots = new ArrayList();
   private static final Set<Integer> hidden = new HashSet();
   private static final List<Pending> pending = new ArrayList();
   private static ClientLevel world;
   private static int now;
   private static int nextId = -2000000;
   private static Use use;
   private static double rtt = (double)-1.0F;
   public static int testHandoffDelay;
   public static boolean testTrace;
   public static int framePearls;
   public static int frameCharges;
   public static int framePredictions;
   public static FloatConsumer testFrame;
   private static float sentYRot = Float.NaN;
   private static float sentXRot = Float.NaN;
   private static final double FIT_DISTANCE = (double)1.5F;
   private static final double FIT_COS = Math.cos(Math.toRadians((double)12.0F));
   private static final double PATH_TOLERANCE = 0.3;
   private static final double PATH_COS = Math.cos(Math.toRadians((double)6.0F));

   private ClientPearls() {
   }

   public static void init() {
      ClientTickEvents.END_CLIENT_TICK.register(ClientPearls::tick);
      ClientEntityEvents.ENTITY_LOAD.register((ClientEntityEvents.Load)(entity, level) -> onLoad(entity, level));
      ClientEntityEvents.ENTITY_UNLOAD.register((ClientEntityEvents.Unload)(entity, level) -> onUnload(entity));
      if (System.getenv("FROST_PEARLGRIM") != null) {
         try {
            Method grim = Class.forName("vanta.visual.VisualTestPearls").getDeclaredMethod("grim");
            grim.setAccessible(true);
            grim.invoke((Object)null);
         } catch (ReflectiveOperationException error) {
            System.out.println("[VT] pearlgrim register failed: " + String.valueOf(error));
         }
      }

   }

   public static void setEnabled(boolean on) {
      enabled = on;
      if (!on) {
         clear();
      }

   }

   public static void clear() {
      shots.clear();
      hidden.clear();
      pending.clear();
      use = null;
   }

   public static List<Shot> shots() {
      return List.copyOf(shots);
   }

   public static boolean isPrediction(Entity entity) {
      return entity != null && entity.getId() <= -2000000 && entity.getId() > -3000000;
   }

   public static boolean hidden(Entity entity) {
      return enabled && !hidden.isEmpty() && hidden.contains(entity.getId()) && entity.level() == world;
   }

   public static void usePacket(float yRot, float xRot) {
      if (use != null) {
         sentYRot = yRot;
         sentXRot = xRot;
      }

   }

   public static void beforeUse(Player player, InteractionHand hand) {
      use = null;
      sentYRot = Float.NaN;
      sentXRot = Float.NaN;
      Minecraft mc = Minecraft.getInstance();
      if (enabled && player == mc.player && mc.level != null) {
         ItemStack stack = player.getItemInHand(hand);
         boolean pearl = stack.is(Items.ENDER_PEARL);
         if (pearl || stack.is(Items.WIND_CHARGE)) {
            Vec3 moved = player.position().subtract(player.xo, player.yo, player.zo);
            use = new Use(player, hand, pearl, stack.copyWithCount(1), player.getCooldowns().isOnCooldown(stack), player.getYRot(), player.getXRot(), player.position(), player.getEyeY(), moved, player.onGround());
         }
      }
   }

   public static void afterUse(Player player, InteractionHand hand, InteractionResult result) {
      Use used = use;
      use = null;
      if (used != null && used.player == player && used.hand == hand && !used.cooling && result instanceof InteractionResult.Success) {
         if (!Float.isNaN(sentYRot)) {
            used = new Use(used.player, used.hand, used.pearl, used.stack, used.cooling, Mth.wrapDegrees(sentYRot), Mth.clamp(Mth.wrapDegrees(sentXRot), -90.0F, 90.0F), used.position, used.eyeY, used.moved, used.onGround);
         }

         sentYRot = Float.NaN;
         spawn(used);
      }
   }

   private static void spawn(Use used) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != null) {
         if (world != level) {
            clear();
            world = level;
         }

         float xRot = used.xRot;
         float yRot = used.yRot;
         float xd = -Mth.sin((double)(yRot * ((float)Math.PI / 180F))) * Mth.cos((double)(xRot * ((float)Math.PI / 180F)));
         float yd = -Mth.sin((double)(xRot * ((float)Math.PI / 180F)));
         float zd = Mth.cos((double)(yRot * ((float)Math.PI / 180F))) * Mth.cos((double)(xRot * ((float)Math.PI / 180F)));
         Vec3 vel = (new Vec3((double)xd, (double)yd, (double)zd)).normalize().scale((double)1.5F);
         vel = vel.add(used.moved.x, used.onGround ? (double)0.0F : used.moved.y, used.moved.z);
         Projectile body;
         Vec3 start;
         if (used.pearl) {
            ThrownEnderpearl pearl = new ThrownEnderpearl(EntityTypes.ENDER_PEARL, level);
            pearl.setItem(used.stack);
            body = pearl;
            start = new Vec3(used.position.x, used.eyeY - (double)0.1F, used.position.z);
         } else {
            body = new WindCharge(EntityTypes.WIND_CHARGE, level);
            start = new Vec3(used.position.x, used.eyeY, used.position.z);
         }

         body.setId(nextId--);
         if (nextId < -2900000) {
            nextId = -2000000;
         }

         body.setPos(start);
         body.setOldPosAndRot();
         double sd = vel.horizontalDistance();
         body.setYRot((float)(Mth.atan2(vel.x, vel.z) * (double)180.0F / (double)(float)Math.PI));
         body.setXRot((float)(Mth.atan2(vel.y, sd) * (double)180.0F / (double)(float)Math.PI));
         body.yRotO = body.getYRot();
         body.xRotO = body.getXRot();
         body.setDeltaMovement(vel);
         shots.add(new Shot(used.pearl, body, start, vel, now));
         if (testTrace) {
            StringBuilder by = new StringBuilder();

            for(StackTraceElement frame : Thread.currentThread().getStackTrace()) {
               String c = frame.getClassName();
               if (c.startsWith("frost.") && !c.contains("ClientPearls") && by.length() < 300) {
                  by.append(' ').append(c.substring(c.lastIndexOf(46) + 1)).append('.').append(frame.getMethodName());
               }
            }

            String var10001 = used.pearl ? "pearl" : "charge";
            System.out.println("[VT] clientpearls throw " + var10001 + " by" + String.valueOf(by));
         }

      }
   }

   public static Shot testInject(boolean pearl, Vec3 pos, Vec3 vel, int age) {
      Minecraft mc = Minecraft.getInstance();
      if (world != mc.level) {
         clear();
         world = mc.level;
      }

      Projectile body = (Projectile)(pearl ? new ThrownEnderpearl(EntityTypes.ENDER_PEARL, mc.level) : new WindCharge(EntityTypes.WIND_CHARGE, mc.level));
      body.setId(nextId--);
      body.setPos(pos);
      body.setOldPosAndRot();
      Shot shot = new Shot(pearl, body, pos, vel, now - age);
      shot.age = age;
      body.tickCount = age;
      shots.add(shot);
      return shot;
   }

   private static void onLoad(Entity entity, ClientLevel level) {
      Minecraft mc = Minecraft.getInstance();
      if (enabled && !shots.isEmpty() && mc.player != null && level == world) {
         EntityType<?> type = entity.getType();
         boolean pearl = type == EntityTypes.ENDER_PEARL;
         if (pearl || type == EntityTypes.WIND_CHARGE) {
            if (entity instanceof Projectile) {
               Projectile projectile = (Projectile)entity;
               if (projectile.getOwner() == mc.player) {
                  Shot shot = bestFit(entity, pearl);
                  if (shot == null) {
                     return;
                  }

                  hidden.add(entity.getId());
                  if (testHandoffDelay > 0) {
                     shot.claim = entity;
                     pending.add(new Pending(entity, shot, now + testHandoffDelay));
                     return;
                  }

                  activate(shot, entity, 0);
                  return;
               }
            }

         }
      }
   }

   private static Shot bestFit(Entity real, boolean pearl) {
      Vec3 at = real.position();
      Vec3 v = real.getDeltaMovement();
      Shot best = null;
      double bestDistance = (double)1.5F;

      for(Shot shot : shots) {
         if (shot.pearl == pearl && shot.real == null && shot.claim == null && now - shot.born <= 40) {
            Vec3 p = shot.start;
            Vec3 sv = shot.startVel;

            for(int k = 0; k <= 3; ++k) {
               double distance = p.distanceTo(at);
               if (sameWay(sv, v)) {
                  label57: {
                     if (best == null) {
                        if (!(distance < bestDistance)) {
                           break label57;
                        }
                     } else if (best == shot) {
                        if (!(distance < bestDistance)) {
                           break label57;
                        }
                     } else if (!(distance < bestDistance - (double)0.25F)) {
                        break label57;
                     }

                     best = shot;
                     bestDistance = distance;
                  }
               }

               sv = pearl ? sv.add((double)0.0F, -0.03, (double)0.0F).scale(0.99) : sv;
               p = p.add(sv);
            }
         }
      }

      return best;
   }

   private static boolean sameWay(Vec3 a, Vec3 b) {
      double la = a.length();
      double lb = b.length();
      if (!(la < 0.05) && !(lb < 0.05)) {
         return a.dot(b) / (la * lb) >= FIT_COS;
      } else {
         return true;
      }
   }

   private static void activate(Shot shot, Entity real, int pendingStep) {
      shot.claim = null;
      shot.real = real;
      shot.matchedAt = now;
      shot.lead = Math.max(0, shot.age + pendingStep - real.tickCount);
      rtt = rtt < (double)0.0F ? (double)shot.lead : rtt * 0.7 + (double)shot.lead * 0.3;
      shot.matchIndex = shot.trail.size();
      hidden.add(real.getId());
      shot.accel = accelOf(real);
      shot.realPos = real.position();
      shot.realVel = real.getDeltaMovement();
      if (!shot.ended) {
         Vec3[] ahead = forward(shot, real.position(), real.getDeltaMovement(), shot.accel, Math.max(0, shot.lead - pendingStep));
         shot.offset = ahead == null ? Vec3.ZERO : shot.pos.subtract(ahead[0]);
         if (ahead != null) {
            shot.sim = ahead[0];
            shot.vel = ahead[1];
         }

         if (testTrace) {
            System.out.println(String.format(Locale.ROOT, "[VT] clientpearls hand-off: age %d, real ticks %d, lead %d, gap %.3f", shot.age, real.tickCount, shot.lead, shot.offset.length()));
         }

         if (shot.offset.lengthSqr() > (double)9.0F) {
            shot.offset = Vec3.ZERO;
         }

         shot.matchGap = shot.offset.length();
      }

   }

   private static void onUnload(Entity entity) {
      if (!hidden.isEmpty() || !pending.isEmpty()) {
         int id = entity.getId();
         hidden.remove(id);
         pending.removeIf((p) -> p.real == entity);
         shots.removeIf((shot) -> shot.real == entity || shot.claim == entity);
      }
   }

   private static void tick(Minecraft mc) {
      if (shots.isEmpty() && hidden.isEmpty() && pending.isEmpty()) {
         ++now;
      } else {
         ++now;
         if (enabled && mc.player != null && mc.level != null && mc.level == world) {
            Iterator<Pending> it = pending.iterator();

            while(it.hasNext()) {
               Pending p = (Pending)it.next();
               if (p.real.isRemoved()) {
                  hidden.remove(p.real.getId());
                  shots.remove(p.shot);
                  it.remove();
               } else if (now >= p.due) {
                  it.remove();
                  if (shots.contains(p.shot)) {
                     activate(p.shot, p.real, 1);
                  } else {
                     hidden.remove(p.real.getId());
                  }
               }
            }

            int wait = waitTicks(mc);
            Iterator<Shot> it = shots.iterator();

            while(it.hasNext()) {
               Shot shot = (Shot)it.next();
               if (shot.real != null && shot.real.isRemoved()) {
                  hidden.remove(shot.real.getId());
                  it.remove();
               } else if (shot.real == null) {
                  if (shot.claim == null && shot.age > wait) {
                     it.remove();
                  }
               } else if (shot.ended) {
                  int due = Math.max(shot.endedAt + shot.lead, shot.matchedAt) + slack(shot.lead);
                  if (now > due && !resume(shot) && now > due + 20) {
                     hidden.remove(shot.real.getId());
                     it.remove();
                  }
               }
            }

            for(Shot shot : shots) {
               step(mc, shot);
            }

         } else {
            clear();
            world = mc.level;
         }
      }
   }

   private static int slack(int lead) {
      return 3 + lead / 2;
   }

   private static int waitTicks(Minecraft mc) {
      double trip = rtt >= (double)0.0F ? rtt : (Math.max(rttTicks(mc), 0) > 0 ? (double)rttTicks(mc) : (double)(40 - slack(40)));
      return Mth.clamp((int)Math.ceil(trip) + slack((int)Math.ceil(trip)) + 2, 6, 40);
   }

   private static boolean resume(Shot shot) {
      Vec3[] ahead = forward(shot, shot.real.position(), shot.real.getDeltaMovement(), accelOf(shot.real), shot.lead);
      if (ahead != null && !(shot.real.getDeltaMovement().lengthSqr() < 1.0E-4)) {
         shot.ended = false;
         shot.accel = accelOf(shot.real);
         shot.realPos = shot.real.position();
         shot.realVel = shot.real.getDeltaMovement();
         shot.sim = ahead[0];
         shot.vel = ahead[1];
         Vec3 shift = shot.pos.subtract(ahead[0]);
         shot.offset = shift.lengthSqr() > (double)64.0F ? Vec3.ZERO : shift;
         ++shot.corrections;
         return true;
      } else {
         return false;
      }
   }

   private static int rttTicks(Minecraft mc) {
      if (mc.getConnection() != null && mc.player != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         return info == null ? 0 : Mth.ceil((double)Math.max(0, info.getLatency()) / (double)50.0F);
      } else {
         return 0;
      }
   }

   private static double accelOf(Entity real) {
      double var10000;
      if (real instanceof AbstractHurtingProjectile hurting) {
         var10000 = hurting.accelerationPower;
      } else {
         var10000 = (double)0.0F;
      }

      return var10000;
   }

   private static void step(Minecraft mc, Shot shot) {
      ++shot.age;
      if (shot.ended) {
         shot.body.setOldPosAndRot();
         shot.body.tickCount = shot.age;
      } else {
         Vec3 from = shot.pos;
         HitResult blocked = null;
         shot.offset = shot.offset.scale(0.7);
         if (shot.offset.lengthSqr() < 1.0E-6) {
            shot.offset = Vec3.ZERO;
         }

         Vec3 v = inertia(shot, shot.sim, shot.vel, shot.accel);
         Vec3 next = shot.sim.add(v);
         BlockHitResult hit = clip(shot, shot.sim, next);
         boolean deflecting = shot.deflectedAt >= 0 && now - shot.deflectedAt <= shot.lead + slack(shot.lead);
         if (shot.real != null && !deflecting && !follows(shot)) {
            shot.accel = accelOf(shot.real);
            Vec3[] ahead = forward(shot, shot.real.position(), shot.real.getDeltaMovement(), shot.accel, shot.lead);
            ++shot.corrections;
            if (ahead == null) {
               end(shot);
               return;
            }

            Vec3 shift = next.add(shot.offset).subtract(ahead[0]);
            shot.offset = shift.lengthSqr() > (double)64.0F ? Vec3.ZERO : shift;
            next = ahead[0];
            v = ahead[1];
            hit = null;
         }

         shot.sim = next;
         Vec3 to;
         if (hit != null && hit.getType() != Type.MISS) {
            shot.sim = hit.getLocation();
            blocked = hit;
            to = hit.getLocation();
         } else {
            to = next.add(shot.offset);
         }

         Shot charge = null;
         double nearest = blocked == null ? from.distanceToSqr(to) + 1.0E-7 : from.distanceToSqr(blocked.getLocation());
         boolean entityHit = false;
         float margin = Math.max(0.0F, Math.min(0.3F, (float)(shot.age - 1 - 2) / 20.0F));
         AABB area = shot.body.getBoundingBox().expandTowards(to.subtract(from)).inflate((double)1.0F);
         if (shot.pearl) {
            for(Shot other : shots) {
               if (!other.pearl && !other.ended) {
                  Optional<Vec3> at = other.body.getBoundingBox().inflate((double)margin).clip(from, to);
                  if (at.isPresent() && from.distanceToSqr((Vec3)at.get()) < nearest) {
                     nearest = from.distanceToSqr((Vec3)at.get());
                     charge = other;
                     entityHit = true;
                  }
               }
            }
         }

         for(Entity entity : mc.level.getEntities(shot.body, area, (e) -> canHit(shot, e))) {
            Optional<Vec3> at = entity.getBoundingBox().inflate((double)margin).clip(from, to);
            if (at.isPresent() && from.distanceToSqr((Vec3)at.get()) < nearest) {
               nearest = from.distanceToSqr((Vec3)at.get());
               charge = null;
               entityHit = true;
            }
         }

         shot.vel = v;
         move(shot, !entityHit && blocked == null ? to : from.add(to.subtract(from).normalize().scale(Math.sqrt(nearest))));
         if (entityHit) {
            if (charge != null && charge.age >= 5) {
               charge.deflectedAt = now;
               float yaw = VantaClient.ROTATIONS.sentYaw(mc.player.getYRot()) * ((float)Math.PI / 180F);
               float pitch = VantaClient.ROTATIONS.sentPitch(mc.player.getXRot()) * ((float)Math.PI / 180F);
               charge.vel = new Vec3((double)(-Mth.sin((double)yaw) * Mth.cos((double)pitch)), (double)(-Mth.sin((double)pitch)), (double)(Mth.cos((double)yaw) * Mth.cos((double)pitch)));
               charge.accel = 0.1;
               charge.sim = charge.pos.subtract(charge.offset);
            }

            end(shot);
         } else if (blocked != null) {
            end(shot);
         }

      }
   }

   private static boolean follows(Shot shot) {
      Entity real = shot.real;
      Vec3 at = real.position();
      Vec3 realVel = real.getDeltaMovement();
      if (Math.abs(accelOf(real) - shot.accel) > 1.0E-6) {
         shot.realPos = at;
         shot.realVel = realVel;
         return false;
      } else {
         Vec3 v = inertia(shot, shot.realPos, shot.realVel, shot.accel);
         Vec3 p = shot.realPos.add(v);
         Vec3 back1 = p.subtract(v);
         Vec3 back2 = back1.subtract(shot.realVel);
         Vec3 v1 = inertia(shot, p, v, shot.accel);
         Vec3 on1 = p.add(v1);
         Vec3 on2 = on1.add(inertia(shot, on1, v1, shot.accel));
         double off = Math.min(Math.min(segment(at, back2, back1), segment(at, back1, p)), Math.min(segment(at, p, on1), segment(at, on1, on2)));
         double lv = v.length();
         double lr = realVel.length();
         boolean sameWay = lv < 0.05 && lr < 0.05 || lv >= 0.05 && lr >= 0.05 && v.dot(realVel) / (lv * lr) >= PATH_COS && Math.abs(lr - lv) <= 0.15 * lv + 0.02;
         if (off <= 0.3 && sameWay) {
            shot.realPos = p;
            shot.realVel = v;
            return true;
         } else {
            shot.realPos = at;
            shot.realVel = realVel;
            return false;
         }
      }
   }

   private static double segment(Vec3 point, Vec3 a, Vec3 b) {
      Vec3 ab = b.subtract(a);
      double len = ab.lengthSqr();
      double t = len < 1.0E-9 ? (double)0.0F : Mth.clamp(point.subtract(a).dot(ab) / len, (double)0.0F, (double)1.0F);
      return point.distanceTo(a.add(ab.scale(t)));
   }

   private static boolean canHit(Shot shot, Entity entity) {
      Minecraft mc = Minecraft.getInstance();
      if (entity != mc.player && !hidden(entity) && entity.canBeHitByProjectile()) {
         if (shot.pearl) {
            return true;
         } else {
            return !(entity instanceof AbstractWindCharge) && entity.getType() != EntityTypes.END_CRYSTAL && !entity.noPhysics;
         }
      } else {
         return false;
      }
   }

   private static void end(Shot shot) {
      shot.ended = true;
      shot.endedAt = now;
   }

   private static void move(Shot shot, Vec3 to) {
      Projectile body = shot.body;
      body.setOldPosAndRot();
      shot.pos = to;
      body.setPos(to);
      body.setDeltaMovement(shot.vel);
      if (shot.pearl) {
         double sd = shot.vel.horizontalDistance();
         body.setXRot((float)(Mth.atan2(shot.vel.y, sd) * (double)180.0F / (double)(float)Math.PI));
         body.setYRot((float)(Mth.atan2(shot.vel.x, shot.vel.z) * (double)180.0F / (double)(float)Math.PI));
      }

      body.tickCount = shot.age;
      if (testTrace) {
         shot.trail.add(to);
         Entity real = shot.real != null ? shot.real : shot.claim;
         shot.realTrail.add(real == null ? null : real.position());
      }

   }

   private static Vec3 inertia(Shot shot, Vec3 pos, Vec3 vel, double accel) {
      boolean water = world != null && world.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
      if (shot.pearl) {
         Vec3 v = vel.add((double)0.0F, -0.03, (double)0.0F);
         return v.scale(water ? 0.8 : 0.99);
      } else {
         return vel.add(vel.normalize().scale(accel));
      }
   }

   private static Vec3[] forward(Shot shot, Vec3 pos, Vec3 vel, double accel, int ticks) {
      for(int i = 0; i < ticks; ++i) {
         Vec3 v = inertia(shot, pos, vel, accel);
         Vec3 next = pos.add(v);
         if (clip(shot, pos, next).getType() != Type.MISS) {
            return null;
         }

         pos = next;
         vel = v;
      }

      return new Vec3[]{pos, vel};
   }

   private static BlockHitResult clip(Shot shot, Vec3 from, Vec3 to) {
      return world.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, shot.body));
   }

   public static void extract(List<EntityRenderState> out, Frustum frustum, Vec3 camera, float partialTick, Visibility visibility) {
      if (testFrame != null) {
         testFrame.accept(partialTick);
      }

      framePredictions = 0;
      Minecraft mc = Minecraft.getInstance();
      if (enabled && !shots.isEmpty() && mc.level == world) {
         for(Shot shot : shots) {
            if (visibility.visible(shot.body, frustum, camera.x, camera.y, camera.z)) {
               out.add(mc.getEntityRenderDispatcher().extractEntity(shot.body, partialTick));
               ++framePredictions;
            }
         }
      }

      if (testTrace) {
         int pearls = 0;
         int charges = 0;

         for(EntityRenderState state : out) {
            if (state.entityType == EntityTypes.ENDER_PEARL) {
               ++pearls;
            } else if (state.entityType == EntityTypes.WIND_CHARGE) {
               ++charges;
            }
         }

         framePearls = pearls;
         frameCharges = charges;
      }
   }

   private static record Use(Player player, InteractionHand hand, boolean pearl, ItemStack stack, boolean cooling, float yRot, float xRot, Vec3 position, double eyeY, Vec3 moved, boolean onGround) {
   }

   private static record Pending(Entity real, Shot shot, int due) {
   }

   public static final class Shot {
      final boolean pearl;
      final Projectile body;
      final int born;
      final Vec3 start;
      final Vec3 startVel;
      Vec3 pos;
      Vec3 vel;
      double accel;
      int age;
      Entity real;
      Entity claim;
      int lead;
      int matchedAt;
      double matchGap = (double)-1.0F;
      final List<Vec3> trail = new ArrayList();
      final List<Vec3> realTrail = new ArrayList();
      int matchIndex = -1;
      Vec3 offset;
      Vec3 sim;
      Vec3 realPos;
      Vec3 realVel;
      int corrections;
      boolean ended;
      int endedAt;
      int deflectedAt;

      Shot(boolean pearl, Projectile body, Vec3 pos, Vec3 vel, int born) {
         this.offset = Vec3.ZERO;
         this.deflectedAt = -1;
         this.pearl = pearl;
         this.body = body;
         this.pos = pos;
         this.vel = vel;
         this.start = pos;
         this.startVel = vel;
         this.born = born;
         this.sim = pos;
         this.trail.add(pos);
         this.realTrail.add((Object)null);
      }

      public boolean pearl() {
         return this.pearl;
      }

      public boolean shown() {
         return !this.ended;
      }

      public boolean drawn() {
         return true;
      }

      public boolean matched() {
         return this.real != null;
      }

      public Entity real() {
         return this.real;
      }

      public double matchGap() {
         return this.matchGap;
      }

      public List<Vec3> trail() {
         return this.trail;
      }

      public List<Vec3> realTrail() {
         return this.realTrail;
      }

      public int matchIndex() {
         return this.matchIndex;
      }

      public int lead() {
         return this.lead;
      }

      public Vec3 position() {
         return this.pos;
      }

      public Entity body() {
         return this.body;
      }

      public int corrections() {
         return this.corrections;
      }
   }

   public interface FloatConsumer {
      void accept(float var1);
   }

   public interface Visibility {
      boolean visible(Entity var1, Frustum var2, double var3, double var5, double var7);
   }
}
