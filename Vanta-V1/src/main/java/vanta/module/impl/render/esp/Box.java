package vanta.module.impl.render.esp;

import java.util.Objects;
import java.util.Set;
import vanta.render.Edges;
import vanta.render.Style;
import vanta.setting.ColorSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.MultiSetting;
import vanta.setting.SliderSetting;
import vanta.util.Colors;
import net.minecraft.world.phys.AABB;

final class Box {
   static final int SHOW_BOX = 0;
   static final int SHOW_FILL = 1;
   static final int SHOW_OUTLINE = 2;
   static final int SHOW_HEALTH = 3;
   static final int SHOW_GLOW = 4;
   private static final String[] TYPES = new String[]{"Full", "Cornered"};
   private static final int TYPE_CORNERED = 1;
   private static final String[] SHOW = new String[]{"Box", "Fill", "Outline", "Health", "Glow"};
   private static final int OUTLINE_COLOR = -16777216;
   private final Esp esp;
   private final EnumSetting type;
   private final ColorSetting color;
   private final MultiSetting show;
   private final SliderSetting fillOpacity;
   private final SliderSetting edgeWidth;

   Box(Esp esp) {
      this.esp = esp;
      this.type = (EnumSetting)esp.setting(new EnumSetting("type", 0, TYPES));
      this.color = (ColorSetting)esp.setting(new ColorSetting("color", 16777215));
      this.show = (MultiSetting)esp.setting(new MultiSetting("show", SHOW, new int[]{0, 2, 3}));
      this.fillOpacity = (SliderSetting)esp.setting(new SliderSetting("fillOpacity", 12, 0, 100, Measure.PERCENT));
      this.edgeWidth = (SliderSetting)esp.setting((new SliderSetting("edgeWidth", 1, 1, 4)).withFormat(Esp::pixels));
      this.edgeWidth.visibleWhen(() -> !esp.shaded() && !esp.outlined() && this.shows(0) && this.shows(2));
      this.edgeWidth.describe();
      EnumSetting var10000 = this.type;
      Objects.requireNonNull(esp);
      var10000.visibleWhen(esp::flat);
      this.color.visibleWhen(() -> !esp.shaded());
      this.show.visibleWhen(() -> !esp.shaded() && !esp.outlined());
      this.show.optionVisibleWhen((option) -> {
         boolean var10000;
         switch (option) {
            case 1 -> var10000 = esp.boxed() || ((Set)this.show.get()).contains(0);
            case 2 -> var10000 = ((Set)this.show.get()).contains(0);
            default -> var10000 = true;
         }

         return var10000;
      });
      this.fillOpacity.visibleWhen(() -> {
         boolean var10000;
         label25: {
            if (!esp.shaded()) {
               if (esp.outlined()) {
                  if (esp.outlineFilled()) {
                     break label25;
                  }
               } else if (this.shows(1)) {
                  break label25;
               }
            }

            var10000 = false;
            return var10000;
         }

         var10000 = true;
         return var10000;
      });
   }

   boolean shows(int element) {
      return this.show.has(element);
   }

   boolean shown() {
      return !this.esp.outlined() && this.shows(0);
   }

   boolean cornered() {
      return (Integer)this.type.get() == 1;
   }

   boolean outlined() {
      return this.shown() && this.shows(2);
   }

   boolean filled() {
      return this.shown() && this.shows(1);
   }

   boolean glows() {
      return !this.esp.shaded() && !this.esp.outlined() && this.shows(4);
   }

   int rgb() {
      return (Integer)this.color.get();
   }

   float edgePixels() {
      return (float)Math.clamp((long)(Integer)this.edgeWidth.get(), 1, 4);
   }

   float fillOpacity() {
      return (float)(Integer)this.fillOpacity.get() / 100.0F;
   }

   Style worldStyle() {
      return this.worldStyle((Integer)this.color.get());
   }

   Style screenStyle() {
      return this.screenStyle((Integer)this.color.get());
   }

   Style worldStyle(int color) {
      float line = this.esp.weightPixels();
      return this.styled(line, line + this.edgePixels() * 2.0F, color);
   }

   Style screenStyle(int color) {
      float line = this.esp.weightPixels();
      Style style = this.styled(line, line + this.edgePixels() * 2.0F, color);
      return this.cornered() ? style.withEdges(Edges.CORNERED) : style;
   }

   private Style styled(float lineWidth, float outlineWidth, int color) {
      int rgb = Colors.opaque(color);
      float alpha = color >>> 24 == 0 ? 1.0F : (float)(color >>> 24) / 255.0F;
      if (!this.esp.shaded() && !this.esp.outlined()) {
         Style style = this.shown() ? Style.stroke(Colors.withAlpha(rgb, alpha), lineWidth) : Style.EMPTY;
         if (this.shows(1)) {
            style = style.withFill(Colors.withAlpha(rgb, this.fillOpacity() * alpha));
         }

         if (this.shows(2)) {
            style = style.withOutline(Colors.withAlpha(-16777216, alpha), outlineWidth);
         }

         return style;
      } else {
         return Style.EMPTY;
      }
   }

   static AABB fitPlayer(AABB bounds) {
      return new AABB(bounds.minX - 0.12, bounds.minY, bounds.minZ - 0.12, bounds.maxX + 0.12, bounds.maxY + 0.06, bounds.maxZ + 0.12);
   }
}
