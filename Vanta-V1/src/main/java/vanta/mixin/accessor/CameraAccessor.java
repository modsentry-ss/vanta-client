package vanta.mixin.accessor;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Camera.class})
public interface CameraAccessor {
   @Accessor("eyeHeight")
   float client$eyeHeight();
}
