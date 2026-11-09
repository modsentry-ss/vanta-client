package vanta;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VantaCore implements ModInitializer {
   public static final String MOD_ID = "vanta";
   public static final String NAME = "Vanta V1";
   public static final String VERSION = "1.0";
   public static final Logger LOGGER = LoggerFactory.getLogger("vanta");

   public void onInitialize() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("vanta", path);
   }
}
