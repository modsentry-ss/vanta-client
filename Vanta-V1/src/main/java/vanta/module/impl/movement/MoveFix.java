package vanta.module.impl.movement;

import vanta.module.Module;
import vanta.setting.EnumSetting;

public final class MoveFix extends Module {
   private static final String[] MODES = new String[]{"Silent", "Strict"};
   private static final int SILENT = 0;
   private final EnumSetting mode;

   public MoveFix() {
      super("moveFix");
      this.mode = (EnumSetting)this.add(new EnumSetting("mode", 0, MODES));
   }

   public String info() {
      return this.mode.selected();
   }

   public boolean corrects() {
      return this.isEnabled();
   }

   public boolean correctsInput() {
      return this.isEnabled() && (Integer)this.mode.get() == 0;
   }
}
