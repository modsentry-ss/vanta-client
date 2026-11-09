package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.Minecraft;

public record MouseMoveEvent(Minecraft client, double x, double y) implements Event {
}
