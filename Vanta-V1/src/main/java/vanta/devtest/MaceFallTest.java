package vanta.devtest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import vanta.VantaClient;
import vanta.event.events.AttackEvent;
import vanta.event.events.TickEvent;
import vanta.mixin.accessor.KeyMappingAccessor;
import vanta.module.Module;
import vanta.setting.RangeSetting;
import vanta.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class MaceFallTest {
   private static int attacks;
   private static final String VICTIM = System.getenv("MACE_VICTIM");
   private static final String STUN = System.getenv("MACE_STUN");

   private static LivingEntity zombie(Minecraft mc) {
      for(Entity entity : mc.level.entitiesForRendering()) {
         if (VICTIM != null) {
            if (entity instanceof Player) {
               Player p = (Player)entity;
               if (p != mc.player && p.getGameProfile().name().equals(VICTIM)) {
                  return p;
               }
            }
         } else if (entity.getType().toShortString().equals("zombie") && entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            return living;
         }
      }

      return null;
   }

   private static void look(Minecraft mc, LivingEntity target) {
      Vec3 eye = mc.player.getEyePosition();
      Vec3 at = target.position().add((double)0.0F, (double)target.getBbHeight() * 0.6, (double)0.0F);
      double dx = at.x - eye.x;
      double dy = at.y - eye.y;
      double dz = at.z - eye.z;
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
      mc.player.setXRot((float)Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
   }

   static void add() {
      UiTest.then(5, (mc) -> {
         ClientPacketListener c = mc.getConnection();
         c.sendCommand("gamemode survival");
         c.sendCommand("clear");
         c.sendCommand("kill @e[type=!player]");
         c.sendCommand("time set night");
         c.sendCommand("effect give @s resistance 600 4 true");
         c.sendCommand("item replace entity @s hotbar.0 with dirt");
         c.sendCommand("item replace entity @s hotbar.1 with netherite_axe");
         c.sendCommand("item replace entity @s hotbar.2 with mace");
      });
      UiTest.then(10, (mc) -> {
         int x = mc.player.getBlockX();
         int y = mc.player.getBlockY();
         int z = mc.player.getBlockZ();
         if (VICTIM != null) {
            mc.getConnection().sendCommand("gamemode survival " + VICTIM);
            mc.getConnection().sendCommand("effect give " + VICTIM + " resistance 600 4 true");
            mc.getConnection().sendCommand("effect give " + VICTIM + " regeneration 600 4 true");
            mc.getConnection().sendCommand("tp " + VICTIM + " " + x + ".5 " + y + " " + (z + 2) + ".5");
            if (STUN != null) {
               mc.getConnection().sendCommand("item replace entity " + VICTIM + " weapon.offhand with shield");
            }
         } else {
            mc.getConnection().sendCommand("summon zombie " + x + ".5 " + y + " " + (z + 2) + ".5 {NoAI:1b,PersistenceRequired:1b}");
         }

         mc.player.getInventory().setSelectedSlot(0);
         int[] lastSlot = new int[]{mc.player.getInventory().getSelectedSlot()};
         VantaClient.EVENTS.subscribe(TickEvent.class, -1000, (e) -> {
            if (mc.player != null && mc.player.getInventory().getSelectedSlot() != lastSlot[0]) {
               lastSlot[0] = mc.player.getInventory().getSelectedSlot();
               int var10000 = mc.player.tickCount;
               UiTest.log("slot t" + var10000 + " -> " + lastSlot[0] + " " + String.valueOf(mc.player.getMainHandItem().getItem()));
            }

         });
         VantaClient.EVENTS.subscribe(AttackEvent.class, 100, (e) -> {
            ++attacks;
            UiTest.log(String.format(Locale.ROOT, "attack t%d -> %s slot %d %s fall %.2f", mc.player.tickCount, e.target().getType().toShortString(), mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandItem().getItem(), mc.player.fallDistance));
         });
         Module mace = VantaClient.MODULES.get("autoMace");

         for(Setting<?> st : VICTIM != null ? List.of() : vanta.settings()) {
            if (st.id().equals("targets")) {
               st.set(new LinkedHashSet(List.of(0, 2)));
            } else if (st.id().equals("entities")) {
               st.set(new LinkedHashSet(List.of("minecraft:zombie")));
            }
         }

         if (STUN != null) {
            String[] d = STUN.split(",");

            for(Setting<?> st : vanta.settings()) {
               switch (st.id()) {
                  case "stunSlam":
                     st.set(1);
                     break;
                  case "axeDelay":
                     ((RangeSetting)st).setHigh(10);
                     ((RangeSetting)st).setLow(Integer.parseInt(d[0]));
                     ((RangeSetting)st).setHigh(Integer.parseInt(d[0]));
                     break;
                  case "stunDelay":
                     ((RangeSetting)st).setHigh(10);
                     ((RangeSetting)st).setLow(Integer.parseInt(d[1]));
                     ((RangeSetting)st).setHigh(Integer.parseInt(d[1]));
                     break;
                  case "maceDelay":
                     ((RangeSetting)st).setHigh(10);
                     ((RangeSetting)st).setLow(Integer.parseInt(d[2]));
                     ((RangeSetting)st).setHigh(Integer.parseInt(d[2]));
               }
            }

            Stream var10000 = vanta.settings().stream().filter((stxx) -> stxx.id().matches("stunSlam|axeDelay|stunDelay|maceDelay|swapBack"));
            UiTest.log("stun settings " + String.valueOf(var10000.map((stxx) -> {
               String var10000 = stxx.id();
               return var10000 + "=" + String.valueOf(stxx.save());
            }).toList()));
         }

         vanta.setEnabled(true);
         UiTest.log("autoMace enabled=" + vanta.isEnabled());
      });

      for(int round = 1; round <= (VICTIM != null ? 3 : 2); ++round) {
         UiTest.then(20, (mc) -> {
            LivingEntity z = zombie(mc);
            if (z == null) {
               UiTest.log("no zombie");
            } else {
               UiTest.log("round " + round + " zombie hp " + z.getHealth() + " at " + String.valueOf(z.position()) + " me " + String.valueOf(mc.player.position()) + " screen " + String.valueOf(mc.gui.screen()) + " using " + mc.player.isUsingItem());
               ((KeyMappingAccessor)mc.options.keyHotbarSlots[0]).client$setClickCount(1);
               mc.getConnection().sendCommand(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f", z.getX(), z.getY() + (double)7.0F, z.getZ() - 0.9));
            }
         });

         for(int t = 0; t < 30; ++t) {
            UiTest.then(1, (mc) -> {
               LivingEntity z = zombie(mc);
               if (z != null && !mc.player.onGround()) {
                  look(mc, z);
                  UiTest.log(String.format(Locale.ROOT, "  t%d slot %d %s blocking %b | y %.2f pitch %.1f yaw %.1f hit %s dist %.2f", mc.player.tickCount, mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandItem().getItem(), z.isBlocking(), mc.player.getY(), mc.player.getXRot(), mc.player.getYRot(), mc.hitResult == null ? null : mc.hitResult.getType(), mc.player.distanceTo(z)));
               }

            });
         }

         UiTest.then(5, (mc) -> {
            LivingEntity z = zombie(mc);
            UiTest.log("round " + round + " after: attacks=" + attacks + " zombie hp " + String.valueOf(z == null ? "dead" : z.getHealth()) + " slot " + mc.player.getInventory().getSelectedSlot());
         });
      }

      UiTest.then(5, (mc) -> {
         String var10000 = attacks > 0 ? "PASS" : "FAIL";
         UiTest.log("macefall " + var10000 + " attacks=" + attacks);
      });
   }

   private MaceFallTest() {
   }
}
