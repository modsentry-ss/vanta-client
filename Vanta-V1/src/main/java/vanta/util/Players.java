package vanta.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class Players {
   private static final float MOVE_EPSILON = 1.0E-10F;

   public static boolean isMoving(LocalPlayer player) {
      return player.input.getMoveVector().lengthSquared() > 1.0E-10F;
   }

   public static boolean canCrit(LocalPlayer player) {
      return player.fallDistance > (double)0.0F && !player.onClimbable() && !player.isInWater() && !player.isMobilityRestricted() && !player.isPassenger();
   }

   public static boolean consuming(LocalPlayer player) {
      if (player.isUsingItem()) {
         return true;
      } else {
         ItemStack main = player.getMainHandItem();
         return main.has(DataComponents.CONSUMABLE) ? true : player.getOffhandItem().has(DataComponents.CONSUMABLE) && !(main.getItem() instanceof BlockItem);
      }
   }

   private Players() {
   }
}
