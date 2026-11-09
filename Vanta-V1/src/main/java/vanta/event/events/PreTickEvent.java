package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.Minecraft;

public record PreTickEvent(Minecraft client) implements Event {
}
