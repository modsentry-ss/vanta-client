package vanta.render.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vanta.render.Style;
import net.minecraft.world.phys.AABB;

public final class WorldBuffer {
   private final List<Box> boxes = new ArrayList();
   private final List<Box> behindWalls = new ArrayList();

   public void box(AABB bounds, Style style) {
      this.boxes.add(new Box(bounds, style));
   }

   public List<Box> boxes() {
      return Collections.unmodifiableList(this.boxes);
   }

   public void boxBehindWalls(AABB bounds, Style style) {
      this.behindWalls.add(new Box(bounds, style));
   }

   public List<Box> behindWalls() {
      return Collections.unmodifiableList(this.behindWalls);
   }

   public static record Box(AABB bounds, Style style) {
   }
}
