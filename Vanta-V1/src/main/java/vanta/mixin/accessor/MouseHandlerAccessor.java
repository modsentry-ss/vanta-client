package vanta.mixin.accessor;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MouseHandler.class})
public interface MouseHandlerAccessor {
   @Invoker("onButton")
   void client$onButton(long var1, MouseButtonInfo var3, int var4);

   @Invoker("onScroll")
   void client$onScroll(long var1, double var3, double var5);

   @Invoker("turnPlayer")
   void client$turnPlayer(double var1);

   @Accessor("accumulatedDX")
   double client$accumulatedDX();

   @Accessor("accumulatedDX")
   void client$setAccumulatedDX(double var1);
}
