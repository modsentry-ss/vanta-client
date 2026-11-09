package vanta.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import vanta.VantaClient;
import vanta.event.events.WorldRenderEvent;
import vanta.render.Style;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;

public final class WorldRenderer {
   private static volatile List<WorldBuffer.Box> frame = List.of();
   private static volatile List<WorldBuffer.Box> frameBehindWalls = List.of();
   private static long frameCount;

   public static long frame() {
      return frameCount;
   }

   public static void init() {
      LevelExtractionEvents.END_EXTRACTION.register(WorldRenderer::extract);
      LevelRenderEvents.COLLECT_SUBMITS.register(WorldRenderer::submit);
      LevelRenderEvents.START_MAIN.register((LevelRenderEvents.StartMain)(context) -> TerrainDepth.reset());
   }

   public static void afterOpaqueTerrain(LevelRenderState state) {
      TerrainDepth.capture(state.cameraRenderState, frameBehindWalls);
   }

   private static void extract(LevelExtractionContext context) {
      WorldBuffer buffer = new WorldBuffer();
      VantaClient.EVENTS.post(new WorldRenderEvent(context.level(), context.camera(), context.deltaTracker(), buffer));
      frame = buffer.boxes();
      frameBehindWalls = buffer.behindWalls();
   }

   private static void submit(LevelRenderContext context) {
      ++frameCount;
      List<WorldBuffer.Box> boxes = frame;
      List<WorldBuffer.Box> hidden = frameBehindWalls;
      if (!hidden.isEmpty()) {
         Vec3 at = context.levelState().cameraRenderState.pos;
         BoxGeometry.towardCamera(true);

         try {
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), WorldRenderTypes.FILL_DEPTH, (pose, consumer) -> {
               BoxGeometry.towardCamera(true);

               try {
                  submitFills(pose, consumer, hidden, at);
               } finally {
                  BoxGeometry.towardCamera(false);
               }

            });
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), WorldRenderTypes.LINES_DEPTH, (pose, consumer) -> {
               BoxGeometry.towardCamera(true);

               try {
                  submitEdges(pose, consumer, hidden, at);
               } finally {
                  BoxGeometry.towardCamera(false);
               }

            });
         } finally {
            BoxGeometry.towardCamera(false);
         }
      }

      if (!boxes.isEmpty()) {
         Vec3 camera = context.levelState().cameraRenderState.pos;
         SubmitNodeCollector collector = context.submitNodeCollector();
         PoseStack poseStack = context.poseStack();
         collector.submitCustomGeometry(poseStack, WorldRenderTypes.FILL, (pose, consumer) -> submitFills(pose, consumer, boxes, camera));
         collector.submitCustomGeometry(poseStack, WorldRenderTypes.LINES, (pose, consumer) -> submitEdges(pose, consumer, boxes, camera));
      }

   }

   private static void submitFills(PoseStack.Pose pose, VertexConsumer consumer, List<WorldBuffer.Box> boxes, Vec3 camera) {
      for(WorldBuffer.Box box : boxes) {
         if (box.style().hasFill()) {
            BoxGeometry.fill(pose, consumer, box.bounds(), camera, box.style().fill());
         }
      }

   }

   private static void submitEdges(PoseStack.Pose pose, VertexConsumer consumer, List<WorldBuffer.Box> boxes, Vec3 camera) {
      for(WorldBuffer.Box box : boxes) {
         Style style = box.style();
         float border = style.outlineWidth() - style.strokeWidth();
         if (style.hasOutline() && !(border < 2.0F)) {
            BoxGeometry.edges(pose, consumer, box.bounds(), camera, style.outline(), lineWidth(style) + border);
         }
      }

      for(WorldBuffer.Box box : boxes) {
         Style style = box.style();
         if (style.hasStroke()) {
            BoxGeometry.edges(pose, consumer, box.bounds(), camera, style.stroke(), lineWidth(style));
         }
      }

   }

   private static float lineWidth(Style style) {
      return Math.max(1.0F, style.strokeWidth());
   }

   private WorldRenderer() {
   }
}
