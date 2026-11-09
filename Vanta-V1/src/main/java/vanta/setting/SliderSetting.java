package vanta.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class SliderSetting extends Setting<Integer> {
   private final int min;
   private final int max;
   private final Measure measure;
   private IntFunction<String> formatter;
   private Function<String, Integer> parser;
   private int limit;
   private boolean unlimitedAtMax;

   public SliderSetting(String id, int defaultValue, int min, int max) {
      this(id, defaultValue, min, max, Measure.NONE);
   }

   public SliderSetting(String id, int defaultValue, int min, int max, Measure measure) {
      super(id, defaultValue);
      this.limit = -1;
      this.min = min;
      this.max = max;
      this.measure = measure;
   }

   public SliderSetting withFormat(IntFunction<String> formatter) {
      this.formatter = formatter;
      return this;
   }

   public SliderSetting withParse(Function<String, Integer> parser) {
      this.parser = parser;
      return this;
   }

   public SliderSetting percentOfRange() {
      this.limit = this.max;
      this.formatter = (v) -> {
         float var10000 = (float)(v - this.min()) * 100.0F;
         int var10001 = Math.max(1, this.max() - this.min());
         return Math.round(var10000 / (float)var10001) + "%";
      };
      this.parser = (text) -> {
         try {
            float p = Float.parseFloat(text.replace("%", "").trim());
            return this.min() + Math.round(Math.clamp(p, 0.0F, 100.0F) * (float)(this.max() - this.min()) / 100.0F);
         } catch (NumberFormatException var3) {
            return null;
         }
      };
      return this;
   }

   public String typedText() {
      return this.parser != null ? this.format((Integer)this.get()).replaceAll("[^0-9.-]", "") : this.measure.plain((Integer)this.get());
   }

   public Integer parse(String text) {
      return this.parser != null ? (Integer)this.parser.apply(text) : this.measure.parse(text);
   }

   public SliderSetting uncapped() {
      this.limit = 1000000000;
      return this;
   }

   public SliderSetting capped() {
      this.limit = this.max;
      return this;
   }

   public int limit() {
      return this.limit >= 0 ? this.limit : this.measure.ceiling(this.max);
   }

   public Measure measure() {
      return this.measure;
   }

   public SliderSetting unlimitedAtMax() {
      this.unlimitedAtMax = true;
      return this;
   }

   public boolean unlimited() {
      return this.unlimitedAtMax && (Integer)this.value >= this.max;
   }

   public double limitValue() {
      return this.unlimited() ? Double.POSITIVE_INFINITY : (double)(Integer)this.value;
   }

   public String format(int value) {
      if (this.unlimitedAtMax && value >= this.max) {
         return "Unlimited";
      } else {
         return this.formatter != null ? (String)this.formatter.apply(value) : this.measure.format(value);
      }
   }

   public void set(Integer value) {
      super.set(Math.clamp((long)value, this.min, this.limit()));
   }

   public JsonElement save() {
      return new JsonPrimitive((Number)this.value);
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonPrimitive()) {
         this.set(json.getAsInt());
      }

   }

   public int min() {
      return this.min;
   }

   public int max() {
      return this.max;
   }
}
