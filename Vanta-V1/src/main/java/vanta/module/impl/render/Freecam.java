package vanta.module.impl.render;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import vanta.VantaClient;
import vanta.event.events.TickEvent;
import vanta.mixin.extra.IAccessorKeyMapping;
import vanta.module.Module;
import vanta.module.impl.movement.Sprint;
import vanta.rotation.Rotation;
import vanta.rotation.RotationTarget;
import vanta.rotation.SmoothTracker;
import vanta.setting.BoolSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.SliderSetting;
import vanta.ui.Units;
import vanta.util.DrivenKeys;
import vanta.util.KeyBindUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Freecam extends Module {
   private static final int INSTANT = 0;
   private static final int SMOOTH = 1;
   private static final int CAMERA = 0;
   private static final int PLAYER = 1;
   private static final int OFF = 2;
   private static final int CONTROL_CAMERA = 0;
   private static final double CAMERA_RADIUS = 0.15;
   private static final double MAX_SPEED = (double)500.0F;
   private static final double SCROLL_STEP = (double)1.25F;
   private static final double MIN_SPEED = (double)0.5F;
   private static final double BODY_MARGIN = 0.35;
   private static final Input SNEAKING = new Input(false, false, false, false, false, true, false);
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final double INSET = 0.12;
   private static final double BLOCK_DIAGONAL = 1.7320508;
   private static final double POINT_RANGE = (double)128.0F;
   private static final float TURN_SPEED = 80.0F;
   private static final int AIM_PRIORITY = 2;
   private static final int CLICK_TICKS = 15;
   private static final float OPEN_AIR_TOLERANCE = 3.0F;
   private static final float SETTLED = 0.5F;
   private final SliderSetting speed;
   private final SliderSetting verticalSpeed;
   private final SliderSetting sprintMultiplier;
   private final BoolSetting scrollSpeed;
   private final BoolSetting smooth;
   private final SliderSetting acceleration;
   private final BoolSetting noclip;
   private final EnumSetting interact;
   private final EnumSetting control;
   private final BoolSetting keepSneaking;
   private final BoolSetting disableOnDamage;
   private final BoolSetting showBody;
   private final BoolSetting hideHand;
   private final BoolSetting customFov;
   private final SliderSetting fov;
   private final BoolSetting follow;
   private final BoolSetting keepPosition;
   private final BoolSetting renderAroundCamera;
   private final EnumSetting returnMode;
   private final SliderSetting returnTime;
   private static Freecam instance;
   private boolean started;
   private LocalPlayer player;
   private ClientLevel level;
   private Vec3 position;
   private Vec3 previous;
   private Vec3 velocity;
   private float yaw;
   private float pitch;
   private Vec3 playerAt;
   private boolean gridOnCamera;
   private CameraType perspective;
   private boolean instantOff;
   private float health;
   private int ticks;
   private double scrollFactor;
   private boolean keysTaken;
   private boolean sneakKept;
   private String info;
   private int infoSpeed;
   private Vec3 keptPosition;
   private float keptYaw;
   private float keptPitch;
   private WeakReference<ClientLevel> keptLevel;
   private boolean returning;
   private Vec3 returnFrom;
   private float returnYaw;
   private float returnPitch;
   private float returnFov;
   private long returnStart;
   private long returnNanos;
   private float drawnFov;
   private HitResult cameraHit;
   private Target target;
   private BlockHitResult outline;
   private Click attackClick;
   private Click useClick;
   private Click pickClick;
   private Target settling;
   private int settleUntil;
   private final SmoothTracker tracker;
   private boolean turning;
   private Rotation turnedTo;

   public Freecam() {
      super("freecam");
      this.speed = (SliderSetting)this.add((new SliderSetting("speed", 20, 1, 60, Measure.DISTANCE)).withFormat(Freecam::perSecond));
      this.verticalSpeed = (SliderSetting)this.add((new SliderSetting("verticalSpeed", 20, 1, 60, Measure.DISTANCE)).withFormat(Freecam::perSecond));
      this.sprintMultiplier = (SliderSetting)this.add((new SliderSetting("sprintMultiplier", 200, 100, 500, Measure.FRACTION)).withFormat((v) -> String.format(Locale.ROOT, "%.1fx", (double)v / (double)100.0F)));
      this.scrollSpeed = (BoolSetting)this.add(new BoolSetting("scrollSpeed", true));
      this.smooth = (BoolSetting)this.add(new BoolSetting("smooth", false));
      this.acceleration = (SliderSetting)this.add(new SliderSetting("acceleration", 35, 5, 100, Measure.PERCENT));
      this.noclip = (BoolSetting)this.add(new BoolSetting("noclip", true));
      this.interact = (EnumSetting)this.add(new EnumSetting("interact", 0, new String[]{"Camera", "Player", "Off"}));
      this.control = (EnumSetting)this.add(new EnumSetting("control", 0, new String[]{"Camera", "Player"}));
      this.keepSneaking = (BoolSetting)this.add(new BoolSetting("keepSneaking", true));
      this.disableOnDamage = (BoolSetting)this.add(new BoolSetting("disableOnDamage", true));
      this.showBody = (BoolSetting)this.add(new BoolSetting("showBody", true));
      this.hideHand = (BoolSetting)this.add(new BoolSetting("hideHand", true));
      this.customFov = (BoolSetting)this.add(new BoolSetting("customFov", false));
      this.fov = (SliderSetting)this.add((new SliderSetting("fov", 90, 30, 150, Measure.DEGREES)).capped());
      this.follow = (BoolSetting)this.add(new BoolSetting("follow", false));
      this.keepPosition = (BoolSetting)this.add(new BoolSetting("keepPosition", false));
      this.renderAroundCamera = (BoolSetting)this.add(new BoolSetting("renderAroundCamera", true));
      this.returnMode = (EnumSetting)this.add(new EnumSetting("returnMode", 0, new String[]{"Instant", "Smooth"}));
      this.returnTime = (SliderSetting)this.add(new SliderSetting("returnTime", 350, 50, 2000, Measure.MILLIS));
      this.position = Vec3.ZERO;
      this.previous = Vec3.ZERO;
      this.velocity = Vec3.ZERO;
      this.scrollFactor = (double)1.0F;
      this.info = "";
      this.infoSpeed = -1;
      this.keptLevel = new WeakReference((Object)null);
      this.returnFrom = Vec3.ZERO;
      this.drawnFov = 70.0F;
      this.tracker = new SmoothTracker(1.0F, 1.0F);
      instance = this;
      this.speed.describe();
      this.verticalSpeed.describe();
      this.sprintMultiplier.describe();
      this.scrollSpeed.describe();
      this.smooth.describe();
      this.acceleration.describe();
      SliderSetting var10000 = this.acceleration;
      BoolSetting var10001 = this.smooth;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.noclip.describe();
      this.interact.describe();
      this.control.describe();
      this.keepSneaking.describe();
      this.disableOnDamage.describe();
      this.showBody.describe();
      this.hideHand.describe();
      this.customFov.describe();
      this.fov.describe();
      var10000 = this.fov;
      var10001 = this.customFov;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.follow.describe();
      this.keepPosition.describe();
      this.renderAroundCamera.describe();
      this.returnMode.describe();
      this.returnTime.describe();
      this.returnTime.visibleWhen(() -> (Integer)this.returnMode.get() == 1);
   }

   private static String perSecond(int blocks) {
      return Units.distance(blocks) + "/s";
   }

   public String info() {
      int shown = (int)Math.round(this.horizontalSpeed((double)1.0F));
      if (shown != this.infoSpeed) {
         this.infoSpeed = shown;
         this.info = perSecond(shown);
      }

      return this.info;
   }

   public static Freecam on() {
      Freecam module = instance;
      return module != null && module.placed() ? module : null;
   }

   public static Freecam camera() {
      Freecam module = instance;
      if (module == null) {
         return null;
      } else {
         if (module.returning && !module.returnStillValid()) {
            module.endReturn();
         }

         return !module.placed() && !module.returning ? null : module;
      }
   }

   public static Freecam controls() {
      Freecam module = instance;
      return module != null && module.placed() && (Integer)module.control.get() == 0 ? module : null;
   }

   public static boolean blocksClicks() {
      Freecam module = controls();
      return module != null && (Integer)module.interact.get() == 2;
   }

   public static HitResult outlineHit(HitResult vanilla) {
      Freecam module = controls();
      return (HitResult)(module != null && (Integer)module.interact.get() == 0 ? module.outline : vanilla);
   }

   public static boolean keepsToggle(KeyMapping key) {
      return controls() != null && (key == mc.options.keyShift || key == mc.options.keySprint);
   }

   public static boolean scrolled(double wheel) {
      Freecam module = controls();
      if (module != null && (Boolean)module.scrollSpeed.get() && wheel != (double)0.0F) {
         double step = Math.pow((double)1.25F, Math.abs(wheel));
         double factor = wheel > (double)0.0F ? module.scrollFactor * step : module.scrollFactor / step;
         double base = (double)(Integer)module.speed.get();
         module.scrollFactor = Mth.clamp(factor, (double)0.5F / base, (double)500.0F / base);
         return true;
      } else {
         return false;
      }
   }

   public static SectionPos gridCenter(SectionPos cameraSection) {
      Freecam module = camera();
      return module != null && !(Boolean)module.renderAroundCamera.get() && !module.gridOnCamera && module.player != null ? SectionPos.of(module.player.getEyePosition()) : cameraSection;
   }

   private boolean placed() {
      return this.isEnabled() && this.started && this.player != null && mc.player == this.player && mc.level == this.level;
   }

   private boolean returnStillValid() {
      return mc.player != null && mc.player == this.player && mc.level == this.level && System.nanoTime() - this.returnStart < this.returnNanos;
   }

   public float returnProgress() {
      if (!this.returning) {
         return 1.0F;
      } else {
         double t = Mth.clamp((double)(System.nanoTime() - this.returnStart) / (double)this.returnNanos, (double)0.0F, (double)1.0F);
         if (t >= (double)1.0F) {
            this.endReturn();
            return 1.0F;
         } else {
            return (float)(t < (double)0.5F ? (double)4.0F * t * t * t : (double)1.0F - Math.pow((double)-2.0F * t + (double)2.0F, (double)3.0F) / (double)2.0F);
         }
      }
   }

   public boolean returning() {
      return this.returning;
   }

   private void endReturn() {
      this.returning = false;
      if (!this.isEnabled()) {
         this.player = null;
         this.level = null;
      }

   }

   public Vec3 framePosition(float partialTicks) {
      return this.previous.lerp(this.position, (double)partialTicks);
   }

   public float yaw() {
      return this.yaw;
   }

   public float pitch() {
      return this.pitch;
   }

   public Vec3 returnFrom() {
      return this.returnFrom;
   }

   public float returnYaw() {
      return this.returnYaw;
   }

   public float returnPitch() {
      return this.returnPitch;
   }

   public void turn(double yawDelta, double pitchDelta) {
      this.yaw += (float)yawDelta * 0.15F;
      this.pitch = Mth.clamp(this.pitch + (float)pitchDelta * 0.15F, -90.0F, 90.0F);
   }

   public boolean showsBody(Vec3 camera) {
      return (Boolean)this.showBody.get() && this.player != null && !this.player.getBoundingBox().inflate(0.35).contains(camera);
   }

   public boolean hidesHand() {
      return (Boolean)this.hideHand.get();
   }

   public float fov(float vanilla, float fluidNarrowing) {
      float wanted = (float)(Boolean)this.customFov.get() ? (Integer)this.fov.get() : (Integer)mc.options.fov().get() * fluidNarrowing;
      if (this.returning) {
         wanted = Mth.lerp(this.returnProgress(), this.returnFov, vanilla);
      }

      this.drawnFov = wanted;
      return wanted;
   }

   public Input playerInput() {
      if (!this.keysTaken) {
         this.keysTaken = true;
         this.sneakKept = (Boolean)this.keepSneaking.get() && mc.options.keyShift.isDown();
      }

      Options options = mc.options;
      boolean forward = DrivenKeys.driven(options.keyUp);
      boolean backward = DrivenKeys.driven(options.keyDown);
      boolean left = DrivenKeys.driven(options.keyLeft);
      boolean right = DrivenKeys.driven(options.keyRight);
      boolean jump = DrivenKeys.driven(options.keyJump);
      boolean sprint = DrivenKeys.driven(options.keySprint);
      if (!forward && !backward && !left && !right && !jump && !sprint) {
         return this.sneakKept ? SNEAKING : Input.EMPTY;
      } else {
         return new Input(forward, backward, left, right, jump, this.sneakKept, sprint);
      }
   }

   protected void onEnable() {
      this.listen(TickEvent.class, this::onTick);
      this.started = false;
      this.velocity = Vec3.ZERO;
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null) {
         this.start(player, mc.level);
      }

   }

   protected void onDisable() {
      boolean wasPlaced = this.started && this.player != null;
      boolean smooth = !this.instantOff && wasPlaced && mc.player == this.player && mc.level == this.level && (Integer)this.returnMode.get() == 1;
      this.started = false;
      this.velocity = Vec3.ZERO;
      this.scrollFactor = (double)1.0F;
      this.keysTaken = false;
      this.dropClicks();
      if (this.perspective != null) {
         mc.options.setCameraType(this.perspective);
         this.perspective = null;
      }

      if (!wasPlaced) {
         this.endReturn();
      } else {
         this.keptPosition = this.position;
         this.keptYaw = this.yaw;
         this.keptPitch = this.pitch;
         this.keptLevel = new WeakReference(this.level);
         Camera camera = mc.gameRenderer.mainCamera();
         if (smooth && camera.isInitialized()) {
            this.returnFrom = camera.position();
            this.returnYaw = camera.yRot();
            this.returnPitch = camera.xRot();
            this.returnFov = this.drawnFov;
            this.returnStart = System.nanoTime();
            this.returnNanos = Math.max(1L, (long)(Integer)this.returnTime.get()) * 1000000L;
            this.returning = true;
         } else {
            this.endReturn();
         }

      }
   }

   private void turnOffNow() {
      this.instantOff = true;
      this.setEnabled(false);
      this.instantOff = false;
   }

   private void start(LocalPlayer player, ClientLevel level) {
      Camera camera = mc.gameRenderer.mainCamera();
      Vec3 at;
      float startYaw;
      float startPitch;
      if (this.returning && camera.isInitialized()) {
         at = camera.position();
         startYaw = camera.yRot();
         startPitch = camera.xRot();
      } else if ((Boolean)this.keepPosition.get() && this.keptPosition != null && this.keptLevel.get() == level) {
         at = this.keptPosition;
         startYaw = this.keptYaw;
         startPitch = this.keptPitch;
      } else if (camera.isInitialized() && camera.entity() == player) {
         at = camera.position();
         startYaw = camera.yRot();
         startPitch = camera.xRot();
      } else {
         at = player.getEyePosition();
         startYaw = player.getYRot();
         startPitch = player.getXRot();
      }

      this.returning = false;
      this.player = player;
      this.level = level;
      this.position = at;
      this.previous = at;
      this.velocity = Vec3.ZERO;
      this.yaw = startYaw;
      this.pitch = Mth.clamp(startPitch, -90.0F, 90.0F);
      this.playerAt = player.position();
      this.gridOnCamera = false;
      this.health = player.getHealth();
      this.scrollFactor = (double)1.0F;
      this.keysTaken = false;
      this.dropClicks();
      this.perspective = mc.options.getCameraType();
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      this.started = true;
   }

   private void onTick(TickEvent event) {
      LocalPlayer player = mc.player;
      ClientLevel level = mc.level;
      if (player != null && level != null) {
         if (!this.started) {
            this.start(player, level);
         } else if (player != this.player || level != this.level) {
            this.turnOffNow();
            return;
         }

         float health = player.getHealth();
         boolean hurt = health < this.health;
         this.health = health;
         if (hurt && (Boolean)this.disableOnDamage.get() && !player.isCreative()) {
            this.turnOffNow();
         } else {
            ++this.ticks;
            boolean keys = (Integer)this.control.get() == 0;
            if (!keys) {
               this.keysTaken = false;
            }

            this.previous = this.position;
            Vec3 playerNow = player.position();
            if ((Boolean)this.follow.get() && this.playerAt != null) {
               this.position = this.position.add(playerNow.subtract(this.playerAt));
            }

            this.playerAt = playerNow;
            if (!keys) {
               this.velocity = Vec3.ZERO;
            } else if ((Boolean)this.smooth.get()) {
               Vec3 wanted = this.wantedVelocity();
               double share = Mth.clamp((double)(Integer)this.acceleration.get() / (double)100.0F, 0.01, (double)1.0F);
               this.velocity = this.velocity.add(wanted.subtract(this.velocity).scale(share));
               if (wanted.lengthSqr() == (double)0.0F && this.velocity.lengthSqr() < 1.0E-6) {
                  this.velocity = Vec3.ZERO;
               }
            } else {
               this.velocity = this.wantedVelocity();
            }

            Vec3 move = this.velocity;
            if (!(Boolean)this.noclip.get() && move.lengthSqr() > (double)0.0F) {
               AABB box = (new AABB(this.position, this.position)).inflate(0.15);
               Vec3 allowed = Entity.collideBoundingBox(CollisionContext.empty(), move, box, level, List.of());
               this.velocity = new Vec3(allowed.x != move.x ? (double)0.0F : this.velocity.x, allowed.y != move.y ? (double)0.0F : this.velocity.y, allowed.z != move.z ? (double)0.0F : this.velocity.z);
               move = allowed;
            }

            this.position = this.position.add(move);
            this.updateGrid(player);
            if (keys && (Integer)this.interact.get() == 0) {
               this.updateTarget(player);
               this.aimPlayer(player);
            } else {
               this.dropClicks();
            }

         }
      } else {
         this.turnOffNow();
      }
   }

   private Vec3 wantedVelocity() {
      Options options = mc.options;
      float forward = (cameraKey(options.keyUp) ? 1.0F : 0.0F) - (cameraKey(options.keyDown) ? 1.0F : 0.0F);
      float strafe = (cameraKey(options.keyLeft) ? 1.0F : 0.0F) - (cameraKey(options.keyRight) ? 1.0F : 0.0F);
      float vertical = (cameraKey(options.keyJump) ? 1.0F : 0.0F) - (this.sneakHeld() ? 1.0F : 0.0F);
      double multiplier = this.sprintHeld() ? (double)(Integer)this.sprintMultiplier.get() / (double)100.0F : (double)1.0F;
      double horizontalSpeed = this.horizontalSpeed(multiplier) / (double)20.0F;
      double verticalSpeed = Math.min((double)(Integer)this.verticalSpeed.get() * this.scrollFactor * multiplier, (double)500.0F) / (double)20.0F;
      double x = (double)0.0F;
      double z = (double)0.0F;
      if (forward != 0.0F || strafe != 0.0F) {
         double length = Math.sqrt((double)(forward * forward + strafe * strafe));
         double f = (double)forward / length * horizontalSpeed;
         double s = (double)strafe / length * horizontalSpeed;
         double radians = Math.toRadians((double)this.yaw);
         double sin = Math.sin(radians);
         double cos = Math.cos(radians);
         x = s * cos - f * sin;
         z = f * cos + s * sin;
      }

      return new Vec3(x, (double)vertical * verticalSpeed, z);
   }

   private static boolean cameraKey(KeyMapping key) {
      return key.isDown() && !DrivenKeys.driven(key);
   }

   private double horizontalSpeed(double multiplier) {
      return Math.min((double)(Integer)this.speed.get() * this.scrollFactor * multiplier, (double)500.0F);
   }

   private boolean sneakHeld() {
      return (Boolean)mc.options.toggleCrouch().get() ? physicallyDown(mc.options.keyShift) : mc.options.keyShift.isDown();
   }

   private boolean sprintHeld() {
      if ((Boolean)mc.options.toggleSprint().get()) {
         return physicallyDown(mc.options.keySprint);
      } else {
         return VantaClient.MODULES.isEnabled(Sprint.class) ? physicallyDown(mc.options.keySprint) : mc.options.keySprint.isDown();
      }
   }

   private static boolean physicallyDown(KeyMapping key) {
      return mc.gui.screen() == null && KeyBindUtil.isKeyDown(KeyBindUtil.getKeyCode(key));
   }

   private void updateGrid(LocalPlayer player) {
      int distance = mc.options.getEffectiveRenderDistance();
      if (!(Boolean)this.renderAroundCamera.get() && distance > 4) {
         Vec3 eye = player.getEyePosition();
         int dx = Math.abs(SectionPos.blockToSectionCoord(this.position.x) - SectionPos.blockToSectionCoord(eye.x));
         int dz = Math.abs(SectionPos.blockToSectionCoord(this.position.z) - SectionPos.blockToSectionCoord(eye.z));
         int away = Math.max(dx, dz);
         if (this.gridOnCamera) {
            if (away > distance - 4) {
               return;
            }
         } else if (away < distance - 1) {
            return;
         }

         this.gridOnCamera = !this.gridOnCamera;
      } else {
         this.gridOnCamera = true;
      }
   }

   private void dropClicks() {
      this.attackClick = null;
      this.useClick = null;
      this.pickClick = null;
      this.settling = null;
      this.cameraHit = null;
      this.target = null;
      this.outline = null;
      this.stopTurn();
   }

   private void stopTurn() {
      if (this.turning) {
         this.tracker.reset();
         this.turning = false;
      }

      this.turnedTo = null;
   }

   public void clicks() {
      int mode = (Integer)this.interact.get();
      if (mode != 1) {
         Options options = mc.options;
         boolean attack = drain(options.keyAttack);
         boolean use = drain(options.keyUse);
         boolean pick = drain(options.keyPickItem);
         LocalPlayer player = mc.player;
         if (mode != 2 && player != null) {
            if (attack) {
               this.attackClick = this.target == null ? null : new Click(this.target, this.ticks + 15);
            }

            if (use) {
               this.useClick = this.target != null ? new Click(this.target, this.ticks + 15) : this.openAirClick(player);
            }

            if (pick) {
               this.pickClick = this.target == null ? null : new Click(this.target, this.ticks + 15);
            }

            this.attackClick = this.handOver(this.attackClick, options.keyAttack, player);
            this.useClick = this.handOver(this.useClick, options.keyUse, player);
            this.pickClick = this.handOver(this.pickClick, options.keyPickItem, player);
         }
      }
   }

   private static boolean drain(KeyMapping key) {
      boolean clicked;
      for(clicked = false; key.consumeClick(); clicked = true) {
      }

      return clicked;
   }

   private Click handOver(Click click, KeyMapping key, LocalPlayer player) {
      if (click != null && this.ticks <= click.until() && !click.target().gone(player.level())) {
         if (!this.landed(click.target(), player)) {
            return click;
         } else {
            IAccessorKeyMapping mapping = (IAccessorKeyMapping)key;
            mapping.setClickCount(mapping.getClickCount() + 1);
            this.settling = click.target();
            this.settleUntil = this.ticks + 15;
            return null;
         }
      } else {
         return null;
      }
   }

   public boolean held(KeyMapping key, boolean down) {
      if (down && DrivenKeys.driven(key)) {
         return true;
      } else {
         int mode = (Integer)this.interact.get();
         if (mode == 2) {
            return false;
         } else if (mode != 1 && down) {
            LocalPlayer player = mc.player;
            if (player == null) {
               return false;
            } else if (key == mc.options.keyUse && player.isUsingItem()) {
               return true;
            } else {
               return this.target != null && this.landed(this.target, player);
            }
         } else {
            return down;
         }
      }
   }

   private boolean landed(Target target, LocalPlayer player) {
      HitResult hit = mc.hitResult;
      if (target.entity() != null) {
         boolean var6;
         if (hit instanceof EntityHitResult) {
            EntityHitResult entityHit = (EntityHitResult)hit;
            if (entityHit.getEntity() == target.entity()) {
               var6 = true;
               return var6;
            }
         }

         var6 = false;
         return var6;
      } else if (target.pos() != null) {
         boolean var10000;
         if (hit instanceof BlockHitResult) {
            BlockHitResult blockHit = (BlockHitResult)hit;
            if (blockHit.getType() == Type.BLOCK && blockHit.getBlockPos().equals(target.pos()) && blockHit.getDirection() == target.face()) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      } else {
         return (hit == null || hit.getType() == Type.MISS) && Rotation.of(player).distanceTo(target.goal(player.getEyePosition(), 1.0F)) <= 3.0F;
      }
   }

   private Click openAirClick(LocalPlayer player) {
      Target point = this.pointed(player);
      Vec3 eye = player.getEyePosition();
      return pick(player, eye, eye.add(point.goal(eye, 1.0F).direction())) != null ? null : new Click(point, this.ticks + 15);
   }

   private Target pointed(LocalPlayer player) {
      BlockHitResult hit = player.level().clip(new ClipContext(this.position, this.position.add(this.look().scale((double)128.0F)), Block.OUTLINE, Fluid.NONE, player));
      return hit.getType() == Type.MISS ? new Target((BlockPos)null, (Direction)null, (Entity)null, (Vec3)null, new Rotation(this.yaw, this.pitch)) : new Target((BlockPos)null, (Direction)null, (Entity)null, hit.getLocation(), (Rotation)null);
   }

   private Vec3 look() {
      return Vec3.directionFromRotation(this.pitch, this.yaw);
   }

   private void updateTarget(LocalPlayer player) {
      this.cameraHit = null;
      this.target = null;
      this.outline = null;
      Vec3 eye = player.getEyePosition();
      Vec3 look = this.look();
      double reach = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
      double far = exitDistance(this.position, look, eye, reach + 1.7320508);
      if (!(far <= (double)0.0F)) {
         Level level = player.level();
         BlockHitResult block = level.clip(new ClipContext(this.position, this.position.add(look.scale(far)), Block.OUTLINE, Fluid.NONE, player));
         double distance = block.getType() == Type.MISS ? far : block.getLocation().distanceTo(this.position);
         Vec3 end = this.position.add(look.scale(distance));
         EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, this.position, end, (new AABB(this.position, end)).inflate((double)1.0F), EntitySelector.CAN_BE_PICKED, distance * distance);
         if (entityHit != null) {
            Entity entity = entityHit.getEntity();
            this.cameraHit = entityHit;
            this.target = reachable(player, eye, entity, entity.getBoundingBox().inflate((double)entity.getPickRadius()), entityHit.getLocation(), (BlockHitResult)null);
         } else if (block.getType() == Type.BLOCK) {
            this.cameraHit = block;
            BlockPos pos = block.getBlockPos();
            VoxelShape shape = level.getBlockState(pos).getShape(level, pos, CollisionContext.of(player));
            if (!shape.isEmpty()) {
               this.target = reachable(player, eye, (Entity)null, shape.bounds().move(pos), block.getLocation(), block);
               this.outline = this.target != null ? block : null;
            }
         }

      }
   }

   private static Target reachable(LocalPlayer player, Vec3 eye, Entity entity, AABB box, Vec3 hit, BlockHitResult block) {
      Direction face = block != null ? block.getDirection() : faceOf(box, hit);

      for(int i = 0; i < 6; ++i) {
         Vec3 point = aimPoint(i, box, face, hit);
         HitResult result = pick(player, eye, point);
         if (entity != null) {
            if (result instanceof EntityHitResult) {
               EntityHitResult entityHit = (EntityHitResult)result;
               if (entityHit.getEntity() == entity) {
                  return new Target((BlockPos)null, (Direction)null, entity, point.subtract(entity.position()), (Rotation)null);
               }
            }
         } else if (result instanceof BlockHitResult) {
            BlockHitResult blockHit = (BlockHitResult)result;
            if (blockHit.getBlockPos().equals(block.getBlockPos()) && blockHit.getDirection() == face) {
               return new Target(block.getBlockPos(), face, (Entity)null, point, (Rotation)null);
            }
         }
      }

      return null;
   }

   private static Vec3 aimPoint(int i, AABB box, Direction face, Vec3 hit) {
      Direction.Axis axis = face.getAxis();
      double inset = Math.min(0.12, (box.max(axis) - box.min(axis)) / (double)2.0F);
      double plane = face.getAxisDirection() == AxisDirection.POSITIVE ? box.max(axis) : box.min(axis);
      double depth = (i == 0 ? hit.get(axis) : plane) - (double)face.getAxisDirection().getStep() * inset;
      if (i == 0) {
         return hit.with(axis, depth);
      } else {
         Vec3 point = box.getCenter().with(axis, depth);
         if (i == 1) {
            return point;
         } else {
            Direction.Axis u = axis == Axis.X ? Axis.Y : Axis.X;
            Direction.Axis v = axis == Axis.Z ? Axis.Y : Axis.Z;
            double du = (box.max(u) - box.min(u)) / (double)4.0F * ((i & 1) == 0 ? (double)-1.0F : (double)1.0F);
            double dv = (box.max(v) - box.min(v)) / (double)4.0F * (i < 4 ? (double)-1.0F : (double)1.0F);
            return point.with(u, point.get(u) + du).with(v, point.get(v) + dv);
         }
      }
   }

   private static Direction faceOf(AABB box, Vec3 hit) {
      Direction nearest = Direction.UP;
      double best = Double.MAX_VALUE;

      for(Direction direction : DIRECTIONS) {
         Direction.Axis axis = direction.getAxis();
         double plane = direction.getAxisDirection() == AxisDirection.POSITIVE ? box.max(axis) : box.min(axis);
         double off = Math.abs(hit.get(axis) - plane);
         if (off < best) {
            best = off;
            nearest = direction;
         }
      }

      return nearest;
   }

   private static HitResult pick(LocalPlayer player, Vec3 eye, Vec3 point) {
      Vec3 direction = point.subtract(eye).normalize();
      double blockRange = player.blockInteractionRange();
      double entityRange = player.entityInteractionRange();
      double distance = Math.max(blockRange, entityRange);
      BlockHitResult block = player.level().clip(new ClipContext(eye, eye.add(direction.scale(distance)), Block.OUTLINE, Fluid.NONE, player));
      double blockSq = block.getLocation().distanceToSqr(eye);
      if (block.getType() != Type.MISS) {
         distance = Math.sqrt(blockSq);
      }

      Vec3 to = eye.add(direction.scale(distance));
      AABB area = player.getBoundingBox().expandTowards(direction.scale(distance)).inflate((double)1.0F);
      EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, to, area, EntitySelector.CAN_BE_PICKED, distance * distance);
      if (entity != null && entity.getLocation().distanceToSqr(eye) < blockSq) {
         return entity.getLocation().closerThan(eye, entityRange) ? entity : null;
      } else {
         return block.getType() == Type.BLOCK && block.getLocation().closerThan(eye, blockRange) ? block : null;
      }
   }

   private static double exitDistance(Vec3 from, Vec3 direction, Vec3 centre, double radius) {
      Vec3 toCentre = centre.subtract(from);
      double along = toCentre.dot(direction);
      double asideSq = toCentre.lengthSqr() - along * along;
      double radiusSq = radius * radius;
      return asideSq > radiusSq ? (double)0.0F : Math.max((double)0.0F, along + Math.sqrt(radiusSq - asideSq));
   }

   private void aimPlayer(LocalPlayer player) {
      Target aim = this.aimTarget(player);
      if (aim == null) {
         this.stopTurn();
      } else {
         this.turning = true;
         this.tracker.rampUp(80.0F);
         VantaClient.ROTATIONS.aim((RotationTarget)((partialTick) -> this.turnFrame(player, aim, partialTick)), 2, SmoothTracker.CONFIG);
      }
   }

   private Rotation turnFrame(LocalPlayer player, Target aim, float partialTick) {
      Rotation view = Rotation.of(player);
      Rotation from = this.turnedTo != null && this.turnedTo.distanceTo(view) <= mouseStep() ? this.turnedTo : view;
      this.turnedTo = this.tracker.step(from, aim.goal(player.getEyePosition(partialTick), partialTick), (Rotation)null, 80.0F, partialTick);
      return this.turnedTo;
   }

   private static float mouseStep() {
      float factor = (float)((Double)mc.options.sensitivity().get() * 0.6 + 0.2);
      return factor * factor * factor * 1.2F;
   }

   private Target aimTarget(LocalPlayer player) {
      Click latest = this.later(this.later(this.attackClick, this.useClick), this.pickClick);
      if (latest != null) {
         return latest.target();
      } else {
         Options options = mc.options;
         if (player.isUsingItem() && options.keyUse.isDown()) {
            return this.pointed(player);
         } else if ((options.keyAttack.isDown() || options.keyUse.isDown()) && this.target != null) {
            return this.target;
         } else if (this.settling != null && this.ticks <= this.settleUntil && Rotation.of(player).distanceTo(this.settling.goal(player.getEyePosition(), 1.0F)) > Math.max(0.5F, mouseStep())) {
            return this.settling;
         } else {
            this.settling = null;
            return null;
         }
      }
   }

   private Click later(Click a, Click b) {
      Click first = a != null && a.until() >= this.ticks ? a : null;
      Click second = b != null && b.until() >= this.ticks ? b : null;
      if (first != null && second != null) {
         return second.until() >= first.until() ? second : first;
      } else {
         return first != null ? first : second;
      }
   }

   public Vec3 testPosition() {
      return this.position;
   }

   public void testLook(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = Mth.clamp(pitch, -90.0F, 90.0F);
   }

   public void testPlace(Vec3 at, float yaw, float pitch) {
      this.position = at;
      this.previous = at;
      this.velocity = Vec3.ZERO;
      this.testLook(yaw, pitch);
   }

   public String testAim() {
      return this.attackClick == null && this.useClick == null && this.pickClick == null ? (this.settling != null ? "settle" : (this.turning ? "held" : "-")) : "click";
   }

   public String testTarget() {
      HitResult hit = this.cameraHit;
      String var10000;
      if (hit instanceof BlockHitResult block) {
         var10000 = "block " + block.getBlockPos().toShortString() + " " + String.valueOf(block.getDirection());
      } else if (hit instanceof EntityHitResult entity) {
         var10000 = "entity " + entity.getEntity().getType().toShortString();
      } else {
         var10000 = "nothing in reach";
      }

      String seen = var10000;
      Target target = this.target;
      if (target == null) {
         return seen + ", not reachable";
      } else {
         Vec3 aim = target.entity() != null ? target.entity().position().add(target.aim()) : target.aim();
         return seen + String.format(Locale.ROOT, ", reachable, aim %.3f %.3f %.3f", aim.x, aim.y, aim.z);
      }
   }

   private static record Target(BlockPos pos, Direction face, Entity entity, Vec3 aim, Rotation look) {
      Rotation goal(Vec3 eye, float partialTick) {
         if (this.entity != null) {
            return Rotation.toward(eye, this.entity.getPosition(partialTick).add(this.aim));
         } else {
            return this.aim != null ? Rotation.toward(eye, this.aim) : this.look;
         }
      }

      boolean gone(Level level) {
         if (this.entity != null) {
            return !this.entity.isAlive();
         } else {
            return this.pos != null && level.getBlockState(this.pos).isAir();
         }
      }
   }

   private static record Click(Target target, int until) {
   }
}
