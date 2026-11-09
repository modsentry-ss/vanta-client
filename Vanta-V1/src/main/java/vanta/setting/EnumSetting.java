package vanta.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import vanta.render.UiIcon;
import net.minecraft.resources.Identifier;

public final class EnumSetting extends Setting<Integer> {
   private final String[] options;
   private List<UiIcon> icons = List.of();
   private List<Identifier> images = List.of();

   public EnumSetting(String id, int defaultIndex, String... options) {
      super(id, defaultIndex);
      this.options = options;
   }

   public EnumSetting withArt(List<UiIcon> icons, List<Identifier> images) {
      if (icons.size() == this.options.length && images.size() == this.options.length) {
         this.icons = List.copyOf(icons);
         this.images = List.copyOf(images);
         return this;
      } else {
         throw new IllegalArgumentException("Every option of " + this.id() + " needs an icon and an image");
      }
   }

   public String selected() {
      return this.options[Math.clamp((long)(Integer)this.get(), 0, this.options.length - 1)];
   }

   public void set(Integer value) {
      super.set(Math.clamp((long)value, 0, this.options.length - 1));
   }

   public JsonElement save() {
      return new JsonPrimitive(this.options[(Integer)this.value]);
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonPrimitive()) {
         String name = json.getAsString();

         for(int i = 0; i < this.options.length; ++i) {
            if (this.options[i].equals(name)) {
               this.set(i);
            }
         }
      }

   }

   public String[] options() {
      return this.options;
   }

   public List<UiIcon> icons() {
      return this.icons;
   }

   public List<Identifier> images() {
      return this.images;
   }
}
