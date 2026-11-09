package vanta.module.impl.combat;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import vanta.VantaClient;
import vanta.event.events.AttackEvent;
import vanta.event.events.PreTickEvent;
import vanta.event.events.TickEvent;
import vanta.mixin.accessor.LocalPlayerAccessor;
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
import vanta.setting.ItemSetting;
import vanta.setting.KeySetting;
import vanta.setting.Measure;
import vanta.setting.RangeSetting;
import vanta.setting.SliderSetting;
import vanta.setting.TargetSettings;
import vanta.util.Clicks;
import vanta.util.DrivenKeys;
import vanta.util.Hotbar;
import vanta.util.Humanize;
import vanta.util.KeyBindUtil;
import vanta.util.Maths;
import vanta.util.Shields;
import vanta.util.SwapBack;
import vanta.util.Weapons;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class AutoMace extends Module {
   private static final String[] PREFER = new String[]{"Auto", "Density", "Breach"};
   private static final String[] STUN = new String[]{"Off", "Auto", "Bind"};
   private static final int STUN_AUTO = 1;
   private static final boolean TRACE = System.getenv("MACE_TEST") != null;
   private static final int STUN_BIND = 2;
   private static final int ARMED_TICKS = 60;
   private static final int HIT_AIM_PRIORITY = 9;
   private static final double HIT_AIM_INSET = 0.1;
   private static final int LOOKAHEAD = 160;
   private static final double TRACK_RANGE = (double)12.0F;
   private static final double FALL_SEARCH = (double)320.0F;
   private static final int TARGET_LEAD = 6;
   private static final int LOST_TICKS = 3;
   private static final int STUN_RETRY = 20;
   private static final int STUN_GAP = 20;
   private static final double COVER_SLACK = (double)20.0F;
   private static final int CROUCH_LEAD = 3;
   private static final double CROUCH_FALL = (double)1.5F;
   private static final int SPEAR_LEAD = 4;
   private static final int LONG_AGO = 1000;
   private static final int LANDING_LOOKAHEAD = 100;
   private static final String[] DRAIN_BIND = new String[]{"Hold", "Toggle"};
   private static final int DRAIN_TOGGLE = 1;
   private final KeySetting drainKey = (KeySetting)this.add(new KeySetting("drainKey"));
   private final EnumSetting drainBind;
   private final RangeSetting drainClicks;
   private final SliderSetting drainFrom;
   private final ItemSetting onlyWith;
   private final SliderSetting smashFallSetting;
   private final EnumSetting prefer;
   private final BoolSetting autoAttack;
   private final EnumSetting stunSlam;
   private final RangeSetting axeDelay;
   private final RangeSetting stunDelay;
   private final RangeSetting maceDelay;
   private final RangeSetting reaction;
   private final BoolSetting spearReach;
   private static final String[] SPEAR_USE = new String[]{"Every slam", "Stun slams only"};
   private static final int SPEAR_STUN_ONLY = 1;
   private final EnumSetting spearUse;
   private static final String[] HIT_AIM = new String[]{"Legit", "Silent", "Off"};
   private static final int AIM_LEGIT = 0;
   private static final int AIM_SILENT = 1;
   private static final int LEGIT_AIM_PRIORITY = 1;
   private static final int LEGIT_LEAD = 10;
   private final EnumSetting hitAim;
   private final SliderSetting aimSpeed;
   private final SliderSetting aimFov;
   private final SmoothTracker tracker;
   private LivingEntity legitTarget;
   private final RangeSetting swapBack;
   private final TargetSettings targets;
   private final SwapBack swap;
   private LivingEntity fallTarget;
   private int reachAt;
   private double reachFall;
   private LivingEntity lastHit;
   private int lastHitAt;
   private LivingEntity stunned;
   private int stunnedAt;
   private int stunWait;
   private LivingEntity axeTarget;
   private int axeWait;
   private int axeReadyAt;
   private int maceLead;
   private LivingEntity lastStun;
   private int lastStunAt;
   private LivingEntity planned;
   private int plannedAt;
   private int plannedWait;
   private LivingEntity seen;
   private int seenReadyAt;
   private LivingEntity drained;
   private int drainedAt;
   private int drainHits;
   private boolean drainToggled;
   private boolean drainKeyWas;
   private boolean drainArmed;
   private boolean followUp;
   private int armedUntil;
   private final Map<Integer, Integer> raisedSince;
   private Set<Integer> inView;
   private final Set<KeyMapping> heldKeys;
   private double serverFall;
   private double lastY;
   private boolean fallCanSmash;
   private int lostTicks;
   private int attackedTick;
   private Entity attacked;
   private LocalPlayer stampsOf;
   private static final boolean DRAIN_TRACE = System.getenv("FROST_DRAINTRACE") != null;
   private double lastReachFall;

   public AutoMace() {
      super("autoMace");
      this.drainBind = (EnumSetting)this.add(new EnumSetting("drainBind", 0, DRAIN_BIND));
      this.drainClicks = (RangeSetting)this.add(new RangeSetting("drainClicks", 3, 3, 1, 10, Measure.NONE));
      this.drainFrom = (SliderSetting)this.add((new SliderSetting("drainFrom", 200, 15, 300, Measure.TENTH_BLOCKS)).uncapped());
      this.onlyWith = (ItemSetting)this.add(new ItemSetting("onlyWith", new String[0]));
      this.smashFallSetting = (SliderSetting)this.add((new SliderSetting("minFall", 15, 15, 300, Measure.TENTH_BLOCKS)).uncapped());
      this.prefer = (EnumSetting)this.add(new EnumSetting("prefer", 0, PREFER));
      this.autoAttack = (BoolSetting)this.add(new BoolSetting("autoAttack", true));
      this.stunSlam = (EnumSetting)this.add(new EnumSetting("stunSlam", 1, STUN));
      this.axeDelay = (RangeSetting)this.add(new RangeSetting("axeDelay", 0, 0, 0, 10, Measure.TICKS));
      this.stunDelay = (RangeSetting)this.add(new RangeSetting("stunDelay", 1, 1, 1, 10, Measure.TICKS));
      this.maceDelay = (RangeSetting)this.add(new RangeSetting("maceDelay", 0, 0, 0, 10, Measure.TICKS));
      this.reaction = (RangeSetting)this.add(new RangeSetting("reaction", 0, 0, 0, 10, Measure.TICKS));
      this.spearReach = (BoolSetting)this.add(new BoolSetting("spearReach", false));
      this.spearUse = (EnumSetting)this.add(new EnumSetting("spearUse", 0, SPEAR_USE));
      this.hitAim = (EnumSetting)this.add(new EnumSetting("hitAim", 0, HIT_AIM));
      this.aimSpeed = (SliderSetting)this.add((new SliderSetting("aimSpeed", 85, 0, 100, Measure.PERCENT)).uncapped());
      this.aimFov = (SliderSetting)this.add(new SliderSetting("aimFov", 70, 10, 360, Measure.DEGREES));
      this.tracker = new SmoothTracker(1.0F, 0.75F);
      this.swapBack = (RangeSetting)this.add(new RangeSetting("swapBack", 1, 3, 1, 10, Measure.TICKS));
      this.targets = (TargetSettings)this.add(new TargetSettings());
      this.swap = new SwapBack(3);
      this.reachAt = -1;
      this.lastHitAt = -1073741824;
      this.stunWait = 1;
      this.axeReadyAt = Integer.MIN_VALUE;
      this.lastStunAt = -1073741824;
      this.plannedAt = Integer.MIN_VALUE;
      this.drainedAt = -1073741824;
      this.armedUntil = Integer.MIN_VALUE;
      this.raisedSince = new HashMap();
      this.inView = new HashSet();
      this.heldKeys = new HashSet();
      this.lastY = Double.NaN;
      this.attackedTick = Integer.MIN_VALUE;
      this.onlyWith.describe();
      this.smashFallSetting.describe();
      this.prefer.describe();
      this.autoAttack.describe();
      this.stunSlam.describe();
      this.axeDelay.describe();
      this.stunDelay.describe();
      this.maceDelay.describe();
      this.reaction.describe();
      this.spearReach.describe();
      this.spearUse.describe();
      EnumSetting var10000 = this.spearUse;
      BoolSetting var10001 = this.spearReach;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.swapBack.describe();
      this.stunDelay.visibleWhen(() -> (Integer)this.stunSlam.get() != 0);
      this.axeDelay.visibleWhen(() -> (Integer)this.stunSlam.get() != 0);
      this.maceDelay.visibleWhen(() -> (Integer)this.stunSlam.get() != 0);
      this.hitAim.describe();
      this.aimSpeed.describe();
      this.aimFov.describe();
      this.aimSpeed.visibleWhen(() -> (Integer)this.hitAim.get() == 0);
      this.aimFov.visibleWhen(() -> (Integer)this.hitAim.get() != 2);
      this.drainKey.describe();
      this.drainBind.describe();
      this.drainClicks.describe();
      this.drainFrom.describe();
      var10000 = this.drainBind;
      KeySetting var4 = this.drainKey;
      Objects.requireNonNull(var4);
      var10000.visibleWhen(var4::bound);
      RangeSetting var2 = this.drainClicks;
      var4 = this.drainKey;
      Objects.requireNonNull(var4);
      var2.visibleWhen(var4::bound);
      SliderSetting var3 = this.drainFrom;
      var4 = this.drainKey;
      Objects.requireNonNull(var4);
      var3.visibleWhen(var4::bound);
   }

   public String info() {
      if (this.drainArmed) {
         return "Drain";
      } else {
         String var10000;
         if ((Integer)this.stunSlam.get() == 0) {
            var10000 = PREFER[(Integer)this.prefer.get()];
         } else {
            var10000 = STUN;
            var10000 = "Stun " + var10000[(Integer)this.stunSlam.get()];
         }

         return var10000;
      }
   }

   public boolean activatable() {
      return (Integer)this.stunSlam.get() == 2;
   }

   protected void onActivate() {
      if (mc.player != null) {
         if ((Integer)this.stunSlam.get() == 2) {
            this.armedUntil = mc.player.tickCount + 60;
         }

      }
   }

   protected void onEnable() {
      this.listen(AttackEvent.class, this::onAttack);
      this.listen(PreTickEvent.class, this::onTick);
      this.listen(TickEvent.class, this::onTickEnd);
   }

   protected void onDisable() {
      this.swap.cancel(mc.player);
      this.followUp = false;
      this.keysBack();
      this.reset();
   }

   private void reset() {
      this.stunned = null;
      this.axeTarget = null;
      this.drained = null;
      this.drainHits = 0;
      this.drainedAt = -1073741824;
      this.fallTarget = null;
      this.reachAt = -1;
      this.lostTicks = 0;
      this.lastHit = null;
      this.lastHitAt = -1073741824;
      this.lastStun = null;
      this.lastStunAt = -1073741824;
      this.planned = null;
      this.plannedAt = Integer.MIN_VALUE;
      this.armedUntil = Integer.MIN_VALUE;
      this.attackedTick = Integer.MIN_VALUE;
      this.attacked = null;
      this.raisedSince.clear();
      this.inView = new HashSet();
      this.tracker.reset();
      this.legitTarget = null;
      this.serverFall = (double)0.0F;
      this.lastY = Double.NaN;
      this.fallCanSmash = false;
   }

   private double minFall() {
      return (double)(Integer)this.smashFallSetting.get() / (double)10.0F;
   }

   private static boolean airborne(LocalPlayer player) {
      return !player.onGround() && !player.isFallFlying() && !player.getAbilities().flying && !player.isInWater() && !player.onClimbable();
   }

   private boolean smashing(LocalPlayer player) {
      return this.serverFall >= this.minFall() && this.serverFall > (double)1.5F && airborne(player);
   }

   private void trackFall(LocalPlayer player) {
      double y = player.getY();
      if (Double.isNaN(this.lastY)) {
         this.serverFall = (double)0.0F;
      } else if (!player.onGround() && !(y > this.lastY) && !(this.lastY - y > (double)4.0F) && !(Math.abs(player.yo - this.lastY) > 1.0E-7)) {
         this.serverFall += this.lastY - y;
      } else {
         this.serverFall = (double)0.0F;
      }

      this.serverFall = Math.min(this.serverFall, player.fallDistance);
      this.lastY = y;
   }

   private boolean canSmash(LocalPlayer player) {
      if (!player.hasEffect(MobEffects.SLOW_FALLING) && !player.hasEffect(MobEffects.LEVITATION)) {
         double needed = Math.max(this.minFall(), 1.501);
         Vec3 motion = player.getDeltaMovement();
         double fall = this.serverFall;
         AABB box = player.getBoundingBox();

         for(int tick = 0; tick < 100; ++tick) {
            if (fall >= needed) {
               return true;
            }

            Vec3 allowed = player.level().noCollision(player, box.move(motion)) ? motion : Entity.collideBoundingBox(player, motion, box, player.level(), player.level().getEntityCollisions(player, box.expandTowards(motion)));
            if (motion.y < (double)0.0F && allowed.y > motion.y + 1.0E-7) {
               return false;
            }

            box = box.move(allowed);
            fall = allowed.y > (double)0.0F ? (double)0.0F : fall - allowed.y;
            motion = new Vec3(allowed.x * 0.91, (allowed.y - 0.08) * 0.98, allowed.z * 0.91);
         }

         return true;
      } else {
         return false;
      }
   }

   private ItemStack held(LocalPlayer player) {
      int slot = this.swap.active() ? this.swap.origin() : Hotbar.selected(player);
      return player.getInventory().getItem(slot);
   }

   private boolean allowed(LocalPlayer player) {
      return this.onlyWith.contains(this.held(player), true);
   }

   private void onTick(PreTickEvent event) {
      Minecraft client = event.client();
      LocalPlayer player = client.player;
      if (player != this.stampsOf) {
         this.stampsOf = player;
         this.reset();
      }

      this.swap.tick(player);
      this.pollDrainKey(client);
      this.watchShields(player);
      this.keysBack();
      if (player != null && client.gui.screen() == null && !player.isUsingItem()) {
         this.fallCanSmash = airborne(player) && (player.getDeltaMovement().y < 0.1 || this.fallTarget != null) && this.canSmash(player);
         if (this.fallCanSmash && this.allowed(player)) {
            this.track(player);
            this.crouchForStun(player);
            this.spearForStun(player);
            this.axeForStun(player);
         } else {
            this.axeTarget = null;
            this.fallTarget = null;
            this.reachAt = -1;
            this.lostTicks = 0;
         }

         ((MinecraftAccessor)client).client$pick(1.0F);
         if (this.stunned != null) {
            int since = player.tickCount - this.stunnedAt;
            if (since < this.stunWait) {
               if (this.maceLead > 0 && since >= this.stunWait - this.maceLead && this.stunned.isAlive() && !Hotbar.actedThisTick()) {
                  int mace = this.maceSlot(player, this.stunned);
                  if (mace >= 0 && mace != Hotbar.selected(player)) {
                     this.swap.select(player, mace, SwapBack.ticks(this.swapBack.low(), this.swapBack.high()) + this.maceLead + 20);
                  }
               }

               return;
            }

            LivingEntity target = this.stunned;
            if (since <= this.stunWait + 20 && target.isAlive()) {
               if (this.underAim(client, target) && !Hotbar.actedThisTick() && client.missTime <= 0 && !player.isHandsBusy()) {
                  int mace = this.maceSlot(player, target);
                  int back = SwapBack.ticks(this.swapBack.low(), this.swapBack.high());
                  if (mace < 0) {
                     this.stunned = null;
                  } else if (mace == Hotbar.selected(player) || this.swap.select(player, mace, back)) {
                     ((MinecraftAccessor)client).client$pick(1.0F);
                     if (this.underAim(client, target)) {
                        Hotbar.sendPickWithNext(player);
                        this.followUp = true;

                        try {
                           Clicks.click(0);
                        } finally {
                           this.followUp = false;
                        }

                        if (this.attackedNow(player, target)) {
                           this.stunned = null;
                           this.booked(player, target);
                           this.swap.backIn(back);
                        }
                     }
                  }
               }

               return;
            }

            this.stunned = null;
         }

         boolean smash = this.smashing(player) && this.allowed(player) && player.getDeltaMovement().y < (double)0.0F;
         if (TRACE) {
            HitResult seen = LocalPlayerAccessor.client$pick(player, player.blockInteractionRange(), player.entityInteractionRange(), 1.0F);
            PrintStream var10000 = System.out;
            int var10001 = player.tickCount;
            boolean var10003 = this.smashing(player);
            boolean var10004 = this.allowed(player);
            double var10005 = player.getDeltaMovement().y;
            double var10006 = this.serverFall;
            boolean var10007 = Hotbar.actedThisTick();
            int var10008 = client.missTime;
            boolean var10009 = player.isHandsBusy();
            Object var10010;
            if (seen instanceof EntityHitResult) {
               EntityHitResult h = (EntityHitResult)seen;
               var10010 = h.getEntity().getType().toShortString();
            } else {
               var10010 = seen.getType();
            }

            var10000.println("[AM] t" + var10001 + " smash " + smash + " (smashing " + var10003 + " allowed " + var10004 + " vy " + var10005 + " serverFall " + var10006 + ") acted " + var10007 + " miss " + var10008 + " busy " + var10009 + " under " + String.valueOf(var10010) + " stunned " + String.valueOf(this.stunned) + " swap " + this.swap.active());
         }

         if ((Boolean)this.autoAttack.get() && !Hotbar.actedThisTick()) {
            if (client.missTime <= 0 && !player.isHandsBusy()) {
               double spear = this.spearRange(player);
               HitResult under = LocalPlayerAccessor.client$pick(player, player.blockInteractionRange(), spear > (double)0.0F ? spear : player.entityInteractionRange(), 1.0F);
               if (under instanceof EntityHitResult) {
                  EntityHitResult hit = (EntityHitResult)under;
                  Entity var10 = hit.getEntity();
                  if (var10 instanceof LivingEntity) {
                     LivingEntity target = (LivingEntity)var10;
                     if (this.targets.accepts(player, target)) {
                        if (target != this.seen) {
                           this.seen = target;
                           this.seenReadyAt = player.tickCount + Humanize.ticks(this.reaction);
                        }

                        if (player.tickCount < this.seenReadyAt) {
                           return;
                        }

                        int maceSlot = this.maceSlot(player, target);
                        if (maceSlot < 0) {
                           return;
                        }

                        boolean drain = smash && this.drains(player, target);
                        boolean stun = smash && !drain && this.stunned == null && this.stunWait(player, target) > 0;
                        if (stun || drain || smash && (target != this.lastHit || player.tickCount - this.lastHitAt >= 10)) {
                           int weapon = stun ? axeSlot(player) : maceSlot;
                           int back = SwapBack.ticks(this.swapBack.low(), this.swapBack.high());
                           if (weapon != Hotbar.selected(player) && !this.swap.select(player, weapon, stun ? back + this.stunHold() : back)) {
                              return;
                           }

                           if (stun) {
                              this.planAxe(target);
                              if (this.axeReadyAt == Integer.MIN_VALUE) {
                                 this.axeReadyAt = player.tickCount + this.axeWait;
                              }

                              if (player.tickCount < this.axeReadyAt) {
                                 return;
                              }
                           } else {
                              this.axeTarget = null;
                           }

                           if (spear > (double)0.0F) {
                              client.hitResult = hit;
                           } else {
                              ((MinecraftAccessor)client).client$pick(1.0F);
                           }

                           if (this.underAim(client, target)) {
                              Hotbar.sendPickWithNext(player);
                              if (drain) {
                                 this.drainBurst(client, player, target);
                                 if (this.attackedNow(player, target)) {
                                    this.swap.backIn(back + 1);
                                 }

                                 return;
                              }

                              Clicks.click(0);
                              if (!stun && this.attackedNow(player, target)) {
                                 this.booked(player, target);
                                 this.swap.backIn(back);
                              }
                           }

                           return;
                        }

                        return;
                     }
                  }
               }

               this.seen = null;
            }
         }
      } else {
         this.stunned = null;
         this.fallTarget = null;
         this.fallCanSmash = false;
      }
   }

   private void drainHit(LocalPlayer player, LivingEntity target) {
      if (target != this.drained) {
         this.drained = target;
         this.drainHits = 0;
      }

      ++this.drainHits;
      this.drainedAt = player.tickCount;
      if (TRACE || DRAIN_TRACE) {
         ItemStack shield = target.getItemInHand(target.getUsedItemHand());
         System.out.println(String.format(Locale.ROOT, "[Vanta V1] drain hit %d on %s at t%d: fall %.2f, shield %d/%d, %.0f a hit", this.drainHits, target.getName().getString(), player.tickCount, this.serverFall, shield.getMaxDamage() - shield.getDamageValue(), shield.getMaxDamage(), this.drainPerHit(player, target, this.serverFall)));
      }

   }

   private boolean drains(LocalPlayer player, LivingEntity target) {
      if (this.drainArmed && !(this.serverFall < (double)(Integer)this.drainFrom.get() / (double)10.0F) && this.maceSlot(player, target) >= 0 && this.blocking(player, target)) {
         ItemStack shield = target.getItemInHand(target.getUsedItemHand());
         BlocksAttacks blocks = (BlocksAttacks)shield.get(DataComponents.BLOCKS_ATTACKS);
         return blocks != null && Shields.covers(target, blocks, player.position(), player.getDeltaMovement(), (double)20.0F);
      } else {
         return false;
      }
   }

   public void testDrain(boolean on) {
      this.drainToggled = on;
   }

   private void pollDrainKey(Minecraft client) {
      boolean down = this.drainKey.down(client);
      if (!this.drainKey.bound()) {
         this.drainToggled = false;
      } else if (down && !this.drainKeyWas && (Integer)this.drainBind.get() == 1) {
         this.drainToggled = !this.drainToggled;
      }

      boolean var10001;
      label34: {
         label33: {
            this.drainKeyWas = down;
            if (this.drainKey.bound()) {
               if ((Integer)this.drainBind.get() == 1) {
                  if (this.drainToggled) {
                     break label33;
                  }
               } else if (down) {
                  break label33;
               }
            }

            var10001 = false;
            break label34;
         }

         var10001 = true;
      }

      this.drainArmed = var10001;
   }

   private void drainBurst(Minecraft client, LocalPlayer player, LivingEntity target) {
      int clicks = Humanize.ticks(this.drainClicks);
      int landed = 0;

      for(int i = 0; i < clicks; ++i) {
         if (i > 0) {
            ((MinecraftAccessor)client).client$pick(1.0F);
            if (!this.underAim(client, target) || player.isUsingItem() || player.isHandsBusy() || client.missTime > 0) {
               break;
            }
         }

         this.attackedTick = Integer.MIN_VALUE;
         Clicks.click(0);
         if (this.attackedNow(player, target)) {
            ++landed;
            this.drainHit(player, target);
         }
      }

      if (TRACE || DRAIN_TRACE) {
         System.out.println("[Vanta V1] drain burst at t" + player.tickCount + ": " + landed + "/" + clicks + " attacks this tick");
      }

      if (landed > 0) {
         this.attackedTick = player.tickCount;
         this.attacked = target;
      }

   }

   private double drainPerHit(LocalPlayer player, LivingEntity target, double fall) {
      int mace = this.maceSlot(player, target, fall);
      if (mace >= 0 && !(fall <= (double)1.5F)) {
         ItemStack stack = player.getInventory().getItem(mace);
         double smash = fall <= (double)3.0F ? (double)4.0F * fall : (fall <= (double)8.0F ? (double)12.0F + (double)2.0F * (fall - (double)3.0F) : (double)22.0F + fall - (double)8.0F);
         double charge = (double)player.getAttackStrengthScale(0.5F);
         double base = (double)6.0F * (0.2 + charge * charge * 0.8);
         double damage = base + smash + (double)0.5F * (double)Weapons.level(stack, Enchantments.DENSITY) * fall;
         return damage < (double)3.0F ? (double)0.0F : (double)1.0F + Math.floor(damage);
      } else {
         return (double)0.0F;
      }
   }

   private boolean attackedNow(LocalPlayer player, LivingEntity target) {
      return this.attackedTick == player.tickCount && this.attacked == target;
   }

   private void booked(LocalPlayer player, LivingEntity target) {
      this.lastHit = target;
      this.lastHitAt = player.tickCount;
      this.serverFall = (double)0.0F;
   }

   private void track(LocalPlayer player) {
      LivingEntity best = null;
      int bestTicks = Integer.MAX_VALUE;
      double bestDistance = Double.MAX_VALUE;
      boolean kept = false;
      int keptTicks = -1;

      for(LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, searchBox(player))) {
         if (entity != player && this.targets.accepts(player, entity) && visible(player, entity)) {
            int ticks = this.ticksToReach(player, entity);
            if (entity == this.fallTarget) {
               kept = true;
               keptTicks = ticks;
            }

            double distance = entity.distanceToSqr(player);
            int rank = ticks < 0 ? 2147483646 : ticks;
            if (rank < bestTicks || rank == bestTicks && distance < bestDistance) {
               best = entity;
               bestTicks = rank;
               bestDistance = distance;
            }
         }
      }

      if (kept) {
         this.lostTicks = keptTicks >= 0 ? 0 : this.lostTicks + 1;
         if (keptTicks >= 0 || this.lostTicks < 3 || bestTicks == 2147483646) {
            best = this.fallTarget;
         }
      } else {
         this.lostTicks = 0;
      }

      if (best != this.fallTarget) {
         this.lostTicks = 0;
      }

      this.fallTarget = best;
      int ticks = best == null ? -1 : this.ticksToReach(player, best);
      this.reachAt = ticks < 0 ? -1 : player.tickCount + ticks;
      this.reachFall = ticks < 0 ? this.serverFall : this.lastReachFall;
   }

   private static AABB searchBox(LocalPlayer player) {
      return player.getBoundingBox().inflate((double)12.0F).expandTowards((double)0.0F, (double)-320.0F, (double)0.0F);
   }

   private static boolean visible(LocalPlayer player, LivingEntity entity) {
      if (player.hasLineOfSight(entity)) {
         return true;
      } else {
         Vec3 eye = player.getEyePosition();

         for(Vec3 point : new Vec3[]{entity.getBoundingBox().getCenter(), entity.position().add((double)0.0F, 0.1, (double)0.0F)}) {
            ClipContext clip = new ClipContext(eye, point, Block.COLLIDER, Fluid.NONE, player);
            if (player.level().clip(clip).getType() == Type.MISS) {
               return true;
            }
         }

         return false;
      }
   }

   private int ticksToReach(LocalPlayer player, LivingEntity entity) {
      Vec3 position = player.position();
      Vec3 motion = player.getDeltaMovement();
      double fall = this.serverFall;
      double reach = this.hitReach(player);
      double eyeHeight = (double)player.getEyeHeight();
      AABB box = entity.getBoundingBox();
      Vec3 theirs = moving(entity);

      for(int tick = 0; tick <= 160; ++tick) {
         Vec3 eye = position.add((double)0.0F, eyeHeight, (double)0.0F);
         AABB at = box.move(theirs.scale((double)Math.min(tick, 6)));
         if (fall >= this.minFall() && fall > (double)1.5F && motion.y < (double)0.0F && eye.distanceToSqr(clamp(eye, at)) <= reach * reach) {
            this.lastReachFall = fall;
            return tick;
         }

         AABB moved = player.getBoundingBox().move(position.subtract(player.position())).move(motion);
         if (!player.level().noCollision(player, moved)) {
            return -1;
         }

         fall = motion.y > (double)0.0F ? (double)0.0F : fall - motion.y;
         position = position.add(motion);
         motion = new Vec3(motion.x * 0.91, (motion.y - 0.08) * 0.98, motion.z * 0.91);
      }

      return -1;
   }

   private boolean inReach(LocalPlayer player, LivingEntity target, int ticks, double margin) {
      Vec3 position = player.position();
      Vec3 motion = player.getDeltaMovement();

      for(int tick = 0; tick < ticks; ++tick) {
         position = position.add(motion);
         motion = new Vec3(motion.x * 0.91, (motion.y - 0.08) * 0.98, motion.z * 0.91);
      }

      Vec3 eye = position.add((double)0.0F, (double)player.getEyeHeight(), (double)0.0F);
      Vec3 step = new Vec3(target.getX() - target.xo, target.getY() - target.yo, target.getZ() - target.zo);
      AABB box = target.getBoundingBox().move(step.scale((double)Math.min(ticks, 6)));
      double reach = this.hitReach(player) - margin;
      return eye.distanceToSqr(clamp(eye, box)) <= reach * reach;
   }

   private static Vec3 moving(LivingEntity entity) {
      return new Vec3(entity.getX() - entity.xo, (double)0.0F, entity.getZ() - entity.zo);
   }

   private void crouchForStun(LocalPlayer player) {
      LivingEntity target = this.stunned != null ? this.stunned : this.fallTarget;
      if (target != null && !(player.getDeltaMovement().y >= (double)0.0F) && !pressing(mc.options.keyShift)) {
         boolean soon = this.reachAt >= 0 && this.reachAt - player.tickCount <= 3 || this.inReach(player, target, 1, (double)0.0F);
         boolean coming = this.stunned != null || soon && (this.stuns(player, target) || player.getDeltaMovement().y < (double)-1.5F);
         if (coming) {
            this.setKey(mc.options.keyShift, true);
         }

      }
   }

   private void planAxe(LivingEntity target) {
      if (this.axeTarget != target) {
         this.axeTarget = target;
         this.axeWait = Humanize.ticks(this.axeDelay);
         this.axeReadyAt = Integer.MIN_VALUE;
      }

   }

   private void axeForStun(LocalPlayer player) {
      if (this.axeDelay.high() > 0 && this.stunned == null && this.fallTarget != null && this.reachAt >= 0 && !Hotbar.actedThisTick() && this.stuns(player, this.fallTarget) && !this.wantsSpear(player)) {
         this.planAxe(this.fallTarget);
         int axe = axeSlot(player);
         if (axe >= 0 && this.axeReadyAt == Integer.MIN_VALUE && this.reachAt - player.tickCount <= this.axeWait) {
            if (axe == Hotbar.selected(player) || this.swap.select(player, axe, this.reachAt - player.tickCount + this.stunHold())) {
               this.axeReadyAt = player.tickCount + this.axeWait;
            }

         }
      }
   }

   private void spearForStun(LocalPlayer player) {
      if (this.wantsSpear(player) && !Weapons.isSpear(player.getMainHandItem()) && this.reachAt >= 0 && this.reachAt - player.tickCount <= 4 && !Hotbar.actedThisTick()) {
         int spear = Weapons.bestSlot(player, (stack) -> Weapons.isSpear(stack) ? 1 : 0);
         if (spear >= 0) {
            this.swap.select(player, spear, this.reachAt - player.tickCount + 20);
         }

      }
   }

   private boolean wantsSpear(LocalPlayer player) {
      if ((Boolean)this.spearReach.get() && this.stunned == null && this.fallTarget != null && (!this.drainArmed || !(this.reachFall >= (double)(Integer)this.drainFrom.get() / (double)10.0F)) && this.maceSlot(player, this.fallTarget) >= 0 && Weapons.bestSlot(player, (stack) -> Weapons.isSpear(stack) ? 1 : 0) >= 0) {
         if ((Integer)this.spearUse.get() != 1) {
            return true;
         } else {
            return (Integer)this.stunSlam.get() != 0 && (this.blocking(player, this.fallTarget) || shielded(this.fallTarget)) && axeSlot(player) >= 0;
         }
      } else {
         return false;
      }
   }

   private static boolean pressing(KeyMapping key) {
      return KeyBindUtil.isKeyDown(KeyBindUtil.getKeyCode(key));
   }

   private double hitReach(LocalPlayer player) {
      double spear = this.spearRange(player);
      return spear > (double)0.0F ? spear : player.entityInteractionRange();
   }

   private double spearRange(LocalPlayer player) {
      if (!(Boolean)this.spearReach.get()) {
         return (double)0.0F;
      } else {
         ItemStack held = player.getMainHandItem();
         AttackRange range = (AttackRange)held.get(DataComponents.ATTACK_RANGE);
         return Weapons.isSpear(held) && range != null ? Math.max(player.entityInteractionRange(), (double)range.effectiveMaxRange(player)) : (double)0.0F;
      }
   }

   private void setKey(KeyMapping key, boolean down) {
      key.setDown(down);
      DrivenKeys.drive(key, down);
      this.heldKeys.add(key);
   }

   private void keysBack() {
      for(KeyMapping key : this.heldKeys) {
         key.setDown(KeyBindUtil.isKeyDown(KeyBindUtil.getKeyCode(key)));
         DrivenKeys.drive(key, false);
      }

      this.heldKeys.clear();
   }

   private static Vec3 clamp(Vec3 point, AABB box) {
      return new Vec3(Math.max(box.minX, Math.min(point.x, box.maxX)), Math.max(box.minY, Math.min(point.y, box.maxY)), Math.max(box.minZ, Math.min(point.z, box.maxZ)));
   }

   private void onTickEnd(TickEvent event) {
      LocalPlayer player = event.client().player;
      if (player != null && player == this.stampsOf) {
         this.trackFall(player);
         switch ((Integer)this.hitAim.get()) {
            case 0 -> this.aimLegit(event.client(), player);
            case 1 -> this.aimHit(event.client(), player);
         }

      }
   }

   private boolean inAimFov(LocalPlayer player, Vec3 point) {
      return (Integer)this.aimFov.get() >= 360 || Maths.angle(player.getLookAngle(), point.subtract(player.getEyePosition())) <= (double)(Integer)this.aimFov.get() * (double)0.5F;
   }

   private void aimLegit(Minecraft client, LocalPlayer player) {
      LivingEntity target = null;
      if (client.gui.screen() == null && (Boolean)this.autoAttack.get()) {
         if (this.stunned != null) {
            target = this.stunned;
         } else if (this.fallCanSmash && this.fallTarget != null && this.allowed(player) && airborne(player) && player.getDeltaMovement().y < (double)0.0F && this.maceSlot(player, this.fallTarget) >= 0) {
            target = this.fallTarget;
         }
      }

      if (target != null) {
         AABB box = target.getBoundingBox();
         Vec3 eye = player.getEyePosition();
         double near = player.entityInteractionRange() + (double)3.0F;
         boolean soon = eye.distanceToSqr(clamp(eye, box)) <= near * near || target == this.fallTarget && this.reachAt >= 0 && this.reachAt - player.tickCount <= 10;
         if (!target.isAlive() || !soon || !visible(player, target) || !this.inAimFov(player, box.getCenter())) {
            target = null;
         }
      }

      float speed = (float)(Integer)this.aimSpeed.get();
      if (target == null) {
         LivingEntity last = this.legitTarget;
         if (this.tracker.fadeOut(speed) && last != null && last.isAlive() && client.gui.screen() == null) {
            VantaClient.ROTATIONS.aim((RotationTarget)this.legitPull(player, last, speed), 1, SmoothTracker.CONFIG);
         } else {
            this.tracker.reset();
            this.legitTarget = null;
         }
      } else {
         if (target != this.legitTarget) {
            this.tracker.reset();
            this.legitTarget = target;
         }

         this.tracker.rampUp(speed);
         VantaClient.ROTATIONS.aim((RotationTarget)this.legitPull(player, target, speed), 1, SmoothTracker.CONFIG);
      }
   }

   private RotationTarget legitPull(LocalPlayer player, LivingEntity target, float speed) {
      return (partialTick) -> {
         Vec3 eye = player.getEyePosition(partialTick);
         AABB box = target.getBoundingBox().move(target.getPosition(partialTick).subtract(target.position()));
         Rotation goal = Rotation.toward(eye, clamp(eye, box.deflate(0.1)));
         Rotation body = Rotation.toward(eye, box.getCenter());
         return this.tracker.step(Rotation.of(player), goal, body, speed, partialTick);
      };
   }

   private void aimHit(Minecraft client, LocalPlayer player) {
      if (client.gui.screen() == null) {
         boolean falling = airborne(player) && player.getDeltaMovement().y < (double)0.0F;
         boolean smash = false;
         LivingEntity target;
         double spear;
         if (this.stunned != null) {
            target = this.stunned;
            spear = (double)0.0F;
         } else {
            if (!falling) {
               return;
            }

            if ((Integer)this.stunSlam.get() != 0 && (Boolean)this.autoAttack.get() && this.fallTarget != null && this.stuns(player, this.fallTarget)) {
               target = this.fallTarget;
               spear = this.spearRange(player);
            } else {
               if (!(Boolean)this.autoAttack.get() || this.fallTarget == null || !this.allowed(player) || !this.smashing(player) || this.fallTarget == this.lastHit && player.tickCount - this.lastHitAt < 10 || this.maceSlot(player, this.fallTarget) < 0) {
                  if (TRACE && falling && this.fallTarget != null) {
                     int var10001 = player.tickCount;
                     System.out.println("[AM] aim t" + var10001 + ": no hit coming (smashing " + this.smashing(player) + ", allowed " + this.allowed(player) + ", mace " + this.maceSlot(player, this.fallTarget) + ")");
                  }

                  return;
               }

               target = this.fallTarget;
               spear = this.spearRange(player);
               smash = true;
            }
         }

         if (target.isAlive()) {
            double reach = spear > (double)0.0F ? spear : player.entityInteractionRange();
            Vec3 eye = player.getEyePosition();
            AABB box = target.getBoundingBox();
            double turnFrom = reach + (RotationManager.legit() ? Math.min((double)8.0F, player.getDeltaMovement().length() * (double)4.0F) : (double)0.0F);
            if (!box.contains(eye) && !(eye.distanceToSqr(clamp(eye, box)) > turnFrom * turnFrom)) {
               Vec3 look = VantaClient.ROTATIONS.rotation(player).direction();
               if (VantaClient.ROTATIONS.active() || !box.clip(eye, eye.add(look.scale(reach))).isPresent()) {
                  Vec3 point = clamp(eye, box.deflate(0.1));
                  Rotation onto = Rotation.toward(eye, point);
                  if (!this.inAimFov(player, point)) {
                     if (TRACE) {
                        int var19 = player.tickCount;
                        System.out.println("[AM] aim t" + var19 + ": outside Aim FOV (turn " + VantaClient.ROTATIONS.rotation(player).distanceTo(onto) + ")");
                     }

                  } else {
                     if (TRACE) {
                        int var18 = player.tickCount;
                        System.out.println("[AM] aim t" + var18 + ": turned onto " + String.valueOf(onto) + " (reach " + reach + ")");
                     }

                     VantaClient.ROTATIONS.aim((Rotation)onto, 9, RotationConfig.silent(0.0F, Smoothing.EASE_OUT_CUBIC).fixingMovement());
                  }
               }
            } else {
               if (TRACE) {
                  System.out.println(String.format(Locale.ROOT, "[AM] aim t%d: out of reach %.2f > %.2f", player.tickCount, Math.sqrt(eye.distanceToSqr(clamp(eye, box))), reach));
               }

            }
         }
      }
   }

   private boolean underAim(Minecraft client, LivingEntity target) {
      HitResult var4 = client.hitResult;
      boolean var10000;
      if (var4 instanceof EntityHitResult hit) {
         if (hit.getEntity() == target) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   private boolean stuns(LocalPlayer player, LivingEntity target) {
      int mode = (Integer)this.stunSlam.get();
      if (mode != 0 && axeSlot(player) >= 0 && this.maceSlot(player, target) >= 0) {
         if (this.smashing(player) && this.drains(player, target)) {
            return false;
         } else {
            return mode == 1 ? this.blocking(player, target) && Shields.covers(target, (BlocksAttacks)target.getItemInHand(target.getUsedItemHand()).get(DataComponents.BLOCKS_ATTACKS), player.position(), player.getDeltaMovement(), (double)20.0F) : player.tickCount <= this.armedUntil;
         }
      } else {
         return false;
      }
   }

   private int stunHold() {
      return this.axeDelay.high() + this.stunDelay.high() + 20;
   }

   private int stunWait(LocalPlayer player, LivingEntity target) {
      if (this.plannedAt == player.tickCount && this.planned == target) {
         return this.plannedWait;
      } else {
         int wait = 0;
         if (this.stuns(player, target) && !this.sameRaise(player, target)) {
            wait = Humanize.ticks(this.stunDelay);
            if (airborne(player) && !this.maceTickComes(player, target, wait)) {
               wait = Math.max(1, this.stunDelay.low());
            }
         }

         this.planned = target;
         this.plannedAt = player.tickCount;
         this.plannedWait = wait;
         return wait;
      }
   }

   private boolean sameRaise(LocalPlayer player, LivingEntity target) {
      if (target == this.lastStun && player.tickCount - this.lastStunAt < 20) {
         Integer since = (Integer)this.raisedSince.get(target.getId());
         return since == null || since <= this.lastStunAt;
      } else {
         return false;
      }
   }

   private boolean maceTickComes(LocalPlayer player, LivingEntity target, int wait) {
      Vec3 position = player.position();
      Vec3 motion = player.getDeltaMovement();

      for(int tick = 0; tick < wait; ++tick) {
         AABB moved = player.getBoundingBox().move(position.subtract(player.position())).move(motion);
         if (!player.level().noCollision(player, moved)) {
            return false;
         }

         position = position.add(motion);
         motion = new Vec3(motion.x * 0.91, (motion.y - 0.08) * 0.98, motion.z * 0.91);
      }

      Vec3 eye = position.add((double)0.0F, (double)player.getEyeHeight(), (double)0.0F);
      AABB box = target.getBoundingBox().move(moving(target).scale((double)Math.min(wait, 6)));
      double reach = player.entityInteractionRange();
      return eye.distanceToSqr(clamp(eye, box)) <= reach * reach;
   }

   private void watchShields(LocalPlayer player) {
      if (player != null && ((Integer)this.stunSlam.get() == 1 || this.drainKey.bound())) {
         Set<Integer> seen = new HashSet();
         Set<Integer> inView = new HashSet();

         for(LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, searchBox(player).inflate((double)4.0F))) {
            inView.add(entity.getId());
            if (entity != player && entity.isUsingItem() && entity.getItemInHand(entity.getUsedItemHand()).has(DataComponents.BLOCKS_ATTACKS)) {
               this.raisedSince.putIfAbsent(entity.getId(), this.inView.contains(entity.getId()) ? player.tickCount : player.tickCount - 1000);
               seen.add(entity.getId());
            }
         }

         this.raisedSince.keySet().retainAll(seen);
         this.inView = inView;
      } else {
         this.raisedSince.clear();
      }
   }

   private static boolean shielded(LivingEntity target) {
      return target.getMainHandItem().has(DataComponents.BLOCKS_ATTACKS) || target.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
   }

   private boolean blocking(LocalPlayer player, LivingEntity target) {
      if (target.isBlocking()) {
         return true;
      } else {
         Integer since = (Integer)this.raisedSince.get(target.getId());
         if (since == null) {
            return false;
         } else {
            BlocksAttacks blocks = (BlocksAttacks)target.getItemInHand(target.getUsedItemHand()).get(DataComponents.BLOCKS_ATTACKS);
            return blocks != null && player.tickCount - since >= blocks.blockDelayTicks();
         }
      }
   }

   private void onAttack(AttackEvent event) {
      Player var3 = event.player();
      if (var3 instanceof LocalPlayer local) {
         this.attackedTick = local.tickCount;
         this.attacked = event.target();
      }

      Player var4 = event.player();
      if (var4 instanceof LocalPlayer player) {
         Entity var9 = event.target();
         if (var9 instanceof LivingEntity target) {
            int var10 = SwapBack.ticks(this.swapBack.low(), this.swapBack.high());
            int wait = !this.followUp && this.stunned == null ? this.stunWait(player, target) : 0;
            if (wait > 0) {
               int axe = axeSlot(player);
               if (axe >= 0 && this.swap.select(player, axe, var10 + this.stunHold())) {
                  Hotbar.sendPickWithNext(player);
                  this.stunned = target;
                  this.stunnedAt = player.tickCount;
                  this.stunWait = wait;
                  this.maceLead = Math.min(Humanize.ticks(this.maceDelay), wait - 1);
                  this.axeTarget = null;
                  this.lastStun = target;
                  this.lastStunAt = player.tickCount;
                  this.armedUntil = Integer.MIN_VALUE;
               }

               return;
            }

            if (!this.followUp && this.smashing(player) && this.allowed(player)) {
               int slot = this.maceSlot(player, target);
               if (slot >= 0 && slot != Hotbar.selected(player)) {
                  if (this.swap.select(player, slot, var10)) {
                     Hotbar.sendPickWithNext(player);
                  }
               } else if (slot >= 0) {
                  this.swap.backIn(var10);
               }

               return;
            }

            return;
         }
      }

   }

   private int maceSlot(LocalPlayer player, LivingEntity target) {
      return this.maceSlot(player, target, this.serverFall);
   }

   private int maceSlot(LocalPlayer player, LivingEntity target, double fall) {
      int mode = (Integer)this.prefer.get();
      return mode == 0 ? Weapons.bestSlot(player, (stack) -> Weapons.isMace(stack) ? 1 + (int)Math.round(this.afterArmor(player, stack, target, fall) * (double)100.0F) : 0) : Weapons.bestSlot(player, (stack) -> score(stack, mode == 2));
   }

   private double afterArmor(LocalPlayer player, ItemStack mace, LivingEntity target, double fall) {
      double smash = fall <= (double)3.0F ? (double)4.0F * fall : (fall <= (double)8.0F ? (double)12.0F + (double)2.0F * (fall - (double)3.0F) : (double)22.0F + fall - (double)8.0F);
      double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE) + smash + (double)0.5F * (double)Weapons.level(mace, Enchantments.DENSITY) * fall;
      if (player.getAttackStrengthScale(0.5F) > 0.9F && !player.isSprinting()) {
         damage *= (double)1.5F;
      }

      double armor = (double)target.getArmorValue();
      double toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      double fraction = Mth.clamp(armor - damage / ((double)2.0F + toughness / (double)4.0F), armor * 0.2, (double)20.0F) / (double)25.0F;
      fraction = Mth.clamp(fraction - 0.15 * (double)Weapons.level(mace, Enchantments.BREACH), (double)0.0F, (double)1.0F);
      return damage * ((double)1.0F - fraction);
   }

   private static int score(ItemStack stack, boolean breachFirst) {
      if (!Weapons.isMace(stack)) {
         return 0;
      } else {
         int density = Weapons.level(stack, Enchantments.DENSITY);
         int breach = Weapons.level(stack, Enchantments.BREACH);
         return 1 + (breachFirst ? breach * 10 + density : density * 10 + breach);
      }
   }

   private static int axeSlot(LocalPlayer player) {
      return Weapons.bestSlot(player, (stack) -> {
         Weapon weapon = (Weapon)stack.get(DataComponents.WEAPON);
         return weapon != null && weapon.disableBlockingForSeconds() > 0.0F ? 1 : 0;
      });
   }
}
