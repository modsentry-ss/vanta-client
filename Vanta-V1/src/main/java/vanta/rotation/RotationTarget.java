package vanta.rotation;

@FunctionalInterface
public interface RotationTarget {
   Rotation at(float var1);

   static RotationTarget fixed(Rotation rotation) {
      return (partialTick) -> rotation;
   }
}
