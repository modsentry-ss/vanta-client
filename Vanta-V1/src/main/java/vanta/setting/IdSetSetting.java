package vanta.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class IdSetSetting extends Setting<Set<String>> {
   protected IdSetSetting(String id) {
      super(id, new LinkedHashSet());
   }

   public JsonElement save() {
      JsonArray array = new JsonArray();

      for(String id : this.value) {
         array.add(id);
      }

      return array;
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonArray()) {
         ((Set)this.value).clear();

         for(JsonElement element : json.getAsJsonArray()) {
            ((Set)this.value).add(element.getAsString());
         }
      }

   }
}
