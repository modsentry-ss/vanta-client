package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public record BlockUseEvent(LocalPlayer player, InteractionHand hand, BlockHitResult hit) implements Event {
}
