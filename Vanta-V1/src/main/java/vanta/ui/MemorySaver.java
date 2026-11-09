package vanta.ui;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

public final class MemorySaver {
   private static final String MIN_FREE = "15";
   private static final String MAX_FREE = "35";
   private static final int CALM_TICKS = 30;
   private static final long EVERY_MILLIS = 120000L;
   private static boolean enabled = true;
   private static String[] original;
   private static Screen calm;
   private static int calmTicks;
   private static long lastTrim = System.currentTimeMillis();

   private MemorySaver() {
   }

   public static boolean enabled() {
      return enabled;
   }

   public static void setEnabled(boolean on) {
      enabled = on;
      if (!on) {
         restore();
      }

      GuiState.changed();
   }

   public static void tick(Minecraft mc) {
      if (enabled) {
         if (original == null) {
            tune();
         }

         Screen screen = mc.gui.screen();
         if (!calm(screen)) {
            calm = null;
         } else {
            if (screen != calm) {
               calm = screen;
               calmTicks = 0;
            }

            if (++calmTicks == 30 && System.currentTimeMillis() - lastTrim >= 120000L) {
               trim();
            }

         }
      }
   }

   static void trim() {
      lastTrim = System.currentTimeMillis();
      Thread thread = new Thread(System::gc, "Mace memory saver");
      thread.setDaemon(true);
      thread.start();
   }

   private static boolean calm(Screen screen) {
      return screen instanceof PauseScreen || screen instanceof TitleScreen || screen instanceof JoinMultiplayerScreen || screen instanceof SelectWorldScreen || screen instanceof DisconnectedScreen;
   }

   private static void tune() {
      original = new String[]{"", ""};

      try {
         HotSpotDiagnosticMXBean vm = (HotSpotDiagnosticMXBean)ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
         original = new String[]{vm.getVMOption("MinHeapFreeRatio").getValue(), vm.getVMOption("MaxHeapFreeRatio").getValue()};
         vm.setVMOption("MinHeapFreeRatio", "15");
         vm.setVMOption("MaxHeapFreeRatio", "35");
      } catch (LinkageError | RuntimeException var1) {
      }

   }

   private static void restore() {
      if (original != null && !original[0].isEmpty()) {
         try {
            HotSpotDiagnosticMXBean vm = (HotSpotDiagnosticMXBean)ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            vm.setVMOption("MaxHeapFreeRatio", original[1]);
            vm.setVMOption("MinHeapFreeRatio", original[0]);
         } catch (LinkageError | RuntimeException var1) {
         }

         original = null;
      } else {
         original = null;
      }
   }
}
