package vanta.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting<Boolean> {
   public BoolSetting(String id, boolean defaultValue) {
      super(id, defaultValue);
   }

   public JsonElement save() {
      return new JsonPrimitive(this.value);
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonPrimitive()) {
         this.value = json.getAsBoolean();
      }

   }
}
