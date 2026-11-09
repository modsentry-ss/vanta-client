package vanta.mixin.input;

import vanta.VantaClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public abstract class KeyboardInputMixin extends ClientInput {
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void client$correctInput(CallbackInfo info) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         Vec2 corrected = VantaClient.ROTATIONS.correctInput(this.moveVector, player.getYRot());
         if (corrected != this.moveVector) {
            this.moveVector = corrected;
            Input keys = this.keyPresses;
            this.keyPresses = new Input(corrected.y > 0.0F, corrected.y < 0.0F, corrected.x > 0.0F, corrected.x < 0.0F, keys.jump(), keys.shift(), keys.sprint());
         }
      }

   }
}
