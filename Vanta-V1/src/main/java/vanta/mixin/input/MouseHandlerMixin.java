package vanta.mixin.input;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import vanta.VantaClient;
import vanta.event.events.MouseMoveEvent;
import vanta.event.events.TurnEvent;
import vanta.module.impl.combat.PearlCatch;
import vanta.module.impl.render.FreeLook;
import vanta.module.impl.render.Freecam;
import vanta.util.Clicks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public abstract class MouseHandlerMixin {
   @Unique
   private static final float TICKS_PER_SECOND = 20.0F;

   @Redirect(
      method = {"turnPlayer"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
)}
   )
   private void client$mouseTurn(LocalPlayer player, double yaw, double pitch) {
      FreeLook freeLook = (FreeLook)VantaClient.MODULES.get(FreeLook.class);
      if (freeLook != null && freeLook.isFreeLooking()) {
         freeLook.updateCamera(yaw, pitch);
      } else {
         Freecam freecam = Freecam.controls();
         if (freecam != null) {
            freecam.turn(yaw, pitch);
         } else {
            PearlCatch pearl = (PearlCatch)VantaClient.MODULES.get(PearlCatch.class);
            if (!pearl.locksMouse()) {
               if (yaw != (double)0.0F || pitch != (double)0.0F) {
                  pearl.mouseTurned();
               }

               player.turn(yaw, pitch);
            }

         }
      }
   }

   @Redirect(
      method = {"onScroll"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"
)}
   )
   private int client$scroll(double wheel, int current, int limit) {
      if (!Freecam.scrolled(wheel) && !Clicks.locked(1)) {
         Clicks.picked();
         return ScrollWheelHandler.getNextScrollWheelSelection(wheel, current, limit);
      } else {
         return current;
      }
   }

   @Inject(
      method = {"onButton"},
      at = {@At("HEAD")}
   )
   private void client$button(long handle, MouseButtonInfo info, int action, CallbackInfo callback) {
      if (handle == Minecraft.getInstance().getWindow().handle()) {
         Clicks.input(Type.MOUSE.getOrCreate(info.button()), action);
      }

   }

   @WrapOperation(
      method = {"onButton"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V"
)}
   )
   private void client$buttonSet(InputConstants.Key key, boolean down, Operation<Void> original) {
      Clicks.guard(key, () -> original.call(new Object[]{key, down}));
   }

   @WrapOperation(
      method = {"onButton"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"
)}
   )
   private void client$buttonClick(InputConstants.Key key, Operation<Void> original) {
      Clicks.guard(key, () -> original.call(new Object[]{key}));
   }

   @Inject(
      method = {"turnPlayer"},
      at = {@At("TAIL")}
   )
   private void client$turned(double seconds, CallbackInfo info) {
      VantaClient.EVENTS.post(new TurnEvent(Minecraft.getInstance(), (float)seconds * 20.0F));
   }

   @Inject(
      method = {"handleAccumulatedMovement"},
      at = {@At("TAIL")}
   )
   private void client$moved(CallbackInfo info) {
      Minecraft client = Minecraft.getInstance();
      if (client.gui.screen() != null) {
         MouseHandler handler = (MouseHandler)this;
         VantaClient.EVENTS.post(new MouseMoveEvent(client, handler.getScaledXPos(client.getWindow()), handler.getScaledYPos(client.getWindow())));
      }

   }
}
