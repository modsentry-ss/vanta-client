package vanta.mixin.network;

import vanta.VantaClient;
import vanta.event.events.AttackEvent;
import vanta.event.events.BlockUseEvent;
import vanta.util.Clicks;
import vanta.util.ClientPearls;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public abstract class MultiPlayerGameModeMixin {
   @Inject(
      method = {"attack"},
      at = {@At("HEAD")}
   )
   private void client$attack(Player player, Entity target, CallbackInfo info) {
      VantaClient.EVENTS.post(new AttackEvent(player, target));
   }

   @Inject(
      method = {"attack"},
      at = {@At("HEAD")}
   )
   private void client$attackClick(Player player, Entity target, CallbackInfo info) {
      Clicks.attacked(target);
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At("HEAD")}
   )
   private void client$useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> info) {
      Clicks.usedOn(hit);
      VantaClient.EVENTS.post(new BlockUseEvent(player, hand, hit));
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At("RETURN")}
   )
   private void client$useItemOnResult(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> info) {
      Clicks.useResult((InteractionResult)info.getReturnValue());
   }

   @Inject(
      method = {"useItem"},
      at = {@At("HEAD")}
   )
   private void client$beforeUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
      ClientPearls.beforeUse(player, hand);
   }

   @Inject(
      method = {"useItem"},
      at = {@At("RETURN")}
   )
   private void client$afterUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
      ClientPearls.afterUse(player, hand, (InteractionResult)info.getReturnValue());
   }
}
