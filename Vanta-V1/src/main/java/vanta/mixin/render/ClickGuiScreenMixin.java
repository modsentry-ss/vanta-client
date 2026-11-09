package vanta.mixin.render;

import vanta.ui.click.ClickGui;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({Gui.class})
public abstract class ClickGuiScreenMixin {
   @ModifyVariable(
      method = {"setScreen"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Screen mace$keepClickGui(Screen screen) {
      return ClickGui.replacementFor(screen);
   }
}
