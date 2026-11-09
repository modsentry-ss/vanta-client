package vanta.event.events;

import com.mojang.blaze3d.platform.InputConstants;
import vanta.event.Event;

public record ButtonEvent(InputConstants.Key key, int action, boolean simulated, long nanos) implements Event {
}
