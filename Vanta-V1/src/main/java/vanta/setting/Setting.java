package vanta.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import vanta.config.StateStore;
import vanta.util.Lang;

public abstract class Setting<T> {
   private final String id;
   private final T defaultValue;
   private String moduleId = "";
   private BooleanSupplier visible = () -> true;
   private boolean described;
   protected T value;
   private Supplier<T> boundGet;
   private Consumer<T> boundSet;
   private String displayName;

   protected Setting(String id, T defaultValue) {
      this.id = id;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
   }

   public void attach(String moduleId) {
      this.moduleId = moduleId;
   }

   public void visibleWhen(BooleanSupplier condition) {
      this.visible = condition;
   }

   public boolean isVisible() {
      return this.visible.getAsBoolean();
   }

   public void describe() {
      this.described = true;
   }

   public boolean described() {
      return this.described;
   }

   public String description() {
      return Lang.getOrDefault("setting." + this.moduleId + "." + this.id + ".help", Lang.getOrDefault("setting." + this.id + ".help", ""));
   }

   public Setting<T> bind(Supplier<T> get, Consumer<T> set, String displayName) {
      this.boundGet = get;
      this.boundSet = set;
      this.displayName = displayName;
      return this;
   }

   public boolean bound() {
      return this.boundGet != null;
   }

   public String name() {
      if (this.displayName != null) {
         return this.displayName;
      } else {
         String fallback = Lang.getOrDefault("setting." + this.id, Lang.capitalize(this.id));
         return Lang.getOrDefault("setting." + this.moduleId + "." + this.id, fallback);
      }
   }

   public T get() {
      return (T)(this.boundGet != null ? this.boundGet.get() : this.value);
   }

   public void set(T value) {
      this.value = value;
      if (this.boundSet != null) {
         this.boundSet.accept(value);
      }

      StateStore.changed();
   }

   public void reset() {
      this.value = this.defaultValue;
   }

   public abstract JsonElement save();

   public abstract void load(JsonElement var1);

   public String id() {
      return this.id;
   }
}
