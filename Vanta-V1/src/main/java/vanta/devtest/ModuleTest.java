package vanta.devtest;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.stream.Stream;
import vanta.VantaClient;
import vanta.mixin.accessor.MinecraftAccessor;
import vanta.mixin.extra.IAccessorLivingEntity;
import vanta.module.Module;
import vanta.module.impl.render.FreeLook;
import vanta.module.impl.render.Freecam;
import vanta.ui.click.ClickGui;
import net.minecraft.client.CameraType;
import net.minecraft.client.ClientBrandRetriever;

final class ModuleTest {
   private static void check(String name, boolean ok, String detail) {
      UiTest.log("module " + (ok ? "PASS " : "FAIL ") + name + " " + detail);
   }

   static void add() {
      UiTest.then(1, (mc) -> {
         Stream var10000 = VantaClient.MODULES.all().stream().filter((m) -> m.isEnabled());
         UiTest.log("modules enabled before: " + String.valueOf(var10000.map((m) -> m.id()).toList()));
         String var1 = String.valueOf(mc.gui.screen());
         UiTest.log("screen=" + var1 + " guiOpen=" + ClickGui.isOpen());
      });
      UiTest.then(5, (mc) -> {
         UiTest.enable(false, "sprint");
         mc.options.keyUp.setDown(true);
      });
      UiTest.then(15, (mc) -> UiTest.log("sprint off -> sprinting=" + mc.player.isSprinting()));
      UiTest.then(1, (mc) -> UiTest.enable(true, "sprint"));
      UiTest.then(20, (mc) -> {
         boolean var10001 = mc.player.isSprinting();
         boolean var10002 = mc.player.isSprinting();
         check("sprint", var10001, "sprinting=" + var10002 + " keySprint=" + mc.options.keySprint.isDown());
         mc.options.keyUp.setDown(false);
      });
      UiTest.then(5, (mc) -> UiTest.enable(true, "perspective"));
      UiTest.then(5, (mc) -> check("perspective", mc.options.getCameraType() != CameraType.FIRST_PERSON, "camera=" + String.valueOf(mc.options.getCameraType())));
      UiTest.then(1, (mc) -> UiTest.enable(false, "perspective"));
      UiTest.then(5, (mc) -> check("perspective.off", mc.options.getCameraType() == CameraType.FIRST_PERSON, "camera=" + String.valueOf(mc.options.getCameraType())));
      UiTest.then(5, (mc) -> UiTest.enable(true, "freeLook"));
      UiTest.then(5, (mc) -> {
         FreeLook fl = (FreeLook)VantaClient.MODULES.get(FreeLook.class);
         boolean var10001 = fl.isFreeLooking() && mc.options.getCameraType() != CameraType.FIRST_PERSON;
         boolean var10002 = fl.isFreeLooking();
         check("freeLook", var10001, "looking=" + var10002 + " camera=" + String.valueOf(mc.options.getCameraType()));
      });
      UiTest.then(1, (mc) -> UiTest.enable(false, "freeLook"));
      UiTest.then(5, (mc) -> {
         UiTest.enable(true, "noJumpDelay");
         ((IAccessorLivingEntity)mc.player).frost$setNoJumpDelay(10);
      });
      UiTest.then(2, (mc) -> {
         int d = ((IAccessorLivingEntity)mc.player).frost$getNoJumpDelay();
         check("noJumpDelay", d <= 4, "noJumpDelay=" + d);
      });
      UiTest.then(2, (mc) -> {
         UiTest.enable(true, "fastPlace");
         ((MinecraftAccessor)mc).client$setRightClickDelay(4);
      });
      UiTest.then(3, (mc) -> {
         boolean var10001 = ((MinecraftAccessor)mc).client$rightClickDelay() < 3;
         int var10002 = ((MinecraftAccessor)mc).client$rightClickDelay();
         check("fastPlace", var10001, "rightClickDelay=" + var10002 + " held=" + String.valueOf(mc.player.getMainHandItem()));
      });
      UiTest.then(2, (mc) -> UiTest.enable(true, "clientSpoofer"));
      UiTest.then(2, (mc) -> check("clientSpoofer", "vanilla".equals(ClientBrandRetriever.getClientModName()), "brand=" + ClientBrandRetriever.getClientModName()));
      UiTest.then(1, (mc) -> UiTest.enable(false, "clientSpoofer"));
      UiTest.then(2, (mc) -> UiTest.enable(true, "freecam"));
      UiTest.then(10, (mc) -> check("freecam", Freecam.controls() != null || VantaClient.MODULES.get("freecam").isEnabled(), "controls=" + String.valueOf(Freecam.controls())));
      UiTest.then(1, (mc) -> UiTest.enable(false, "freecam"));
      UiTest.then(2, (mc) -> {
         Module m = VantaClient.MODULES.get("perspective");
         m.setKey(InputConstants.getKey("key.keyboard.v"));
         m.setHold(false);
         VantaClient.MODULES.pressed(m.key());
      });
      UiTest.then(3, (mc) -> check("keybind", VantaClient.MODULES.get("perspective").isEnabled(), "perspective enabled after V"));
      UiTest.then(1, (mc) -> UiTest.enable(false, "perspective"));
      UiTest.then(1, (mc) -> UiTest.log("modules done"));
   }

   private ModuleTest() {
   }
}
