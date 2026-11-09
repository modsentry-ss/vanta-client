package vanta.mixin.input;

import vanta.VantaClient;
import vanta.event.events.PacketProcessEvent;
import vanta.util.Clicks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MinecraftMixin {
   @Shadow
   public HitResult hitResult;
   @Shadow
   public MultiPlayerGameMode gameMode;

   @Inject(
      method = {"runTick"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/PacketProcessor;processQueuedPackets()V",
   shift = Shift.AFTER
)}
   )
   private void client$packetsProcessed(boolean renderLevel, CallbackInfo info) {
      VantaClient.EVENTS.post(new PacketProcessEvent((Minecraft)this));
      Clicks.frame();
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void client$tickHead(CallbackInfo info) {
      Clicks.beginTick();
   }

   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   private void client$tickReturn(CallbackInfo info) {
      Clicks.endTick();
   }

   @Inject(
      method = {"handleKeybinds"},
      at = {@At("HEAD")}
   )
   private void client$keybinds(CallbackInfo info) {
      Clicks.beforeKeybinds();
   }

   @Inject(
      method = {"handleKeybinds"},
      at = {@At("RETURN")}
   )
   private void client$keybindsDone(CallbackInfo info) {
      Clicks.afterKeybinds();
   }

   @ModifyArg(
      method = {"handleKeybinds"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V"
)}
   )
   private boolean client$continueAttackArg(boolean down) {
      return Clicks.continues(down);
   }

   @Inject(
      method = {"startAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void client$startAttack(CallbackInfoReturnable<Boolean> info) {
      if (!Clicks.targeted()) {
         ;
      }
   }
}
