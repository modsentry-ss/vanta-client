package vanta.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum Bone {
   MULTIPOINT((Vec3)null),
   CLOSEST((Vec3)null),
   HEAD(new Vec3((double)0.0F, (double)1.75F, (double)0.0F)),
   BODY(new Vec3((double)0.0F, (double)1.125F, (double)0.0F)),
   LEFT_ARM(new Vec3((double)0.375F, (double)1.125F, (double)0.0F)),
   RIGHT_ARM(new Vec3((double)-0.375F, (double)1.125F, (double)0.0F)),
   LEFT_LEG(new Vec3((double)0.125F, (double)0.375F, (double)0.0F)),
   RIGHT_LEG(new Vec3((double)-0.125F, (double)0.375F, (double)0.0F));

   public static final Bone[] ALL = values();
   public static final String[] NAMES = new String[]{"Multipoint", "Closest", "Head", "Body", "Left arm", "Right arm", "Left leg", "Right leg"};
   private static final double EDGE = 0.2;
   private static final double EDGE_FRACTION = 0.3;
   private final Vec3 center;

   public Vec3 point(Entity viewer, LivingEntity entity, float partialTick) {
      Vec3 position = entity.getPosition(partialTick);
      if (this.center == null) {
         AABB box = entity.getBoundingBox().move(position.subtract(entity.position()));
         return this == CLOSEST ? nearest(viewer.getEyePosition(partialTick), viewer.getLookAngle(), box) : closest(viewer.getEyePosition(partialTick), viewer.getLookAngle(), box);
      } else {
         float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
         double angle = Math.toRadians((double)bodyYaw);
         double sin = Math.sin(angle);
         double cos = Math.cos(angle);
         Vec3 offset = this.center.scale((double)entity.getScale());
         return position.add(offset.x * cos - offset.z * sin, offset.y, offset.x * sin + offset.z * cos);
      }
   }

   static Vec3 closest(Vec3 eye, Vec3 look, AABB box) {
      AABB inner = box.deflate(margin(box.getXsize()), margin(box.getYsize()), margin(box.getZsize()));
      Vec3 point = inner.getCenter();

      for(int i = 0; i < 2; ++i) {
         double depth = Math.max((double)0.0F, point.subtract(eye).dot(look));
         Vec3 along = eye.add(look.scale(depth));
         point = new Vec3(Mth.clamp(along.x, inner.minX, inner.maxX), Mth.clamp(along.y, inner.minY, inner.maxY), Mth.clamp(along.z, inner.minZ, inner.maxZ));
      }

      return point;
   }

   public static Vec3 nearest(Vec3 eye, Vec3 look, AABB box) {
      return closest(eye, look, box, 0.2, 0.3);
   }

   private static Vec3 closest(Vec3 eye, Vec3 look, AABB box, double margin, double fraction) {
      AABB inner = box.deflate(Math.min(margin, box.getXsize() * fraction), Math.min(margin, box.getYsize() * fraction), Math.min(margin, box.getZsize() * fraction));
      Vec3 point = inner.getCenter();

      for(int i = 0; i < 3; ++i) {
         double depth = Math.max((double)0.0F, point.subtract(eye).dot(look));
         Vec3 along = eye.add(look.scale(depth));
         point = new Vec3(Mth.clamp(along.x, inner.minX, inner.maxX), Mth.clamp(along.y, inner.minY, inner.maxY), Mth.clamp(along.z, inner.minZ, inner.maxZ));
      }

      return point;
   }

   private static double margin(double size) {
      return Math.min(0.1, size * (double)0.25F);
   }

   private Bone(final Vec3 center) {
      this.center = center;
   }

   // $FF: synthetic method
   private static Bone[] $values() {
      return new Bone[]{MULTIPOINT, CLOSEST, HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG};
   }
}
