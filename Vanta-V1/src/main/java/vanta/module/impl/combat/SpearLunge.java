package vanta.module.impl.combat;

import java.util.Locale;
import java.util.Objects;
import vanta.event.events.PreTickEvent;
import vanta.mixin.extra.IAccessorKeyMapping;
import vanta.module.Module;
import vanta.setting.BoolSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.RangeSetting;
import vanta.setting.SliderSetting;
import vanta.util.Clicks;
import vanta.util.Hotbar;
import vanta.util.Humanize;
import vanta.util.KeyBindUtil;
import vanta.util.SwapBack;
import vanta.util.Weapons;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;

public final class SpearLunge extends Module {
   private static final String[] PICKS = new String[]{"Auto", "Same tick", "Spear first"};
   private static final int AUTO = 0;
   private static final int SAME_TICK = 1;
   private static final int SPEAR_FIRST = 2;
   private static final int SEQUENCE_LOCK = 3;
   private static final boolean TRACE = System.getenv("FROST_LUNGETRACE") != null;
   private final RangeSetting swapBack;
   private final SliderSetting buffer;
   private final EnumSetting pick;
   private final BoolSetting chain;
   private final RangeSetting chainDelay;
   private final BoolSetting jump;
   private int pressUntil;
   private int chainReadyAt;
   private int heldAt;
   private int origin;
   private int picked;
   private int restAt;
   private boolean jumpHeld;
   private int jumpedAt;
   private LocalPlayer player;

   public SpearLunge() {
      super("spearLunge");
      this.swapBack = (RangeSetting)this.add(new RangeSetting("swapBack", 1, 1, 1, 10, Measure.TICKS));
      this.buffer = (SliderSetting)this.add(new SliderSetting("buffer", 2, 0, 10, Measure.TICKS));
      this.pick = (EnumSetting)this.add(new EnumSetting("pick", 1, PICKS));
      this.chain = (BoolSetting)this.add(new BoolSetting("chain", true));
      this.chainDelay = (RangeSetting)this.add(new RangeSetting("chainDelay", 0, 0, 0, 20, Measure.TICKS));
      this.jump = (BoolSetting)this.add(new BoolSetting("jump", false));
      this.pressUntil = -1;
      this.heldAt = -1073741824;
      this.origin = -1;
      this.picked = -1;
      this.restAt = -1;
      this.jumpedAt = -1073741824;
      this.swapBack.describe();
      this.buffer.describe();
      this.pick.describe();
      this.chain.describe();
      this.chainDelay.describe();
      RangeSetting var10000 = this.chainDelay;
      BoolSetting var10001 = this.chain;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.jump.describe();
   }

   public String info() {
      LocalPlayer player = mc.player;
      return player != null && !fed(player) ? "Food" : "";
   }

   public boolean activatable() {
      return true;
   }

   protected void onActivate() {
      LocalPlayer player = mc.player;
      if (player != null) {
         this.heldAt = player.tickCount;
         this.press(player);
         this.tryJab(mc, player);
      }
   }

   public void testPress() {
      this.onActivate();
   }

   public void testHold() {
      this.onHold();
   }

   public void testRelease() {
      this.onRelease();
   }

   protected void onHold() {
      if (mc.player != null) {
         this.heldAt = mc.player.tickCount;
      }

   }

   protected void onRelease() {
      this.heldAt = -1073741824;
   }

   protected void onEnable() {
      this.listen(PreTickEvent.class, this::onTick);
   }

   protected void onDisable() {
      Clicks.unlock(this);
      this.releaseJump(true);
      LocalPlayer player = mc.player;
      if (player != null && this.origin >= 0 && Hotbar.selected(player) == this.picked && mc.gui.screen() == null) {
         SwapBack.pickNow(player, this.origin);
      }

      this.pressUntil = -1;
      this.origin = -1;
      this.picked = -1;
      this.restAt = -1;
   }

   private void press(LocalPlayer player) {
      this.pressUntil = player.tickCount + (Integer)this.buffer.get();
   }

   private void onTick(PreTickEvent event) {
      Minecraft client = event.client();
      LocalPlayer player = client.player;
      if (player != null) {
         if (player != this.player) {
            this.player = player;
            this.pressUntil = -1;
            this.origin = -1;
            this.restAt = -1;
            this.heldAt = -1073741824;
         }

         this.releaseJump(false);
         this.restSlot(client, player);
         if (this.pressUntil < 0 && this.chainHeld(player) && player.tickCount >= this.chainReadyAt && Hotbar.selected(player) != lungeSlot(player)) {
            this.press(player);
         }

         if (this.pressUntil >= 0) {
            this.tryJab(client, player);
         }

         if (this.origin >= 0) {
            Clicks.lock(this, 3, 3);
         } else {
            Clicks.unlock(this);
         }

      }
   }

   private void tryJab(Minecraft client, LocalPlayer player) {
      if (this.pressUntil >= 0) {
         int slot = lungeSlot(player);
         if (player.tickCount <= this.pressUntil && slot >= 0 && canLunge(player) && client.gui.screen() == null) {
            if (client.gui.overlay() == null && !player.isUsingItem() && !Hotbar.actedThisTick() && !hotbarKeyPressed(client)) {
               int held = Hotbar.selected(player);
               boolean withPick = slot != held;
               ItemStack spear = player.getInventory().getItem(slot);
               if (TRACE) {
                  int var7 = player.tickCount;
                  System.out.println("[Vanta V1] lunge try at " + var7 + ": slot " + slot + " held " + held + " spearFirst=" + this.spearFirst() + " picked this tick=" + SwapBack.pickedThisTick(player) + " until " + this.pressUntil);
               }

               if (withPick && this.spearFirst()) {
                  if (SwapBack.pickNow(player, slot)) {
                     this.picked(held, slot);
                     this.pressUntil = player.tickCount + (Integer)this.buffer.get() + chargeTicks(player, spear) + 1;
                  }

               } else if (Weapons.charged(player, spear) && canClick(client, player, spear)) {
                  if (withPick) {
                     if (!SwapBack.pickNow(player, slot)) {
                        return;
                     }

                     this.picked(held, slot);
                  }

                  Hotbar.sendPickWithNext(player);
                  Clicks.lunge();
                  if ((Boolean)this.jump.get() && player.onGround()) {
                     client.options.keyJump.setDown(true);
                     this.jumpHeld = true;
                     this.jumpedAt = player.tickCount;
                  }

                  this.pressUntil = -1;
                  this.chainReadyAt = player.tickCount + 1 + Humanize.ticks(this.chainDelay);
                  if (this.origin >= 0) {
                     this.restAt = player.tickCount + SwapBack.ticks(this.swapBack.low(), this.swapBack.high());
                  }

               } else {
                  if (TRACE) {
                     int var8 = player.tickCount;
                     System.out.println("[Vanta V1] lunge wait at " + var8 + ": charged=" + Weapons.charged(player, spear) + " missTime=" + client.missTime + " hit=" + (client.hitResult != null) + " busy=" + player.isHandsBusy());
                  }

               }
            } else {
               if (TRACE) {
                  int var10001 = player.tickCount;
                  System.out.println("[Vanta V1] lunge wait at " + var10001 + ": overlay=" + (client.gui.overlay() != null) + " using=" + player.isUsingItem() + " acted=" + Hotbar.actedThisTick() + " numberKey=" + hotbarKeyPressed(client));
               }

            }
         } else {
            this.pressUntil = -1;
         }
      }
   }

   private boolean spearFirst() {
      int mode = (Integer)this.pick.get();
      if (mode != 0) {
         return mode == 2;
      } else {
         String brand = mc.getConnection() == null ? null : mc.getConnection().serverBrand();
         if (brand == null) {
            return false;
         } else {
            String lower = brand.toLowerCase(Locale.ROOT);
            return lower.contains("paper") || lower.contains("purpur") || lower.contains("pufferfish") || lower.contains("folia") || lower.contains("leaf");
         }
      }
   }

   private void picked(int held, int slot) {
      if (this.origin < 0) {
         int theirs = SwapBack.heldFor(held);
         this.origin = theirs >= 0 ? theirs : held;
      }

      this.picked = slot;
      this.restAt = -1;
   }

   private boolean chainHeld(LocalPlayer player) {
      return (Boolean)this.chain.get() && player.tickCount - this.heldAt <= 1;
   }

   private void restSlot(Minecraft client, LocalPlayer player) {
      if (this.origin >= 0) {
         int held = Hotbar.selected(player);
         if (held != this.picked) {
            this.origin = -1;
            this.restAt = -1;
            this.pressUntil = -1;
         } else if (this.pressUntil < 0) {
            if (this.restAt < 0) {
               this.restAt = player.tickCount;
            }

            int spear = lungeSlot(player);
            int target = this.chainHeld(player) && spear >= 0 ? quickSlot(player, spear, this.origin) : this.origin;
            if (held != target && player.tickCount >= this.restAt && client.gui.screen() == null && SwapBack.pickNow(player, target)) {
               this.picked = target;
               if (target == this.origin) {
                  this.origin = -1;
                  this.restAt = -1;
               }

            }
         }
      }
   }

   private static int quickSlot(LocalPlayer player, int spear, int prefer) {
      int best = prefer;
      int bestTicks = prefer >= 0 && prefer != spear ? chargeTicks(player, player.getInventory().getItem(prefer)) : Integer.MAX_VALUE;

      for(int slot = 0; slot < 9; ++slot) {
         if (slot != spear) {
            int ticks = chargeTicks(player, player.getInventory().getItem(slot));
            if (ticks < bestTicks) {
               best = slot;
               bestTicks = ticks;
            }
         }
      }

      return best;
   }

   private void releaseJump(boolean now) {
      LocalPlayer player = mc.player;
      if (this.jumpHeld && (now || player == null || player.tickCount > this.jumpedAt)) {
         KeyMapping key = mc.options.keyJump;
         key.setDown(KeyBindUtil.isKeyDown(KeyBindUtil.getKeyCode(key)));
         this.jumpHeld = false;
      }

   }

   private static boolean fed(LocalPlayer player) {
      return player.getAbilities().instabuild || player.getFoodData().getFoodLevel() >= 7;
   }

   private static boolean canLunge(LocalPlayer player) {
      return fed(player) && !player.isInWater() && !player.isPassenger() && !player.isFallFlying();
   }

   private static boolean canClick(Minecraft client, LocalPlayer player, ItemStack spear) {
      return client.missTime <= 0 && client.hitResult != null && !player.isHandsBusy() && !player.isSpectator() && spear.isItemEnabled(player.level().enabledFeatures()) && spear.has(DataComponents.PIERCING_WEAPON);
   }

   private static boolean hotbarKeyPressed(Minecraft client) {
      for(KeyMapping key : client.options.keyHotbarSlots) {
         if (((IAccessorKeyMapping)key).getClickCount() > 0) {
            return true;
         }
      }

      return false;
   }

   private static int chargeTicks(LocalPlayer player, ItemStack stack) {
      double speed = (double)4.0F;
      ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      double[] add = new double[]{(double)0.0F};
      modifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
         if (attribute.equals(Attributes.ATTACK_SPEED) && modifier.operation() == Operation.ADD_VALUE) {
            add[0] += modifier.amount();
         }

      });
      return (int)Math.ceil((double)20.0F / Math.max(0.1, speed + add[0]));
   }

   private static int lungeSlot(LocalPlayer player) {
      int held = Hotbar.selected(player);
      int best = -1;
      int bestLevel = 0;

      for(int slot = 0; slot < 9; ++slot) {
         ItemStack stack = player.getInventory().getItem(slot);
         int level = Weapons.isSpear(stack) ? Weapons.level(stack, Enchantments.LUNGE) : 0;
         if (level > bestLevel || level > 0 && level == bestLevel && slot == held) {
            best = slot;
            bestLevel = level;
         }
      }

      return best;
   }
}
