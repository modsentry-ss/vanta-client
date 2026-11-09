package vanta.hud;

import vanta.VantaClient;
import vanta.VantaCore;
import vanta.event.events.HudRenderEvent;
import vanta.render.Gfx;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;

public final class Hud {
   public static void init() {
      HudElementRegistry.addLast(VantaCore.id("hud"), (graphics, delta) -> {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            Gfx.begin(graphics);

            try {
               VantaClient.EVENTS.post(new HudRenderEvent(graphics, delta));
            } finally {
               Gfx.end();
            }

         }
      });
   }

   private Hud() {
   }
}
