package vanta.module.impl.player;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import vanta.event.events.PreTickEvent;
import vanta.mixin.accessor.MinecraftAccessor;
import vanta.module.Module;
import vanta.setting.BoolSetting;
import vanta.setting.SliderSetting;
import vanta.util.BlockUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public final class FastPlace extends Module {
   private final SliderSetting delay = (SliderSetting)this.add((new SliderSetting("delay", 10, 10, 30)).withFormat((v) -> String.format(Locale.ROOT, "%.1f", (float)v / 10.0F)));
   private final BoolSetting blocksOnly = (BoolSetting)this.add(new BoolSetting("blocksOnly", true));
   private final BoolSetting placeFix = (BoolSetting)this.add(new BoolSetting("placeFix", true));
   private final BoolSetting skipObsidian = (BoolSetting)this.add(new BoolSetting("skipObsidian", true));
   private final BoolSetting skipInteractable = (BoolSetting)this.add(new BoolSetting("skipInteractable", true));
   private long delayMs;

   public FastPlace() {
      super("fastPlace");
      this.delay.describe();
      this.placeFix.describe();
   }

   public String info() {
      return String.format(Locale.ROOT, "%.1f", (float)(Integer)this.delay.get() / 10.0F);
   }

   protected void onEnable() {
      this.delayMs = 0L;
      this.listen(PreTickEvent.class, (event) -> {
         if (mc.player != null) {
            MinecraftAccessor access = (MinecraftAccessor)mc;
            int rightClickDelay = access.client$rightClickDelay();
            if (rightClickDelay == 4) {
               this.delayMs += (long)(50.0F * ((float)(Integer)this.delay.get() / 10.0F) * (0.9F + ThreadLocalRandom.current().nextFloat() * 0.45F));
            }

            if (this.delayMs > 0L) {
               this.delayMs -= 50L;
            }

            if (this.delayMs <= 0L && rightClickDelay > 1 && this.canPlace()) {
               access.client$setRightClickDelay(0);
            }

         }
      });
   }

   protected void onDisable() {
      this.delayMs = 0L;
   }

   private boolean canPlace() {
      ItemStack stack = mc.player.getMainHandItem();
      if (!stack.isEmpty()) {
         if (stack.getItem() instanceof FishingRodItem) {
            return false;
         }

         Item var3 = stack.getItem();
         if (var3 instanceof BlockItem) {
            BlockItem blockItem = (BlockItem)var3;
            Block block = blockItem.getBlock();
            if ((Boolean)this.skipObsidian.get() && block == Blocks.OBSIDIAN) {
               return false;
            }

            if ((Boolean)this.skipInteractable.get() && BlockUtil.isInteractable(block)) {
               return false;
            }

            if (!(Boolean)this.placeFix.get()) {
               return true;
            }

            HitResult var5 = mc.hitResult;
            boolean var10000;
            if (var5 instanceof BlockHitResult) {
               BlockHitResult hit = (BlockHitResult)var5;
               if (hit.getType() == Type.BLOCK && BlockUtil.canPlace(stack, hit)) {
                  var10000 = true;
                  return var10000;
               }
            }

            var10000 = false;
            return var10000;
         }
      }

      return !(Boolean)this.blocksOnly.get();
   }
}
