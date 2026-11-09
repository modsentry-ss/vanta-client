package vanta.mixin.input;

import vanta.module.impl.render.Freecam;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public abstract class FreecamInputMixin extends ClientInput {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$freecam(CallbackInfo info) {
      Freecam freecam = Freecam.controls();
      if (freecam != null) {
         this.keyPresses = freecam.playerInput();
         float strafe = (this.keyPresses.left() ? 1.0F : 0.0F) - (this.keyPresses.right() ? 1.0F : 0.0F);
         float forward = (this.keyPresses.forward() ? 1.0F : 0.0F) - (this.keyPresses.backward() ? 1.0F : 0.0F);
         this.moveVector = (new Vec2(strafe, forward)).normalized();
         info.cancel();
      }

   }
}
