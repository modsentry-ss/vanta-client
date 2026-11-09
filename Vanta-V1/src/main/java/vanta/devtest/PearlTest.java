package vanta.devtest;

import java.lang.reflect.Field;
import java.util.Locale;
import vanta.VantaClient;
import vanta.mixin.accessor.MouseHandlerAccessor;
import vanta.module.Module;
import vanta.rotation.Rotation;
import vanta.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;

final class PearlTest {
   private static double startX;
   private static double startZ;
   private static int caughtTicks;

   private static float mouseTurn(Minecraft mc) {
      float before = mc.player.getYRot();
      MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
      mouse.client$setAccumulatedDX((double)40.0F);
      mouse.client$turnPlayer(0.05);
      float after = mc.player.getYRot();
      mc.player.setYRot(before);
      mc.player.yRotO = before;
      return after - before;
   }

   private static String phase(Module m) {
      try {
         Field f = m.getClass().getDeclaredField("phase");
         f.setAccessible(true);
         return String.valueOf(f.get(m));
      } catch (Exception var2) {
         return "?";
      }
   }

   static void add() {
      UiTest.then(5, (mc) -> {
         ClientPacketListener c = mc.getConnection();
         c.sendCommand("gamemode survival");
         c.sendCommand("clear");
         c.sendCommand("effect give @s resistance 600 4 true");
         c.sendCommand("item replace entity @s hotbar.0 with dirt");
         c.sendCommand("item replace entity @s hotbar.3 with ender_pearl 16");
         c.sendCommand("item replace entity @s hotbar.4 with wind_charge 16");
      });

      for(int round = 1; round <= 3; ++round) {
         UiTest.then(40, (mc) -> {
            Module m = VantaClient.MODULES.get("pearlCatch");
            String smooth = System.getenv("MACE_PEARL_SMOOTH");

            for(Setting<?> st : m.settings()) {
               if (st.id().equals("smoothness") && smooth != null) {
                  st.set(Integer.parseInt(smooth));
               }
            }

            m.setEnabled(true);
            mc.player.getInventory().setSelectedSlot(0);
            mc.player.setYRot(0.0F);
            mc.player.setXRot(-25.0F);
            startX = mc.player.getX();
            startZ = mc.player.getZ();
            UiTest.log(String.format(Locale.ROOT, "round %d settings: %s", round, m.settings().stream().map((s) -> {
               String var10000 = s.id();
               return var10000 + "=" + String.valueOf(s.save());
            }).toList()));
            UiTest.log(String.format(Locale.ROOT, "round %d mouse yaw before %.2f", round, mouseTurn(mc)));
         });
         UiTest.then(2, (mc) -> {
            Module m = VantaClient.MODULES.get("pearlCatch");
            VantaClient.MODULES.pressed(m.key());
            UiTest.log("round " + round + " pressed " + m.key().getName());
         });

         for(int t = 0; t < 70; ++t) {
            UiTest.then(1, (mc) -> {
               Module m = VantaClient.MODULES.get("pearlCatch");
               ThrownEnderpearl pearl = null;

               for(Entity e : mc.level.entitiesForRendering()) {
                  if (e instanceof ThrownEnderpearl p) {
                     if (p.getOwner() == mc.player) {
                        pearl = p;
                     }
                  }
               }

               Rotation rot = VantaClient.ROTATIONS.rotation(mc.player);
               UiTest.log(String.format(Locale.ROOT, "  t%02d phase %s slot %d silent %s (%.1f %.1f) view (%.1f %.1f) pos (%.1f %.1f %.1f) pearl %s", t, phase(m), mc.player.getInventory().getSelectedSlot(), VantaClient.ROTATIONS.active(), rot.yaw(), rot.pitch(), mc.player.getYRot(), mc.player.getXRot(), mc.player.getX(), mc.player.getY(), mc.player.getZ(), pearl == null ? "-" : String.format(Locale.ROOT, "(%.1f %.1f %.1f)", pearl.getX(), pearl.getY(), pearl.getZ())));
            });
         }

         UiTest.then(1, (mc) -> {
            double moved = Math.hypot(mc.player.getX() - startX, mc.player.getZ() - startZ);
            UiTest.log(String.format(Locale.ROOT, "round %d result: moved %.1f blocks, mouse yaw after %.2f, silent still %s", round, moved, mouseTurn(mc), VantaClient.ROTATIONS.active()));
         });
         UiTest.then(5, (mc) -> {
            ClientPacketListener var10000 = mc.getConnection();
            int var10001 = (int)startX;
            var10000.sendCommand("tp @s " + var10001 + " -60 " + (int)startZ);
         });
      }

   }

   private PearlTest() {
   }
}
