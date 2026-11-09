package vanta.module.impl.combat;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;
import vanta.VantaClient;
import vanta.event.events.PreTickEvent;
import vanta.event.events.TickEvent;
import vanta.mixin.accessor.MinecraftAccessor;
import vanta.module.Module;
import vanta.rotation.Rotation;
import vanta.rotation.RotationConfig;
import vanta.rotation.RotationManager;
import vanta.rotation.RotationTarget;
import vanta.rotation.SmoothTracker;
import vanta.rotation.Smoothing;
import vanta.setting.BoolSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.RangeSetting;
import vanta.setting.SliderSetting;
import vanta.util.ChatUtil;
import vanta.util.Clicks;
import vanta.util.Hotbar;
import vanta.util.Humanize;
import vanta.util.SwapBack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PearlCatch extends Module {
   private static final int AIM_PRIORITY = 7;
   private static final int AIM_LEAD = 3;
   private static final int LEGIT_SILENT_AIM_LEAD = 5;
   private static final int LEGIT_AIM_LEAD = 40;
   private static final int MAX_EXTRA = 20;
   private static final int MAX_EXTRA_LAUNCH = 60;
   private static final double MIN_CHANCE = (double)0.5F;
   private static final double FIRST_CHANCE = 0.9;
   private static final double CHANCE_STEP = 0.2;
   private static final double LAST_CHANCE = 0.05;
   private static final double KEEP_CHANCE = 0.8;
   private static final double NEAR_BEST = 0.1;
   private static final int LATER = 2;
   private static final int LOOK_AHEAD = 12;
   private static final double WAIT_GAIN = 0.05;
   private static final int FULLY_SCORED = 3;
   private static final boolean TRACE = System.getenv("FROST_PEARLTRACE") != null;
   private static final int COOLDOWN_WAIT = 25;
   private static final int RETURN_TICKS = 40;
   private static final float LEGIT_RAMP_SCALE = 2.5F;
   private static final float RETURN_DONE = 0.25F;
   private static final float RETURN_SPEED = 150.0F;
   private static final double LAUNCH_CHANCE_STEP = 0.02;
   private static final double LEGIT_SPIN = 0.8;
   private static final double SILENT_SPIN = (double)0.5F;
   private static final double DRIFTING = 0.03;
   private static final double TELEPORT = (double)8.0F;
   private final RangeSetting delay;
   private final EnumSetting aimMode;
   private final SliderSetting smoothness;
   private final SliderSetting legitSpeed;
   private final BoolSetting lockMouse;
   private final BoolSetting turnBack;
   private final SliderSetting legitJitter;
   private final SliderSetting legitNoise;
   private final SliderSetting returnOffset;
   private final BoolSetting launch;
   private final RangeSetting swapBack;
   private final BoolSetting alwaysThrow;
   private final BoolSetting debug;
   private final SwapBack swap;
   private Phase phase;
   private final SmoothTracker tracker;
   private Rotation pressView;
   private Rotation returnView;
   private int returnUntil;
   private int movedTick;
   private LocalPlayer tracked;
   private int pearlTick;
   private int windTick;
   private int delayTick;
   private int plannedTick;
   private int deadline;
   private int landTick;
   private int pressTick;
   private boolean scheduled;
   private long tickNanos;
   private long pearlNanos;
   private Vec3 pearlSpawn;
   private Vec3 pearlVelocity;
   private CatchSolver.PearlPath path;
   private ThrownEnderpearl pearl;
   private int pathTick;
   private int roundTrip;
   private boolean vanished;
   private LocalPlayer thrower;
   private Rotation aim;
   private Vec3 lastPosition;
   private Vec3 previousPosition;
   private boolean lastOnGround;

   public PearlCatch() {
      super("pearlCatch");
      this.delay = (RangeSetting)this.add(new RangeSetting("delay", 3, 5, 1, 40, Measure.TICKS));
      this.aimMode = (EnumSetting)this.add(new EnumSetting("aim", 0, new String[]{"Silent", "Legit"}));
      this.smoothness = (SliderSetting)this.add((new SliderSetting("smoothness", 0, 0, 40, Measure.PERCENT)).percentOfRange());
      this.legitSpeed = (SliderSetting)this.add((new SliderSetting("legitSpeed", 95, 0, 100, Measure.PERCENT)).uncapped());
      this.lockMouse = (BoolSetting)this.add(new BoolSetting("lockMouse", true));
      this.turnBack = (BoolSetting)this.add(new BoolSetting("turnBack", false));
      this.legitJitter = (SliderSetting)this.add((new SliderSetting("legitJitter", 0, 0, 90, Measure.PERCENT)).percentOfRange());
      this.legitNoise = (SliderSetting)this.add((new SliderSetting("legitNoise", 0, 0, 50)).withFormat((v) -> String.format(Locale.ROOT, "%.1f°", (double)v / (double)10.0F)));
      this.returnOffset = (SliderSetting)this.add((new SliderSetting("returnOffset", 0, 0, 50)).withFormat((v) -> String.format(Locale.ROOT, "%.1f°", (double)v / (double)10.0F)));
      this.launch = (BoolSetting)this.add(new BoolSetting("launch", false));
      this.swapBack = (RangeSetting)this.add(new RangeSetting("swapBack", 1, 3, 1, 10, Measure.TICKS));
      this.alwaysThrow = (BoolSetting)this.add(new BoolSetting("alwaysThrow", true));
      this.debug = (BoolSetting)this.add(new BoolSetting("debug", false));
      this.swap = new SwapBack(5);
      this.phase = PearlCatch.Phase.IDLE;
      this.tracker = new SmoothTracker(2.5F, 1.0F);
      this.movedTick = Integer.MIN_VALUE;
      this.pressTick = -1;
      this.roundTrip = -1;
      this.delay.describe();
      this.launch.describe();
      this.swapBack.describe();
      this.alwaysThrow.describe();
      this.debug.describe();
      this.smoothness.visibleWhen(() -> !this.legit());
      this.legitSpeed.visibleWhen(this::legit);
      this.legitSpeed.describe();
      this.lockMouse.describe();
      this.lockMouse.visibleWhen(this::legit);
      this.legitJitter.visibleWhen(this::legit);
      this.legitNoise.visibleWhen(this::legit);
      this.returnOffset.visibleWhen(() -> this.legit() && (Boolean)this.turnBack.get());
      this.turnBack.visibleWhen(this::legit);
      this.turnBack.describe();
      this.legitJitter.describe();
      this.legitNoise.describe();
      this.returnOffset.describe();
      ClientEntityEvents.ENTITY_LOAD.register((ClientEntityEvents.Load)(entity, level) -> this.onEntityLoad(entity));
   }

   public boolean activatable() {
      return true;
   }

   protected void onActivate() {
      if (TRACE) {
         PrintStream var10000 = System.out;
         String var10001 = String.valueOf(this.phase);
         var10000.println("[GRIMFIVE]   pearlcatch key: phase " + var10001 + ", player " + String.valueOf(mc.player == null ? "none" : mc.player.getId()));
      }

      if ((this.phase == PearlCatch.Phase.IDLE || this.phase == PearlCatch.Phase.RETURN) && mc.player != null) {
         if (this.phase == PearlCatch.Phase.IDLE || mc.player != this.thrower) {
            this.pressView = Rotation.of(mc.player);
         }

         this.phase = PearlCatch.Phase.PEARL;
         this.thrower = mc.player;
         this.why("pressed");
      } else if (mc.player != null) {
         this.why("pressed while the last catch is still going - the press is dropped");
      }

   }

   protected void onEnable() {
      this.forgetPositions((LocalPlayer)null);
      this.listen(PreTickEvent.class, this::onTick);
      this.listen(TickEvent.class, this::onTickEnd);
   }

   protected void onDisable() {
      this.swap.cancel(mc.player);
      this.halt();
      this.forgetPositions((LocalPlayer)null);
   }

   private void forgetPositions(LocalPlayer player) {
      this.tracked = player;
      this.lastPosition = null;
      this.previousPosition = null;
      this.lastOnGround = false;
      this.movedTick = Integer.MIN_VALUE;
   }

   private void stop() {
      boolean back = this.legit() && (Boolean)this.turnBack.get() && this.pressView != null && mc.player != null && mc.player == this.thrower && Rotation.of(mc.player).distanceTo(this.pressView) >= this.returnDone();
      if (back) {
         this.pearl = null;
         this.aim = null;
         this.pressTick = -1;
         this.phase = PearlCatch.Phase.RETURN;
         float offset = (float)(Integer)this.returnOffset.get() / 10.0F;
         this.returnView = offset > 0.0F ? this.pressView.moved(offset * Humanize.signed(), 0.6F * offset * Humanize.signed()) : this.pressView;
         this.returnUntil = mc.player.tickCount + 40;
      } else {
         this.halt();
      }

   }

   private float returnDone() {
      return 0.25F + (float)(Integer)this.legitNoise.get() / 10.0F;
   }

   private void halt() {
      this.tracker.reset();
      this.phase = PearlCatch.Phase.IDLE;
      this.pearl = null;
      this.aim = null;
      this.pressTick = -1;
      this.pressView = null;
      this.returnView = null;
      this.thrower = null;
      this.vanished = false;
   }

   public boolean locksMouse() {
      return this.isEnabled() && this.legit() && (Boolean)this.lockMouse.get() && (this.phase == PearlCatch.Phase.PEARL || this.phase == PearlCatch.Phase.FLIGHT) && mc.gui.screen() == null;
   }

   public void mouseTurned() {
      if (this.phase == PearlCatch.Phase.RETURN) {
         this.phase = PearlCatch.Phase.IDLE;
         this.pressView = null;
         this.returnView = null;
         this.tracker.reset();
      }

   }

   private void onEntityLoad(Entity entity) {
      if (this.isEnabled() && this.phase == PearlCatch.Phase.FLIGHT && this.pearl == null && mc.player != null && mc.player == this.thrower && entity instanceof ThrownEnderpearl thrown) {
         if (entity.tickCount == 0 && thrown.getOwner() == mc.player) {
            int now = mc.player.tickCount;
            if (this.vanished) {
               int trip = this.roundTrip >= 0 ? this.roundTrip : this.latencyTicks();
               this.pathTick = Math.max(this.pearlTick, now - trip);
               this.vanished = false;
               this.why(String.format(Locale.ROOT, "the server sent the pearl again mid-flight (there since t+%d) - aiming at that one", this.pathTick - this.pearlTick));
            } else {
               this.roundTrip = Math.max(0, now - this.pearlTick);
               this.pathTick = this.pearlTick;
            }

            this.pearl = thrown;
            this.pearlSpawn = thrown.position();
            this.pearlVelocity = thrown.getDeltaMovement();
            this.path = CatchSolver.PearlPath.of(thrown.level(), this.pearlSpawn, this.pearlVelocity);
            this.landTick = this.pathTick + this.path.ticks();
            this.scheduled = false;
            return;
         }
      }

   }

   private int latencyTicks() {
      PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
      return 1 + (info == null ? 0 : Math.round((float)info.getLatency() / 50.0F));
   }

   private void onTick(PreTickEvent event) {
      Minecraft client = event.client();
      LocalPlayer player = client.player;
      this.tickNanos = System.nanoTime();
      if (this.phase == PearlCatch.Phase.FLIGHT && player != null && this.swap.active() && player.tickCount + 1 >= this.windTick) {
         this.swap.backIn(2);
      }

      this.swap.tick(player);
      if (player != null && (this.phase == PearlCatch.Phase.IDLE || player == this.thrower)) {
         if (this.tracked == player && CatchSolver.serverMoved(this.lastPosition, player.position())) {
            this.movedTick = player.tickCount;
         }

         if (this.phase != PearlCatch.Phase.IDLE && player.isDeadOrDying()) {
            if (this.phase != PearlCatch.Phase.RETURN) {
               this.why("you died - the catch is given up");
            }

            this.halt();
         } else {
            boolean busy = client.gui.screen() != null || player.isUsingItem() || player.isHandsBusy();
            if (TRACE && this.phase == PearlCatch.Phase.PEARL) {
               PrintStream var10000 = System.out;
               String var10001 = String.valueOf(client.gui.screen());
               var10000.println("[GRIMFIVE]   pearlcatch press: screen " + var10001 + ", using " + player.isUsingItem() + ", busy " + player.isHandsBusy() + ", acted " + Hotbar.actedThisTick() + ", picked " + SwapBack.pickedThisTick(player) + ", enabled " + this.isEnabled());
            }

            if (this.phase == PearlCatch.Phase.PEARL) {
               if (busy) {
                  this.why(client.gui.screen() != null ? "a screen is open - no pearl thrown" : (player.isUsingItem() ? "you are using an item (eating, blocking, drawing a bow...) - no pearl thrown" : "your hands are busy - no pearl thrown"));
                  this.stop();
               } else {
                  this.throwPearl(client, player);
               }
            } else if (this.phase == PearlCatch.Phase.FLIGHT) {
               if (this.pearl != null && this.pearl.isRemoved()) {
                  if (CatchSolver.tookYou(player.tickCount, this.movedTick)) {
                     this.why(String.format(Locale.ROOT, "the pearl hit something at t+%d and took you there, before the wind charge went", player.tickCount - this.pearlTick));
                     this.stop();
                     return;
                  }

                  if (player.tickCount >= this.landTick - 3) {
                     this.why(String.format(Locale.ROOT, "the pearl came down (or hit something) at t+%d, before the wind charge went", player.tickCount - this.pearlTick));
                     this.stop();
                     return;
                  }

                  this.why(String.format(Locale.ROOT, "the pearl vanished from your client at t+%d, %d ticks before its landing - going on along its worked-out flight", player.tickCount - this.pearlTick, this.landTick - player.tickCount));
                  this.pearl = null;
                  this.vanished = true;
               }

               if (player.tickCount > this.deadline) {
                  this.why("no wind charge by the end of the wait - given up");
                  this.stop();
               } else if (!busy && player.tickCount >= this.windTick) {
                  this.throwWind(client, player);
               }
            }

         }
      } else {
         if (player != null && this.phase != PearlCatch.Phase.RETURN) {
            this.why("you respawned or changed world - the catch is given up");
         }

         this.halt();
      }
   }

   private void throwPearl(Minecraft client, LocalPlayer player) {
      int pearlSlot = Hotbar.find(player, (stack) -> stack.is(Items.ENDER_PEARL));
      int windSlot = Hotbar.find(player, (stack) -> stack.is(Items.WIND_CHARGE));
      if (pearlSlot >= 0 && windSlot >= 0) {
         if (this.pressTick < 0) {
            this.pressTick = player.tickCount;
         }

         if (!onCooldown(player, pearlSlot) && !onCooldown(player, windSlot)) {
            if (!Hotbar.actedThisTick() && this.pick(player, pearlSlot)) {
               Rotation sent = sent(player);
               Vec3 inherited = this.inherited();
               Vec3 position = player.position();
               if (!this.use(client, player, pearlSlot)) {
                  this.why("the pearl's right click didn't throw it (a block or entity under the aim took it, or the server turned it down)");
                  this.stop();
               } else {
                  this.pearlTick = player.tickCount;
                  this.pearlNanos = this.tickNanos;
                  this.windTick = this.pearlTick + SwapBack.ticks(this.delay.low(), this.delay.high());
                  this.delayTick = this.windTick;
                  this.plannedTick = this.windTick;
                  this.deadline = this.windTick + this.extra();
                  this.pearlSpawn = position.add((double)0.0F, (double)(player.getEyeHeight() - 0.1F), (double)0.0F);
                  this.pearlVelocity = CatchSolver.pearlVelocity(sent.yaw(), sent.pitch(), inherited);
                  this.path = CatchSolver.PearlPath.of(player.level(), this.pearlSpawn, this.pearlVelocity);
                  this.pathTick = this.pearlTick;
                  this.roundTrip = -1;
                  this.vanished = false;
                  this.landTick = this.pathTick + this.path.ticks();
                  this.scheduled = false;
                  this.pearl = null;
                  this.phase = PearlCatch.Phase.FLIGHT;
               }
            } else {
               if (player.tickCount - this.pressTick > 25) {
                  this.why("another module clicked or switched your slot every tick - no pearl thrown");
                  this.stop();
               }

            }
         } else {
            if (player.tickCount - this.pressTick > 25) {
               this.why(onCooldown(player, windSlot) ? "the wind charge stayed on cooldown - no pearl thrown" : "the pearl stayed on cooldown");
               this.stop();
            }

         }
      } else {
         this.why(pearlSlot < 0 ? "no ender pearl in your hotbar" : "no wind charge in your hotbar");
         this.stop();
      }
   }

   private void throwWind(Minecraft client, LocalPlayer player) {
      int windSlot = Hotbar.find(player, (stack) -> stack.is(Items.WIND_CHARGE));
      if (windSlot < 0) {
         this.why("no wind charge in your hotbar");
         this.stop();
      } else if (onCooldown(player, windSlot)) {
         if (player.tickCount < this.landTick - 2 && player.tickCount <= this.deadline) {
            this.windTick = player.tickCount + 1;
         } else {
            this.why(String.format(Locale.ROOT, "the wind charge was on cooldown until the pearl came down (%.0f%% left) - the server puts it on one with the pearl", player.getCooldowns().getCooldownPercent(player.getInventory().getItem(windSlot), 0.0F) * 100.0F));
            this.stop();
         }

      } else {
         boolean lastCall = this.lastCall(player.tickCount);
         Rotation sent = sent(player);
         CatchSolver.Plan plan = CatchSolver.plan(this.situation(player, player.tickCount, player.tickCount), sent.yaw(), sent.pitch(), (Boolean)this.launch.get() && !lastCall);
         double needed = this.needed(player.tickCount);
         boolean catches = plan.chance() > (double)0.0F;
         boolean ready = catches && plan.chance() >= needed;
         if (!ready && catches && plan.chance() >= 0.05) {
            ready = !this.likelierLater(player, plan.chance() + 0.05);
         }

         if (TRACE) {
            System.out.println(String.format(Locale.ROOT, "[GRIMFIVE]   pearlcatch t+%d along %.2f %.2f: caught %b chance %.2f needed %.2f -> %s", player.tickCount - this.pearlTick, sent.yaw(), sent.pitch(), plan.outcome().caught(), plan.chance(), needed, ready ? "throw" : "wait"));
         }

         boolean landed = CatchSolver.landed(player.tickCount, this.landTick);
         if (!ready && lastCall && (Boolean)this.alwaysThrow.get() && player.tickCount >= this.landTick - 2 && !landed) {
            this.why(String.format(Locale.ROOT, "no catch by the physics worked out here (best %.0f%%) - thrown anyway", plan.chance() * (double)100.0F));
            ready = true;
         }

         if (ready && !Hotbar.actedThisTick() && this.pick(player, windSlot)) {
            if (!this.use(client, player, windSlot)) {
               this.why("the right click didn't throw it (it used a block or entity under the aim, or the server turned it down)");
            } else {
               this.why(String.format(Locale.ROOT, "wind charge thrown at t+%d (catch chance %.0f%%, pearl %s)", player.tickCount - this.pearlTick, plan.chance() * (double)100.0F, this.pearl != null ? "seen" : "not seen by your client"));
            }

            this.stop();
         } else if (lastCall && !plan.outcome().caught() && player.tickCount >= this.landTick - 2) {
            this.why(landed ? String.format(Locale.ROOT, "the pearl came down at t+%d by its worked-out flight, before the wind charge was due - not thrown", this.landTick - this.pearlTick) : (!Hotbar.actedThisTick() && ready ? "the wind charge's slot couldn't be picked in time (another module's pick or click that tick)" : "no catch by the physics worked out here, and Always throw is off"));
            this.stop();
         } else {
            this.windTick = player.tickCount + 1;
         }
      }
   }

   private void why(String reason) {
      if ((Boolean)this.debug.get() && mc.player != null) {
         ChatUtil.sendFormatted("&dPearl Catch&7: " + reason);
      }

      if (TRACE) {
         System.out.println("[GRIMFIVE]   pearlcatch: " + reason);
      }

   }

   private boolean likelierLater(LocalPlayer player, double beat) {
      int last = Math.min(Math.min(this.deadline, this.landTick - 2), player.tickCount + 12);

      for(int tick = player.tickCount + 1; tick <= last; ++tick) {
         CatchSolver.Plan plan = this.roughAt(player, tick, player.tickCount);
         if (plan != null && plan.chance() > beat) {
            CatchSolver.Plan sure = this.bestAt(player, tick, player.tickCount);
            if (sure != null && sure.chance() > beat) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean legit() {
      return (Integer)this.aimMode.get() == 1;
   }

   private double needed(int tick) {
      if (this.lastCall(tick)) {
         return 0.05;
      } else {
         double step = (Boolean)this.launch.get() ? 0.02 : 0.2;
         return Math.max((double)0.5F, 0.9 - step * (double)(tick - this.plannedTick));
      }
   }

   private boolean lastCall(int tick) {
      return tick >= Math.min(this.deadline, this.landTick - 2);
   }

   private int extra() {
      return (Boolean)this.launch.get() ? 60 : 20;
   }

   private void onTickEnd(TickEvent event) {
      LocalPlayer player = event.client().player;
      if (player != null) {
         if (player != this.tracked) {
            this.forgetPositions(player);
         }

         this.previousPosition = this.lastPosition;
         this.lastPosition = player.position();
         this.lastOnGround = player.onGround();
         if (this.phase == PearlCatch.Phase.RETURN && player == this.thrower) {
            this.turnBack(event.client(), player);
         } else if (this.phase == PearlCatch.Phase.FLIGHT && player == this.thrower) {
            if (!this.scheduled) {
               this.scheduled = true;
               this.schedule(player);
            }

            int throwAt = Math.max(this.windTick, player.tickCount);
            if (throwAt - player.tickCount <= (this.legit() ? 40 : (RotationManager.legit() ? 5 : 3)) && event.client().gui.screen() == null) {
               CatchSolver.Plan best = this.aimAt(player, throwAt);
               if (this.aim != null) {
                  Rotation target = this.aim;
                  if (TRACE) {
                     System.out.println(String.format(Locale.ROOT, "[GRIMFIVE]   pearlcatch aim t+%d: target %.2f %.2f, view %.2f %.2f, best %s", player.tickCount - this.pearlTick, target.yaw(), target.pitch(), player.getYRot(), player.getXRot(), best == null ? "none" : String.format(Locale.ROOT, "%.2f", best.chance())));
                  }

                  if (this.legit()) {
                     int ticksLeft = Math.max(1, throwAt - player.tickCount);
                     float needed = SmoothTracker.speedFor(Math.max(0.2F, (float)ticksLeft / 4.0F));
                     this.turnOnScreen(player, target, Math.max((float)(Integer)this.legitSpeed.get(), needed));
                  } else {
                     RotationConfig config = RotationConfig.silent((float)(Integer)this.smoothness.get() / 100.0F, Smoothing.EASE_OUT_CUBIC).fixingMovement();
                     VantaClient.ROTATIONS.aim((RotationTarget)((partialTick) -> target), 7, config);
                  }

               }
            }
         }
      }
   }

   private CatchSolver.Plan aimAt(LocalPlayer player, int tick) {
      boolean burst = (Boolean)this.launch.get() && !this.lastCall(tick);
      CatchSolver.Throw situation = this.situation(player, tick, player.tickCount - 1);
      Rotation sent = sent(player);
      if (this.aim == null) {
         double along = CatchSolver.plan(situation, sent.yaw(), sent.pitch(), burst).chance();
         double needed = this.needed(tick);
         CatchSolver.Plan best = along >= needed ? null : CatchSolver.best(situation, sent, burst);
         if (best == null || !(along <= (double)0.0F) && !(along < best.chance() - 0.04)) {
            if (TRACE) {
               System.out.println(String.format(Locale.ROOT, "[GRIMFIVE]   pearlcatch t+%d: no turn, along %.2f %.2f catches %.2f (needed %.2f, best %s)", player.tickCount - this.pearlTick, sent.yaw(), sent.pitch(), along, needed, best == null ? "-" : String.format(Locale.ROOT, "%.2f", best.chance())));
            }
         } else {
            this.aim = new Rotation(best.yaw(), best.pitch());
         }

         return best;
      } else {
         CatchSolver.Plan best = CatchSolver.best(situation, sent, burst);
         if (best != null && CatchSolver.plan(situation, this.aim.yaw(), this.aim.pitch(), burst).chance() < best.chance() - 0.04) {
            this.aim = new Rotation(best.yaw(), best.pitch());
         }

         return best;
      }
   }

   private void turnBack(Minecraft client, LocalPlayer player) {
      Rotation back = this.returnView;
      if (back != null && !(Rotation.of(player).distanceTo(back) < this.returnDone()) && player.tickCount <= this.returnUntil) {
         if (client.gui.screen() == null) {
            this.turnOnScreen(player, back, Math.max((float)(Integer)this.legitSpeed.get(), 150.0F));
         }
      } else {
         this.phase = PearlCatch.Phase.IDLE;
         this.pressView = null;
         this.returnView = null;
         this.tracker.reset();
      }
   }

   private void turnOnScreen(LocalPlayer player, Rotation target, float speed) {
      this.tracker.humanize((float)(Integer)this.legitJitter.get(), (float)(Integer)this.legitNoise.get() / 10.0F);
      this.tracker.rampUp(speed);
      VantaClient.ROTATIONS.aim((RotationTarget)((partialTick) -> this.tracker.step(Rotation.of(player), target, (Rotation)null, speed, partialTick)), 7, SmoothTracker.CONFIG);
   }

   private void schedule(LocalPlayer player) {
      long started = System.nanoTime();
      int from = player.tickCount;
      int to = Math.min(this.delayTick + 2, this.landTick - 2);
      if (from <= to) {
         Rotation sent = sent(player);
         boolean drifting = this.inherited().horizontalDistance() > 0.03;
         int still = -1;
         double stillChance = (double)0.0F;

         for(int tick = from; tick <= to; ++tick) {
            double along = CatchSolver.plan(this.situation(player, tick, player.tickCount - 1), sent.yaw(), sent.pitch(), (Boolean)this.launch.get()).chance();
            if (along >= 0.9 && (still < 0 || !drifting && Math.abs(tick - this.delayTick) < Math.abs(still - this.delayTick))) {
               still = tick;
               stillChance = along;
            }
         }

         if (still >= 0) {
            this.windTick = still;
            this.plannedTick = still;
            this.deadline = still + this.extra();
            this.trace(player, "no turn", stillChance, started);
         } else {
            boolean inRange = this.windTick >= from && this.windTick <= to;
            CatchSolver.Plan kept = inRange ? this.bestAt(player, this.windTick, player.tickCount - 1) : null;
            if (kept != null && this.score(kept, sent) >= 0.8) {
               this.trace(player, "kept", kept.chance(), started);
            } else {
               int count = to - from + 1;
               double[] rough = new double[count];

               for(int tick = from; tick <= to; ++tick) {
                  CatchSolver.Plan plan = tick == this.windTick ? kept : this.roughAt(player, tick, player.tickCount - 1);
                  rough[tick - from] = plan == null ? (double)0.0F : this.score(plan, sent);
               }

               double[] full = new double[count];
               Arrays.fill(full, (double)-1.0F);
               if (inRange) {
                  full[this.windTick - from] = kept == null ? (double)0.0F : this.score(kept, sent);
               }

               for(int n = 0; n < 3; ++n) {
                  int likeliest = -1;

                  for(int i = 0; i < count; ++i) {
                     if (full[i] < (double)0.0F && rough[i] > (double)0.0F && (likeliest < 0 || rough[i] > rough[likeliest])) {
                        likeliest = i;
                     }
                  }

                  if (likeliest < 0) {
                     break;
                  }

                  CatchSolver.Plan plan = this.bestAt(player, from + likeliest, player.tickCount - 1);
                  full[likeliest] = plan == null ? (double)0.0F : this.score(plan, sent);
               }

               double top = (double)0.0F;

               for(double chance : full) {
                  top = Math.max(top, chance);
               }

               if (top < (double)0.5F) {
                  this.trace(player, "none likely, kept", top, started);
               } else {
                  int pick = -1;

                  for(int tick = from; tick <= to; ++tick) {
                     if (full[tick - from] >= top - 0.1 && (pick < 0 || Math.abs(tick - this.delayTick) < Math.abs(pick - this.delayTick))) {
                        pick = tick;
                     }
                  }

                  this.windTick = pick;
                  this.plannedTick = pick;
                  this.deadline = pick + this.extra();
                  this.trace(player, "moved", full[pick - from], started);
               }
            }
         }
      }
   }

   private void trace(LocalPlayer player, String what, double chance, long started) {
      if (TRACE) {
         System.out.println(String.format(Locale.ROOT, "[GRIMFIVE]   pearlcatch plan at t+%d (%s pearl): %s, throw at t+%d (delay t+%d) chance %.2f, %.1f ms", player.tickCount - this.pearlTick, this.pearl != null ? "server" : "predicted", what, this.windTick - this.pearlTick, this.delayTick - this.pearlTick, chance, (double)(System.nanoTime() - started) / (double)1000000.0F));
      }

   }

   private double score(CatchSolver.Plan plan, Rotation from) {
      return Math.max((double)0.0F, plan.chance() - CatchSolver.spinCost(plan, from, this.spin()));
   }

   private double spin() {
      return this.legit() ? 0.8 : (double)0.5F;
   }

   private CatchSolver.Plan bestAt(LocalPlayer player, int tick, int startedAt) {
      return CatchSolver.best(this.situation(player, tick, startedAt), VantaClient.ROTATIONS.lastSent(), (Boolean)this.launch.get());
   }

   private CatchSolver.Plan roughAt(LocalPlayer player, int tick, int startedAt) {
      return CatchSolver.best(this.situation(player, tick, startedAt), VantaClient.ROTATIONS.lastSent(), (Boolean)this.launch.get(), true);
   }

   private CatchSolver.Throw situation(LocalPlayer player, int tick, int startedAt) {
      Vec3 position = player.position();
      Vec3 inherited = this.inherited();
      AABB box = player.getBoundingBox();
      int ahead = tick - player.tickCount;
      if (ahead > 0 && !player.onGround()) {
         Vec3 motion = player.getDeltaMovement();
         Vec3 step = inherited;

         for(int i = 0; i < ahead; ++i) {
            AABB moved = box.move(motion);
            if (!player.level().noCollision(player, moved)) {
               step = new Vec3(step.x, (double)0.0F, step.z);
               break;
            }

            box = moved;
            position = position.add(motion);
            step = motion;
            motion = new Vec3(motion.x * 0.91, (motion.y - 0.08) * 0.98, motion.z * 0.91);
         }

         inherited = step.lengthSqr() <= 4.0E-8 ? Vec3.ZERO : step;
      }

      Vec3 spawn = new Vec3(position.x, position.y + (double)player.getEyeHeight(), position.z);
      double drift = (double)(this.tickNanos - this.pearlNanos) / (double)1000000.0F - (double)(startedAt - this.pearlTick) * (double)50.0F;
      int whole = (int)Math.round(drift / (double)50.0F);
      Vec3 look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
      double smooth = (double)(Integer)this.smoothness.get() / (double)100.0F;
      double duration = (double)40.0F * ((double)1.0F - ((double)1.0F - smooth) * ((double)1.0F - smooth));
      double handBack = this.legit() ? (double)1.0F : (duration <= (double)1.0F ? (double)0.0F : Math.pow((double)1.0F - (double)1.0F / duration, (double)3.0F));
      return new CatchSolver.Throw(player.level(), this.path, tick - this.pathTick + whole, spawn, inherited, player.getDimensions(player.getPose()), box, CatchSolver.lags(drift - (double)whole * (double)50.0F), look, handBack, this.spin());
   }

   private Vec3 inherited() {
      if (this.lastPosition != null && this.previousPosition != null) {
         Vec3 step = this.lastPosition.subtract(this.previousPosition);
         return !(step.lengthSqr() <= 4.0E-8) && !(step.lengthSqr() > (double)64.0F) ? new Vec3(step.x, this.lastOnGround ? (double)0.0F : step.y, step.z) : Vec3.ZERO;
      } else {
         return Vec3.ZERO;
      }
   }

   private static Rotation sent(LocalPlayer player) {
      return new Rotation(VantaClient.ROTATIONS.sentYaw(player.getYRot()), VantaClient.ROTATIONS.sentPitch(player.getXRot()));
   }

   private static boolean onCooldown(LocalPlayer player, int slot) {
      return player.getCooldowns().isOnCooldown(player.getInventory().getItem(slot));
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

   private static enum Phase {
      IDLE,
      PEARL,
      FLIGHT,
      RETURN;

      // $FF: synthetic method
      private static Phase[] $values() {
         return new Phase[]{IDLE, PEARL, FLIGHT, RETURN};
      }
   }
}
