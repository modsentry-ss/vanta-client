package vanta.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.function.IntSupplier;
import vanta.config.StateStore;

public final class RangeSetting extends Setting<int[]> {
   private final int min;
   private IntSupplier max;
   private final Measure measure;

   public RangeSetting(String id, int defaultLow, int defaultHigh, int min, int max) {
      this(id, defaultLow, defaultHigh, min, max, Measure.NONE);
   }

   public RangeSetting(String id, int defaultLow, int defaultHigh, int min, int max, Measure measure) {
      super(id, new int[]{defaultLow, defaultHigh});
      this.min = min;
      this.max = () -> max;
      this.measure = measure;
   }

   public int max() {
      return this.max.getAsInt();
   }

   public void maxWhen(IntSupplier supplier) {
      this.max = supplier;
   }

   public int limit() {
      return Math.max(this.max(), this.measure.ceiling(this.max()));
   }

   public Measure measure() {
      return this.measure;
   }

   public String format(int value) {
      return this.measure.format(value);
   }

   public int low() {
      return Math.clamp((long)((int[])this.value)[0], this.min, this.limit());
   }

   public int high() {
      return Math.clamp((long)((int[])this.value)[1], this.min, this.limit());
   }

   public void setLow(int low) {
      this.value = new int[]{Math.clamp((long)low, this.min, this.high()), ((int[])this.value)[1]};
      StateStore.changed();
   }

   public void setHigh(int high) {
      this.value = new int[]{((int[])this.value)[0], Math.clamp((long)high, this.low(), this.limit())};
      StateStore.changed();
   }

   public JsonElement save() {
      JsonArray array = new JsonArray();
      array.add(((int[])this.value)[0]);
      array.add(((int[])this.value)[1]);
      return array;
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonArray() && json.getAsJsonArray().size() == 2) {
         JsonArray array = json.getAsJsonArray();
         this.value = new int[]{Math.clamp((long)array.get(0).getAsInt(), this.min, this.limit()), Math.clamp((long)array.get(1).getAsInt(), this.min, this.limit())};
         if (((int[])this.value)[0] > ((int[])this.value)[1]) {
            this.value = new int[]{((int[])this.value)[1], ((int[])this.value)[0]};
         }
      }

   }

   public int min() {
      return this.min;
   }
}
