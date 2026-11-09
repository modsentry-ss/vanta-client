package vanta.module.impl.misc;

import vanta.VantaClient;
import vanta.module.Module;
import vanta.setting.EnumSetting;
import vanta.setting.TextSetting;

public final class ClientSpoofer extends Module {
   private static final String[] MODES = new String[]{"Vanilla", "OptiFine", "Fabric", "Feather", "LunarClient", "LabyMod", "CheatBreaker", "PvPLounge", "Minebuilders", "FML", "Geyser", "Custom"};
   private static final String[] BRANDS = new String[]{"vanilla", "optifine", "fabric", "Feather Forge", "lunarclient", "LMC", "CB", "PLC18", "minebuilders", "fml,forge", "Geyser", ""};
   private final EnumSetting mode;
   private final TextSetting custom;

   public ClientSpoofer() {
      super("clientSpoofer");
      this.mode = (EnumSetting)this.add(new EnumSetting("brand", 0, MODES));
      this.custom = (TextSetting)this.add(new TextSetting("customBrand", "vanilla", 32));
      this.custom.visibleWhen(() -> (Integer)this.mode.get() == MODES.length - 1);
      this.mode.describe();
   }

   public String info() {
      return this.mode.selected();
   }

   public static String spoofedBrand() {
      if (VantaClient.MODULES == null) {
         return null;
      } else {
         ClientSpoofer spoofer = (ClientSpoofer)VantaClient.MODULES.get(ClientSpoofer.class);
         if (spoofer != null && spoofer.isEnabled()) {
            int index = (Integer)spoofer.mode.get();
            return index == MODES.length - 1 ? (String)spoofer.custom.get() : BRANDS[Math.max(0, Math.min(index, BRANDS.length - 1))];
         } else {
            return null;
         }
      }
   }
}
