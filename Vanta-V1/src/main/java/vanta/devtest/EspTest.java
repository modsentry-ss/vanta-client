package vanta.devtest;

import java.util.LinkedHashSet;
import java.util.Set;
import vanta.VantaClient;
import vanta.module.Module;
import vanta.setting.EntitySetting;
import vanta.setting.EnumSetting;
import vanta.setting.MultiSetting;
import vanta.setting.Setting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

final class EspTest {
   private static final String[] MODES = new String[]{"3d", "2d", "shader", "outline"};

   static void add() {
      UiTest.then(1, EspTest::setup);
      UiTest.then(40, (mc) -> {
         Module esp = VantaClient.MODULES.get("esp2d");
         EnumSetting mode = (EnumSetting)setting(esp, "mode");
         mode.set(1);
         MultiSetting targets = (MultiSetting)setting(esp, "targets");
         targets.set(new LinkedHashSet(Set.of(0, 1, 2)));
         EntitySetting entities = (EntitySetting)setting(esp, "entities");
         entities.set(new LinkedHashSet(Set.of("minecraft:husk", "minecraft:pig", "minecraft:creeper", "minecraft:cow", "minecraft:armor_stand")));
         esp.setEnabled(true);
         mc.gui.hud.getChat().clearMessages(false);
         String var10000 = mode.selected();
         UiTest.log("esp on mode=" + var10000 + " targets=" + String.valueOf(targets.get()) + " entities=" + String.valueOf(entities.get()) + " info=" + esp.info());
      });

      for(int i = 0; i < MODES.length; ++i) {
         UiTest.then(1, (mc) -> {
            EnumSetting mode = (EnumSetting)setting(VantaClient.MODULES.get("esp2d"), "mode");
            mode.set(i);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            UiTest.log("mode " + mode.selected() + " third person");
         });
         UiTest.then(20, (mc) -> {
            String var10000 = MODES[i];
            UiTest.log("rendered " + var10000 + " third");
         });
         UiTest.shot("esp_" + MODES[i] + "_third");
         UiTest.then(1, (mc) -> mc.options.setCameraType(CameraType.FIRST_PERSON));
         UiTest.then(20, (mc) -> {
            String var10000 = MODES[i];
            UiTest.log("rendered " + var10000 + " first");
         });
         UiTest.shot("esp_" + MODES[i] + "_first");
      }

      UiTest.then(1, (mc) -> {
         VantaClient.MODULES.get("esp2d").setEnabled(false);
         mc.options.setCameraType(CameraType.FIRST_PERSON);
      });
      UiTest.then(10, (mc) -> UiTest.log("esp off"));
      UiTest.shot("esp_off");
   }

   private static void setup(Minecraft mc) {
      ClientPacketListener c = mc.getConnection();
      c.sendCommand("gamemode creative");
      c.sendCommand("time set day");
      c.sendCommand("weather clear");
      c.sendCommand("kill @e[type=!player]");
      c.sendCommand("tp @s ~ ~ ~ 0 15");
      c.sendCommand("summon husk ~-2.5 ~ ~6 {NoAI:1b,PersistenceRequired:1b}");
      c.sendCommand("summon pig ~0 ~ ~6 {NoAI:1b,PersistenceRequired:1b}");
      c.sendCommand("summon creeper ~2.5 ~ ~6 {NoAI:1b,PersistenceRequired:1b}");
      c.sendCommand("summon armor_stand ~1 ~ ~3.5");
      c.sendCommand("fill ~3 ~ ~9 ~6 ~2 ~9 minecraft:stone");
      c.sendCommand("summon cow ~4.5 ~ ~11 {NoAI:1b,PersistenceRequired:1b}");
      UiTest.log("esp mobs summoned at " + String.valueOf(mc.player.blockPosition()));
   }

   private static <S extends Setting<?>> S setting(Module module, String id) {
      for(Setting<?> s : module.settings()) {
         if (s.id().equals(id)) {
            return (S)s;
         }
      }

      throw new IllegalStateException("no setting " + id);
   }

   private EspTest() {
   }
}
