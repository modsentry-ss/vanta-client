package vanta.util;

import vanta.ui.Theme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public final class ChatUtil {
   private static final Minecraft mc = Minecraft.getInstance();

   public static void send(Component component) {
      if (mc.player != null) {
         mc.gui.hud.getChat().addClientSystemMessage(component);
      }

   }

   public static void sendFormatted(String string) {
      int accent = Theme.ACCENT & 16777215;
      send(Component.empty().append(Component.literal("[").withStyle(ChatFormatting.GRAY)).append(Component.literal("Vanta V1").withStyle((style) -> style.withColor(TextColor.fromRgb(accent)))).append(Component.literal("] ").withStyle(ChatFormatting.GRAY)).append(Component.literal(string.replace('&', '§'))));
   }

   private ChatUtil() {
   }
}
