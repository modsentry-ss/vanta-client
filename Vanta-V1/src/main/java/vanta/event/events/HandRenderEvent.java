package vanta.event.events;

import vanta.event.Event;
import vanta.render.entity.EntityFill;

public final class HandRenderEvent implements Event {
   private EntityFill fill;

   public void setFill(EntityFill fill) {
      this.fill = fill;
   }

   public EntityFill fill() {
      return this.fill;
   }
}
