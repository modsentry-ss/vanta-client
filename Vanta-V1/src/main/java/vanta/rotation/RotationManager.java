package vanta.rotation;

import vanta.VantaClient;
import vanta.event.events.EntityRenderStateEvent;
import vanta.event.events.PreTickEvent;
import vanta.event.events.TickEvent;
import vanta.event.events.TurnEvent;
import vanta.module.impl.movement.MoveFix;
import vanta.ui.GuiState;
import vanta.util.Entities;
import vanta.util.Placement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class RotationManager {
   private Request request;
   private Request visible;
   private Rotation current;
   private Rotation previous;
   private RotationConfig config;
   private Smoothing smoothing;
   private Rotator rotator;
   private Rotation lastSent = new Rotation(0.0F, 0.0F);
   private Rotation goal;
   private final HumanFlick flick = new HumanFlick();
   private float yawRest;
   private float pitchRest;

   public void init() {
      VantaClient.EVENTS.subscribe(PreTickEvent.class, 300, (event) -> GuiState.pollSilentKey(event.client()));
      VantaClient.EVENTS.subscribe(TickEvent.class, -200, this::onTick);
      VantaClient.EVENTS.subscribe(TurnEvent.class, this::onTurn);
      VantaClient.EVENTS.subscribe(EntityRenderStateEvent.class, this::onRenderState);
   }

   public void aim(RotationTarget target, int priority, RotationConfig config) {
      if (this.request == null || priority > this.request.priority()) {
         this.request = new Request(target, priority, config);
      }

   }

   public void aim(Rotation target, int priority, RotationConfig config) {
      this.aim(RotationTarget.fixed(target), priority, config);
   }

   public boolean active() {
      return this.current != null;
   }

   public Rotation rotation(LocalPlayer player) {
      return this.current != null ? this.current : Rotation.of(player);
   }

   public Rotation rendered(float partialTick) {
      if (this.current == null) {
         return null;
      } else {
         Rotation from = this.previous != null ? this.previous : this.current;
         return new Rotation(Mth.rotLerp(partialTick, from.yaw(), this.current.yaw()), Mth.lerp(partialTick, from.pitch(), this.current.pitch()));
      }
   }

   public float sentYaw(float actual) {
      return this.current != null ? this.current.yaw() : actual;
   }

   public float sentPitch(float actual) {
      return this.current != null ? this.current.pitch() : actual;
   }

   public void sent(Rotation rotation) {
      if (!rotation.equals(this.lastSent)) {
         Placement.rotated(Math.abs(rotation.yaw() - this.lastSent.yaw()));
      }

      this.lastSent = rotation;
   }

   public Rotation lastSent() {
      return this.lastSent;
   }

   public Rotation goal() {
      return this.goal;
   }

   public static boolean legit() {
      return GuiState.legitSilent();
   }

   public Vec3 viewVector(Entity entity) {
      return this.current != null && Entities.isLocal(entity) ? this.current.direction() : null;
   }

   public float movementYaw(Entity entity, float actual) {
      return this.correctsMovement(entity) ? this.current.yaw() : actual;
   }

   public float movementPitch(Entity entity, float actual) {
      return this.correctsMovement(entity) ? this.current.pitch() : actual;
   }

   public Vec3 movementLook(Entity entity, Vec3 actual) {
      return this.correctsMovement(entity) ? this.current.direction() : actual;
   }

   private boolean correctsMovement(Entity entity) {
      return this.current != null && (moveFix().corrects() || this.fixesMovement()) && Entities.isLocal(entity);
   }

   private boolean fixesMovement() {
      return this.config != null && this.config.fixesMovement();
   }

   public Vec2 correctInput(Vec2 input, float actualYaw) {
      boolean corrects = moveFix().isEnabled() ? moveFix().correctsInput() : this.fixesMovement();
      if (this.current != null && corrects && !(input.lengthSquared() < 1.0E-8F)) {
         double theta = Math.toRadians((double)(actualYaw - this.current.yaw()));
         float sin = (float)Math.sin(theta);
         float cos = (float)Math.cos(theta);
         return (new Vec2(snap(input.x * cos - input.y * sin), snap(input.y * cos + input.x * sin))).normalized();
      } else {
         return input;
      }
   }

   public void sync(Rotation rotation) {
      this.lastSent = rotation;
      if (this.current != null) {
         this.current = rotation;
         this.previous = rotation;
         this.rotator = null;
      }

      this.flick.reset();
   }

   private static MoveFix moveFix() {
      return (MoveFix)VantaClient.MODULES.get(MoveFix.class);
   }

   private Rotation distinct(Rotation rotation) {
      return Placement.duplicates(Math.abs(rotation.yaw() - this.lastSent.yaw())) ? rotation.moved(gcd(), 0.0F) : rotation;
   }

   private Rotation quantized(Rotation rotation) {
      return new Rotation(quantized(rotation.yaw(), this.lastSent.yaw()), Mth.clamp(quantized(rotation.pitch(), this.lastSent.pitch()), -90.0F, 90.0F));
   }

   private static float quantized(float value, float base) {
      float gcd = gcd();
      return base + (float)Math.round((value - base) / gcd) * gcd;
   }

   private static float snap(float value) {
      return Math.signum(value) * (float)Math.round(Math.abs(value));
   }

   private void onTick(TickEvent event) {
      LocalPlayer player = event.client().player;
      Request request = this.request;
      this.request = null;
      this.visible = null;
      this.previous = this.current;
      if (player == null) {
         this.current = null;
         this.previous = null;
         this.rotator = null;
         this.flick.reset();
      } else if (event.client().gui.screen() == null) {
         Rotation actual = Rotation.of(player);
         Rotation from = this.current != null ? this.current : actual;
         if (request != null) {
            this.config = request.config();
            if (this.config.silent()) {
               Rotation target = request.target().at(1.0F);
               this.goal = target;
               if (!legit()) {
                  this.flick.reset();
               }

               Rotation next = legit() ? this.legitStep(from, target, true) : this.step(from, target, 1.0F);
               this.current = this.distinct(this.quantized(next));
            } else if (this.current != null) {
               this.handBack(player, actual, from);
               if (this.current == null) {
                  this.visible = request;
               }
            } else {
               this.visible = request;
            }
         } else if (this.current != null) {
            this.handBack(player, actual, from);
         } else {
            this.rotator = null;
            this.flick.reset();
         }
      }

   }

   private void handBack(LocalPlayer player, Rotation actual, Rotation from) {
      float step = gcd();
      Rotation next = this.quantized(legit() ? this.legitStep(from, actual, false) : this.step(from, actual, 1.0F));
      boolean stuck = Math.abs(from.yawTo(next)) < step * 0.5F && Math.abs(from.pitchTo(next)) < step * 0.5F;
      if (stuck && next.distanceTo(actual) >= step) {
         float yaw = from.yawTo(actual);
         float pitch = from.pitchTo(actual);
         next = this.quantized(from.moved(Math.abs(yaw) >= step ? Math.signum(yaw) * step : 0.0F, Math.abs(pitch) >= step ? Math.signum(pitch) * step : 0.0F));
      }

      if (next.distanceTo(actual) < step) {
         float turns = (float)Math.round((next.yaw() - actual.yaw()) / 360.0F) * 360.0F;
         if (turns != 0.0F) {
            float shift = (float)Math.round(turns / step) * step;
            player.setYRot(player.getYRot() + shift);
            player.yRotO += shift;
            player.yBob += shift;
            player.yBobO += shift;
            player.yHeadRot += shift;
            player.yHeadRotO += shift;
            player.yBodyRot += shift;
            player.yBodyRotO += shift;
         }

         this.current = null;
         this.flick.reset();
      } else {
         this.current = next;
      }

   }

   private void onTurn(TurnEvent event) {
      LocalPlayer player = event.client().player;
      if (this.visible != null && player != null) {
         float partialTick = event.client().getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Rotation actual = Rotation.of(player);
         Rotation from = actual.moved(this.yawRest, this.pitchRest);
         Rotation next = this.step(from, this.visible.target().at(partialTick), event.deltaTicks());
         float step = gcd();
         float yaw = actual.yawTo(next);
         float pitch = actual.pitchTo(next);
         float yawTurn = (float)Math.round(yaw / step) * step;
         float pitchTurn = (float)Math.round(pitch / step) * step;
         this.yawRest = yaw - yawTurn;
         this.pitchRest = pitch - pitchTurn;
         if (yawTurn != 0.0F || pitchTurn != 0.0F) {
            player.turn((double)(yawTurn / 0.15F), (double)(pitchTurn / 0.15F));
         }
      } else {
         this.yawRest = 0.0F;
         this.pitchRest = 0.0F;
      }

   }

   private void onRenderState(EntityRenderStateEvent event) {
      EntityRenderState var3 = event.state();
      if (var3 instanceof LivingEntityRenderState state) {
         if (Entities.isLocal(event.entity())) {
            Rotation rotation = this.rendered(event.partialTicks());
            if (rotation != null) {
               state.bodyRot = rotation.yaw();
               state.yRot = 0.0F;
               state.xRot = rotation.pitch();
            }
         }
      }

   }

   private Rotation step(Rotation from, Rotation target, float deltaTicks) {
      if (this.rotator == null || this.smoothing != this.config.smoothing()) {
         this.smoothing = this.config.smoothing();
         this.rotator = this.smoothing.create();
      }

      return this.rotator.step(from, target, this.config, deltaTicks);
   }

   private Rotation legitStep(Rotation from, Rotation target, boolean follow) {
      float slow = 1.0F;
      if (this.config != null && this.config.smoothness() > 0.0F) {
         slow += this.config.scaled(40.0F, 0.0F) / 8.0F;
      }

      return this.flick.step(from, target, slow, follow);
   }

   private static float gcd() {
      float factor = (float)((Double)Minecraft.getInstance().options.sensitivity().get() * 0.6 + 0.2);
      return factor * factor * factor * 8.0F * 0.15F;
   }

   private static record Request(RotationTarget target, int priority, RotationConfig config) {
   }
}
