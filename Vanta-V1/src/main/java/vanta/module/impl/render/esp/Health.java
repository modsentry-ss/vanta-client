package vanta.module.impl.render.esp;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import vanta.render.screen.HealthBar;
import vanta.setting.BoolSetting;
import vanta.setting.EnumSetting;
import vanta.setting.SliderSetting;
import vanta.visual.style.TargetDraw;
import net.minecraft.world.entity.LivingEntity;

final class Health {
   private static final String[] POSITIONS = new String[]{"Left", "Right", "Top", "Bottom"};
   private static final HealthBar.Side[] SIDES;
   private static final long TRAIL_HOLD_MILLIS = 300L;
   private static final float DRAIN_PER_SECOND = 1.6F;
   private static final long FORGET_MILLIS = 1500L;
   private final Esp esp;
   private final Box box;
   private final EnumSetting position;
   private final EnumSetting colors;
   private final BoolSetting number;
   private final SliderSetting width;
   private final Int2ObjectOpenHashMap<Bar> bars = new Int2ObjectOpenHashMap();
   private float dt;
   private long now;
   private long lastFrame;
   private boolean replay;
   private long pruned;

   Health(Esp esp, Box box) {
      this.esp = esp;
      this.box = box;
      this.position = (EnumSetting)esp.setting(new EnumSetting("healthPosition", 0, POSITIONS));
      this.colors = (EnumSetting)esp.setting(new EnumSetting("healthColor", 0, TargetDraw.HEALTH_COLORS));
      this.number = (BoolSetting)esp.setting(new BoolSetting("healthText", false));
      this.width = (SliderSetting)esp.setting((new SliderSetting("healthWidth", 2, 1, 8)).withFormat(Esp::pixels));
      this.width.visibleWhen(() -> this.side() != null);
      this.position.visibleWhen(() -> this.side() != null);
      this.colors.visibleWhen(() -> this.side() != null || esp.coloredByHealth());
      this.number.visibleWhen(() -> this.side() != null);
      this.colors.describe();
      this.number.describe();
   }

   HealthBar.Side side() {
      int side = this.sideIndex();
      return side < 0 ? null : SIDES[side];
   }

   int sideIndex() {
      return !this.esp.shaded() && !this.esp.outlined() && this.box.shows(3) ? Math.clamp((long)(Integer)this.position.get(), 0, SIDES.length - 1) : -1;
   }

   boolean smooth() {
      return (Integer)this.colors.get() == 1;
   }

   float widthPixels() {
      return (float)Math.clamp((long)(Integer)this.width.get(), 1, 8);
   }

   boolean numbered() {
      return (Boolean)this.number.get();
   }

   int color(float fraction) {
      return TargetDraw.health(fraction, this.smooth());
   }

   void beginFrame(boolean replay) {
      this.replay = replay;
      if (replay) {
         this.dt = 0.0F;
      } else {
         long now = System.currentTimeMillis();
         this.dt = this.lastFrame == 0L ? 0.0F : Math.clamp((float)(now - this.lastFrame) / 1000.0F, 0.0F, 0.1F);
         this.lastFrame = now;
         this.now = now;
         if (now - this.pruned > 1000L) {
            this.pruned = now;
            ObjectIterator<Int2ObjectMap.Entry<Bar>> it = this.bars.int2ObjectEntrySet().fastIterator();

            while(it.hasNext()) {
               if (now - ((Bar)((Int2ObjectMap.Entry)it.next()).getValue()).seen > 1500L) {
                  it.remove();
               }
            }
         }

      }
   }

   void clear() {
      this.bars.clear();
      this.lastFrame = 0L;
   }

   Bar bar(LivingEntity living) {
      float max = living.getMaxHealth();
      float fraction = max <= 0.0F ? 0.0F : Math.clamp(living.getHealth() / max, 0.0F, 1.0F);
      Bar bar = (Bar)this.bars.get(living.getId());
      if (bar == null) {
         bar = new Bar();
         bar.shown = fraction;
         bar.trail = fraction;
         bar.seen = this.now;
         this.bars.put(living.getId(), bar);
         return bar;
      } else if (this.replay) {
         return bar;
      } else {
         if (fraction < bar.shown - 5.0E-4F) {
            bar.holdUntil = this.now + 300L;
            bar.trail = Math.max(bar.trail, bar.shown);
         }

         bar.shown = fraction;
         if (bar.trail <= fraction) {
            bar.trail = fraction;
         } else if (this.now > bar.holdUntil) {
            bar.trail = Math.max(fraction, bar.trail - 1.6F * this.dt);
         }

         bar.seen = this.now;
         return bar;
      }
   }

   static {
      SIDES = new HealthBar.Side[]{HealthBar.Side.LEFT, HealthBar.Side.RIGHT, HealthBar.Side.TOP, HealthBar.Side.BOTTOM};
   }

   static final class Bar {
      float shown = 1.0F;
      float trail = 1.0F;
      long holdUntil;
      long seen;
   }
}
