package vanta.render.font;

public final class Fonts {
   public static final Font COMFORTAA_BOLD = Font.load("fonts/comfortaa-bold", 63);
   public static final Font INTER_MEDIUM = Font.load("fonts/inter-medium", 63);
   public static final Font INTER_SEMIBOLD = Font.load("fonts/inter-semibold", 63);
   public static final Font MATERIAL_ICONS = Font.load("icons/material", 59576);
   public static final Font UI_ICONS = Font.load("icons/ui", 1);
   public static final Font UI_ICONS_SMALL = Font.load("icons/ui_small", 1);
   public static final Font LOGO = Font.load("brand/logo", 1);
   public static final Font LOGO_SMALL = Font.load("brand/logo_small", 1);
   public static final float DISTANCE_RANGE;

   private Fonts() {
   }

   static {
      DISTANCE_RANGE = COMFORTAA_BOLD.distanceRange();

      for(Font font : new Font[]{INTER_MEDIUM, INTER_SEMIBOLD, MATERIAL_ICONS, LOGO, LOGO_SMALL, UI_ICONS, UI_ICONS_SMALL}) {
         if (font.distanceRange() != DISTANCE_RANGE) {
            throw new IllegalStateException("Every MSDF atlas must share the text pipeline's distance range");
         }
      }

   }
}
