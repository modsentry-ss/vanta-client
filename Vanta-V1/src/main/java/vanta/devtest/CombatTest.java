package vanta.devtest;

import java.util.Locale;
import java.util.stream.Stream;
import vanta.VantaClient;
import vanta.event.events.AttackEvent;
import vanta.event.events.TickEvent;
import vanta.mixin.accessor.KeyMappingAccessor;
import vanta.module.Module;
import vanta.util.Clicks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

final class CombatTest {
   private static final String VICTIM = System.getenv("MACE_VICTIM");
   private static String lastAttack = "none";
   private static boolean listening;
   private static int spearTicks;

   private static void check(String name, boolean ok, String detail) {
      UiTest.log("combat " + (ok ? "PASS " : "FAIL ") + name + " " + detail);
   }

   private static Player victim(Minecraft mc) {
      for(Player p : mc.level.players()) {
         if (p != mc.player && p.getGameProfile().name().equals(VICTIM)) {
            return p;
         }
      }

      return null;
   }

   private static void look(Minecraft mc, Vec3 at) {
      Vec3 eye = mc.player.getEyePosition();
      double dx = at.x - eye.x;
      double dy = at.y - eye.y;
      double dz = at.z - eye.z;
      mc.player.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
      mc.player.setXRot((float)Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
   }

   private static void pickSlot(Minecraft mc, int slot) {
      ((KeyMappingAccessor)mc.options.keyHotbarSlots[slot]).client$setClickCount(1);
   }

   private static void press(String module) {
      Module m = VantaClient.MODULES.get(module);
      m.setEnabled(true);
      VantaClient.MODULES.pressed(m.key());
      UiTest.log(module + " pressed " + m.key().getName() + " (hold " + m.hold() + ")");
   }

   private static void halt(Minecraft mc) {
      mc.options.keyUp.setDown(false);
      mc.player.setSprinting(false);
      mc.player.setDeltaMovement((double)0.0F, mc.player.getDeltaMovement().y, (double)0.0F);
   }

   private static void setup(Minecraft mc, String... items) {
      ClientPacketListener c = mc.getConnection();
      c.sendCommand("gamemode survival");
      c.sendCommand("clear");
      c.sendCommand("effect clear @s");
      c.sendCommand("kill @e[type=ender_pearl]");

      for(int i = 0; i < items.length; ++i) {
         if (items[i] != null) {
            c.sendCommand("item replace entity @s " + items[i]);
         }
      }

      if (!listening) {
         listening = true;
         VantaClient.EVENTS.subscribe(TickEvent.class, -1000, (e) -> {
            if (mc.player != null && mc.player.getMainHandItem().is(Items.IRON_SPEAR)) {
               ++spearTicks;
            }

         });
         VantaClient.EVENTS.subscribe(AttackEvent.class, -1000, (e) -> lastAttack = String.format(Locale.ROOT, "%s with slot %d %s", e.target().getType().toShortString(), mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandItem().getItem()));
      }

   }

   static void add(String phase) {
      switch (phase) {
         case "breach" -> breach();
         case "lunge" -> lunge();
         case "grapple" -> grapple();
         case "totem" -> totem();
         default -> UiTest.log("unknown combat phase " + phase);
      }

   }

   private static void breach() {
      UiTest.then(5, CombatTest::halt);
      UiTest.then(10, (mc) -> {
         setup(mc, "hotbar.0 with netherite_sword", "hotbar.2 with mace[enchantments={\"minecraft:breach\":4}]");
         ClientPacketListener c = mc.getConnection();
         c.sendCommand("gamemode survival " + VICTIM);
         c.sendCommand("effect give " + VICTIM + " resistance 600 4 true");
         c.sendCommand("item replace entity " + VICTIM + " armor.chest with diamond_chestplate");
         c.sendCommand("item replace entity " + VICTIM + " armor.legs with diamond_leggings");
         c.sendCommand(String.format(Locale.ROOT, "tp %s %.2f %.2f %.2f", VICTIM, mc.player.getX(), mc.player.getY(), mc.player.getZ() + (double)2.5F));
         VantaClient.MODULES.get("breachSwap").setEnabled(true);
         Stream var10000 = VantaClient.MODULES.get("breachSwap").settings().stream();
         UiTest.log("breachSwap settings " + String.valueOf(var10000.map((s) -> {
            String var10000 = s.id();
            return var10000 + "=" + String.valueOf(s.save());
         }).toList()));
      });
      UiTest.then(5, (mc) -> pickSlot(mc, 0));

      for(int round = 1; round <= 3; ++round) {
         UiTest.then(5, CombatTest::halt);
         UiTest.then(10, (mc) -> mc.getConnection().sendCommand(String.format(Locale.ROOT, "tp %s %.2f %.2f %.2f", VICTIM, mc.player.getX(), mc.player.getY(), mc.player.getZ() + (double)2.5F)));
         UiTest.then(25, (mc) -> {
            Player v = victim(mc);
            if (v == null) {
               check("breach", false, "no victim " + VICTIM);
            } else {
               look(mc, v.position().add((double)0.0F, 1.2, (double)0.0F));
               lastAttack = "none";
            }
         });
         UiTest.then(1, (mc) -> Clicks.click(0));
         UiTest.then(1, (mc) -> UiTest.log("breach round " + round + " attack: " + lastAttack + " armor " + (victim(mc) == null ? -1 : victim(mc).getArmorValue())));
         UiTest.then(8, (mc) -> {
            String var10000 = "breach." + round;
            boolean var10001 = lastAttack.contains("vanta") && mc.player.getInventory().getSelectedSlot() == 0;
            String var10002 = lastAttack;
            check(var10000, var10001, "attack " + var10002 + ", slot after " + mc.player.getInventory().getSelectedSlot());
         });
      }

   }

   private static void lunge() {
      double[] start = new double[3];
      int[] best = new int[1];
      UiTest.then(5, CombatTest::halt);
      UiTest.then(10, (mc) -> setup(mc, "hotbar.0 with dirt", "hotbar.5 with iron_spear[enchantments={\"minecraft:lunge\":3}]"));
      UiTest.then(5, (mc) -> pickSlot(mc, 0));

      for(int round = 1; round <= 3; ++round) {
         UiTest.then(30, (mc) -> {
            mc.player.setYRot(0.0F);
            mc.player.setXRot(0.0F);
            start[0] = mc.player.getX();
            start[2] = mc.player.getZ();
            lastAttack = "none";
            best[0] = -1;
            spearTicks = 0;
            Stream var10000 = VantaClient.MODULES.get("spearLunge").settings().stream();
            UiTest.log("lunge settings " + String.valueOf(var10000.map((s) -> {
               String var10000 = s.id();
               return var10000 + "=" + String.valueOf(s.save());
            }).toList()));
            press("spearLunge");
         });

         for(int t = 0; t < 12; ++t) {
            UiTest.then(1, (mc) -> {
               int slot = mc.player.getInventory().getSelectedSlot();
               if (slot == 5 && best[0] < 0) {
                  best[0] = t;
               }

               UiTest.log(String.format(Locale.ROOT, "  lunge t%d slot %d vel %.2f swing %b", t, slot, mc.player.getDeltaMovement().horizontalDistance(), mc.player.swinging));
            });
         }

         UiTest.then(10, (mc) -> {
            double moved = Math.hypot(mc.player.getX() - start[0], mc.player.getZ() - start[2]);
            check("lunge." + round, spearTicks > 0 && moved > (double)1.5F && mc.player.getInventory().getSelectedSlot() == 0, String.format(Locale.ROOT, "spear held at %d tick ends, moved %.1f blocks, slot after %d", spearTicks, moved, mc.player.getInventory().getSelectedSlot()));
         });
         UiTest.then(5, CombatTest::halt);
         UiTest.then(10, (mc) -> mc.getConnection().sendCommand(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f", start[0], mc.player.getY(), start[2])));
      }

   }

   private static void grapple() {
      double[] start = new double[3];
      UiTest.then(5, CombatTest::halt);
      UiTest.then(10, (mc) -> {
         setup(mc, "hotbar.0 with dirt", "hotbar.3 with ender_pearl 16");
         mc.getConnection().sendCommand("gamemode survival " + VICTIM);
         mc.getConnection().sendCommand("effect give @s resistance 600 4 true");
         Stream var10000 = VantaClient.MODULES.get("pearlGrapple").settings().stream();
         UiTest.log("grapple settings " + String.valueOf(var10000.map((s) -> {
            String var10000 = s.id();
            return var10000 + "=" + String.valueOf(s.save());
         }).toList()));
      });
      UiTest.then(5, (mc) -> pickSlot(mc, 0));

      for(int round = 1; round <= 3; ++round) {
         UiTest.then(30, (mc) -> {
            start[0] = mc.player.getX();
            start[1] = mc.player.getY();
            start[2] = mc.player.getZ();
            mc.getConnection().sendCommand(String.format(Locale.ROOT, "tp %s %.2f %.2f %.2f", VICTIM, start[0] + (double)3.0F, start[1], start[2] + (double)12.0F));
         });
         UiTest.then(10, (mc) -> {
            Player v = victim(mc);
            mc.player.setYRot(0.0F);
            mc.player.setXRot(0.0F);
            UiTest.log("grapple round " + round + " victim " + (v == null ? "none" : String.format(Locale.ROOT, "%.1f away", mc.player.distanceTo(v))));
            press("pearlGrapple");
         });
         boolean[] thrown = new boolean[1];

         for(int t = 0; t < 40; ++t) {
            UiTest.then(1, (mc) -> {
               for(Entity e : mc.level.entitiesForRendering()) {
                  if (e instanceof ThrownEnderpearl p) {
                     if (p.getOwner() == mc.player) {
                        thrown[0] = true;
                     }
                  }
               }

            });
         }

         UiTest.then(1, (mc) -> {
            Player v = victim(mc);
            double moved = Math.hypot(mc.player.getX() - start[0], mc.player.getZ() - start[2]);
            check("grapple." + round, thrown[0] && moved > (double)5.0F, String.format(Locale.ROOT, "pearl thrown %b, moved %.1f blocks, now %.1f from victim, slot %d, pearls left %d", thrown[0], moved, v == null ? -1.0F : mc.player.distanceTo(v), mc.player.getInventory().getSelectedSlot(), mc.player.getInventory().countItem(Items.ENDER_PEARL)));
            thrown[0] = false;
         });
         UiTest.then(2, (mc) -> mc.getConnection().sendCommand(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f", start[0], start[1], start[2])));
      }

   }

   private static void totem() {
      for(int round = 1; round <= 2; ++round) {
         UiTest.then(5, CombatTest::halt);
         UiTest.then(10, (mc) -> {
            setup(mc, "hotbar.0 with dirt", "weapon.offhand with totem_of_undying", "inventory.10 with totem_of_undying", "inventory.20 with totem_of_undying");
            Stream var10000 = VantaClient.MODULES.get("autoTotem").settings().stream();
            UiTest.log("totem settings " + String.valueOf(var10000.map((s) -> {
               String var10000 = s.id();
               return var10000 + "=" + String.valueOf(s.save());
            }).toList()));
            VantaClient.MODULES.get("autoTotem").setEnabled(true);
         });
         UiTest.then(10, (mc) -> pickSlot(mc, 0));
         UiTest.then(10, (mc) -> {
            UiTest.log("totem round " + round + " before: offhand " + String.valueOf(mc.player.getOffhandItem()) + " health " + mc.player.getHealth());
            mc.getConnection().sendCommand("damage @s 100 minecraft:generic");
         });

         for(int t = 0; t < 8; ++t) {
            UiTest.then(5, (mc) -> UiTest.log("  totem +" + t * 5 + " offhand " + String.valueOf(mc.player.getOffhandItem()) + " screen " + String.valueOf(mc.gui.screen()) + " health " + mc.player.getHealth()));
         }

         UiTest.then(1, (mc) -> {
            String var10000 = "totem." + round;
            boolean var10001 = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) && mc.gui.screen() == null;
            String var10002 = String.valueOf(mc.player.getOffhandItem());
            check(var10000, var10001, "offhand " + var10002 + ", totems left " + mc.player.getInventory().countItem(Items.TOTEM_OF_UNDYING) + ", screen " + String.valueOf(mc.gui.screen()));
         });
      }

   }

   private CombatTest() {
   }
}
