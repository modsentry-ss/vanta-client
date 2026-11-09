package vanta.module;

import vanta.setting.PositionSetting;

public abstract class HudModule extends Module {
   private final PositionSetting position;
   private float x;
   private float y;
   private float width;
   private float height;

   protected HudModule(String id, PositionSetting.Anchor horizontal, PositionSetting.Anchor vertical, float offsetX, float offsetY) {
      super(id);
      this.position = (PositionSetting)this.add(new PositionSetting("position", new PositionSetting.Position(horizontal, vertical, offsetX, offsetY)));
   }

   public PositionSetting.Position position() {
      return (PositionSetting.Position)this.position.get();
   }

   protected float left(float width, float screenWidth) {
      return ((PositionSetting.Position)this.position.get()).x(width, screenWidth);
   }

   protected float top(float height, float screenHeight) {
      return ((PositionSetting.Position)this.position.get()).y(height, screenHeight);
   }

   protected void placed(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public boolean contains(double pointX, double pointY) {
      return pointX >= (double)this.x && pointX < (double)(this.x + this.width) && pointY >= (double)this.y && pointY < (double)(this.y + this.height);
   }

   public void moveTo(float x, float y, float screenWidth, float screenHeight) {
      this.position.set(PositionSetting.Position.of(x, y, this.width, this.height, screenWidth, screenHeight));
   }

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }
}
