package vanta.module;

public enum Category {
   COMBAT("combat", "Combat"),
   MOVEMENT("movement", "Movement"),
   RENDER("render", "Render"),
   PLAYER("player", "Player"),
   HUD("hud", "HUD"),
   MISC("misc", "Misc");

   private final String id;
   private final String label;

   private Category(String id, String label) {
      this.id = id;
      this.label = label;
   }

   public String id() {
      return this.id;
   }

   public String label() {
      return this.label;
   }

   // $FF: synthetic method
   private static Category[] $values() {
      return new Category[]{COMBAT, MOVEMENT, RENDER, PLAYER, HUD, MISC};
   }
}
