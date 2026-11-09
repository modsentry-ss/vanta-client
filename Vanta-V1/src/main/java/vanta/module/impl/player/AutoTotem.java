package vanta.module.impl.player;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import vanta.event.events.PreTickEvent;
import vanta.event.events.TotemPopEvent;
import vanta.mixin.accessor.AbstractContainerScreenAccessor;
import vanta.module.Module;
import vanta.setting.BoolSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.RangeSetting;
import vanta.setting.SliderSetting;
import vanta.util.Time;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public final class AutoTotem extends Module {
   private static final String[] MODES = new String[]{"Auto", "Legit", "Hover"};
   private static final int LEGIT = 1;
   private static final int HOVER = 2;
   private static final int OFFHAND_BUTTON = 40;
   private static final int OFFHAND_SLOT = 45;
   private static final long TIMEOUT = 3000L;
   private static final int POP_WAIT = 20;
   private final EnumSetting mode;
   private final BoolSetting openInventory;
   private final BoolSetting keep;
   private final BoolSetting refill;
   private final SliderSetting slot;
   private final RangeSetting reaction;
   private final RangeSetting openDelay;
   private final RangeSetting clickDelay;
   private final RangeSetting closeDelay;
   private Step step;
   private int popPending;
   private boolean opened;
   private long startedAt;
   private long due;
   private int movedAt;

   public AutoTotem() {
      super("autoTotem");
      this.mode = (EnumSetting)this.add(new EnumSetting("mode", 0, MODES));
      this.openInventory = (BoolSetting)this.add(new BoolSetting("openInventory", true));
      this.keep = (BoolSetting)this.add(new BoolSetting("keep", false));
      this.refill = (BoolSetting)this.add(new BoolSetting("refill", true));
      this.slot = (SliderSetting)this.add(new SliderSetting("slot", 9, 1, 9));
      this.reaction = (RangeSetting)this.add(new RangeSetting("reaction", 150, 300, 0, 1000, Measure.MILLIS));
      this.openDelay = (RangeSetting)this.add(new RangeSetting("openDelay", 0, 50, 0, 500, Measure.MILLIS));
      this.clickDelay = (RangeSetting)this.add(new RangeSetting("clickDelay", 50, 100, 0, 500, Measure.MILLIS));
      this.closeDelay = (RangeSetting)this.add(new RangeSetting("closeDelay", 50, 100, 0, 500, Measure.MILLIS));
      this.step = AutoTotem.Step.IDLE;
      this.mode.describe();
      this.openInventory.describe();
      this.keep.describe();
      this.refill.describe();
      this.reaction.describe();
      this.openDelay.describe();
      this.clickDelay.describe();
      this.closeDelay.describe();
      this.openInventory.visibleWhen(() -> (Integer)this.mode.get() == 2);
      SliderSetting var10000 = this.slot;
      BoolSetting var10001 = this.refill;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.reaction.visibleWhen(() -> (Integer)this.mode.get() == 1);
      this.openDelay.visibleWhen(() -> (Integer)this.mode.get() != 2 || (Boolean)this.openInventory.get());
      this.closeDelay.visibleWhen(() -> (Integer)this.mode.get() != 2 || (Boolean)this.openInventory.get());
   }

   public String info() {
      return this.mode.selected();
   }

   protected void onEnable() {
      this.listen(TotemPopEvent.class, (event) -> this.popPending = 20);
      this.listen(PreTickEvent.class, this::onTick);
   }

   protected void onDisable() {
      this.step = AutoTotem.Step.IDLE;
      this.popPending = 0;
   }

   private void onTick(PreTickEvent event) {
      LocalPlayer player = event.client().player;
      if (player != null && !player.isCreative() && !player.isSpectator()) {
         long now = Time.tickMillis();
         Input input = player.input.keyPresses;
         if (input.forward() || input.backward() || input.left() || input.right() || player.isSprinting() || player.isShiftKeyDown()) {
            this.movedAt = player.tickCount;
         }

         boolean still = player.tickCount > this.movedAt + 1;
         if (this.step != AutoTotem.Step.IDLE) {
            if (now - this.startedAt > 3000L) {
               this.finish(player, still);
            } else {
               switch (this.step.ordinal()) {
                  case 1:
                     this.open(player, now);
                     break;
                  case 2:
                     if (!this.inInventory()) {
                        this.step = AutoTotem.Step.IDLE;
                     } else if (still && now >= this.due && carriesNothing(player)) {
                        this.fillHand(player, now);
                     }
                     break;
                  case 3:
                     if (!this.inInventory()) {
                        this.step = AutoTotem.Step.IDLE;
                     } else if (still && now >= this.due && carriesNothing(player)) {
                        this.fillHotbar(player, now);
                     }
                     break;
                  case 4:
                     if (!this.inInventory()) {
                        this.step = AutoTotem.Step.IDLE;
                     } else if (still && now >= this.due) {
                        this.finish(player, true);
                     }
               }

            }
         } else {
            boolean wanted = this.popPending > 0 || (Boolean)this.keep.get() && mc.gui.screen() == null && !player.isUsingItem();
            if (this.popPending > 0) {
               --this.popPending;
            }

            if (wanted && handNeeds(player) && this.source(player, -1, false) != null) {
               this.popPending = 0;
               this.step = AutoTotem.Step.OPEN;
               this.startedAt = now;
               this.due = now + ((Integer)this.mode.get() == 1 ? draw(this.reaction) : 0L) + draw(this.openDelay);
               this.opened = false;
            }

         }
      } else {
         this.step = AutoTotem.Step.IDLE;
         this.popPending = 0;
      }
   }

   private void open(LocalPlayer player, long now) {
      if (this.inInventory()) {
         this.step = AutoTotem.Step.HAND;
         this.due = now + draw(this.clickDelay);
      } else {
         boolean byYou = (Integer)this.mode.get() == 2 && !(Boolean)this.openInventory.get();
         if (!byYou && mc.gui.screen() == null && now >= this.due && !player.isUsingItem()) {
            mc.gui.setScreen(new InventoryScreen(player));
            this.opened = true;
            this.step = AutoTotem.Step.HAND;
            this.due = now + draw(this.clickDelay);
         }
      }
   }

   private void fillHand(LocalPlayer player, long now) {
      if (!handNeeds(player)) {
         this.next(AutoTotem.Step.REFILL, now);
      } else {
         Slot source = this.source(player, this.reserved(), (Integer)this.mode.get() == 2);
         if (source == null) {
            if ((Integer)this.mode.get() != 2) {
               source = this.source(player, -1, false);
            }

            if (source == null) {
               return;
            }
         }

         mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, source.index, 40, ContainerInput.SWAP, player);
         this.next(AutoTotem.Step.REFILL, now);
      }
   }

   private void fillHotbar(LocalPlayer player, long now) {
      int index = (Integer)this.slot.get() - 1;
      if ((Boolean)this.refill.get() && player.getInventory().getItem(index).isEmpty()) {
         Slot source = this.source(player, this.reserved(), (Integer)this.mode.get() == 2);
         if (source != null && source.index < 36) {
            mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, source.index, index, ContainerInput.SWAP, player);
         }

         this.next(AutoTotem.Step.CLOSE, now);
      } else {
         this.next(AutoTotem.Step.CLOSE, now);
      }
   }

   private void next(Step step, long now) {
      this.step = step;
      this.due = now + draw(step == AutoTotem.Step.CLOSE ? this.closeDelay : this.clickDelay);
   }

   private void finish(LocalPlayer player, boolean still) {
      if (this.opened && this.inInventory()) {
         if (!still) {
            return;
         }

         Screen var4 = mc.gui.screen();
         if (var4 instanceof InventoryScreen) {
            InventoryScreen screen = (InventoryScreen)var4;
            screen.onClose();
         }
      }

      this.step = AutoTotem.Step.IDLE;
      this.opened = false;
   }

   private boolean inInventory() {
      return mc.gui.screen() instanceof InventoryScreen;
   }

   private static boolean handNeeds(LocalPlayer player) {
      return player.getOffhandItem().isEmpty();
   }

   private static boolean carriesNothing(LocalPlayer player) {
      return player.inventoryMenu.getCarried().isEmpty() && player.containerMenu.getCarried().isEmpty();
   }

   private int reserved() {
      return (Boolean)this.refill.get() ? 36 + (Integer)this.slot.get() - 1 : -1;
   }

   private Slot source(LocalPlayer player, int skip, boolean hovered) {
      if (hovered) {
         Screen var8 = mc.gui.screen();
         if (!(var8 instanceof InventoryScreen)) {
            return null;
         } else {
            InventoryScreen screen = (InventoryScreen)var8;
            Slot slot = ((AbstractContainerScreenAccessor)screen).client$getHoveredSlot();
            return slot != null && slot.index != 45 && slot.index != skip && slot.index >= 9 && slot.getItem().is(Items.TOTEM_OF_UNDYING) ? slot : null;
         }
      } else {
         InventoryMenu menu = player.inventoryMenu;

         for(int index = 9; index < 45; ++index) {
            Slot slot = menu.getSlot(index);
            if (index != skip && slot.getItem().is(Items.TOTEM_OF_UNDYING)) {
               return slot;
            }
         }

         return null;
      }
   }

   private static long draw(RangeSetting range) {
      int low = range.low();
      int high = range.high();
      return high > low ? (long)ThreadLocalRandom.current().nextInt(low, high + 1) : (long)low;
   }

   private static enum Step {
      IDLE,
      OPEN,
      HAND,
      REFILL,
      CLOSE;

      // $FF: synthetic method
      private static Step[] $values() {
         return new Step[]{IDLE, OPEN, HAND, REFILL, CLOSE};
      }
   }
}
