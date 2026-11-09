package vanta.devtest;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import vanta.VantaClient;
import vanta.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.tutorial.TutorialSteps;

public final class UiTest {
   private static final Deque<Step> STEPS = new ArrayDeque();
   private static int countdown = -1;
   private static boolean started;
   private static boolean acceptedWarning;

   public static void init() {
      String phases = System.getenv("MACE_UITEST");
      if (phases != null && !phases.isBlank()) {
         ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(mc) -> tick(mc, phases));
      }
   }

   public static void log(String message) {
      System.out.println("[UT] " + message);
   }

   public static void then(int wait, Consumer<Minecraft> action) {
      STEPS.add(new Step(wait, action));
   }

   public static void shot(String name) {
      then(3, (mc) -> {
         File dir = new File(mc.gameDirectory, "vt");
         dir.mkdirs();
         Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), (image) -> {
            try {
               NativeImage img = image;

               try {
                  img.writeToFile(new File(dir, name + ".png"));
                  log("shot " + name);
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
               log("shot failed " + name + " " + String.valueOf(exception));
            }

         });
      });
   }

   public static void enable(boolean on, String... ids) {
      for(String id : ids) {
         VantaClient.MODULES.find(id).ifPresentOrElse((m) -> m.setEnabled(on), () -> log("no module " + id));
      }

   }

   private static boolean acceptScreens(Minecraft mc) {
      if (!acceptedWarning) {
         Screen var2 = mc.gui.screen();
         if (var2 instanceof BackupConfirmScreen) {
            BackupConfirmScreen screen = (BackupConfirmScreen)var2;

            for(GuiEventListener child : screen.children()) {
               if (child instanceof AbstractButton) {
                  AbstractButton button = (AbstractButton)child;
                  if (button.getMessage().getString().contains("Know What")) {
                     log("accepting experimental world");
                     acceptedWarning = true;
                     button.onPress(new KeyEvent(257, 0, 0));
                     return true;
                  }
               }
            }
         }
      }

      if (mc.gui.screen() instanceof AccessibilityOnboardingScreen) {
         log("skipping the accessibility onboarding (a fresh game dir)");
         mc.options.onboardingAccessibilityFinished();
         mc.gui.setScreen(new TitleScreen());
         return true;
      } else {
         return false;
      }
   }

   private static void tick(Minecraft mc, String phases) {
      mc.options.pauseOnLostFocus = false;
      if (!acceptScreens(mc)) {
         if (started) {
            if (mc.player != null) {
               if (countdown < 0) {
                  Step next = (Step)STEPS.peek();
                  if (next == null) {
                     return;
                  }

                  countdown = next.delay();
               }

               if (countdown-- <= 0) {
                  Step step = (Step)STEPS.poll();
                  countdown = -1;
                  if (step != null) {
                     try {
                        step.action().accept(mc);
                     } catch (Throwable throwable) {
                        log("step failed " + String.valueOf(throwable));
                        throwable.printStackTrace();
                     }
                  }

               }
            }
         } else {
            if (mc.gui.screen() instanceof TitleScreen) {
               started = true;
               mc.options.pauseOnLostFocus = false;
               mc.options.tutorialStep = TutorialSteps.NONE;
               String server = System.getenv("MACE_SERVER");
               if (server != null && !server.isBlank()) {
                  log("joining server " + server);
                  ConnectScreen.startConnecting(mc.gui.screen(), mc, ServerAddress.parseString(server), new ServerData("test", server, Type.OTHER), false, (TransferState)null);
               } else {
                  mc.createWorldOpenFlows().openWorld("MaceTest", () -> log("open cancelled"));
               }

               then(60, (m) -> log("world ready local=" + m.isLocalServer()));

               for(String phase : phases.split(",")) {
                  UiTestPhases.add(phase.trim());
               }

               then(20, (m) -> {
                  log("done");
                  m.stop();
               });
            }

         }
      }
   }

   static Module module(String id) {
      return VantaClient.MODULES.get(id);
   }

   private UiTest() {
   }

   private static record Step(int delay, Consumer<Minecraft> action) {
   }
}
