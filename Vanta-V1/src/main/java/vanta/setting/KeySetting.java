package vanta.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import vanta.module.ModuleManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KeySetting extends Setting<InputConstants.Key> {
   public KeySetting(String id) {
      super(id, InputConstants.UNKNOWN);
   }

   public boolean bound() {
      return this.get() != null && this.get() != InputConstants.UNKNOWN;
   }

   public boolean down(Minecraft client) {
      InputConstants.Key key = (InputConstants.Key)this.get();
      if (this.bound() && client.gui.screen() == null) {
         long window = client.getWindow().handle();
         boolean var10000;
         switch (key.getType()) {
            case KEYSYM -> var10000 = GLFW.glfwGetKey(window, key.getValue()) == 1;
            case MOUSE -> var10000 = GLFW.glfwGetMouseButton(window, key.getValue()) == 1;
            default -> var10000 = false;
         }

         return var10000;
      } else {
         return false;
      }
   }

   public void set(InputConstants.Key value) {
      super.set(value == null ? InputConstants.UNKNOWN : value);
   }

   public JsonElement save() {
      return new JsonPrimitive(((InputConstants.Key)this.get()).getName());
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonPrimitive()) {
         this.set(ModuleManager.parseKey(json.getAsString()));
      }

   }
}
