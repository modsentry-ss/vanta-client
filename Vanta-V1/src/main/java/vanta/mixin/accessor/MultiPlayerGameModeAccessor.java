package vanta.mixin.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MultiPlayerGameMode.class})
public interface MultiPlayerGameModeAccessor {
   @Invoker("startPrediction")
   void client$startPrediction(ClientLevel var1, PredictiveAction var2);

   @Accessor("carriedIndex")
   int client$getCarriedIndex();

   @Accessor("carriedIndex")
   void client$setCarriedIndex(int var1);
}
