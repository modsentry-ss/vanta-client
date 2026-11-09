package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record HudRenderEvent(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) implements Event {
}
