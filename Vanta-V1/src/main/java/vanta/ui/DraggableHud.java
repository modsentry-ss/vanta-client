package vanta.ui;

public interface DraggableHud {
   String getHudName();

   float[] getHudBounds();

   void setHudPosition(float var1, float var2);

   void resetHudPosition();

   boolean isHudEnabled();

   default boolean canScaleHud() {
      return false;
   }

   default float getHudScale() {
      return 1.0F;
   }

   default void setHudScale(float scale) {
   }
}
