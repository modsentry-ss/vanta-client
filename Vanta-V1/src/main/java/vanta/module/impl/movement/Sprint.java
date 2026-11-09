package vanta.module.impl.movement;

import vanta.event.events.PreTickEvent;
import vanta.event.events.TickEvent;
import vanta.module.Module;
import vanta.setting.BoolSetting;
import vanta.util.KeyBindUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public final class Sprint extends Module {
   private static final Identifier SPRINTING_MODIFIER_ID = Identifier.withDefaultNamespace("sprinting");
   private final BoolSetting fovFix = (BoolSetting)this.add(new BoolSetting("fovFix", true));
   private boolean wasSprinting;

   public Sprint() {
      super("sprint");
      this.setEnabled(true);
   }

   public boolean shouldApplyFovFix(AttributeInstance attribute) {
      return (Boolean)this.fovFix.get() && attribute.getModifier(SPRINTING_MODIFIER_ID) == null && this.wasSprinting;
   }

   protected void onEnable() {
      this.listen(PreTickEvent.class, (event) -> {
         if (event.client().player != null) {
            KeyBindUtil.setKeyBindState(KeyBindUtil.getKeyCode(mc.options.keySprint), true);
         }

      });
      this.listen(TickEvent.class, (event) -> {
         if (event.client().player != null) {
            this.wasSprinting = event.client().player.isSprinting();
         }

      });
   }

   protected void onDisable() {
      this.wasSprinting = false;
      KeyBindUtil.updateKeyState(KeyBindUtil.getKeyCode(mc.options.keySprint));
   }
}
