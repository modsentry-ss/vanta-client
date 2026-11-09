package vanta.mixin.extra;

import vanta.module.impl.misc.ClientSpoofer;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {ClientBrandRetriever.class},
   priority = 9999
)
public abstract class MixinClientBrandRetriever {
   @Inject(
      method = {"getClientModName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void frost$brand(CallbackInfoReturnable<String> cir) {
      String brand = ClientSpoofer.spoofedBrand();
      if (brand != null) {
         cir.setReturnValue(brand);
      }

   }
}
