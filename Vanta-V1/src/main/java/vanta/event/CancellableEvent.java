package vanta.event;

public abstract class CancellableEvent implements Event {
   private boolean cancelled;

   public void cancel() {
      this.cancelled = true;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }
}
