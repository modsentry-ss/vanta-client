package vanta.mixin.extra;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({KeyMapping.class})
public interface IAccessorKeyMapping {
   @Accessor("key")
   InputConstants.Key getKey();

   @Accessor("clickCount")
   int getClickCount();

   @Accessor("clickCount")
   void setClickCount(int var1);
}
