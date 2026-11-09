package vanta.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import vanta.VantaCore;

public final class EventBus {
   private static final Handler<?>[] NONE = new Handler[0];
   private final Map<Class<? extends Event>, List<Handler<?>>> subscribers = new HashMap();
   private final Map<Class<? extends Event>, Handler<?>[]> dispatch = new ConcurrentHashMap();

   public <E extends Event> Subscription subscribe(Class<E> type, Listener<E> listener) {
      return this.subscribe(type, 0, listener);
   }

   public <E extends Event> Subscription subscribe(Class<E> type, int priority, Listener<E> listener) {
      Handler<E> handler = new Handler<E>(type, priority, listener);
      synchronized(this) {
         ((List)this.subscribers.computeIfAbsent(type, (key) -> new ArrayList())).add(handler);
         this.dispatch.clear();
      }

      return () -> {
         handler.cancelled = true;
         synchronized(this) {
            List<Handler<?>> registered = (List)this.subscribers.get(type);
            if (registered != null && registered.remove(handler)) {
               this.dispatch.clear();
            }

         }
      };
   }

   public <E extends Event> E post(E event) {
      Handler<?>[] handlers = (Handler[])this.dispatch.get(event.getClass());
      if (handlers == null) {
         handlers = this.resolve(event.getClass());
      }

      for(Handler<?> handler : handlers) {
         if (!handler.cancelled) {
            if (event instanceof CancellableEvent) {
               CancellableEvent cancellable = (CancellableEvent)event;
               if (cancellable.isCancelled()) {
                  break;
               }
            }

            try {
               handler.accept(event);
            } catch (RuntimeException exception) {
               VantaCore.LOGGER.error("Listener for {} failed", handler.type.getSimpleName(), exception);
            }
         }
      }

      return event;
   }

   private synchronized Handler<?>[] resolve(Class<? extends Event> type) {
      List<Handler<?>> found = new ArrayList();
      this.subscribers.forEach((registered, handlers) -> {
         if (registered.isAssignableFrom(type)) {
            found.addAll(handlers);
         }

      });
      found.sort((a, b) -> Integer.compare(b.priority, a.priority));
      Handler<?>[] resolved = (Handler[])found.toArray(NONE);
      this.dispatch.put(type, resolved);
      return resolved;
   }

   private static final class Handler<E extends Event> {
      private final Class<E> type;
      private final int priority;
      private final Listener<E> listener;
      private volatile boolean cancelled;

      private Handler(Class<E> type, int priority, Listener<E> listener) {
         this.type = type;
         this.priority = priority;
         this.listener = listener;
      }

      void accept(Event event) {
         this.listener.on(event);
      }
   }
}
