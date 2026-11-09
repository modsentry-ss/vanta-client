package vanta.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({AbstractContainerScreen.class})
public interface AbstractContainerScreenAccessor {
   @Accessor("hoveredSlot")
   Slot client$getHoveredSlot();

   @Accessor("leftPos")
   int client$getLeftPos();

   @Accessor("topPos")
   int client$getTopPos();

   @Invoker("slotClicked")
   void client$slotClicked(Slot var1, int var2, int var3, ContainerInput var4);
}
