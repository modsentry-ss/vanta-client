package vanta.mixin.network;

import vanta.VantaClient;
import vanta.event.events.TotemPopEvent;
import vanta.rotation.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.world.entity.Relative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPacketListener.class})
public abstract class ClientPacketListenerMixin {
   @Inject(
      method = {"handleMovePlayer"},
      at = {@At("TAIL")}
   )
   private void client$syncMove(ClientboundPlayerPositionPacket packet, CallbackInfo info) {
      if (!packet.relatives().contains(Relative.Y_ROT) || !packet.relatives().contains(Relative.X_ROT)) {
         client$sync();
      }

   }

   @Inject(
      method = {"handleRotatePlayer"},
      at = {@At("TAIL")}
   )
   private void client$syncRotate(ClientboundPlayerRotationPacket packet, CallbackInfo info) {
      client$sync();
   }

   @Inject(
      method = {"handleEntityEvent"},
      at = {@At("HEAD")}
   )
   private void client$totemPop(ClientboundEntityEventPacket packet, CallbackInfo info) {
      Minecraft client = Minecraft.getInstance();
      if (packet.getEventId() == 35 && client.level != null && client.isSameThread() && packet.getEntity(client.level) == client.player) {
         VantaClient.EVENTS.post(new TotemPopEvent(client));
      }

   }

   @Unique
   private static void client$sync() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         VantaClient.ROTATIONS.sync(Rotation.of(player));
      }

   }
}
