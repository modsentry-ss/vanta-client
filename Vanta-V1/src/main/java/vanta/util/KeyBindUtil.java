package vanta.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import vanta.mixin.extra.IAccessorKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyBindUtil {
   public static final int NONE = 0;
   private static final int MOUSE_OFFSET = 100;
   private static final Map<String, Integer> NAME_TO_GLFW = new LinkedHashMap();
   private static final Map<Integer, String> GLFW_TO_NAME = new HashMap();
   private static final Map<Integer, Integer> LEGACY_TO_GLFW = new HashMap();

   private static void key(String name, int legacy, int glfw) {
      NAME_TO_GLFW.putIfAbsent(name, glfw);
      GLFW_TO_NAME.putIfAbsent(glfw, name);
      if (legacy > 0) {
         LEGACY_TO_GLFW.putIfAbsent(legacy, glfw);
      }

   }

   public static int mouseButtonToKey(int button) {
      return button - 100;
   }

   public static int keyToMouseButton(int keyCode) {
      return keyCode + 100;
   }

   public static boolean isMouseKey(int keyCode) {
      return keyCode < 0;
   }

   public static int fromLegacy(int legacyCode) {
      return legacyCode <= 0 ? legacyCode : (Integer)LEGACY_TO_GLFW.getOrDefault(legacyCode, 0);
   }

   public static boolean isUnbindKey(int keyCode) {
      return keyCode == 256 || keyCode == 261 || keyCode == 259;
   }

   public static boolean isBindableMouseButton(int button) {
      return button > 0;
   }

   public static int parseKey(String input) {
      if (input == null) {
         return Integer.MIN_VALUE;
      } else {
         String name = input.trim().toUpperCase(Locale.ROOT);
         if (name.isEmpty()) {
            return Integer.MIN_VALUE;
         } else if (!name.equals("NONE") && !name.equals("NULL") && !name.equals("0") && !name.equals("UNBIND")) {
            Integer key = (Integer)NAME_TO_GLFW.get(name);
            if (key != null) {
               return key;
            } else {
               int button = parseMouseButton(name);
               if (button != -1) {
                  return mouseButtonToKey(button);
               } else {
                  try {
                     return Integer.parseInt(name);
                  } catch (NumberFormatException var5) {
                     return Integer.MIN_VALUE;
                  }
               }
            }
         } else {
            return 0;
         }
      }
   }

   private static int parseMouseButton(String name) {
      switch (name) {
         case "LBUTTON":
         case "LMB":
         case "LEFTCLICK":
            return 0;
         case "RBUTTON":
         case "RMB":
         case "RIGHTCLICK":
            return 1;
         case "MBUTTON":
         case "MMB":
         case "MIDDLECLICK":
         case "SCROLLCLICK":
            return 2;
         case "XBUTTON1":
         case "SIDEBUTTON1":
         case "BOTTOMSIDE":
            return 3;
         case "XBUTTON2":
         case "SIDEBUTTON2":
         case "TOPSIDE":
            return 4;
         default:
            String[] prefixes = new String[]{"MOUSE", "BUTTON", "MB", "M"};

            for(String prefix : prefixes) {
               if (name.startsWith(prefix) && name.length() > prefix.length()) {
                  try {
                     int button = Integer.parseInt(name.substring(prefix.length()));
                     if (button >= 0 && button <= 7) {
                        return button;
                     }
                  } catch (NumberFormatException var7) {
                  }
               }
            }

            return -1;
      }
   }

   public static String getKeyName(int keyCode) {
      if (keyCode == 0) {
         return "None";
      } else if (keyCode < 0) {
         int mouseButton = keyCode + 100;
         switch (mouseButton) {
            case 0 -> {
               return "LMB";
            }
            case 1 -> {
               return "RMB";
            }
            case 2 -> {
               return "MMB";
            }
            default -> {
               return "MOUSE" + mouseButton;
            }
         }
      } else {
         String name = (String)GLFW_TO_NAME.get(keyCode);
         return name != null ? name : "KEY" + keyCode;
      }
   }

   public static boolean isKeyDown(int keyCode) {
      if (keyCode == 0) {
         return false;
      } else {
         long window = Minecraft.getInstance().getWindow().handle();
         if (keyCode >= 0) {
            return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), keyCode);
         } else {
            int button = keyCode + 100;
            return button >= 0 && button <= 7 && GLFW.glfwGetMouseButton(window, button) == 1;
         }
      }
   }

   public static InputConstants.Key toInputKey(int keyCode) {
      if (keyCode == 0) {
         return InputConstants.UNKNOWN;
      } else {
         return keyCode < 0 ? Type.MOUSE.getOrCreate(keyCode + 100) : Type.KEYSYM.getOrCreate(keyCode);
      }
   }

   public static int getKeyCode(KeyMapping keyMapping) {
      InputConstants.Key key = ((IAccessorKeyMapping)keyMapping).getKey();
      switch (key.getType()) {
         case MOUSE -> {
            return mouseButtonToKey(key.getValue());
         }
         case KEYSYM -> {
            return key.getValue() < 0 ? 0 : key.getValue();
         }
         default -> {
            return 0;
         }
      }
   }

   public static void updateKeyState(int keyCode) {
      setKeyBindState(keyCode, isKeyDown(keyCode));
   }

   public static void setKeyBindState(int keyCode, boolean pressed) {
      if (keyCode != 0) {
         KeyMapping.set(toInputKey(keyCode), pressed);
      }

   }

   public static void pressKeyOnce(int keyCode) {
      if (keyCode != 0) {
         KeyMapping.click(toInputKey(keyCode));
      }

   }

   static {
      key("ESCAPE", 1, 256);
      String digits = "1234567890";

      for(int i = 0; i < digits.length(); ++i) {
         key(String.valueOf(digits.charAt(i)), 2 + i, digits.charAt(i));
      }

      key("MINUS", 12, 45);
      key("EQUALS", 13, 61);
      key("BACK", 14, 259);
      key("TAB", 15, 258);
      String row1 = "QWERTYUIOP";

      for(int i = 0; i < row1.length(); ++i) {
         key(String.valueOf(row1.charAt(i)), 16 + i, row1.charAt(i));
      }

      key("LBRACKET", 26, 91);
      key("RBRACKET", 27, 93);
      key("RETURN", 28, 257);
      key("LCONTROL", 29, 341);
      String row2 = "ASDFGHJKL";

      for(int i = 0; i < row2.length(); ++i) {
         key(String.valueOf(row2.charAt(i)), 30 + i, row2.charAt(i));
      }

      key("SEMICOLON", 39, 59);
      key("APOSTROPHE", 40, 39);
      key("GRAVE", 41, 96);
      key("LSHIFT", 42, 340);
      key("BACKSLASH", 43, 92);
      String row3 = "ZXCVBNM";

      for(int i = 0; i < row3.length(); ++i) {
         key(String.valueOf(row3.charAt(i)), 44 + i, row3.charAt(i));
      }

      key("COMMA", 51, 44);
      key("PERIOD", 52, 46);
      key("SLASH", 53, 47);
      key("RSHIFT", 54, 344);
      key("MULTIPLY", 55, 332);
      key("LMENU", 56, 342);
      key("SPACE", 57, 32);
      key("CAPITAL", 58, 280);

      for(int i = 0; i < 10; ++i) {
         key("F" + (i + 1), 59 + i, 290 + i);
      }

      key("NUMLOCK", 69, 282);
      key("SCROLL", 70, 281);
      key("NUMPAD7", 71, 327);
      key("NUMPAD8", 72, 328);
      key("NUMPAD9", 73, 329);
      key("SUBTRACT", 74, 333);
      key("NUMPAD4", 75, 324);
      key("NUMPAD5", 76, 325);
      key("NUMPAD6", 77, 326);
      key("ADD", 78, 334);
      key("NUMPAD1", 79, 321);
      key("NUMPAD2", 80, 322);
      key("NUMPAD3", 81, 323);
      key("NUMPAD0", 82, 320);
      key("DECIMAL", 83, 330);
      key("F11", 87, 300);
      key("F12", 88, 301);
      key("F13", 100, 302);
      key("F14", 101, 303);
      key("F15", 102, 304);
      key("F16", 103, 305);
      key("F17", 104, 306);
      key("F18", 105, 307);
      key("F19", 113, 308);
      key("NUMPADEQUALS", 141, 336);
      key("NUMPADENTER", 156, 335);
      key("RCONTROL", 157, 345);
      key("DIVIDE", 181, 331);
      key("SYSRQ", 183, 283);
      key("RMENU", 184, 346);
      key("PAUSE", 197, 284);
      key("HOME", 199, 268);
      key("UP", 200, 265);
      key("PRIOR", 201, 266);
      key("LEFT", 203, 263);
      key("RIGHT", 205, 262);
      key("END", 207, 269);
      key("DOWN", 208, 264);
      key("NEXT", 209, 267);
      key("INSERT", 210, 260);
      key("DELETE", 211, 261);
      key("LMETA", 219, 343);
      key("RMETA", 220, 347);
      key("APPS", 221, 348);
      key("ENTER", 0, 257);
      key("BACKSPACE", 0, 259);
      key("LCTRL", 0, 341);
      key("RCTRL", 0, 345);
      key("LALT", 0, 342);
      key("RALT", 0, 346);
      key("CAPSLOCK", 0, 280);
      key("PAGEUP", 0, 266);
      key("PAGEDOWN", 0, 267);
   }
}
