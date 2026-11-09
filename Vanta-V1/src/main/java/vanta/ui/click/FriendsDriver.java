package vanta.ui.click;

import java.util.Locale;
import vanta.friends.FriendList;
import vanta.setting.TextSetting;
import net.minecraft.client.input.KeyEvent;

public final class FriendsDriver {
   private FriendsDriver() {
   }

   private static void log(String s) {
      System.out.println("[UT] friends " + s);
   }

   public static void check(String name, boolean ok, String detail) {
      log((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : " (" + detail + ")"));
   }

   private static boolean click(float[] r) {
      GuiView v = ClickGui.view();
      double[] g = v.toGui((double)(r[0] + r[2] / 2.0F), (double)(r[1] + r[3] / 2.0F));
      boolean used = v.mouseClicked(g[0], g[1], 0);
      v.mouseReleased();
      return used;
   }

   public static void clear() {
      for(String name : FriendList.all()) {
         FriendList.remove(name);
      }

   }

   public static void typeAndEnter(String name) {
      GuiView v = ClickGui.view();
      float[] r = (float[])v.rects.get(fieldOf(v));
      if (r == null) {
         check("name field drawn", false, "no rect");
      } else {
         click(new float[]{r[0], r[1] + 15.5F, r[2], 11.5F});
         check("click focuses the name field", Widgets.typing == fieldOf(v), "typing=" + String.valueOf(Widgets.typing));

         for(char ch : name.toCharArray()) {
            ClickGui.onChar(ch);
         }

         ClickGui.onKey(new KeyEvent(257, 0, 0), 1);
         check("Enter adds " + name, FriendList.contains(name), "list=" + String.valueOf(FriendList.all()));
         check("field cleared and still focused", Widgets.typing == fieldOf(v) && Widgets.buffer.isEmpty(), "buffer='" + String.valueOf(Widgets.buffer) + "'");
      }
   }

   public static void remove(String name) {
      GuiView v = ClickGui.view();
      float[] r = (float[])v.friendRects.get("remove:" + name.toLowerCase(Locale.ROOT));
      if (r == null) {
         check("remove button drawn for " + name, false, "rects=" + String.valueOf(v.friendRects.keySet()));
      } else {
         click(r);
         check("x removes " + name, !FriendList.contains(name), "list=" + String.valueOf(FriendList.all()));
      }
   }

   public static void pickOnline(String name) {
      GuiView v = ClickGui.view();
      float[] r = (float[])v.friendRects.get("online:" + name.toLowerCase(Locale.ROOT));
      if (r == null) {
         check("online row drawn for " + name, false, "rects=" + String.valueOf(v.friendRects.keySet()));
      } else {
         click(r);
         check("online pick adds " + name, FriendList.contains(name), "list=" + String.valueOf(FriendList.all()));
      }
   }

   public static void clickTabButton() {
      GuiView v = ClickGui.view();
      Widgets.typing = null;
      float h = 19.0F;
      float gap = 5.0F;
      float modW = Paint.width("Modules", 7.0F, (byte)0) + 26.0F;
      float x = (v.vwForTest() - (modW + 5.0F * (h + gap))) / 2.0F;
      float bx = x + modW + gap + h + gap;
      click(new float[]{bx, 14.0F, h, h});
      check("top bar button opens Friends", v.tabForTest() == 3, "tab=" + v.tabForTest());
   }

   private static TextSetting fieldOf(GuiView v) {
      return v.friendFieldForTest();
   }
}
