package vanta.visual.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;

public final class VanillaHud {
   public static final HudSlot CHAT;
   public static final HudSlot SCOREBOARD;
   private static float[] scoreboard;
   private static long scoreboardSeen;
   private static final float[] measured;
   private static boolean measuring;
   private static final float[] chat;
   private static final float[] placement;

   private VanillaHud() {
   }

   public static void init() {
   }

   private static boolean scoreboardShown() {
      return scoreboard != null && System.currentTimeMillis() - scoreboardSeen < 1000L;
   }

   private static float[] chatBox(int screenHeight) {
      Minecraft mc = Minecraft.getInstance();
      boolean focused = mc.gui.screen() instanceof ChatScreen;
      double scale = (Double)mc.options.chatScale().get();
      float width = (float)((double)ChatComponent.getWidth((Double)mc.options.chatWidth().get()) + (double)12.0F * scale);
      float height = (float)ChatComponent.getHeight(focused ? (Double)mc.options.chatHeightFocused().get() : (Double)mc.options.chatHeightUnfocused().get());
      float line = (float)((double)9.0F * ((Double)mc.options.chatLineSpacing().get() + (double)1.0F) * scale);
      height = Math.min(height, (float)Math.round(line * 4.0F));
      chat[0] = 0.0F;
      chat[1] = (float)screenHeight - 40.0F - height;
      chat[2] = width;
      chat[3] = height;
      return chat;
   }

   private static float[] place(HudSlot slot, float[] box, float screenWidth, float screenHeight) {
      float scale = slot.scale();
      float w = box[2] * scale;
      float h = box[3] * scale;
      if (slot.isDefault()) {
         slot.placed(box[0], box[1], box[2], box[3]);
         return null;
      } else {
         float x = slot.x(w, screenWidth);
         float y = slot.y(h, screenHeight);
         slot.placed(x, y, w, h);
         placement[0] = x;
         placement[1] = y;
         placement[2] = scale;
         return placement;
      }
   }

   public static void beginChat(GuiGraphicsExtractor graphics) {
      float[] box = chatBox(graphics.guiHeight());
      float[] at = place(CHAT, box, (float)graphics.guiWidth(), (float)graphics.guiHeight());
      graphics.pose().pushMatrix();
      if (at != null) {
         graphics.pose().translate(at[0], at[1]);
         graphics.pose().scale(at[2], at[2]);
         graphics.pose().translate(-box[0], -box[1]);
      }

   }

   public static void endChat(GuiGraphicsExtractor graphics) {
      graphics.pose().popMatrix();
   }

   public static boolean overChat(double x, double y) {
      float[] bounds = CHAT.getHudBounds();
      return bounds != null && x >= (double)bounds[0] && x < (double)(bounds[0] + bounds[2]) && y >= (double)bounds[1] && y < (double)(bounds[1] + bounds[3]);
   }

   public static double chatX(double x) {
      Minecraft mc = Minecraft.getInstance();
      float[] box = chatBox(mc.getWindow().getGuiScaledHeight());
      if (CHAT.isDefault()) {
         return x;
      } else {
         float scale = CHAT.scale();
         float sx = CHAT.x(box[2] * scale, (float)mc.getWindow().getGuiScaledWidth());
         return (double)box[0] + (x - (double)sx) / (double)scale;
      }
   }

   public static double chatY(double y) {
      Minecraft mc = Minecraft.getInstance();
      float[] box = chatBox(mc.getWindow().getGuiScaledHeight());
      if (CHAT.isDefault()) {
         return y;
      } else {
         float scale = CHAT.scale();
         float sy = CHAT.y(box[3] * scale, (float)mc.getWindow().getGuiScaledHeight());
         return (double)box[1] + (y - (double)sy) / (double)scale;
      }
   }

   public static void beginScoreboard(GuiGraphicsExtractor graphics) {
      measuring = false;
      graphics.pose().pushMatrix();
      if (scoreboard != null) {
         float[] at = place(SCOREBOARD, scoreboard, (float)graphics.guiWidth(), (float)graphics.guiHeight());
         if (at != null) {
            graphics.pose().translate(at[0], at[1]);
            graphics.pose().scale(at[2], at[2]);
            graphics.pose().translate(-scoreboard[0], -scoreboard[1]);
         }

      }
   }

   public static void measure(int x0, int y0, int x1, int y1) {
      if (!measuring) {
         measuring = true;
         measured[0] = (float)x0;
         measured[1] = (float)y0;
         measured[2] = (float)x1;
         measured[3] = (float)y1;
      } else {
         measured[0] = Math.min(measured[0], (float)x0);
         measured[1] = Math.min(measured[1], (float)y0);
         measured[2] = Math.max(measured[2], (float)x1);
         measured[3] = Math.max(measured[3], (float)y1);
      }

   }

   public static void endScoreboard(GuiGraphicsExtractor graphics) {
      graphics.pose().popMatrix();
      if (measuring) {
         if (scoreboard == null) {
            scoreboard = new float[4];
         }

         scoreboard[0] = measured[0];
         scoreboard[1] = measured[1];
         scoreboard[2] = measured[2] - measured[0];
         scoreboard[3] = measured[3] - measured[1];
         scoreboardSeen = System.currentTimeMillis();
      }

   }

   static {
      CHAT = HudSlot.of("chat", "Chat", HudSlot.Edge.START, HudSlot.Edge.END, 0.0F, 40.0F, () -> true);
      SCOREBOARD = HudSlot.of("scoreboard", "Scoreboard", HudSlot.Edge.END, HudSlot.Edge.CENTER, 1.0F, 0.0F, VanillaHud::scoreboardShown);
      measured = new float[4];
      chat = new float[4];
      placement = new float[3];
   }
}
