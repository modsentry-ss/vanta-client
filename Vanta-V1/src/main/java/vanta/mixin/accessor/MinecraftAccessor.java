package vanta.mixin.accessor;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Minecraft.class})
public interface MinecraftAccessor {
   @Invoker("startAttack")
   boolean client$startAttack();

   @Invoker("startUseItem")
   void client$startUseItem();

   @Invoker("pick")
   void client$pick(float var1);

   @Accessor("rightClickDelay")
   void client$setRightClickDelay(int var1);

   @Accessor("rightClickDelay")
   int client$rightClickDelay();
}
