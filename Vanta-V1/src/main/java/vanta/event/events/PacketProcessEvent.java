package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.Minecraft;

public record PacketProcessEvent(Minecraft client) implements Event {
}
