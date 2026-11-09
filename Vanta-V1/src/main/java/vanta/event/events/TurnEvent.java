package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.Minecraft;

public record TurnEvent(Minecraft client, float deltaTicks) implements Event {
}
