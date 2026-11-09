package vanta.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SwapBack {
   private static int pickedTick = Integer.MIN_VALUE;
   private static final List<SwapBack> UNDER_WAY = new ArrayList();
   private static final int LOCK_SLACK = 3;
   private final int locks;
   private int from;
   private int to;
   private int ticksLeft;
   private int at;

   public SwapBack() {
      this(0);
   }

   public SwapBack(int locks) {
      this.from = -1;
      this.to = -1;
      this.locks = locks;
   }

   public static boolean pickedThisTick(LocalPlayer player) {
      return player != null && pickedTick == player.tickCount;
   }

   public static boolean pickNow(LocalPlayer player, int slot) {
      if (player != null && slot >= 0 && slot <= 8) {
         if (Hotbar.selected(player) == slot) {
            return true;
         } else if (!Hotbar.actedThisTick() && !pickedThisTick(player) && Clicks.select(slot)) {
            pickedTick = player.tickCount;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int heldFor(int slot) {
      for(SwapBack swap : UNDER_WAY) {
         if (swap.to == slot) {
            return swap.from;
         }
      }

      return -1;
   }

   public static int ticks(int low, int high) {
      return low >= high ? low : ThreadLocalRandom.current().nextInt(low, high + 1);
   }

   public boolean select(LocalPlayer player, int slot, int backAfter) {
      if (player != null && slot >= 0 && slot <= 8) {
         int current = Hotbar.selected(player);
         if (current == slot) {
            return true;
         } else if (!Hotbar.actedThisTick() && !pickedThisTick(player) && Clicks.select(slot)) {
            if (this.to == -1) {
               this.from = current;
            }

            this.to = slot;
            if (!UNDER_WAY.contains(this)) {
               UNDER_WAY.add(this);
            }

            this.ticksLeft = Math.max(1, backAfter);
            this.at = Clicks.now();
            pickedTick = player.tickCount;
            this.lock();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean active() {
      return this.to != -1;
   }

   public void backIn(int ticks) {
      if (this.to != -1) {
         this.ticksLeft = Math.max(1, ticks);
         this.lock();
      }

   }

   public int origin() {
      return this.from;
   }

   public void tick(LocalPlayer player) {
      if (this.to != -1) {
         if (player != null && Hotbar.selected(player) == this.to && !Clicks.userPickedSince(this.at)) {
            this.lock();
            if (--this.ticksLeft <= 0 && !pickedThisTick(player) && !Hotbar.actedThisTick() && Minecraft.getInstance().gui.screen() == null && Clicks.select(this.from)) {
               pickedTick = player.tickCount;
               this.reset();
            }
         } else {
            this.reset();
         }
      }
   }

   public void cancel(LocalPlayer player) {
      if (this.to != -1 && player != null && Hotbar.selected(player) == this.to && !Clicks.userPickedSince(this.at)) {
         Clicks.select(this.from);
      }

      this.reset();
   }

   private void lock() {
      if (this.locks != 0) {
         Clicks.lock(this, this.locks, this.ticksLeft + 3);
      }

   }

   private void reset() {
      if (this.locks != 0) {
         Clicks.unlock(this);
      }

      this.from = -1;
      this.to = -1;
      this.ticksLeft = 0;
      UNDER_WAY.remove(this);
   }
}
