package vanta.module.impl.render;

import vanta.module.Module;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class FreeLook extends Module {
   private float cameraYaw;
   private float cameraPitch;
   private boolean active;
   private CameraType previous;

   public FreeLook() {
      super("freeLook");
   }

   protected void onEnable() {
      LocalPlayer player = mc.player;
      if (player != null) {
         this.cameraYaw = player.getYRot();
         this.cameraPitch = player.getXRot();
         this.active = true;
         this.previous = mc.options.getCameraType();
         if (this.previous == CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         }
      }

   }

   protected void onDisable() {
      this.active = false;
      this.previous = null;
      if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
         mc.options.setCameraType(CameraType.FIRST_PERSON);
      }

   }

   public boolean isFreeLooking() {
      return this.isEnabled() && this.active && mc.player != null;
   }

   public void updateCamera(double yawDelta, double pitchDelta) {
      this.cameraYaw += (float)yawDelta * 0.15F;
      this.cameraPitch = Mth.clamp(this.cameraPitch + (float)pitchDelta * 0.15F, -90.0F, 90.0F);
   }

   public float getCameraYaw() {
      return this.cameraYaw;
   }

   public float getCameraPitch() {
      return this.cameraPitch;
   }
}
