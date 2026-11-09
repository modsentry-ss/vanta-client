package vanta.config;

import java.nio.file.Path;
import vanta.VantaClient;
import vanta.event.events.ModuleToggleEvent;
import vanta.event.events.TickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class StateStore {
   private static final long FLUSH_INTERVAL_NANOS = 10000000000L;
   private static boolean ready;
   private static boolean dirty;
   private static long savedAt;
   private static long changedAt;
   private static final long SETTLE_NANOS = 1000000000L;

   public static void init() {
      ClientLifecycleEvents.CLIENT_STARTED.register((ClientLifecycleEvents.ClientStarted)(client) -> load());
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientLifecycleEvents.ClientStopping)(client) -> save());
      VantaClient.EVENTS.subscribe(ModuleToggleEvent.class, (event) -> changed());
      VantaClient.EVENTS.subscribe(TickEvent.class, (event) -> flush());
   }

   private static Path path() {
      return Storage.file("config");
   }

   public static void load() {
      State state = (State)Storage.read(path(), State.class);
      if (state != null && state.modules != null) {
         VantaClient.MODULES.apply(state.modules);
      }

      ready = true;
      dirty = false;
      savedAt = System.nanoTime();
   }

   public static void save() {
      if (ready) {
         State state = new State();
         state.modules = VantaClient.MODULES.snapshot();
         Storage.write(path(), state);
         dirty = false;
         savedAt = System.nanoTime();
      }

   }

   public static void changed() {
      dirty = true;
      changedAt = System.nanoTime();
   }

   public static void settle() {
      if (ready && dirty) {
         save();
      }

   }

   private static void flush() {
      long now = System.nanoTime();
      if (dirty && (now - changedAt >= 1000000000L || now - savedAt >= 10000000000L)) {
         save();
      }

   }

   private StateStore() {
   }
}
