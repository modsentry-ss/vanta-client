package vanta.module.impl.hud;

import vanta.event.events.HudRenderEvent;
import vanta.module.Module;
import vanta.ui.GuiState;
import vanta.ui.click.ClickGui;
import net.minecraft.client.gui.screens.ChatScreen;

public final class NamePlate extends Module {
   public NamePlate() {
      super("namePlate");
      this.setEnabled(true);
   }

   protected void onEnable() {
      this.listen(HudRenderEvent.class, (event) -> {
         if (mc.player != null && (mc.gui.screen() == null || mc.gui.screen() instanceof ChatScreen) && !GuiState.hudHidden()) {
            ClickGui.drawPlateOnHud(event.graphics());
         }

      });
   }
}
