package vanta.devtest;

import com.google.gson.JsonParser;
import vanta.module.Module;
import vanta.module.impl.hud.Keystrokes;
import vanta.module.impl.hud.Notifications;
import vanta.setting.Setting;
import vanta.util.Clicks;
import net.minecraft.client.Minecraft;

final class HudKeysTest {
   private static final String[] ENTRANCES = new String[]{"Slide", "Drop", "Fade", "Pop"};

   static void add() {
      UiTest.then(1, (mc) -> {
         UiTest.enable(false, "arraylist", "watermark", "esp2d");
         UiTest.enable(true, "keystrokes", "notifications");
         Module keys = UiTest.module("keystrokes");
         set(keys, "style", "\"Classic\"");
         set(keys, "colorMode", "\"Custom\"");
         set(keys, "color", "\"#FFFFFF\"");
         set(keys, "scale", "146");
         set(keys, "keySize", "25");
         set(keys, "gap", "1");
         set(keys, "radius", "0");
         set(keys, "idleAlpha", "0");
         set(keys, "animationSpeed", "30");
         set(keys, "shape", "\"Cut\"");
         set(keys, "shadow", "\"None\"");
         Module notes = UiTest.module("notifications");
         set(notes, "maxCards", "4");
      });
      UiTest.then(10, (mc) -> UiTest.log("keys idle"));
      UiTest.shot("keys_classic_idle");
      press(true);
      UiTest.then(4, HudKeysTest::clicks);
      UiTest.then(4, HudKeysTest::clicks);
      UiTest.then(4, HudKeysTest::clicks);
      UiTest.then(1, (mc) -> UiTest.log("keys pressed up=" + mc.options.keyUp.isDown()));
      UiTest.shot("keys_classic_pressed");
      UiTest.then(1, (mc) -> set(UiTest.module("keystrokes"), "style", "\"Flat\""));
      UiTest.then(4, HudKeysTest::clicks);
      UiTest.then(4, HudKeysTest::clicks);
      UiTest.then(4, HudKeysTest::clicks);
      UiTest.shot("keys_flat_pressed");
      press(false);
      UiTest.then(20, (mc) -> UiTest.log("keys released"));
      UiTest.shot("keys_flat_idle");
      UiTest.then(1, (mc) -> set(UiTest.module("keystrokes"), "style", "\"Classic\""));

      for(String entrance : ENTRANCES) {
         UiTest.then(1, (mc) -> {
            UiTest.enable(false, "notifications");
            UiTest.enable(true, "notifications");
            set(UiTest.module("notifications"), "entrance", "\"" + entrance + "\"");
         });
         UiTest.then(2, (mc) -> {
            Notifications.post("Vanta V1", "Config saved: " + entrance, Notifications.Kind.INFO);
            Notifications.post("Sprint", "Enabled", Notifications.Kind.ON);
            Notifications.post("Vanta V1", "Could not load " + entrance, Notifications.Kind.ERROR);
         });
         UiTest.shot("toasts_" + entrance.toLowerCase() + "_in");
         UiTest.then(20, (mc) -> UiTest.log("notifications " + Notifications.debug()));
         UiTest.shot("toasts_" + entrance.toLowerCase());
         UiTest.then(60, (mc) -> UiTest.log("waited out " + entrance));
      }

      UiTest.then(1, (mc) -> UiTest.enable(true, "arraylist", "watermark"));
   }

   private static void press(boolean down) {
      UiTest.then(1, (mc) -> {
         Keystrokes.testHeld = null;
         mc.options.keyUp.setDown(down);
         mc.options.keyRight.setDown(down);
         mc.options.keyJump.setDown(down);
      });
   }

   private static void clicks(Minecraft mc) {
      for(int i = 0; i < 2; ++i) {
         Clicks.note(0);
      }

      Clicks.note(1);
   }

   private static void set(Module module, String id, String json) {
      for(Setting<?> setting : module.settings()) {
         if (setting.id().equals(id)) {
            setting.load(JsonParser.parseString(json));
            return;
         }
      }

      String var10000 = module.id();
      UiTest.log("no setting " + var10000 + "." + id);
   }

   private HudKeysTest() {
   }
}
