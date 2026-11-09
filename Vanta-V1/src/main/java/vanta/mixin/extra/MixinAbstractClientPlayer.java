package vanta.mixin.extra;

import vanta.VantaClient;
import vanta.module.impl.movement.Sprint;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {AbstractClientPlayer.class},
   priority = 9999
)
public abstract class MixinAbstractClientPlayer {
   @Redirect(
      method = {"getFieldOfViewModifier"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"
)}
   )
   private double frost$fov(AbstractClientPlayer player, Holder<Attribute> attribute) {
      double value = player.getAttributeValue(attribute);
      if (player instanceof LocalPlayer && VantaClient.MODULES != null) {
         Sprint sprint = (Sprint)VantaClient.MODULES.get(Sprint.class);
         AttributeInstance instance = player.getAttribute(attribute);
         if (sprint != null && sprint.isEnabled() && instance != null && sprint.shouldApplyFovFix(instance)) {
            return value * 1.300000011920929;
         }
      }

      return value;
   }
}
