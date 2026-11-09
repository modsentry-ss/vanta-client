package vanta.rotation;

import java.util.function.Supplier;

public enum Smoothing {
   EASE_OUT_CUBIC(EaseOutCubic::new),
   WIND_MOUSE(WindMouse::new);

   public static final String[] NAMES = new String[]{"Ease out cubic", "WindMouse"};
   private final Supplier<Rotator> factory;

   public Rotator create() {
      return (Rotator)this.factory.get();
   }

   private Smoothing(final Supplier<Rotator> factory) {
      this.factory = factory;
   }

   // $FF: synthetic method
   private static Smoothing[] $values() {
      return new Smoothing[]{EASE_OUT_CUBIC, WIND_MOUSE};
   }
}
