package vanta.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.IntPredicate;

public final class MultiSetting extends Setting<Set<Integer>> {
   private final String[] options;
   private IntPredicate optionVisible = (option) -> true;

   public MultiSetting(String id, String[] options, int... defaults) {
      super(id, new LinkedHashSet());
      this.options = options;

      for(int index : defaults) {
         ((Set)this.value).add(index);
      }

   }

   public void optionVisibleWhen(IntPredicate condition) {
      this.optionVisible = condition;
   }

   public boolean isOptionVisible(int option) {
      return this.optionVisible.test(option);
   }

   public boolean has(int option) {
      return ((Set)this.value).contains(option) && this.isOptionVisible(option);
   }

   public JsonElement save() {
      JsonArray array = new JsonArray();

      for(int index : this.value) {
         array.add(this.options[index]);
      }

      return array;
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonArray()) {
         ((Set)this.value).clear();

         for(JsonElement element : json.getAsJsonArray()) {
            for(int i = 0; i < this.options.length; ++i) {
               if (this.options[i].equals(element.getAsString())) {
                  ((Set)this.value).add(i);
               }
            }
         }
      }

   }

   public String[] options() {
      return this.options;
   }
}
