package vanta.mixin.accessor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({KeyMapping.class})
public interface KeyMappingAccessor {
   @Accessor("key")
   InputConstants.Key client$getKey();

   @Accessor("clickCount")
   int client$clickCount();

   @Accessor("clickCount")
   void client$setClickCount(int var1);

   @Accessor("isDown")
   void client$setDownRaw(boolean var1);
}
