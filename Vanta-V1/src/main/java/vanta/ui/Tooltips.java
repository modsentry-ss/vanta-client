package vanta.ui;

import vanta.util.Lang;

public enum Tooltips {
   DESCRIPTION("tooltips.description"),
   OFF("tooltips.off");

   public static final Tooltips[] ALL = values();
   private static Tooltips current = DESCRIPTION;
   private final String key;

   public String displayName() {
      return Lang.getOrDefault(this.key, this == OFF ? "Off" : "Description");
   }

   public static Tooltips current() {
      return current;
   }

   public static void set(Tooltips mode) {
      current = mode;
      GuiState.changed();
   }

   private Tooltips(final String key) {
      this.key = key;
   }

   // $FF: synthetic method
   private static Tooltips[] $values() {
      return new Tooltips[]{DESCRIPTION, OFF};
   }
}
