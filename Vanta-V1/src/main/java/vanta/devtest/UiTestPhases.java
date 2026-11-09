package vanta.devtest;

import java.util.Arrays;
import vanta.VantaClient;
import vanta.event.events.PreTickEvent;
import vanta.module.impl.hud.Notifications;
import vanta.ui.GuiState;
import vanta.ui.click.ClickGui;
import vanta.ui.click.HudEditorScreen;
import vanta.ui.click.PickerTest;
import vanta.ui.click.SliderDriver;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

final class UiTestPhases {
   static void add(String phase) {
      switch (phase) {
         case "world":
            UiTest.shot("world");
            break;
         case "modules":
            ModuleTest.add();
            break;
         case "idle":
            UiTest.then(6000, (mc) -> UiTest.log("idle over"));
            break;
         case "block":
            UiTest.then(1, (mc) -> VantaClient.EVENTS.subscribe(PreTickEvent.class, (e) -> {
                  if (mc.player != null && mc.level != null) {
                     Player near = null;

                     for(Player p : mc.level.players()) {
                        if (p != mc.player && (near == null || p.distanceTo(mc.player) < near.distanceTo(mc.player))) {
                           near = p;
                        }
                     }

                     if (near != null) {
                        Vec3 eye = mc.player.getEyePosition();
                        Vec3 at = near.getEyePosition();
                        double dx = at.x - eye.x;
                        double dy = at.y - eye.y;
                        double dz = at.z - eye.z;
                        mc.player.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
                        mc.player.setXRot((float)Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
                     }

                     mc.options.keyUse.setDown(mc.player.getOffhandItem().is(Items.SHIELD));
                  }
               }));
            UiTest.then(12000, (mc) -> UiTest.log("block over"));
            break;
         case "macefall":
            MaceFallTest.add();
            break;
         case "plate":
            PlateTest.add();
            break;
         case "friends":
            FriendsTest.add();
            break;
         case "pearl":
            PearlTest.add();
            break;
         case "esp":
            EspTest.add();
            break;
         case "breach":
         case "lunge":
         case "grapple":
         case "totem":
            CombatTest.add(phase);
            break;
         case "keys":
            HudKeysTest.add();
            break;
         case "sliders":
            UiTest.then(1, (mc) -> {
               ClickGui.open();
               ClickGui.testResetLayout();
            });
            UiTest.then(25, (mc) -> ClickGui.testExpand("autoMace"));
            UiTest.then(25, (mc) -> SliderDriver.sliderPastEnd());
            UiTest.then(5, (mc) -> SliderDriver.sliderTyping());
            UiTest.then(5, (mc) -> SliderDriver.rangeTyping());
            UiTest.shot("sliders_over");
            UiTest.then(5, (mc) -> SliderDriver.scrollDown());
            UiTest.then(30, (mc) -> SliderDriver.cappedPastEnd());
            UiTest.shot("sliders_capped");
            UiTest.then(5, (mc) -> ClickGui.close());
            break;
         case "platehud":
            UiTest.then(1, (mc) -> {
               ClickGui.open();
               ClickGui.testResetLayout();
            });
            UiTest.then(25, (mc) -> ClickGui.close());
            UiTest.then(20, (mc) -> UiTest.log("plate hud default, namePlate on=" + VantaClient.MODULES.get("namePlate").isEnabled()));
            UiTest.shot("platehud_default");
            UiTest.then(1, (mc) -> ClickGui.open());
            UiTest.then(25, (mc) -> UiTest.log("dragged to " + Arrays.toString(ClickGui.testDragPlate(260.0F, -150.0F))));
            UiTest.then(5, (mc) -> ClickGui.close());
            UiTest.then(20, (mc) -> UiTest.log("plate hud moved"));
            UiTest.shot("platehud_moved");
            UiTest.then(1, (mc) -> VantaClient.MODULES.get("namePlate").setEnabled(false));
            UiTest.then(5, (mc) -> UiTest.log("plate hud off"));
            UiTest.shot("platehud_off");
            UiTest.then(1, (mc) -> VantaClient.MODULES.get("namePlate").setEnabled(true));
            break;
         case "hud":
            UiTest.then(1, (mc) -> UiTest.enable(true, "arraylist", "watermark", "keystrokes", "notifications", "sprint", "esp2d"));
            UiTest.then(10, (mc) -> {
               boolean var10000 = mc.options.keyUp.isDown();
               UiTest.log("hud on; keys up=" + var10000 + " jump=" + mc.options.keyJump.isDown() + " use=" + mc.options.keyUse.isDown());
            });
            UiTest.then(1, (mc) -> {
               UiTest.enable(false, "fastPlace", "noJumpDelay");
               UiTest.enable(true, "fastPlace");
               Notifications.post("Vanta V1", "Welcome back", Notifications.Kind.INFO);
            });
            UiTest.then(8, (mc) -> UiTest.log("notifications " + Notifications.debug()));
            UiTest.shot("hud_toasts");
            UiTest.then(40, (mc) -> UiTest.enable(true, "noJumpDelay", "fastPlace"));
            UiTest.shot("hud");
            break;
         case "gui":
            UiTest.then(1, (mc) -> {
               ClickGui.open();
               ClickGui.testResetLayout();
            });
            UiTest.then(30, (mc) -> UiTest.log("gui open"));
            UiTest.shot("gui_open");
            UiTest.then(1, (mc) -> ClickGui.testAccent(true));
            UiTest.then(15, (mc) -> UiTest.log("accent"));
            UiTest.shot("gui_accent");
            UiTest.then(20, (mc) -> ClickGui.testAccent(false));
            UiTest.then(1, (mc) -> {
               UiTest.enable(true, "sprint");
               ClickGui.testExpand("autoMace");
               ClickGui.testExpand("esp2d");
            });
            UiTest.then(25, (mc) -> UiTest.log("expanded"));
            UiTest.shot("gui_expanded");
            UiTest.then(1, (mc) -> ClickGui.testColorPopup("esp2d", "friendColor"));
            UiTest.then(15, (mc) -> UiTest.log("popup"));
            UiTest.shot("gui_color");
            UiTest.then(20, (mc) -> ClickGui.testColorPopup("none", "none"));
            UiTest.then(1, (mc) -> ClickGui.testSearch("a"));
            UiTest.then(20, (mc) -> UiTest.log("searched"));
            UiTest.shot("gui_search");
            UiTest.then(1, (mc) -> {
               ClickGui.testSearch("");
               ClickGui.testTab(1);
            });
            UiTest.then(25, (mc) -> UiTest.log("settings"));
            UiTest.shot("gui_settings");
            UiTest.then(1, (mc) -> ClickGui.testConfigs());
            UiTest.then(20, (mc) -> UiTest.log("configs"));
            UiTest.shot("gui_configs");
            UiTest.then(20, (mc) -> {
               ClickGui.testTab(0);
               ClickGui.testAlpha();
               ClickGui.testColorPopup("esp2d", "friendColor");
            });
            UiTest.then(15, (mc) -> UiTest.log("alpha"));
            UiTest.shot("gui_alpha");
            UiTest.then(20, (mc) -> {
               ClickGui.testColorPopup("none", "none");
               ClickGui.testAccentColor(16751180);
               UiTest.enable(true, "moduleList", "watermark", "keystrokes", "notifications");
            });
            UiTest.then(20, (mc) -> UiTest.log("orange"));
            UiTest.shot("gui_orange");
            UiTest.then(20, (mc) -> ClickGui.testAccentColor(10851577));
            UiTest.then(1, (mc) -> ClickGui.close());
            UiTest.then(15, (mc) -> mc.gui.setScreen(new InventoryScreen(mc.player)));
            UiTest.then(5, (mc) -> ClickGui.open());
            UiTest.then(25, (mc) -> UiTest.log("over inventory"));
            UiTest.shot("gui_inventory");
            UiTest.then(1, (mc) -> ClickGui.close());
            UiTest.then(15, (mc) -> mc.gui.setScreen(new PauseScreen(true)));
            UiTest.then(5, (mc) -> ClickGui.open());
            UiTest.then(25, (mc) -> UiTest.log("over pause"));
            UiTest.shot("gui_pause");
            UiTest.then(1, (mc) -> ClickGui.close());
            UiTest.then(15, (mc) -> {
               mc.gui.setScreen((Screen)null);
               UiTest.log("closed, screen=" + String.valueOf(mc.gui.screen()));
            });
            UiTest.then(1, (mc) -> mc.gui.setScreen(new TitleScreen()));
            UiTest.then(5, (mc) -> ClickGui.open());
            UiTest.then(25, (mc) -> UiTest.log("over title"));
            UiTest.shot("gui_title");
            UiTest.then(1, (mc) -> ClickGui.close());
            UiTest.then(15, (mc) -> mc.gui.setScreen((Screen)null));
            break;
         case "picker":
            UiTest.then(1, (mc) -> {
               ClickGui.open();
               ClickGui.testResetLayout();
            });
            UiTest.then(20, (mc) -> PickerTest.step(0));

            for(int i = 1; i <= 7; ++i) {
               UiTest.then(12, (mc) -> PickerTest.step(i));
               if (i == 3) {
                  UiTest.shot("gui_picker_drag");
               }
            }

            UiTest.then(5, (mc) -> {
               ClickGui.testExpand("autoMace");
               GuiState.togglePinned("spearLunge");
            });
            UiTest.then(20, (mc) -> PickerTest.sliderTyping());
            UiTest.shot("gui_pinned");
            UiTest.then(10, (mc) -> {
               GuiState.togglePinned("spearLunge");
               ClickGui.close();
            });
            UiTest.then(15, (mc) -> UiTest.log("picker done"));
            break;
         case "hudedit":
            UiTest.then(1, (mc) -> UiTest.enable(true, "watermark", "keystrokes", "notifications"));
            UiTest.then(10, (mc) -> mc.gui.setScreen(new HudEditorScreen((Screen)null)));
            UiTest.then(20, (mc) -> {
               Screen patt0$temp = mc.gui.screen();
               if (patt0$temp instanceof HudEditorScreen e) {
                  e.testResize();
               }

            });
            UiTest.then(10, (mc) -> UiTest.log("hud resized"));
            UiTest.shot("gui_hudedit");
            UiTest.then(20, (mc) -> mc.gui.setScreen((Screen)null));
            break;
         case "scales":
            UiTest.then(1, (mc) -> {
               UiTest.enable(true, "moduleList", "watermark", "keystrokes", "notifications", "esp2d");
               IntegratedServer server = mc.getSingleplayerServer();
               if (server != null) {
                  server.execute(() -> {
                     CommandSourceStack src = server.createCommandSourceStack();
                     server.getCommands().performPrefixedCommand(src, "time set day");
                     server.getCommands().performPrefixedCommand(src, "execute as @a at @s run summon minecraft:zombie ^ ^ ^4 {NoAI:1b,PersistenceRequired:1b}");
                     server.getCommands().performPrefixedCommand(src, "execute as @a at @s run summon minecraft:armor_stand ^2 ^ ^5");
                  });
               }

            });
            int[][] sizes = new int[][]{{1920, 1080}};
            int[] scales = new int[]{2, 3, 0};

            for(int[] size : sizes) {
               for(int scale : scales) {
                  String tag = size[0] + "_s" + (scale == 0 ? "auto" : String.valueOf(scale));
                  UiTest.then(5, (mc) -> {
                     GLFW.glfwSetWindowSize(mc.getWindow().handle(), size[0], size[1]);
                     mc.options.guiScale().set(scale);
                     mc.resizeGui();
                  });
                  UiTest.then(15, (mc) -> {
                     mc.resizeGui();
                     int var10000 = mc.getFps();
                     UiTest.log("fps(closed)=" + var10000 + " size " + tag + " -> " + mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight() + " gui " + mc.getWindow().getGuiScale());
                  });
                  UiTest.shot("hud_" + tag);
                  UiTest.then(20, (mc) -> {
                     ClickGui.open();
                     ClickGui.testExpand("autoMace");
                     ClickGui.testExpand("esp2d");
                  });
                  UiTest.then(40, (mc) -> UiTest.log("gui " + tag + " fps(open)=" + mc.getFps()));
                  UiTest.shot("gui_" + tag);
                  UiTest.then(20, (mc) -> ClickGui.testColorPopup("esp2d", "friendColor"));
                  UiTest.then(10, (mc) -> UiTest.log("popup " + tag));
                  UiTest.shot("pop_" + tag);
                  UiTest.then(20, (mc) -> {
                     ClickGui.testColorPopup("none", "none");
                     ClickGui.testTab(1);
                  });
                  UiTest.then(20, (mc) -> UiTest.log("settings " + tag));
                  UiTest.shot("set_" + tag);
                  UiTest.then(20, (mc) -> {
                     ClickGui.testTab(0);
                     ClickGui.close();
                  });
               }
            }

            UiTest.then(20, (mc) -> UiTest.log("scales done"));
            break;
         default:
            UiTest.log("unknown phase " + phase);
      }

   }

   private UiTestPhases() {
   }
}
