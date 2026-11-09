package vanta.event.events;

import vanta.event.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public record AttackEvent(Player player, Entity target) implements Event {
}
