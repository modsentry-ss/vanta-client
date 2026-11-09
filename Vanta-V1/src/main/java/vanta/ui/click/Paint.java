package vanta.ui.click;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vanta.render.Gfx;
import vanta.render.font.Font;
import vanta.render.state.Submit;
import vanta.render.state.TextState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2f;

final class Paint {
   static final byte MEDIUM = 0;
   static final byte SEMIBOLD = 1;
   static final Font NUNITO = Font.load("fonts/nunito", 63);
   private static final Font[] FACES;
   private static final float[] CAP_MID;
   static float fade;
   static GuiGraphicsExtractor g;
   private static final List<Queued> QUEUE;
   private static final List<TextState.Quad> QUADS;
   private static final Map<String, Float> WIDTHS;
   private static final Vector2f CORNER;
   private static int clipDepth;

   private Paint() {
   }

   static float width(String text, float size, byte style) {
      String key = style + text;
      Float cached = (Float)WIDTHS.get(key);
      if (cached == null) {
         Font face = FACES[style];
         float advance = 0.0F;
         int previous = -1;

         Font.Glyph glyph;
         for(int i = 0; i < text.length(); previous = glyph.unicode()) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            glyph = face.glyph(cp);
            advance += face.kerning(previous, glyph.unicode()) + glyph.advance();
         }

         if (WIDTHS.size() > 4096) {
            WIDTHS.clear();
         }

         cached = advance;
         WIDTHS.put(key, cached);
      }

      return cached * size;
   }

   static void text(String text, float x, float centerY, float size, int color, byte style) {
      if (text != null && !text.isEmpty()) {
         color = a(color, 1.0F);
         if (color >>> 24 >= 3) {
            QUEUE.add(new Queued(text, x, centerY - CAP_MID[style] * size, size, color, style));
         }
      }
   }

   static void textRight(String text, float right, float centerY, float size, int color, byte style) {
      text(text, right - width(text, size, style), centerY, size, color, style);
   }

   static void textCenter(String text, float cx, float centerY, float size, int color, byte style) {
      text(text, cx - width(text, size, style) / 2.0F, centerY, size, color, style);
   }

   static String fit(String text, float max, float size, byte style) {
      if (width(text, size, style) <= max) {
         return text;
      } else {
         String dots = "…";

         int end;
         for(end = text.length(); end > 0 && width(text.substring(0, end) + dots, size, style) > max; --end) {
         }

         String var10000 = text.substring(0, end);
         return var10000 + dots;
      }
   }

   static void flush() {
      Gfx.flush();
      if (!QUEUE.isEmpty()) {
         ScreenRectangle scissor = g.scissorStack.peek();

         for(byte style = 0; style < FACES.length; ++style) {
            Font face = FACES[style];

            for(Queued q : QUEUE) {
               if (q.style == style) {
                  float cursor = q.x;
                  int previous = -1;
                  String text = q.text;

                  Font.Glyph glyph;
                  for(int i = 0; i < text.length(); previous = glyph.unicode()) {
                     int cp = text.codePointAt(i);
                     i += Character.charCount(cp);
                     glyph = face.glyph(cp);
                     cursor += face.kerning(previous, glyph.unicode()) * q.size;
                     Font.Bounds plane = glyph.planeBounds();
                     if (plane != null && glyph.atlasBounds() != null) {
                        Font.Bounds uv = face.uv(glyph);
                        QUADS.add(new TextState.Quad(cursor + plane.left() * q.size, q.baseline + plane.top() * q.size, cursor + plane.right() * q.size, q.baseline + plane.bottom() * q.size, uv.left(), uv.top(), uv.right(), uv.bottom(), q.color));
                     }

                     cursor += glyph.advance() * q.size;
                  }
               }
            }

            if (!QUADS.isEmpty()) {
               TextState state = TextState.of(g, face.texture(), QUADS, scissor);
               if (state != null) {
                  Submit.submit(g, state);
               }

               QUADS.clear();
            }
         }

         QUEUE.clear();
      }
   }

   static void discard() {
      QUEUE.clear();

      while(clipDepth > 0) {
         --clipDepth;
         g.scissorStack.pop();
      }

   }

   static void clip(float x, float y, float w, float h) {
      flush();
      g.pose().transformPosition(x, y, CORNER);
      float ax = CORNER.x;
      float ay = CORNER.y;
      g.pose().transformPosition(x + w, y + h, CORNER);
      int x0 = (int)Math.floor((double)Math.min(ax, CORNER.x));
      int y0 = (int)Math.floor((double)Math.min(ay, CORNER.y));
      int x1 = (int)Math.ceil((double)Math.max(ax, CORNER.x));
      int y1 = (int)Math.ceil((double)Math.max(ay, CORNER.y));
      ScreenRectangle rect = new ScreenRectangle(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
      ScreenRectangle outer = g.scissorStack.peek();
      if (outer != null) {
         ScreenRectangle both = outer.intersection(rect);
         rect = both == null ? new ScreenRectangle(x0, y0, 0, 0) : both;
      }

      g.scissorStack.push(rect);
      ++clipDepth;
   }

   static void unclip() {
      flush();
      if (clipDepth > 0) {
         --clipDepth;
         g.scissorStack.pop();
      }

   }

   static int a(int argb, float alpha) {
      float k = Math.max(0.0F, Math.min(1.0F, alpha * fade));
      int al = Math.round((float)(argb >>> 24) * k);
      return al << 24 | argb & 16777215;
   }

   static int rgba(int rgb, float alpha) {
      return a(-16777216 | rgb, alpha);
   }

   static int mix(int x, int y, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int aa = (int)((float)(x >>> 24) + (float)((y >>> 24) - (x >>> 24)) * t);
      int r = (int)((float)(x >> 16 & 255) + (float)((y >> 16 & 255) - (x >> 16 & 255)) * t);
      int gg = (int)((float)(x >> 8 & 255) + (float)((y >> 8 & 255) - (x >> 8 & 255)) * t);
      int b = (int)((float)(x & 255) + (float)((y & 255) - (x & 255)) * t);
      return aa << 24 | r << 16 | gg << 8 | b;
   }

   static void chevron(float cx, float cy, float size, float angleDeg, float width, int color) {
      double r = Math.toRadians((double)angleDeg);
      float cos = (float)Math.cos(r);
      float sin = (float)Math.sin(r);
      float[] pts = new float[]{-size, -size * 0.5F, 0.0F, size * 0.5F, size, -size * 0.5F};
      float[] out = new float[6];

      for(int i = 0; i < 6; i += 2) {
         out[i] = cx + pts[i] * cos - pts[i + 1] * sin;
         out[i + 1] = cy + pts[i] * sin + pts[i + 1] * cos;
      }

      Gfx.polyline(out, width, a(color, 1.0F), false);
   }

   static void check(float cx, float cy, float s, float progress, int color) {
      if (!(progress <= 0.01F)) {
         float[] pts = new float[]{cx - s, cy, cx - s * 0.3F, cy + s * 0.7F, cx + s, cy - s * 0.7F};
         Gfx.polyline(pts, 1.3F, a(color, progress), false);
      }
   }

   static boolean in(double mx, double my, float x, float y, float w, float h) {
      return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
   }

   static {
      FACES = new Font[]{NUNITO, NUNITO};
      CAP_MID = new float[2];

      for(int i = 0; i < FACES.length; ++i) {
         Font.Bounds cap = FACES[i].glyph(72).planeBounds();
         CAP_MID[i] = cap == null ? -0.36F : (cap.top() + cap.bottom()) * 0.5F;
      }

      fade = 1.0F;
      QUEUE = new ArrayList();
      QUADS = new ArrayList();
      WIDTHS = new HashMap();
      CORNER = new Vector2f();
   }

   private static record Queued(String text, float x, float baseline, float size, int color, byte style) {
   }
}
