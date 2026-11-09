package vanta.module.impl.render.esp;

import java.util.Objects;
import vanta.event.events.EntityRenderStateEvent;
import vanta.render.Rect;
import vanta.render.entity.EntityOutline;
import vanta.render.entity.EntityOutlines;
import vanta.render.screen.Projection;
import vanta.setting.Measure;
import vanta.setting.MultiSetting;
import vanta.setting.SliderSetting;
import vanta.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

final class Outline {
   private static final String[] SHOW = new String[]{"Fill", "Glow"};
   private static final int SHOW_FILL = 0;
   private static final int SHOW_GLOW = 1;
   private final Esp esp;
   private final Box box;
   private final MultiSetting show;
   private final SliderSetting intensity;
   private final SliderSetting radius;
   private Projection projection;

   Outline(Esp esp, Box box) {
      this.esp = esp;
      this.box = box;
      this.show = (MultiSetting)esp.setting(new MultiSetting("outlineShow", SHOW, new int[0]));
      this.intensity = (SliderSetting)esp.setting(new SliderSetting("glowIntensity", 100, 10, 300, Measure.PERCENT));
      this.radius = (SliderSetting)esp.setting(new SliderSetting("glowRadius", 4, 1, 16, Measure.NONE));
      MultiSetting var10000 = this.show;
      Objects.requireNonNull(esp);
      var10000.visibleWhen(esp::outlined);
      this.intensity.visibleWhen(() -> esp.outlined() && this.show.has(1) || box.glows());
      this.radius.visibleWhen(() -> esp.outlined() && this.show.has(1) || box.glows());
   }

   boolean filled() {
      return this.show.has(0);
   }

   int color() {
      return Colors.opaque(this.box.rgb());
   }

   void include(EntityRenderStateEvent event) {
      if (this.projection == null) {
         this.projection = Projection.capture();
      }

      Projection view = this.projection;
      Entity entity = event.entity();
      Rect rect = view.bounds(entity.getBoundingBox().inflate((double)1.0F).move(event.renderPosition().subtract(entity.position())));
      if (rect != null) {
         EntityOutlines.include(view.framebufferX(rect.minX()), view.framebufferY(rect.maxY()), view.framebufferX(rect.maxX()), view.framebufferY(rect.minY()));
      }

   }

   void endFrame() {
      this.projection = null;
   }

   EntityOutline spec() {
      return this.esp.outlined() ? new EntityOutline(this.color(), this.texels(), this.fill(), 0, this.glow(), this.glowRadius()) : null;
   }

   float fill() {
      return this.filled() ? this.box.fillOpacity() : 0.0F;
   }

   float glow() {
      return this.show.has(1) ? (float)(Integer)this.intensity.get() / 100.0F : 0.0F;
   }

   float haloIntensity() {
      return (float)(Integer)this.intensity.get() / 100.0F;
   }

   float haloRadius() {
      return (float)(Integer)this.radius.get() * 1.25F;
   }

   float glowRadius() {
      return (float)(Integer)this.radius.get() * (float)Minecraft.getInstance().getWindow().getGuiScale();
   }

   float texels() {
      return this.esp.outlinePixels();
   }
}
