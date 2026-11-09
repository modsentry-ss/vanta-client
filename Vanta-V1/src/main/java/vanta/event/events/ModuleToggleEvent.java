package vanta.event.events;

import vanta.event.Event;
import vanta.module.Module;

public record ModuleToggleEvent(Module module, boolean enabled) implements Event {
}
