package vanta.event.events;

import vanta.event.Event;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record EntityRenderStateEvent(Entity entity, EntityRenderState state, float partialTicks) implements Event {
   public Vec3 renderPosition() {
      return new Vec3(Mth.lerp((double)this.partialTicks, this.entity.xOld, this.entity.getX()), Mth.lerp((double)this.partialTicks, this.entity.yOld, this.entity.getY()), Mth.lerp((double)this.partialTicks, this.entity.zOld, this.entity.getZ()));
   }
}
