package vanta.render.screen;

import com.mojang.math.Axis;
import vanta.render.Corners;
import vanta.render.Point;
import vanta.render.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class Projection implements Projector {
   public static final float NEAR = 0.05F;
   private final Matrix4f viewProjection;
   private final Vec3 origin;
   private final float width;
   private final float height;
   private final float pixel;

   public Projection(Matrix4fc viewProjection, Vec3 origin, float width, float height, float pixel) {
      this.viewProjection = new Matrix4f(viewProjection);
      this.origin = origin;
      this.width = width;
      this.height = height;
      this.pixel = pixel;
   }

   public static Projection capture() {
      GameRenderState state = Minecraft.getInstance().gameRenderer.gameRenderState();
      CameraRenderState camera = state.levelRenderState.cameraRenderState;
      WindowRenderState window = state.windowRenderState;
      float pixel = 1.0F / (float)Math.max(1, window.guiScale);
      return new Projection(viewProjection(camera, state.optionsRenderState), camera.pos, (float)window.width * pixel, (float)window.height * pixel, pixel);
   }

   public Point project(double x, double y, double z) {
      Vector4f clip = this.clip(x, y, z, new Vector4f());
      return inFront(clip) ? this.toScreen(clip) : null;
   }

   public Rect bounds(AABB box) {
      Vector4f[] corners = new Vector4f[8];

      for(int i = 0; i < corners.length; ++i) {
         corners[i] = this.clip(Corners.x(box, i), Corners.y(box, i), Corners.z(box, i), new Vector4f());
      }

      Extent extent = new Extent();

      for(Vector4f corner : corners) {
         if (inFront(corner)) {
            extent.add(this.toScreen(corner));
         }
      }

      for(int i = 0; i < Corners.EDGES.length; i += 2) {
         Vector4f a = corners[Corners.EDGES[i]];
         Vector4f b = corners[Corners.EDGES[i + 1]];
         if (inFront(a) != inFront(b)) {
            extent.add(this.toScreen(nearIntersection(a, b)));
         }
      }

      return extent.toRect();
   }

   public Vector4f clipPoint(double x, double y, double z) {
      return this.clip(x, y, z, new Vector4f());
   }

   public Point screen(Vector4fc clip) {
      return this.toScreen(clip);
   }

   public static boolean visible(Vector4fc clip) {
      return inFront(clip);
   }

   public Vec3 origin() {
      return this.origin;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }

   public int framebufferX(float x) {
      return Math.round(x / this.pixel);
   }

   public int framebufferY(float y) {
      return Math.round((this.height - y) / this.pixel);
   }

   private Vector4f clip(double x, double y, double z, Vector4f dest) {
      return this.viewProjection.transform((float)(x - this.origin.x), (float)(y - this.origin.y), (float)(z - this.origin.z), 1.0F, dest);
   }

   private Point toScreen(Vector4fc clip) {
      float inverse = 1.0F / clip.w();
      return new Point((clip.x() * inverse + 1.0F) * 0.5F * this.width, (1.0F - clip.y() * inverse) * 0.5F * this.height, clip.w());
   }

   private static boolean inFront(Vector4fc clip) {
      return clip.w() > 0.05F;
   }

   private static Vector4f nearIntersection(Vector4f a, Vector4f b) {
      return (new Vector4f(a)).lerp(b, (0.05F - a.w) / (b.w - a.w));
   }

   private static Matrix4f viewProjection(CameraRenderState camera, OptionsRenderState options) {
      Matrix4f bob = new Matrix4f();
      hurtTilt(camera.entityRenderState, options, bob);
      if (options.bobView) {
         viewBob(camera.entityRenderState, bob);
      }

      return (new Matrix4f(camera.projectionMatrix)).mul(bob).mul(camera.viewRotationMatrix);
   }

   private static void hurtTilt(CameraEntityRenderState entity, OptionsRenderState options, Matrix4f pose) {
      if (entity.isLiving) {
         if (entity.isDeadOrDying) {
            float death = Math.min(entity.deathTime, 20.0F);
            pose.rotate(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (death + 200.0F)));
         }

         if (!(entity.hurtTime < 0.0F)) {
            float progress = entity.hurtTime / (float)entity.hurtDuration;
            float tilt = Mth.sin((double)(progress * progress * progress * progress * (float)Math.PI));
            pose.rotate(Axis.YP.rotationDegrees(-entity.hurtDir));
            pose.rotate(Axis.ZP.rotationDegrees((float)((double)(-tilt) * (double)14.0F * options.damageTiltStrength)));
            pose.rotate(Axis.YP.rotationDegrees(entity.hurtDir));
         }
      }

   }

   private static void viewBob(CameraEntityRenderState entity, Matrix4f pose) {
      if (entity.isPlayer) {
         float walk = entity.backwardsInterpolatedWalkDistance;
         float bob = entity.bob;
         pose.translate(Mth.sin((double)(walk * (float)Math.PI)) * bob * 0.5F, -Math.abs(Mth.cos((double)(walk * (float)Math.PI)) * bob), 0.0F);
         pose.rotate(Axis.ZP.rotationDegrees(Mth.sin((double)(walk * (float)Math.PI)) * bob * 3.0F));
         pose.rotate(Axis.XP.rotationDegrees(Math.abs(Mth.cos((double)(walk * (float)Math.PI - 0.2F)) * bob) * 5.0F));
      }

   }

   public float pixel() {
      return this.pixel;
   }
}
