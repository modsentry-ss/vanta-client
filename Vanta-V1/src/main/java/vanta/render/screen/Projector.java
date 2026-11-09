package vanta.render.screen;

import vanta.render.Point;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface Projector {
   Point project(double var1, double var3, double var5);

   default Point project(Vec3 position) {
      return this.project(position.x, position.y, position.z);
   }
}
