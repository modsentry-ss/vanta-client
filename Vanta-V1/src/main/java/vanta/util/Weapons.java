package vanta.util;

import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class Weapons {
   private Weapons() {
   }

   public static boolean isSpear(ItemStack stack) {
      return !stack.isEmpty() && stack.has(DataComponents.PIERCING_WEAPON);
   }

   public static boolean isMace(ItemStack stack) {
      return stack.is(Items.MACE);
   }

   public static int level(ItemStack stack, ResourceKey<Enchantment> enchantment) {
      Minecraft mc = Minecraft.getInstance();
      if (!stack.isEmpty() && mc.level != null) {
         Holder<Enchantment> holder = (Holder)mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantment).orElse((Object)null);
         return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
      } else {
         return 0;
      }
   }

   public static int bestSlot(LocalPlayer player, ToIntFunction<ItemStack> score) {
      int best = -1;
      int bestScore = 0;

      for(int slot = 0; slot < 9; ++slot) {
         int value = score.applyAsInt(player.getInventory().getItem(slot));
         if (value > bestScore) {
            best = slot;
            bestScore = value;
         }
      }

      return best;
   }

   public static boolean charged(LocalPlayer player, ItemStack stack) {
      return !player.cannotAttackWithItem(stack, 0) && player.getAttackStrengthScale(0.5F) >= 1.0F;
   }
}
