package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.Minecraft;

public record TickEvent(Minecraft client) implements Event {
}
