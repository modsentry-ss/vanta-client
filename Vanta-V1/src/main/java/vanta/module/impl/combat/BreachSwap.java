package vanta.module.impl.combat;

import java.util.concurrent.ThreadLocalRandom;
import vanta.event.events.AttackEvent;
import vanta.event.events.PreTickEvent;
import vanta.module.Module;
import vanta.setting.BoolSetting;
import vanta.setting.Measure;
import vanta.setting.RangeSetting;
import vanta.setting.SliderSetting;
import vanta.util.SwapBack;
import vanta.util.Weapons;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;

public final class BreachSwap extends Module {
   private final SliderSetting minArmor;
   private final SliderSetting chance;
   private final RangeSetting swapBack;
   private final BoolSetting playersOnly;
   private final SwapBack swap;

   public BreachSwap() {
      super("breachSwap");
      this.minArmor = (SliderSetting)this.add((new SliderSetting("minArmor", 8, 0, 20, Measure.NONE)).capped());
      this.chance = (SliderSetting)this.add(new SliderSetting("chance", 100, 1, 100, Measure.PERCENT));
      this.swapBack = (RangeSetting)this.add(new RangeSetting("swapBack", 1, 3, 1, 10, Measure.TICKS));
      this.playersOnly = (BoolSetting)this.add(new BoolSetting("playersOnly", true));
      this.swap = new SwapBack();
      this.minArmor.describe();
      this.chance.describe();
      this.swapBack.describe();
   }

   protected void onEnable() {
      this.listen(AttackEvent.class, -10, this::onAttack);
      this.listen(PreTickEvent.class, (event) -> this.swap.tick(event.client().player));
   }

   protected void onDisable() {
      this.swap.cancel(mc.player);
   }

   private void onAttack(AttackEvent event) {
      Player var4 = event.player();
      if (var4 instanceof LocalPlayer player) {
         Entity var5 = event.target();
         if (var5 instanceof LivingEntity target) {
            if ((!(Boolean)this.playersOnly.get() || target instanceof Player) && target.getArmorValue() >= (Integer)this.minArmor.get()) {
               if (Weapons.level(player.getMainHandItem(), Enchantments.BREACH) > 0) {
                  return;
               }

               int slot = Weapons.bestSlot(player, (stack) -> Weapons.level(stack, Enchantments.BREACH));
               if (slot >= 0 && ThreadLocalRandom.current().nextInt(100) < (Integer)this.chance.get()) {
                  this.swap.select(player, slot, SwapBack.ticks(this.swapBack.low(), this.swapBack.high()));
                  return;
               }

               return;
            }

            return;
         }
      }

   }
}
