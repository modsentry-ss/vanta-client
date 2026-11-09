package vanta;

import vanta.config.StateStore;
import vanta.devtest.UiTest;
import vanta.event.EventBus;
import vanta.event.events.PreTickEvent;
import vanta.event.events.TickEvent;
import vanta.hud.Hud;
import vanta.module.ModuleManager;
import vanta.render.world.WorldRenderer;
import vanta.rotation.RotationManager;
import vanta.ui.click.ClickGui;
import vanta.util.Clicks;
import vanta.util.Time;
import vanta.visual.Visuals;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class VantaClient implements ClientModInitializer {
   public static final EventBus EVENTS = new EventBus();
   public static final ModuleManager MODULES = new ModuleManager();
   public static final RotationManager ROTATIONS = new RotationManager();

   public void onInitializeClient() {
      StateStore.init();
      ROTATIONS.init();
      ClickGui.init();
      Hud.init();
      WorldRenderer.init();
      Visuals.init();
      EVENTS.subscribe(PreTickEvent.class, 200, (event) -> Time.tick());
      EVENTS.subscribe(PreTickEvent.class, (event) -> MODULES.tickKeybinds(event.client()));
      ClientTickEvents.START_CLIENT_TICK.register((ClientTickEvents.StartTick)(client) -> EVENTS.post(new PreTickEvent(client)));
      ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> EVENTS.post(new TickEvent(client)));
      Clicks.init();
      UiTest.init();
   }
}
