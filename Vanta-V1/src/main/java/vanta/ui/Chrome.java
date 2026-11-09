package vanta.ui;

import vanta.render.Gfx;
import vanta.render.Opacity;
import vanta.render.Scissor;
import vanta.visual.Fx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public final class Chrome {
   public static final float RADIUS = 3.0F;
   public static final int[] RISK_COLORS = new int[]{-11870592, -6035915, -340971, -290244, -495247};

   private Chrome() {
   }

   public static void styled(GuiGraphicsExtractor graphics, Runnable draw) {
      Gfx.begin(graphics);
      ScreenRectangle clip = Scissor.current();
      if (clip != null) {
         graphics.scissorStack.push(clip);
      }

      float previous = Gfx.alphaScale();
      Gfx.setAlphaScale(previous * ((float)(Opacity.apply(-16777216) >>> 24) / 255.0F));

      try {
         draw.run();
         Gfx.flush();
      } finally {
         Gfx.setAlphaScale(previous);
         if (clip != null) {
            graphics.scissorStack.pop();
         }

         Gfx.end();
      }

   }

   public static int accent2() {
      return Theme.ACCENT;
   }

   public static int hairline() {
      return Theme.isLight() ? 520093696 : 452984831;
   }

   public static void backdrop(GuiGraphicsExtractor graphics, int width, int height, float alpha) {
      if (!(alpha <= 0.0F)) {
         styled(graphics, () -> {
            int dim = Theme.isLight() ? 1342177280 : 1879048192;
            Gfx.rect(0.0F, 0.0F, (float)width, (float)height, Fx.alpha(dim, alpha));
         });
      }
   }

   public static void panelRim(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, float headerY) {
      styled(graphics, () -> {
         int line = hairline();
         Gfx.roundedOutline(x, y, w, h, radius, Gfx.pixel(), line);
         Gfx.rectWH(x, headerY, w, Gfx.pixel(), line);
      });
   }

   public static void pill(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius) {
      styled(graphics, () -> Gfx.roundedRect(x, y, w, h, radius, Theme.ACCENT));
   }

   public static void enabledBar(float x, float y, float h, float bar, float enabled) {
      if (enabled > 0.01F) {
         Gfx.roundedRect(x, y + h * 0.25F, bar, h * 0.5F, bar * 0.5F, Fx.alpha(Theme.ACCENT, enabled));
      }

   }

   public static void riskSegments(float x, float y, float w, float h, float filled) {
      float gap = Math.max(1.0F, h * 0.8F);
      float segment = (w - 4.0F * gap) / 5.0F;
      float r = h * 0.5F;
      int track = Theme.isLight() ? 436207616 : 536870911;

      for(int i = 0; i < 5; ++i) {
         float sx = x + (float)i * (segment + gap);
         float part = Math.max(0.0F, Math.min(1.0F, filled - (float)i));
         if (part < 1.0F) {
            Gfx.roundedRect(sx, y, segment, h, r, track);
         }

         if (part > 0.0F) {
            Gfx.roundedRect(sx, y, Math.max(h, segment * part), h, r, RISK_COLORS[i]);
         }
      }

   }

   public static void petal(float x, float y, float size, float angle, int color) {
      float c = (float)Math.cos((double)angle);
      float s = (float)Math.sin((double)angle);
      int segments = 10;
      float[] points = new float[segments * 2];

      for(int i = 0; i < segments; ++i) {
         double a = (Math.PI * 2D) * (double)i / (double)segments;
         float px = (float)Math.cos(a) * size;
         float py = (float)Math.sin(a) * size * 0.62F * (0.75F + 0.25F * (float)Math.cos(a));
         points[i * 2] = x + px * c - py * s;
         points[i * 2 + 1] = y + px * s + py * c;
      }

      Gfx.fillConvex(points, color);
      Gfx.circle(x - c * size * 0.35F, y - s * size * 0.35F, size * 0.3F, Fx.alpha(-1, (float)(color >>> 24) / 255.0F * 0.45F));
   }

   public static void crystal(float x, float y, float size, float angle, int color) {
      float w = Math.max(0.6F, size * 0.16F);

      for(int i = 0; i < 6; ++i) {
         double a = (double)angle + (double)i * Math.PI / (double)3.0F;
         float dx = (float)Math.cos(a);
         float dy = (float)Math.sin(a);
         Gfx.line(x, y, x + dx * size, y + dy * size, w, color);
         if (size > 2.5F) {
            float bx = x + dx * size * 0.55F;
            float by = y + dy * size * 0.55F;

            for(int side = -1; side <= 1; side += 2) {
               double b = a + (double)side * Math.PI / (double)3.0F;
               Gfx.line(bx, by, bx + (float)Math.cos(b) * size * 0.32F, by + (float)Math.sin(b) * size * 0.32F, w * 0.8F, color);
            }
         }
      }

   }
}
