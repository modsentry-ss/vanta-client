package vanta.mixin.render;

import vanta.module.impl.render.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public abstract class FreecamCameraMixin {
   @Shadow
   private boolean detached;
   @Shadow
   private Vec3 position;
   @Shadow
   private float xRot;
   @Shadow
   private float yRot;
   @Shadow
   private Entity entity;
   @Shadow
   private Level level;
   @Shadow
   private boolean isPanoramicMode;
   @Shadow
   @Final
   private BlockPos.MutableBlockPos blockPosition;

   @Shadow
   protected abstract void setPosition(Vec3 var1);

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Unique
   private boolean client$main() {
      Minecraft mc = Minecraft.getInstance();
      return mc.gameRenderer != null && this == mc.gameRenderer.mainCamera() && this.entity == mc.player && this.entity != null;
   }

   @Inject(
      method = {"alignWithEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$freecam(float partialTicks, CallbackInfo info) {
      if (this.client$main()) {
         Freecam freecam = Freecam.camera();
         if (freecam != null && !freecam.returning()) {
            Vec3 at = freecam.framePosition(partialTicks);
            this.setRotation(freecam.yaw(), freecam.pitch());
            this.setPosition(at);
            this.detached = freecam.showsBody(at);
            info.cancel();
         }

      }
   }

   @Inject(
      method = {"alignWithEntity"},
      at = {@At("TAIL")}
   )
   private void client$freecamReturn(float partialTicks, CallbackInfo info) {
      if (this.client$main()) {
         Freecam freecam = Freecam.camera();
         if (freecam != null && freecam.returning()) {
            float progress = freecam.returnProgress();
            Vec3 to = this.position;
            float yaw = freecam.returnYaw() + Mth.wrapDegrees(this.yRot - freecam.returnYaw()) * progress;
            float pitch = Mth.lerp(progress, freecam.returnPitch(), this.xRot);
            Vec3 at = freecam.returnFrom().lerp(to, (double)progress);
            boolean vanillaDetached = this.detached;
            this.setRotation(yaw, pitch);
            this.setPosition(at);
            this.detached = vanillaDetached || freecam.showsBody(at);
         }

      }
   }

   @Inject(
      method = {"calculateFov"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void client$freecamFov(float partialTicks, CallbackInfoReturnable<Float> info) {
      if (!this.isPanoramicMode && this.client$main()) {
         Freecam freecam = Freecam.camera();
         if (freecam != null) {
            FogType fluid = ((Camera)this).getFluidInCamera();
            float narrowing = fluid != FogType.WATER && fluid != FogType.LAVA ? 1.0F : Mth.lerp(((Double)Minecraft.getInstance().options.fovEffectScale().get()).floatValue(), 1.0F, 0.85714287F);
            info.setReturnValue(freecam.fov((Float)info.getReturnValue(), narrowing));
         }

      }
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("TAIL")}
   )
   private void client$freecamState(CameraRenderState cameraState, float partialTicks, CallbackInfo info) {
      if (this.client$main() && Freecam.camera() != null) {
         CameraEntityRenderState body = cameraState.entityRenderState;
         body.hurtTime = -1.0F;
         body.isDeadOrDying = false;
         body.bob = 0.0F;
         if (this.level != null && this.level.getBlockState(this.blockPosition).isSolidRender()) {
            cameraState.smartCull = false;
         }

      }
   }
}
