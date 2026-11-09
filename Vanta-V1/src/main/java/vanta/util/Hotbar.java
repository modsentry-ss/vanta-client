package vanta.util;

import io.netty.channel.ChannelFutureListener;
import java.util.function.Predicate;
import vanta.mixin.accessor.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class Hotbar {
   public static final int NONE = -1;
   private static boolean acted;
   private static int silentSlot = -1;
   private static int silentBase = -1;

   public static boolean actedThisTick() {
      return acted;
   }

   public static void sendPickWithNext(LocalPlayer player) {
      MultiPlayerGameModeAccessor gameMode = (MultiPlayerGameModeAccessor)Minecraft.getInstance().gameMode;
      int index = selected(player);
      if (gameMode != null && gameMode.client$getCarriedIndex() != index) {
         gameMode.client$setCarriedIndex(index);
         player.connection.getConnection().send(new ServerboundSetCarriedItemPacket(index), (ChannelFutureListener)null, false);
      }

   }

   public static int selected(LocalPlayer player) {
      return player.getInventory().getSelectedSlot();
   }

   public static int find(LocalPlayer player, Predicate<ItemStack> matcher) {
      Inventory inventory = player.getInventory();

      for(int slot = 0; slot < 9; ++slot) {
         if (matcher.test(inventory.getItem(slot))) {
            return slot;
         }
      }

      return -1;
   }

   public static int serverSlot() {
      int carried = carriedIndex();
      return silentSlot != -1 && carried == silentBase ? silentSlot : carried;
   }

   public static boolean swap(LocalPlayer player, int slot, boolean silent) {
      if (Inventory.isHotbarSlot(slot) && !acted) {
         if (silent) {
            selectSilently(player, slot);
            return true;
         } else {
            return select(player, slot);
         }
      } else {
         return false;
      }
   }

   public static boolean resync(LocalPlayer player) {
      int slot = selected(player);
      if (serverSlot() == slot) {
         silentSlot = -1;
         return true;
      } else if (acted) {
         return false;
      } else {
         send(player, slot);
         silentSlot = -1;
         return true;
      }
   }

   public static void sent(Packet<?> packet) {
      if (packet instanceof ServerboundClientTickEndPacket) {
         acted = false;
      } else if (isAction(packet)) {
         acted = true;
      }

   }

   private static boolean isAction(Packet<?> packet) {
      return packet instanceof ServerboundAttackPacket || packet instanceof ServerboundInteractPacket || packet instanceof ServerboundUseItemPacket || packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundPlayerActionPacket || packet instanceof ServerboundSwingPacket || packet instanceof ServerboundPlayerCommandPacket || packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundPickItemFromBlockPacket || packet instanceof ServerboundPickItemFromEntityPacket;
   }

   private static boolean select(LocalPlayer player, int slot) {
      if (!Clicks.select(slot)) {
         return false;
      } else {
         if (silentSlot != -1 && serverSlot() != slot && carriedIndex() == slot) {
            send(player, slot);
         }

         silentSlot = -1;
         return true;
      }
   }

   private static void selectSilently(LocalPlayer player, int slot) {
      if (serverSlot() != slot) {
         send(player, slot);
         silentSlot = slot;
         silentBase = carriedIndex();
      }

   }

   private static void send(LocalPlayer player, int slot) {
      player.connection.send(new ServerboundSetCarriedItemPacket(slot));
   }

   private static int carriedIndex() {
      return ((MultiPlayerGameModeAccessor)Minecraft.getInstance().gameMode).client$getCarriedIndex();
   }

   private Hotbar() {
   }
}
