package vanta.module.impl.render.esp;

import java.util.Objects;
import vanta.render.Rect;
import vanta.render.entity.EntityFill;
import vanta.render.screen.Projection;
import vanta.setting.BoolSetting;
import vanta.setting.ColorSetting;
import vanta.setting.EnumSetting;
import vanta.util.Colors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class Shader {
   private static final String[] MODES = new String[]{"Solid", "Gradient"};
   private static final int GRADIENT = 1;
   private final Esp esp;
   private final EnumSetting mode;
   private final BoolSetting invisible;
   private final ColorSetting visibleColor;
   private final ColorSetting invisibleColor;
   private final ColorSetting handColor;
   private final BoolSetting heldItems;

   Shader(Esp esp) {
      this.esp = esp;
      this.mode = (EnumSetting)esp.setting(new EnumSetting("shaderMode", 0, MODES));
      this.invisible = (BoolSetting)esp.setting(new BoolSetting("invisible", true));
      this.visibleColor = (ColorSetting)esp.setting(new ColorSetting("visibleColor", 12115376, 12045544));
      this.invisibleColor = (ColorSetting)esp.setting(new ColorSetting("invisibleColor", 15054532, 13810919));
      this.handColor = (ColorSetting)esp.setting(new ColorSetting("handColor", 12115376, 12045544));
      this.heldItems = (BoolSetting)esp.setting(new BoolSetting("heldItems", true));
      BoolSetting var10000 = this.heldItems;
      Objects.requireNonNull(esp);
      var10000.visibleWhen(esp::shaded);
      EnumSetting var2 = this.mode;
      Objects.requireNonNull(esp);
      var2.visibleWhen(esp::shaded);
      BoolSetting var3 = this.invisible;
      Objects.requireNonNull(esp);
      var3.visibleWhen(esp::shaded);
      ColorSetting var4 = this.visibleColor;
      Objects.requireNonNull(esp);
      var4.visibleWhen(esp::shaded);
      this.invisibleColor.visibleWhen(() -> esp.shaded() && (Boolean)this.invisible.get());
      this.handColor.visibleWhen(() -> esp.shaded() && esp.handTargeted());
      this.visibleColor.gradientWhen(() -> esp.shaded() && this.gradient());
      this.invisibleColor.gradientWhen(() -> esp.shaded() && this.gradient());
      this.handColor.gradientWhen(() -> esp.shaded() && this.gradient());
   }

   private boolean gradient() {
      return (Integer)this.mode.get() == 1;
   }

   EntityFill teamFill(int color, Vec3 position, Camera camera) {
      int visible = Colors.opaque(color);
      int occluded = (Boolean)this.invisible.get() ? Colors.opaque(Colors.darken(color, 0.35F)) : visible;
      return EntityFill.solid(visible, occluded);
   }

   EntityFill entityFill(Vec3 position, Camera camera, EntityRenderState state) {
      int visible = Colors.opaque((Integer)this.visibleColor.get());
      int occluded = (Boolean)this.invisible.get() ? Colors.opaque((Integer)this.invisibleColor.get()) : visible;
      if (!this.gradient()) {
         return EntityFill.solid(visible, occluded);
      } else {
         Projection projection = Projection.capture();
         double half = (double)state.boundingBoxWidth / (double)2.0F;
         Rect rect = projection.bounds(new AABB(position.x - half, position.y, position.z - half, position.x + half, position.y + (double)state.boundingBoxHeight, position.z + half));
         int minY = rect == null ? 0 : projection.framebufferY(rect.maxY());
         int maxY = rect == null ? 0 : projection.framebufferY(rect.minY());
         int occludedEnd = (Boolean)this.invisible.get() ? this.invisibleColor.secondary() : this.visibleColor.secondary();
         return EntityFill.gradient(visible, this.visibleColor.secondary(), occluded, occludedEnd, minY, maxY);
      }
   }

   boolean heldItems() {
      return this.esp.shaded() && (Boolean)this.heldItems.get();
   }

   EntityFill handFill() {
      int color = Colors.opaque((Integer)this.handColor.get());
      return !this.gradient() ? EntityFill.solid(color, 0) : EntityFill.gradient(color, this.handColor.secondary(), 0, 0, 0, Minecraft.getInstance().getWindow().getHeight());
   }
}
