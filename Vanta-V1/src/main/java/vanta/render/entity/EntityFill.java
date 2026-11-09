package vanta.render.entity;

import net.minecraft.world.phys.Vec3;

public record EntityFill(int visible, int occluded, int visibleOverlay, int occludedOverlay, int light, int effect) {
   public static EntityFill solid(int visible, int occluded) {
      return gradient(visible, visible, occluded, occluded, 0, 0);
   }

   public static EntityFill gradient(int visible, int visibleEnd, int occluded, int occludedEnd, int screenMinY, int screenMaxY) {
      int light = clamp(screenMinY) & '\uffff' | clamp(screenMaxY) << 16;
      return new EntityFill(visible, occluded, color(visibleEnd), color(occludedEnd), light, 0);
   }

   public static EntityFill effect(int visible, int occluded, int effect, Vec3 origin, float scale) {
      int overlay = wrapped(origin.x, 512.0F) & '\uffff' | wrapped(origin.y, 512.0F) << 16;
      int light = wrapped(origin.z, 512.0F) & '\uffff' | fixed((double)scale, 256.0F) << 16;
      return new EntityFill(visible, occluded, overlay, overlay, light, effect);
   }

   public EntityFill anchored(Vec3 origin) {
      return this.effect == 0 ? this : effect(this.visible, this.occluded, this.effect, origin, (float)(this.light >>> 16) / 256.0F);
   }

   private static int color(int rgb) {
      return rgb >> 16 & 255 | rgb & '\uff00' | (rgb & 255) << 16;
   }

   private static int fixed(double value, float units) {
      return clamp((int)Math.round(value * (double)units));
   }

   private static int wrapped(double value, float units) {
      return (short)((int)Math.round(value * (double)units));
   }

   private static int clamp(int value) {
      return Math.clamp((long)value, -32768, 32767);
   }
}
