package vanta.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class TextSetting extends Setting<String> {
   private final int maxLength;

   public TextSetting(String id, String defaultValue, int maxLength) {
      super(id, defaultValue);
      this.maxLength = maxLength;
   }

   public int maxLength() {
      return this.maxLength;
   }

   public JsonElement save() {
      return new JsonPrimitive((String)this.get());
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonPrimitive()) {
         this.set(json.getAsString());
      }

   }
}
