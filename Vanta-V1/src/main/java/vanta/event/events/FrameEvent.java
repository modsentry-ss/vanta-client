package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface FrameEvent extends Event {
   ClientLevel level();

   Camera camera();

   DeltaTracker deltaTracker();

   default float partialTicks(Entity entity) {
      return this.deltaTracker().getGameTimeDeltaPartialTick(!this.level().tickRateManager().isEntityFrozen(entity));
   }

   default Vec3 renderPosition(Entity entity) {
      float partial = this.partialTicks(entity);
      return new Vec3(Mth.lerp((double)partial, entity.xOld, entity.getX()), Mth.lerp((double)partial, entity.yOld, entity.getY()), Mth.lerp((double)partial, entity.zOld, entity.getZ()));
   }

   default AABB interpolatedBounds(Entity entity) {
      return entity.getBoundingBox().move(this.renderPosition(entity).subtract(entity.position()));
   }
}
