package vanta.setting;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class ItemSetting extends IdSetSetting {
   public ItemSetting(String id, String... defaults) {
      super(id);

      for(String item : defaults) {
         ((Set)this.value).add(item);
      }

   }

   public boolean contains(ItemStack stack, boolean whenEmpty) {
      if (((Set)this.value).isEmpty()) {
         return whenEmpty;
      } else {
         return !stack.isEmpty() && ((Set)this.value).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
      }
   }
}
