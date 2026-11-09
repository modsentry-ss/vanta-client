package vanta.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.function.BooleanSupplier;
import vanta.config.StateStore;
import vanta.util.Colors;

public final class ColorSetting extends Setting<Integer> {
   private final int defaultSecondary;
   private int secondary;
   private BooleanSupplier gradient;
   private Runnable onChange;
   private int alpha;

   public ColorSetting(String id, int defaultRgb) {
      this(id, defaultRgb, defaultRgb);
   }

   public ColorSetting(String id, int defaultRgb, int defaultSecondaryRgb) {
      super(id, Colors.rgb(defaultRgb));
      this.gradient = () -> false;
      this.alpha = 255;
      this.defaultSecondary = Colors.rgb(defaultSecondaryRgb);
      this.secondary = this.defaultSecondary;
   }

   public ColorSetting onChange(Runnable action) {
      this.onChange = action;
      return this;
   }

   public void set(Integer value) {
      super.set(value);
      if (this.onChange != null) {
         this.onChange.run();
      }

   }

   public int alpha() {
      return this.alpha;
   }

   public void setAlpha(int alpha) {
      this.alpha = Math.clamp((long)alpha, 0, 255);
      StateStore.changed();
      if (this.onChange != null) {
         this.onChange.run();
      }

   }

   public int argb() {
      return this.alpha << 24 | (Integer)this.get() & 16777215;
   }

   private String hexA(int rgb) {
      String hex = Colors.hex(rgb);
      return this.alpha < 255 ? hex + String.format("%02X", this.alpha) : hex;
   }

   public void gradientWhen(BooleanSupplier condition) {
      this.gradient = condition;
   }

   public boolean isGradient() {
      return this.gradient.getAsBoolean();
   }

   public void setSecondary(int rgb) {
      this.secondary = Colors.rgb(rgb);
   }

   public void reset() {
      super.reset();
      this.secondary = this.defaultSecondary;
      this.alpha = 255;
   }

   public JsonElement save() {
      if (this.secondary == (Integer)this.value) {
         return new JsonPrimitive(this.hexA((Integer)this.value));
      } else {
         JsonArray array = new JsonArray();
         array.add(this.hexA((Integer)this.value));
         array.add(Colors.hex(this.secondary));
         return array;
      }
   }

   public void load(JsonElement json) {
      if (json != null) {
         if (json.isJsonArray() && json.getAsJsonArray().size() == 2) {
            JsonArray array = json.getAsJsonArray();
            this.alpha = alphaOf(array.get(0));
            this.value = parse(array.get(0), (Integer)this.value);
            this.secondary = parse(array.get(1), this.secondary);
         } else if (json.isJsonPrimitive()) {
            this.alpha = alphaOf(json);
            this.value = parse(json, (Integer)this.value);
            this.secondary = (Integer)this.value;
         }
      }

   }

   private static int alphaOf(JsonElement json) {
      try {
         String hex = json.getAsString().replace("#", "").trim();
         return hex.length() == 8 ? Integer.parseInt(hex.substring(6), 16) : 255;
      } catch (IllegalStateException | NumberFormatException var2) {
         return 255;
      }
   }

   private static int parse(JsonElement json, int fallback) {
      try {
         String hex = json.getAsString().replace("#", "").trim();
         if (hex.length() == 8) {
            hex = hex.substring(0, 6);
         }

         return Colors.rgb(Integer.parseInt(hex, 16));
      } catch (IllegalStateException | NumberFormatException var3) {
         return fallback;
      }
   }

   public int secondary() {
      return this.secondary;
   }
}
