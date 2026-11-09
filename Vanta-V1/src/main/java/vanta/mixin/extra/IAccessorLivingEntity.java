package vanta.mixin.extra;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface IAccessorLivingEntity {
   @Accessor("noJumpDelay")
   int frost$getNoJumpDelay();

   @Accessor("noJumpDelay")
   void frost$setNoJumpDelay(int var1);

   @Accessor("activeEffects")
   Map<Holder<MobEffect>, MobEffectInstance> frost$getActiveEffects();

   @Accessor("DATA_HEALTH_ID")
   static EntityDataAccessor<Float> getHealthDataId() {
      throw new AssertionError();
   }

   @Accessor("jumping")
   boolean frost$isJumping();

   @Accessor("jumping")
   void frost$setJumping(boolean var1);
}
