package vanta.visual;

import java.util.ArrayList;
import java.util.List;
import vanta.VantaClient;
import vanta.VantaCore;
import vanta.event.Event;
import vanta.render.Gfx;
import vanta.visual.hud.HudKit;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class Visuals {
   private static final Minecraft mc = Minecraft.getInstance();
   private static DeltaTracker delta;
   private static final WorldLayer LAYER = new WorldLayer();
   private static final List<Runnable> requested = new ArrayList();

   private Visuals() {
   }

   public static void init() {
      CullBypass.init();
      HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, VantaCore.id("world_visuals"), Visuals::worldLayer);
   }

   private static void worldLayer(GuiGraphicsExtractor vanilla, DeltaTracker deltaTracker) {
      if (mc.player != null && mc.level != null) {
         LAYER.graphics = vanilla;
         LAYER.deltaTracker = deltaTracker;
         VantaClient.EVENTS.post(LAYER);

         for(int i = 0; i < requested.size(); ++i) {
            try {
               draw(LAYER, (Runnable)requested.get(i));
            } catch (RuntimeException error) {
               VantaCore.LOGGER.error("World visual failed", error);
            }
         }

         requested.clear();
      } else {
         requested.clear();
      }
   }

   public static void draw(WorldLayer event, Runnable draw) {
      draw(event.graphics(), event.deltaTracker(), draw);
   }

   private static void draw(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Runnable draw) {
      if (mc.player != null && mc.level != null) {
         delta = deltaTracker;
         Gfx.begin(graphics);

         try {
            Scene.frame();
            draw.run();
         } finally {
            Fx.discardText();
            HudKit.discardText();
            Gfx.end();
         }

      }
   }

   public static void drawWorld(WorldLayer event, Runnable draw) {
      draw(event, draw);
   }

   public static boolean replaying() {
      return false;
   }

   public static float partialTicks(Entity entity) {
      DeltaTracker tracker = delta != null ? delta : mc.getDeltaTracker();
      boolean frozen = mc.level != null && mc.level.tickRateManager().isEntityFrozen(entity);
      return tracker.getGameTimeDeltaPartialTick(!frozen);
   }

   public static Vec3 position(Entity entity) {
      float partial = partialTicks(entity);
      return new Vec3(Mth.lerp((double)partial, entity.xOld, entity.getX()), Mth.lerp((double)partial, entity.yOld, entity.getY()), Mth.lerp((double)partial, entity.zOld, entity.getZ()));
   }

   public static void position(Entity entity, double[] out) {
      float partial = partialTicks(entity);
      out[0] = Mth.lerp((double)partial, entity.xOld, entity.getX());
      out[1] = Mth.lerp((double)partial, entity.yOld, entity.getY());
      out[2] = Mth.lerp((double)partial, entity.zOld, entity.getZ());
   }

   public static final class WorldLayer implements Event {
      private GuiGraphicsExtractor graphics;
      private DeltaTracker deltaTracker;

      public GuiGraphicsExtractor graphics() {
         return this.graphics;
      }

      public DeltaTracker deltaTracker() {
         return this.deltaTracker;
      }
   }
}
