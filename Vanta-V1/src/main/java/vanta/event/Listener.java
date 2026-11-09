package vanta.event;

@FunctionalInterface
public interface Listener<E extends Event> {
   void on(E var1);
}
