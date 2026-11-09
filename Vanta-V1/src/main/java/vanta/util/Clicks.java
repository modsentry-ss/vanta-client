package vanta.util;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import vanta.VantaClient;
import vanta.event.events.ButtonEvent;
import vanta.event.events.PreTickEvent;
import vanta.mixin.accessor.KeyMappingAccessor;
import vanta.mixin.accessor.KeyboardHandlerAccessor;
import vanta.mixin.accessor.MinecraftAccessor;
import vanta.mixin.accessor.MouseHandlerAccessor;
import vanta.module.impl.render.Freecam;
import vanta.rotation.Rotation;
import vanta.ui.click.ClickGui;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.SwingSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.lwjgl.glfw.GLFW;

public final class Clicks {
   public static final int LEFT = 0;
   public static final int RIGHT = 1;
   public static final int SLOT = 1;
   public static final int LMB = 2;
   public static final int RMB = 4;
   public static final int MOVE = 8;
   private static final int LOCK_LIMIT = 200;
   private static final boolean TRACE = System.getenv("FROST_CLICKTRACE") != null;
   private static final int SHORTEST_HOLD = 12;
   private static final int REMEMBERED_PRESSES = 8;
   private static final int TICK_SLACK = 25;
   private static final double HOLD_SHARE = 0.55;
   public static final int HOLD_MIN = 15;
   public static final int HOLD_MAX = 200;
   public static boolean simulate = true;
   public static int holdLow = 35;
   public static int holdHigh = 110;
   private static final Minecraft mc = Minecraft.getInstance();
   private static final Set<InputConstants.Key> physical = new HashSet();
   private static final Map<KeyMapping, Press> presses = new IdentityHashMap();
   private static final Map<KeyMapping, ArrayDeque<Long>> pressedAt = new IdentityHashMap();
   private static final Map<Object, Lock> locks = new IdentityHashMap();
   private static final Map<KeyMapping, Integer> owned = new IdentityHashMap();
   private static final Map<KeyMapping, Long> shownUntil = new IdentityHashMap();
   private static final int SHOWN_HOLD_BACK = 300;
   private static final int[][] counts = new int[2][2];
   private static int injecting;
   private static int processing;
   private static boolean targeted;
   private static boolean inKeybinds;
   private static Vec3 view;
   private static int tick;
   private static int attackedTick = Integer.MIN_VALUE;
   private static int userPickTick = Integer.MIN_VALUE;
   private static Entity realAttack;
   private static int realAttackTick = Integer.MIN_VALUE;
   private static BlockHitResult realUse;
   private static int realUseTick = Integer.MIN_VALUE;
   private static InteractionResult lastUse;
   private static KeyMapping repress;
   private static boolean ticking;
   private static boolean landed;

   public static void init() {
      VantaClient.EVENTS.subscribe(PreTickEvent.class, 300, (event) -> beginTick());
   }

   public static boolean attack(Entity target) {
      return attack(target, (Rotation)null);
   }

   public static boolean attack(Entity target, Rotation rotation) {
      return attack(target, rotation, true);
   }

   public static boolean replay(Entity target, Rotation rotation) {
      return attack(target, rotation, false);
   }

   public static boolean replay() {
      return mc.player != null && mc.gameMode != null && act(false, Clicks::startAttack);
   }

   private static boolean attack(Entity target, Rotation rotation, boolean fresh) {
      LocalPlayer player = mc.player;
      if (player != null && mc.gameMode != null && target != null) {
         if (!live()) {
            trace("attack", blocked());
            if (fresh) {
               note(0);
            }

            directAttack(player, target);
            return true;
         } else if (fresh && realAttackTick == tick && realAttack == target) {
            return true;
         } else {
            KeyMapping key = mc.options.keyAttack;
            look(rotation);

            boolean var12;
            try {
               boolean var10000;
               label143: {
                  HitResult var7 = mc.hitResult;
                  if (var7 instanceof EntityHitResult) {
                     EntityHitResult hit = (EntityHitResult)var7;
                     if (hit.getEntity() == target && attacks(player, false)) {
                        var10000 = true;
                        break label143;
                     }
                  }

                  var10000 = false;
               }

               boolean game = var10000;
               if (fresh) {
                  if (!game || rotation != null || !pending(key)) {
                     press(key, false);
                  }

                  take(key);
               }

               landed = false;
               if (game) {
                  act(true, Clicks::startAttack);
               }

               if (!landed) {
                  String var10001;
                  if (game) {
                     var10001 = "the game sent no attack";
                  } else {
                     var10001 = String.valueOf(mc.hitResult.getType());
                     HitResult var13 = mc.hitResult;
                     Object var10002;
                     if (var13 instanceof EntityHitResult) {
                        EntityHitResult hit = (EntityHitResult)var13;
                        var10002 = hit.getEntity();
                     } else {
                        var10002 = "";
                     }

                     var10001 = "pick " + var10001 + " " + String.valueOf(var10002) + ", miss time " + mc.missTime + ", using " + player.isUsingItem() + ", hands busy " + player.isHandsBusy();
                  }

                  trace("attack", var10001);
                  directAttack(player, target);
               }

               var12 = true;
            } finally {
               unlook(rotation);
            }

            return var12;
         }
      } else {
         return false;
      }
   }

   public static boolean use(BlockHitResult target) {
      return use(target, (Rotation)null);
   }

   public static boolean use(BlockHitResult target, Rotation rotation) {
      LocalPlayer player = mc.player;
      if (player != null && mc.gameMode != null && target != null) {
         if (!live()) {
            note(1);
            return directUse(player, target);
         } else if (realUseTick == tick && same(realUse, target)) {
            return true;
         } else {
            KeyMapping key = mc.options.keyUse;
            look(rotation);

            boolean var10;
            try {
               boolean var10000;
               label104: {
                  HitResult var6 = mc.hitResult;
                  if (var6 instanceof BlockHitResult) {
                     BlockHitResult hit = (BlockHitResult)var6;
                     if (hit.getType() == Type.BLOCK && same(hit, target) && uses(player)) {
                        var10000 = true;
                        break label104;
                     }
                  }

                  var10000 = false;
               }

               boolean game = var10000;
               if (!game || rotation != null || !pending(key)) {
                  press(key, false);
               }

               take(key);
               if (game) {
                  lastUse = null;
                  act(true, () -> {
                     ((MinecraftAccessor)mc).client$startUseItem();
                     return true;
                  });
                  var10 = lastUse instanceof InteractionResult.Success;
                  return var10;
               }

               var10 = directUse(player, target);
            } finally {
               unlook(rotation);
            }

            return var10;
         }
      } else {
         return false;
      }
   }

   public static boolean lunge() {
      LocalPlayer player = mc.player;
      PiercingWeapon weapon = player == null ? null : (PiercingWeapon)player.getMainHandItem().get(DataComponents.PIERCING_WEAPON);
      if (weapon != null && mc.gameMode != null) {
         if (!live()) {
            note(0);
            directLunge(player, weapon);
            return true;
         } else {
            KeyMapping key = mc.options.keyAttack;
            press(key, false);
            take(key);
            if (mc.hitResult == null || !attacks(player, true) || !act(true, Clicks::startAttack)) {
               directLunge(player, weapon);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static void miss(Rotation rotation) {
      LocalPlayer player = mc.player;
      if (player != null && mc.gameMode != null) {
         if (!live()) {
            note(0);
            directMiss(player);
         } else {
            KeyMapping key = mc.options.keyAttack;
            look(rotation);

            try {
               press(key, false);
               take(key);
               if (mc.hitResult != null && mc.hitResult.getType() == Type.MISS && attacks(player, false)) {
                  act(true, Clicks::startAttack);
               } else {
                  directMiss(player);
               }
            } finally {
               unlook(rotation);
            }

         }
      }
   }

   public static boolean click(int button) {
      if (mc.player != null && mc.gameMode != null) {
         KeyMapping key = mapping(button);
         if (live()) {
            press(key, false);
            take(key);
         } else {
            note(button);
         }

         return plain(button);
      } else {
         return false;
      }
   }

   public static void queue(int button) {
      KeyMapping key = mapping(button);
      if (mc.player != null && mc.gameMode != null && !guarded(key)) {
         if (live() && bound(key)) {
            press(key, false);
         } else {
            note(button);
            KeyMappingAccessor accessor = (KeyMappingAccessor)key;
            accessor.client$setClickCount(accessor.client$clickCount() + 1);
         }

      }
   }

   public static void hold(int button) {
      hold(button, false, (BooleanSupplier)null);
   }

   public static void holdUse(Rotation rotation, InteractionHand hand) {
      LocalPlayer player = mc.player;
      if (player != null && mc.gameMode != null && !holding(1)) {
         look(rotation);

         try {
            HitResult under = mc.hitResult;
            boolean game = live() && under != null && under.getType() != Type.BLOCK && uses(player);
            if (game) {
               hold(1, true, (BooleanSupplier)null);
            } else {
               hold(1, true, () -> mc.gameMode.useItem(player, hand) instanceof InteractionResult.Success);
            }
         } finally {
            unlook(rotation);
         }

      }
   }

   public static boolean holding(int button) {
      Press press = (Press)presses.get(mapping(button));
      return press != null && press.held;
   }

   public static void release(int button) {
      KeyMapping key = mapping(button);
      Press press = (Press)presses.get(key);
      if (press != null && press.held) {
         end(key, press);
         LocalPlayer player = mc.player;
         if (button == 1 && player != null && mc.gameMode != null && player.isUsingItem() && !key.isDown()) {
            act(true, () -> {
               mc.gameMode.releaseUsingItem(player);
               return true;
            });
         }

      }
   }

   public static void tap(int button) {
      KeyMapping key = mapping(button);
      if (live() && physical(key) && bound(key)) {
         send(key, 0);
         repress = key;
      }

   }

   public static void pressKey(KeyMapping key) {
      if (live() && bound(key)) {
         press(key, false);
      } else {
         KeyMappingAccessor accessor = (KeyMappingAccessor)key;
         accessor.client$setClickCount(accessor.client$clickCount() + 1);
      }

   }

   public static boolean select(int slot) {
      LocalPlayer player = mc.player;
      if (player != null && slot >= 0 && slot <= 8) {
         if (player.getInventory().getSelectedSlot() == slot) {
            return true;
         } else if (!locked(1) && userPicks(slot)) {
            return false;
         } else {
            KeyMapping key = mc.options.keyHotbarSlots[slot];
            if (live() && !down(292)) {
               press(key, false);
               take(key);
            }

            player.getInventory().setSelectedSlot(slot);
            return true;
         }
      } else {
         return false;
      }
   }

   public static String holdName() {
      return holdLow + "-" + holdHigh + " ms";
   }

   public static void setHold(int low, int high) {
      int a = Math.clamp((long)Math.min(low, high), 15, 200);
      int b = Math.clamp((long)Math.max(low, high), 15, 200);
      holdLow = a;
      holdHigh = b;
   }

   public static int now() {
      return tick;
   }

   public static boolean userPickedSince(int since) {
      return userPickTick >= since;
   }

   public static void lock(Object owner, int mask, int ticks) {
      int before = lockedMask();
      Lock old = (Lock)locks.get(owner);
      locks.put(owner, new Lock(old == null ? mask : old.mask | mask, tick + Math.max(1, Math.min(ticks, 200))));
      int added = lockedMask() & ~before;

      for(KeyMapping key : mappings(added & 6)) {
         if (!presses.containsKey(key)) {
            ((KeyMappingAccessor)key).client$setDownRaw(false);
         }
      }

   }

   public static void unlock(Object owner) {
      if (locks.remove(owner) != null) {
         resume();
      }

   }

   public static boolean locked(int mask) {
      return (lockedMask() & mask) != 0;
   }

   public static boolean physical(KeyMapping key) {
      return physical.contains(key(key));
   }

   public static boolean held(KeyMapping key) {
      return (key.isDown() || owned.containsKey(key)) && physical(key);
   }

   public static void own(int button) {
      KeyMapping key = mapping(button);
      owned.merge(key, tick + 2, Math::max);
      if (!presses.containsKey(key) && key.isDown() && physical(key) && mc.gui.screen() == null && !guarded(key)) {
         if (live()) {
            send(key, 0);
         } else {
            ((KeyMappingAccessor)key).client$setDownRaw(false);
         }
      }

   }

   public static boolean owned(KeyMapping key) {
      return owned.containsKey(key);
   }

   public static boolean shown(KeyMapping key) {
      long now = Util.getMillis();
      Long until = (Long)shownUntil.get(key);
      if (!simulated(key) && (until == null || now >= until)) {
         boolean module = owned.containsKey(key) || until != null && now - until < 300L;
         return physical(key) && !module;
      } else {
         return true;
      }
   }

   public static boolean simulated(KeyMapping key) {
      Press press = (Press)presses.get(key);
      return press != null && (press.event || press.forced);
   }

   public static int clicks(int button, boolean module) {
      int count = counts[button][module ? 1 : 0];
      counts[button][module ? 1 : 0] = 0;
      return count;
   }

   public static boolean injecting() {
      return injecting > 0;
   }

   public static boolean targeted() {
      return targeted;
   }

   public static Vec3 view(Entity entity) {
      return view != null && entity == mc.player ? view : null;
   }

   public static void input(InputConstants.Key key, int action) {
      VantaClient.EVENTS.post(new ButtonEvent(key, action, injecting > 0, System.nanoTime()));
      if (injecting > 0) {
         if (action == 1) {
            countPress(key, true);
         }

      } else if (action == 0) {
         physical.remove(key);
      } else {
         if (action == 1 && mc.gui.screen() == null && !ClickGui.isOpen()) {
            VantaClient.MODULES.pressed(key);
         }

         physical.add(key);
         if (action == 1) {
            countPress(key, false);
            if (mc.gui.screen() == null) {
               for(KeyMapping mapping : new ArrayList(presses.keySet())) {
                  Press press = (Press)presses.get(mapping);
                  if (press.event && key(mapping).equals(key) && !guarded(mapping)) {
                     end(mapping, press);
                  }
               }
            }

         }
      }
   }

   public static void guard(InputConstants.Key key, Runnable original) {
      if (injecting <= 0 && !locks.isEmpty() && mc.gui.screen() == null) {
         List<KeyMapping> kept = new ArrayList();

         for(KeyMapping mapping : mappings(lockedMask())) {
            if (key(mapping).equals(key)) {
               kept.add(mapping);
            }
         }

         boolean[] down = new boolean[kept.size()];
         int[] clicks = new int[kept.size()];

         for(int i = 0; i < kept.size(); ++i) {
            down[i] = ((KeyMapping)kept.get(i)).isDown();
            clicks[i] = ((KeyMappingAccessor)kept.get(i)).client$clickCount();
         }

         original.run();

         for(int i = 0; i < kept.size(); ++i) {
            ((KeyMappingAccessor)kept.get(i)).client$setDownRaw(down[i]);
            ((KeyMappingAccessor)kept.get(i)).client$setClickCount(clicks[i]);
         }

      } else {
         original.run();
      }
   }

   public static void picked() {
      userPickTick = tick;
   }

   public static void beforeKeybinds() {
      inKeybinds = true;
      if (mc.player != null) {
         for(KeyMapping key : mappings(lockedMask())) {
            ((KeyMappingAccessor)key).client$setClickCount(0);
         }

         Options options = mc.options;

         for(KeyMapping key : options.keyHotbarSlots) {
            if (((KeyMappingAccessor)key).client$clickCount() > 0) {
               userPickTick = tick;
            }
         }

         if (((KeyMappingAccessor)options.keyPickItem).client$clickCount() > 0) {
            userPickTick = tick;
         }

         Press attack = (Press)presses.get(options.keyAttack);
         if (attack != null && attack.event && !attack.held && mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
            end(options.keyAttack, attack);
         }

         Press use = (Press)presses.get(options.keyUse);
         if (use != null && use.event && !use.held && ((MinecraftAccessor)mc).client$rightClickDelay() == 0) {
            end(options.keyUse, use);
         }

      }
   }

   public static void endTick() {
      ticking = false;
   }

   public static void afterKeybinds() {
      inKeybinds = false;
   }

   public static boolean continues(boolean down) {
      return down && attackedTick != tick;
   }

   public static void attacked(Entity entity) {
      if (processing > 0) {
         landed = true;
      } else if (inKeybinds) {
         realAttack = entity;
         realAttackTick = tick;
      }

   }

   public static void usedOn(BlockHitResult hit) {
      if (inKeybinds && processing == 0) {
         realUse = hit;
         realUseTick = tick;
      }

   }

   public static void useResult(InteractionResult result) {
      if (processing > 0) {
         lastUse = result;
      }

   }

   public static void frame() {
      long now = Util.getMillis();

      for(KeyMapping key : new ArrayList(presses.keySet())) {
         Press press = (Press)presses.get(key);
         if (!press.held && now >= press.until) {
            end(key, press);
         }
      }

   }

   public static void beginTick() {
      if (!ticking) {
         ticking = true;
         ++tick;
         if (mc.player == null) {
            presses.clear();
            locks.clear();
            owned.clear();
            repress = null;
         } else {
            for(KeyMapping key : new ArrayList(owned.keySet())) {
               if (tick >= (Integer)owned.get(key)) {
                  owned.remove(key);
                  if (!presses.containsKey(key) && mc.gui.screen() == null && physical(key) && !guarded(key)) {
                     ((KeyMappingAccessor)key).client$setDownRaw(true);
                  }
               }
            }

            boolean expired = locks.values().removeIf((lock) -> tick >= lock.until);
            if (expired) {
               resume();
            }

            long soon = Util.getMillis() + 25L;

            for(KeyMapping key : new ArrayList(presses.keySet())) {
               Press press = (Press)presses.get(key);
               if (!press.held && press.until <= soon) {
                  end(key, press);
               }
            }

            if (mc.gui.screen() == null) {
               presses.forEach((keyx, pressx) -> {
                  if (pressx.held && (pressx.event || pressx.forced) && !keyx.isDown()) {
                     ((KeyMappingAccessor)keyx).client$setDownRaw(true);
                  }

               });
            }

            KeyMapping key = repress;
            repress = null;
            if (key != null && live() && physical(key)) {
               send(key, 1);
               take(key);
               plain(key == mc.options.keyAttack ? 0 : 1);
            }

         }
      }
   }

   private static String blocked() {
      if (!simulate) {
         return "simulation off";
      } else if (mc.gui.screen() != null) {
         return "screen " + mc.gui.screen().getClass().getSimpleName();
      } else if (mc.gui.overlay() != null) {
         return "overlay " + mc.gui.overlay().getClass().getSimpleName();
      } else if (!mc.mouseHandler.isMouseGrabbed()) {
         return "mouse not grabbed (window active " + mc.isWindowActive() + ")";
      } else {
         return Freecam.camera() != null ? "freecam" : "no player";
      }
   }

   private static void trace(String what, String why) {
      if (TRACE) {
         System.out.println("[Clicks] " + what + " fell back at tick " + tick + ": " + why);
      }

   }

   private static boolean live() {
      return simulate && mc.player != null && mc.gameMode != null && mc.gui.screen() == null && mc.gui.overlay() == null && mc.mouseHandler.isMouseGrabbed() && Freecam.camera() == null;
   }

   private static boolean act(boolean own, BooleanSupplier action) {
      boolean wasTargeted = targeted;
      ++processing;
      targeted = own;

      boolean var3;
      try {
         var3 = action.getAsBoolean();
      } finally {
         targeted = wasTargeted;
         --processing;
      }

      return var3;
   }

   private static boolean startAttack() {
      boolean done = ((MinecraftAccessor)mc).client$startAttack();
      if (done) {
         attackedTick = tick;
      }

      return done;
   }

   private static boolean plain(int button) {
      if (mc.player.isUsingItem()) {
         return false;
      } else {
         return button == 0 ? act(false, Clicks::startAttack) : act(false, () -> {
            ((MinecraftAccessor)mc).client$startUseItem();
            return true;
         });
      }
   }

   private static void hold(int button, boolean own, BooleanSupplier fallback) {
      if (mc.player != null && mc.gameMode != null && !holding(button)) {
         KeyMapping key = mapping(button);
         if (live()) {
            press(key, true);
            take(key);
         } else {
            note(button);
            presses.put(key, new Press(true, false, true, Long.MAX_VALUE));
            key.setDown(true);
         }

         if (fallback != null) {
            act(own, fallback);
         } else {
            if (mc.player.isUsingItem()) {
               return;
            }

            if (button == 0) {
               act(own, Clicks::startAttack);
            } else {
               act(own, () -> {
                  ((MinecraftAccessor)mc).client$startUseItem();
                  return true;
               });
            }
         }

      }
   }

   private static void press(KeyMapping key, boolean held) {
      Press old = (Press)presses.remove(key);
      if (old != null && old.event) {
         send(key, 0);
      }

      long now = Util.getMillis();
      long until = now + duration(key, now);
      shownUntil.put(key, until);
      if (!held && clearsMiss(key)) {
         owned.merge(key, tick + 2 + (int)((until - now) / 50L), Math::max);
         if (key.isDown() && (old == null || !old.event)) {
            send(key, 0);
         }
      }

      if ((!physical(key) || owned.containsKey(key)) && bound(key)) {
         send(key, 1);
         presses.put(key, new Press(held, true, false, until));
      } else {
         KeyMappingAccessor accessor = (KeyMappingAccessor)key;
         accessor.client$setClickCount(accessor.client$clickCount() + 1);
         countPress(key(key), true);
         if (held) {
            boolean forced = !physical(key);
            presses.put(key, new Press(true, false, forced, until));
            if (forced) {
               key.setDown(true);
            }
         }

      }
   }

   private static long duration(KeyMapping key, long now) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int low = Math.max(1, Math.min(holdLow, holdHigh));
      int high = Math.max(low, Math.max(holdLow, holdHigh));
      long hold = (long)low + Math.round((double)(high - low) * (random.nextDouble() + random.nextDouble()) / (double)2.0F);
      ArrayDeque<Long> times = (ArrayDeque)pressedAt.computeIfAbsent(key, (k) -> new ArrayDeque());
      times.addLast(now);
      if (times.size() > 8) {
         times.removeFirst();
      }

      long gap = Long.MAX_VALUE;
      Long previous = null;

      for(long at : times) {
         if (previous != null) {
            gap = Math.min(gap, at - previous);
         }

         previous = at;
      }

      if (gap < (long)high * 2L) {
         hold = Math.min(hold, Math.max(12L, Math.round((double)gap * 0.55)));
      }

      return hold;
   }

   private static void end(KeyMapping key, Press press) {
      presses.remove(key);
      if (press.event) {
         if (mc.gui.screen() == null) {
            send(key, 0);
         } else {
            ((KeyMappingAccessor)key).client$setDownRaw(false);
         }
      } else if (press.forced) {
         key.setDown(physical(key));
      }

   }

   private static void send(KeyMapping key, int action) {
      InputConstants.Key bound = key(key);
      long window = mc.getWindow().handle();
      ++injecting;

      try {
         switch (bound.getType()) {
            case MOUSE -> ((MouseHandlerAccessor)mc.mouseHandler).client$onButton(window, new MouseButtonInfo(bound.getValue(), modifiers()), action);
            case KEYSYM -> ((KeyboardHandlerAccessor)mc.keyboardHandler).client$keyPress(window, action, new KeyEvent(bound.getValue(), GLFW.glfwGetKeyScancode(bound.getValue()), modifiers()));
            case SCANCODE -> ((KeyboardHandlerAccessor)mc.keyboardHandler).client$keyPress(window, action, new KeyEvent(InputConstants.UNKNOWN.getValue(), bound.getValue(), modifiers()));
         }
      } finally {
         --injecting;
      }

   }

   private static int modifiers() {
      int modifiers = 0;
      if (down(340) || down(344)) {
         modifiers |= 1;
      }

      if (down(341) || down(345)) {
         modifiers |= 2;
      }

      if (down(342) || down(346)) {
         modifiers |= 4;
      }

      if (down(343) || down(347)) {
         modifiers |= 8;
      }

      return modifiers;
   }

   private static boolean down(int glfwKey) {
      return physical.contains(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(glfwKey));
   }

   private static void take(KeyMapping key) {
      KeyMappingAccessor accessor = (KeyMappingAccessor)key;
      if (accessor.client$clickCount() > 0) {
         accessor.client$setClickCount(accessor.client$clickCount() - 1);
      }

   }

   private static boolean pending(KeyMapping key) {
      return ((KeyMappingAccessor)key).client$clickCount() > 0;
   }

   private static void countPress(InputConstants.Key key, boolean module) {
      Options options = mc.options;
      if (key.equals(key(options.keyAttack)) && (module || !suppressed(options.keyAttack))) {
         int var10002 = counts[0][module ? 1 : 0]++;
      }

      if (key.equals(key(options.keyUse)) && (module || !suppressed(options.keyUse))) {
         int var3 = counts[1][module ? 1 : 0]++;
      }

   }

   private static boolean suppressed(KeyMapping key) {
      return mc.gui.screen() == null && guarded(key);
   }

   public static void note(int button) {
      int var10002 = counts[button][1]++;
      KeyMapping key = mapping(button);
      long now = Util.getMillis();
      long until = now + duration(key, now);
      shownUntil.put(key, until);
      if (clearsMiss(key)) {
         owned.merge(key, tick + 2 + (int)((until - now) / 50L), Math::max);
         ((KeyMappingAccessor)key).client$setDownRaw(false);
      }

   }

   private static boolean clearsMiss(KeyMapping key) {
      return mc.player != null && key == mc.options.keyAttack && physical(key) && bound(key) && mc.gui.screen() == null && !guarded(key);
   }

   private static boolean guarded(KeyMapping key) {
      return mappings(lockedMask()).contains(key);
   }

   private static boolean userPicks(int slot) {
      KeyMapping[] hotbar = mc.options.keyHotbarSlots;

      for(int i = 0; i < hotbar.length; ++i) {
         if (i != slot && ((KeyMappingAccessor)hotbar[i]).client$clickCount() > 0) {
            return true;
         }
      }

      return userPickTick == tick;
   }

   private static void resume() {
      int still = lockedMask();

      for(KeyMapping key : mappings(14 & ~still)) {
         if (!presses.containsKey(key) && !owned.containsKey(key) && !(key instanceof ToggleKeyMapping)) {
            ((KeyMappingAccessor)key).client$setDownRaw(mc.gui.screen() == null && physical(key));
         }
      }

   }

   private static int lockedMask() {
      int mask = 0;

      for(Lock lock : locks.values()) {
         if (tick < lock.until) {
            mask |= lock.mask;
         }
      }

      return mask;
   }

   private static List<KeyMapping> mappings(int mask) {
      Options options = mc.options;
      List<KeyMapping> keys = new ArrayList();
      if ((mask & 1) != 0) {
         keys.addAll(List.of(options.keyHotbarSlots));
         keys.add(options.keyPickItem);
         keys.add(options.keyDrop);
         keys.add(options.keySwapOffhand);
      }

      if ((mask & 2) != 0) {
         keys.add(options.keyAttack);
      }

      if ((mask & 4) != 0) {
         keys.add(options.keyUse);
      }

      if ((mask & 8) != 0) {
         keys.addAll(List.of(options.keyUp, options.keyDown, options.keyLeft, options.keyRight, options.keyJump, options.keyShift, options.keySprint));
      }

      return keys;
   }

   private static KeyMapping mapping(int button) {
      return button == 0 ? mc.options.keyAttack : mc.options.keyUse;
   }

   private static InputConstants.Key key(KeyMapping key) {
      return ((KeyMappingAccessor)key).client$getKey();
   }

   private static boolean bound(KeyMapping key) {
      return !key(key).equals(InputConstants.UNKNOWN);
   }

   private static void look(Rotation rotation) {
      if (rotation != null) {
         view = rotation.direction();
      }

      ((MinecraftAccessor)mc).client$pick(1.0F);
   }

   private static void unlook(Rotation rotation) {
      if (rotation != null) {
         view = null;
         ((MinecraftAccessor)mc).client$pick(1.0F);
      }

   }

   private static boolean same(BlockHitResult a, BlockHitResult b) {
      return a != null && b != null && a.getBlockPos().equals(b.getBlockPos()) && a.getDirection() == b.getDirection();
   }

   private static boolean attacks(LocalPlayer player, boolean piercing) {
      ItemStack held = player.getMainHandItem();
      AttackRange range = (AttackRange)held.get(DataComponents.ATTACK_RANGE);
      return mc.missTime <= 0 && !player.isHandsBusy() && !player.isUsingItem() && !mc.gameMode.isSpectator() && held.isItemEnabled(player.level().enabledFeatures()) && !player.cannotAttackWithItem(held, 0) && held.has(DataComponents.PIERCING_WEAPON) == piercing && (piercing || range == null || range.isInRange(player, mc.hitResult.getLocation()));
   }

   private static boolean uses(LocalPlayer player) {
      return !mc.gameMode.isDestroying() && !player.isHandsBusy() && !player.isUsingItem() && player.getMainHandItem().isItemEnabled(player.level().enabledFeatures());
   }

   private static void directAttack(LocalPlayer player, Entity target) {
      act(true, () -> {
         mc.gameMode.attack(player, target);
         player.swing(InteractionHand.MAIN_HAND);
         return true;
      });
   }

   private static boolean directUse(LocalPlayer player, BlockHitResult target) {
      return act(true, () -> {
         ItemStack stack = player.getMainHandItem();
         int count = stack.getCount();
         InteractionResult patt0$temp = mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, target);
         if (!(patt0$temp instanceof InteractionResult.Success success)) {
            return false;
         } else {
            if (success.swingSource() == SwingSource.CLIENT) {
               player.swing(InteractionHand.MAIN_HAND);
               if (!stack.isEmpty() && (stack.getCount() != count || player.hasInfiniteMaterials())) {
                  mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND);
               }
            }

            return true;
         }
      });
   }

   private static void directLunge(LocalPlayer player, PiercingWeapon weapon) {
      act(true, () -> {
         mc.gameMode.piercingAttack(weapon);
         player.swing(InteractionHand.MAIN_HAND);
         return true;
      });
   }

   private static void directMiss(LocalPlayer player) {
      player.resetAttackStrengthTicker();
      player.swing(InteractionHand.MAIN_HAND);
   }

   private Clicks() {
   }

   private static record Press(boolean held, boolean event, boolean forced, long until) {
   }

   private static record Lock(int mask, int until) {
   }
}
