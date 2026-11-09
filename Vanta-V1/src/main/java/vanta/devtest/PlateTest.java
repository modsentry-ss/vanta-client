package vanta.devtest;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.util.Arrays;
import vanta.ui.click.ClickGui;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class PlateTest {
   private static int ticks = -1;
   private static int waited;

   static void add() {
      UiTest.then(1, (mc) -> ClickGui.open());
      UiTest.then(30, (mc) -> UiTest.log("plate pref at open " + Arrays.toString(ClickGui.testPlatePref())));
      UiTest.shot("plate_open");
      UiTest.then(1, (mc) -> {
         String var10000 = Arrays.toString(ClickGui.testDragPlate(260.0F, -150.0F));
         UiTest.log("plate dragged to " + var10000 + " pref " + Arrays.toString(ClickGui.testPlatePref()));
      });
      UiTest.then(20, (mc) -> UiTest.log("plate moved"));
      UiTest.shot("plate_dragged");
      UiTest.then(1, (mc) -> ClickGui.testTab(1));
      UiTest.then(25, (mc) -> UiTest.log("plate settings"));
      UiTest.shot("plate_settings");
      UiTest.then(1, (mc) -> ClickGui.testTab(2));
      UiTest.then(25, (mc) -> UiTest.log("plate configs"));
      UiTest.shot("plate_configs");
      UiTest.then(1, (mc) -> {
         ClickGui.testTab(0);
         UiTest.log("scale before " + ClickGui.testScale(0) + " -> set 140 while open");
         ClickGui.testScale(140);
      });
      UiTest.then(10, (mc) -> UiTest.log("scale while open after change " + ClickGui.testScale(0)));
      UiTest.shot("scale_changed_open");
      UiTest.then(1, (mc) -> ClickGui.close());
      UiTest.then(15, (mc) -> ClickGui.open());
      UiTest.then(25, (mc) -> UiTest.log("scale after reopen " + ClickGui.testScale(0)));
      UiTest.shot("scale_reopened");
      UiTest.then(1, (mc) -> {
         ClickGui.testScale(100);
         ClickGui.close();
      });
      UiTest.then(15, (mc) -> ClickGui.open());
      UiTest.then(20, (mc) -> UiTest.log("scale back " + ClickGui.testScale(0)));
      UiTest.then(1, (mc) -> ClickGui.close());
      UiTest.then(15, (mc) -> ClickGui.open());
      UiTest.then(30, (mc) -> UiTest.log("center 1280x720 gui " + mc.getWindow().getGuiScale()));
      UiTest.shot("center_1280_auto");
      UiTest.then(1, (mc) -> ClickGui.close());
      int[][] modes = new int[][]{{1920, 1080, 0}, {1920, 1080, 3}, {1280, 720, 2}};

      for(int[] m : modes) {
         String tag = m[0] + "_s" + (m[2] == 0 ? "auto" : String.valueOf(m[2]));
         UiTest.then(15, (mc) -> {
            GLFW.glfwSetWindowSize(mc.getWindow().handle(), m[0], m[1]);
            mc.options.guiScale().set(m[2]);
            mc.resizeGui();
         });
         UiTest.then(15, (mc) -> {
            mc.resizeGui();
            ClickGui.open();
         });
         UiTest.then(30, (mc) -> UiTest.log("center " + tag + " -> " + mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight() + " gui " + mc.getWindow().getGuiScale()));
         UiTest.shot("center_" + tag);
         UiTest.then(1, (mc) -> ClickGui.close());
      }

      UiTest.then(15, (mc) -> {
         GLFW.glfwSetWindowSize(mc.getWindow().handle(), 1280, 720);
         mc.options.guiScale().set(0);
         mc.resizeGui();
      });
      UiTest.then(15, (mc) -> mc.gui.setScreen(new InventoryScreen(mc.player)));
      UiTest.then(5, (mc) -> ClickGui.open());
      UiTest.then(25, (mc) -> UiTest.log("plate inventory"));
      UiTest.shot("plate_inventory");
      UiTest.then(1, (mc) -> ClickGui.close());
      UiTest.then(15, (mc) -> {
         mc.gui.setScreen((Screen)null);
         UiTest.log("in game, opening");
         ClickGui.open();
      });
      UiTest.then(25, (mc) -> UiTest.log("plate in game"));
      UiTest.shot("plate_ingame");
      UiTest.then(1, (mc) -> ClickGui.close());
      UiTest.then(15, (mc) -> {
         mc.gui.setScreen((Screen)null);
         UiTest.log("leaving the world");
         mc.disconnectFromWorld(Component.literal("plate test"));
         mc.gui.setScreen(new TitleScreen());
         ClientTickEvents.END_CLIENT_TICK.register(PlateTest::titleTick);
      });
   }

   private static void titleTick(Minecraft mc) {
      if (ticks < 0) {
         if (mc.player != null || mc.level != null || !(mc.gui.screen() instanceof TitleScreen)) {
            if (++waited % 100 == 0 && mc.level == null) {
               UiTest.log("still waiting, screen=" + String.valueOf(mc.gui.screen()) + "; opening the title screen");
               mc.gui.setScreen(new TitleScreen());
            }

            return;
         }

         ticks = 0;
         UiTest.log("on title screen, player=" + String.valueOf(mc.player));
      }

      ++ticks;
      if (ticks == 20) {
         ClickGui.open();
      } else if (ticks == 120) {
         screenshot(mc, "plate_title");
      } else if (ticks == 130) {
         ClickGui.testTab(1);
      } else if (ticks == 160) {
         screenshot(mc, "plate_title_settings");
      } else if (ticks == 175) {
         UiTest.log("done");
         mc.stop();
      }

   }

   private static void screenshot(Minecraft mc, String name) {
      File dir = new File(mc.gameDirectory, "vt");
      dir.mkdirs();
      Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), (image) -> {
         try {
            NativeImage img = image;

            try {
               img.writeToFile(new File(dir, name + ".png"));
               UiTest.log("shot " + name);
            } catch (Throwable var7) {
               if (image != null) {
                  try {
                     img.close();
                  } catch (Throwable x2) {
                     var7.addSuppressed(x2);
                  }
               }

               throw var7;
            }

            if (image != null) {
               image.close();
            }
         } catch (Exception exception) {
            UiTest.log("shot failed " + name + " " + String.valueOf(exception));
         }

      });
   }

   private PlateTest() {
   }
}
