package vanta.event.events;

import vanta.render.world.WorldBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;

public record WorldRenderEvent(ClientLevel level, Camera camera, DeltaTracker deltaTracker, WorldBuffer buffer) implements FrameEvent {
}
