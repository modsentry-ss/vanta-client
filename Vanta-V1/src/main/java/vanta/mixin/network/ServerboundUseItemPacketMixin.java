package vanta.mixin.network;

import vanta.VantaClient;
import vanta.util.ClientPearls;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerboundUseItemPacket.class})
public abstract class ServerboundUseItemPacketMixin {
   @ModifyVariable(
      method = {"<init>(Lnet/minecraft/world/InteractionHand;IFF)V"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 0
   )
   private static float client$yaw(float yaw) {
      return VantaClient.ROTATIONS.sentYaw(yaw);
   }

   @ModifyVariable(
      method = {"<init>(Lnet/minecraft/world/InteractionHand;IFF)V"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 1
   )
   private static float client$pitch(float pitch) {
      return VantaClient.ROTATIONS.sentPitch(pitch);
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/world/InteractionHand;IFF)V"},
      at = {@At("RETURN")}
   )
   private void client$pearlRotation(InteractionHand hand, int sequence, float yRot, float xRot, CallbackInfo info) {
      ServerboundUseItemPacket packet = (ServerboundUseItemPacket)this;
      ClientPearls.usePacket(packet.getYRot(), packet.getXRot());
   }
}
