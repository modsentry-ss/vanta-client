package vanta.mixin.accessor;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CreativeModeInventoryScreen.class})
public interface ClickGuiCreativeAccessor {
   @Accessor("searchBox")
   EditBox mace$searchBox();
}
