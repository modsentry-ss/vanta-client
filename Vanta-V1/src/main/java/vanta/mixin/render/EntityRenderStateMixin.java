package vanta.mixin.render;

import vanta.render.entity.EntityFill;
import vanta.render.entity.EntityOutline;
import vanta.render.entity.Filled;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({EntityRenderState.class})
public abstract class EntityRenderStateMixin implements Filled {
   @Unique
   private EntityFill client$fill;
   @Unique
   private EntityOutline client$outline;

   public EntityFill client$fill() {
      return this.client$fill;
   }

   public void client$setFill(EntityFill fill) {
      this.client$fill = fill;
   }

   public EntityOutline client$outline() {
      return this.client$outline;
   }

   public void client$setOutline(EntityOutline outline) {
      this.client$outline = outline;
   }
}
