package vanta.module.impl.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import vanta.VantaClient;
import vanta.event.events.PreTickEvent;
import vanta.event.events.TickEvent;
import vanta.mixin.accessor.MinecraftAccessor;
import vanta.module.Module;
import vanta.rotation.Rotation;
import vanta.rotation.RotationConfig;
import vanta.rotation.RotationTarget;
import vanta.rotation.Smoothing;
import vanta.setting.BoolSetting;
import vanta.setting.Measure;
import vanta.setting.RangeSetting;
import vanta.setting.SliderSetting;
import vanta.setting.TargetSettings;
import vanta.util.Clicks;
import vanta.util.Hotbar;
import vanta.util.Humanize;
import vanta.util.Maths;
import vanta.util.SwapBack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PearlGrapple extends Module {
   private static final int AIM_PRIORITY = 7;
   private static final int MAX_WAIT = 12;
   private static final double FIRST_CHANCE = (double)0.75F;
   private static final double CHANCE_STEP = 0.05;
   private static final double MIN_CHANCE = 0.4;
   private static final double TRACK_MARGIN = (double)16.0F;
   private static final boolean TRACE = System.getenv("FROST_GRAPPLETRACE") != null;
   private final SliderSetting range;
   private final SliderSetting fov;
   private final BoolSetting airOnly;
   private final SliderSetting smoothness;
   private final RangeSetting swapBack;
   private final RangeSetting reaction;
   private int readyAt;
   private final TargetSettings targets;
   private final SwapBack swap;
   private LivingEntity target;
   private int pressTick;
   private LocalPlayer presser;
   private Vec3 lastPosition;
   private Vec3 previousPosition;
   private boolean lastOnGround;
   private final Map<Integer, Track> tracks;

   public PearlGrapple() {
      super("pearlGrapple");
      this.range = (SliderSetting)this.add(new SliderSetting("range", 24, 6, 40, Measure.DISTANCE));
      this.fov = (SliderSetting)this.add(new SliderSetting("fov", 60, 10, 360, Measure.DEGREES));
      this.airOnly = (BoolSetting)this.add(new BoolSetting("airOnly", false));
      this.smoothness = (SliderSetting)this.add((new SliderSetting("smoothness", 0, 0, 40, Measure.PERCENT)).percentOfRange());
      this.swapBack = (RangeSetting)this.add(new RangeSetting("swapBack", 1, 3, 1, 10, Measure.TICKS));
      this.reaction = (RangeSetting)this.add(new RangeSetting("reaction", 0, 0, 0, 10, Measure.TICKS));
      this.targets = (TargetSettings)this.add(new TargetSettings());
      this.swap = new SwapBack(5);
      this.pressTick = -1;
      this.tracks = new HashMap();
      this.range.describe();
      this.fov.describe();
      this.airOnly.describe();
      this.swapBack.describe();
      this.reaction.describe();
   }

   public boolean activatable() {
      return true;
   }

   protected void onActivate() {
      LocalPlayer player = mc.player;
      if (player != null && this.target == null) {
         if (Hotbar.find(player, (stack) -> stack.is(Items.ENDER_PEARL)) >= 0) {
            LivingEntity found = this.pickTarget(player);
            if (TRACE) {
               StringBuilder others = new StringBuilder();

               for(Player other : player.level().players()) {
                  if (other != player) {
                     others.append(String.format(Locale.ROOT, " %s at %.1f %.1f %.1f (%.1f away)", other.getName().getString(), other.getX(), other.getY(), other.getZ(), player.distanceTo(other)));
                  }
               }

               String var10001 = found == null ? "none" : found.getName().getString();
               System.out.println("[GRAPPLE] key: target " + var10001 + ", you at " + String.valueOf(player.position()) + ", players:" + String.valueOf(others));
            }

            if (found != null) {
               this.target = found;
               this.pressTick = player.tickCount;
               this.readyAt = player.tickCount + Humanize.ticks(this.reaction);
               this.presser = player;
               this.throwIfHits(mc, player);
            }
         }
      }
   }

   protected void onEnable() {
      this.listen(PreTickEvent.class, this::onTick);
      this.listen(TickEvent.class, this::onTickEnd);
   }

   protected void onDisable() {
      this.swap.cancel(mc.player);
      this.tracks.clear();
      this.stop();
   }

   private void stop() {
      this.target = null;
      this.pressTick = -1;
   }

   private void onTick(PreTickEvent event) {
      Minecraft client = event.client();
      LocalPlayer player = client.player;
      this.swap.tick(player);
      if (player == null) {
         this.stop();
      } else if (this.target != null) {
         if (client.gui.screen() == null && !player.isUsingItem() && !player.isHandsBusy() && this.target.isAlive() && !this.target.isRemoved() && player == this.presser) {
            if (player.tickCount - this.readyAt > 12) {
               this.stop();
            } else {
               this.throwIfHits(client, player);
            }
         } else {
            this.stop();
         }
      }
   }

   private void throwIfHits(Minecraft client, LocalPlayer player) {
      if (client.gui.screen() == null && !player.isUsingItem() && !player.isHandsBusy()) {
         if (player.tickCount >= this.readyAt) {
            int slot = Hotbar.find(player, (stack) -> stack.is(Items.ENDER_PEARL));
            if (slot < 0) {
               this.stop();
            } else if (!player.getCooldowns().isOnCooldown(player.getInventory().getItem(slot)) && !Hotbar.actedThisTick()) {
               Rotation sent = sent(player);
               GrappleSolver.Plan plan = GrappleSolver.plan(this.shot(player), sent.yaw(), sent.pitch());
               double needed = Math.max(0.4, (double)0.75F - 0.05 * (double)(player.tickCount - this.readyAt));
               if (plan.hits() && !(plan.chance() < needed)) {
                  if (this.pick(player, slot)) {
                     if (!this.use(client, player, slot)) {
                        this.stop();
                     } else {
                        if (TRACE) {
                           System.out.println(String.format(Locale.ROOT, "[GRAPPLE] thrown at tick=%d along %.2f %.2f: hit in %d ticks, chance %.2f", player.tickCount, sent.yaw(), sent.pitch(), plan.ticks(), plan.chance()));
                        }

                        this.stop();
                     }
                  }
               }
            }
         }
      }
   }

   private void onTickEnd(TickEvent event) {
      LocalPlayer player = event.client().player;
      if (player != null) {
         this.previousPosition = this.lastPosition;
         this.lastPosition = player.position();
         this.lastOnGround = player.onGround();
         this.track(player);
         if (this.target != null && player == this.presser && event.client().gui.screen() == null) {
            GrappleSolver.Plan best = GrappleSolver.best(this.shot(player), player.getYRot());
            if (best != null) {
               Rotation aim = new Rotation(best.yaw(), best.pitch());
               VantaClient.ROTATIONS.aim((RotationTarget)((partialTick) -> aim), 7, RotationConfig.silent((float)(Integer)this.smoothness.get() / 100.0F, Smoothing.EASE_OUT_CUBIC).fixingMovement());
            }
         }
      }
   }

   private void track(LocalPlayer player) {
      int now = player.tickCount - 1;
      Set<Integer> seen = new HashSet();

      for(LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate((double)(Integer)this.range.get() + (double)16.0F))) {
         if (entity != player) {
            seen.add(entity.getId());
            InterpolationHandler interpolation = entity.getInterpolation();
            Vec3 position = interpolation != null ? interpolation.position() : entity.position();
            Track last = (Track)this.tracks.get(entity.getId());
            if (last == null) {
               this.tracks.put(entity.getId(), new Track(position, now, entity.position().subtract(entity.xo, entity.yo, entity.zo)));
            } else if (position.distanceToSqr(last.position()) > 1.0E-8) {
               int ticks = Math.max(1, now - last.tick());
               int interval = Math.max(1, entity.getType().updateInterval());
               if (last.velocity().lengthSqr() < 1.0E-6 && ticks > interval) {
                  ticks = interval;
               }

               Vec3 pace = position.subtract(last.position()).scale((double)1.0F / (double)ticks);
               if (!entity.onGround()) {
                  pace = pace.add((double)0.0F, -0.08 * (double)(ticks - 1) * (double)0.5F, (double)0.0F);
               }

               this.tracks.put(entity.getId(), new Track(position, now, pace));
            } else if (now - last.tick() > 20) {
               this.tracks.put(entity.getId(), new Track(position, now - 20, Vec3.ZERO));
            }
         }
      }

      this.tracks.keySet().retainAll(seen);
   }

   private GrappleSolver.Shot shot(LocalPlayer player) {
      PlayerInfo info = player.connection.getPlayerInfo(player.getUUID());
      int ping = info == null ? 0 : info.getLatency();
      Track track = (Track)this.tracks.get(this.target.getId());
      Vec3 position = track != null ? track.position() : this.target.position();
      Vec3 pace = track != null ? track.velocity() : this.target.position().subtract(this.target.xo, this.target.yo, this.target.zo);
      int age = track != null ? Math.max(0, player.tickCount - track.tick()) : 2;
      int lead = Math.round((float)ping / 50.0F) + age + (this.target instanceof Player ? 1 : 2);
      Vec3 spawn = player.position().add((double)0.0F, (double)(player.getEyeHeight() - 0.1F), (double)0.0F);
      AABB box = this.target.getBoundingBox().move(position.subtract(this.target.position()));
      boolean air = player.level().noCollision(box.move((double)0.0F, -0.1, (double)0.0F)) || Math.abs(pace.y) > 0.08;
      GrappleSolver.TargetPath path = GrappleSolver.TargetPath.of(player.level(), box, pace, air, 60 + lead + 2);
      if (TRACE) {
         System.out.println(String.format(Locale.ROOT, "[GRAPPLE] tick=%d lead=%d age=%d air=%b(onGround %b) server=%.2f %.2f %.2f pace=%.3f %.3f %.3f shown=%.2f", player.tickCount, lead, age, air, this.target.onGround(), position.x, position.y, position.z, pace.x, pace.y, pace.z, this.target.getY()));
      }

      return new GrappleSolver.Shot(player.level(), spawn, this.inherited(), path, lead);
   }

   private LivingEntity pickTarget(LocalPlayer player) {
      double range = (double)(Integer)this.range.get();
      LivingEntity best = null;
      double bestAngle = (double)(Integer)this.fov.get() * (double)0.5F;

      for(LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))) {
         if (TRACE && entity != player) {
            System.out.println(String.format(Locale.ROOT, "[GRAPPLE] candidate %s accepts=%b distance=%.1f angle=%.1f ground=%b", entity.getName().getString(), this.targets.accepts(player, entity), player.distanceTo(entity), Maths.angle(player.getLookAngle(), entity.getBoundingBox().getCenter().subtract(player.getEyePosition())), entity.onGround()));
         }

         if (this.targets.accepts(player, entity) && !((double)player.distanceTo(entity) > range) && (!(Boolean)this.airOnly.get() || !entity.onGround())) {
            double angle = Maths.angle(player.getLookAngle(), entity.getBoundingBox().getCenter().subtract(player.getEyePosition()));
            if (angle < bestAngle) {
               best = entity;
               bestAngle = angle;
            }
         }
      }

      return best;
   }

   private Vec3 inherited() {
      if (this.lastPosition != null && this.previousPosition != null) {
         Vec3 step = this.lastPosition.subtract(this.previousPosition);
         return step.lengthSqr() <= 4.0E-8 ? Vec3.ZERO : new Vec3(step.x, this.lastOnGround ? (double)0.0F : step.y, step.z);
      } else {
         return Vec3.ZERO;
      }
   }

   private static Rotation sent(LocalPlayer player) {
      return new Rotation(VantaClient.ROTATIONS.sentYaw(player.getYRot()), VantaClient.ROTATIONS.sentPitch(player.getXRot()));
   }

   private boolean pick(LocalPlayer player, int slot) {
      return slot == Hotbar.selected(player) || this.swap.select(player, slot, SwapBack.ticks(this.swapBack.low(), this.swapBack.high()));
   }

   private boolean use(Minecraft client, LocalPlayer player, int slot) {
      ItemStack stack = player.getInventory().getItem(slot);
      int count = stack.getCount();
      ((MinecraftAccessor)client).client$pick(1.0F);
      Clicks.click(1);
      return player.getCooldowns().isOnCooldown(stack) || stack.isEmpty() || stack.getCount() < count;
   }

   private static record Track(Vec3 position, int tick, Vec3 velocity) {
   }
}
