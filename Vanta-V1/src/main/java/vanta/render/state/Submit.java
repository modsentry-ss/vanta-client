package vanta.render.state;

import vanta.mixin.accessor.GuiGraphicsExtractorAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

public final class Submit {
   public static void submit(GuiGraphicsExtractor graphics, GuiElementRenderState state) {
      ((GuiGraphicsExtractorAccessor)graphics).client$getGuiRenderState().addGuiElement(state);
   }

   private Submit() {
   }
}
