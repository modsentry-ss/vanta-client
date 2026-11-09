package vanta.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import vanta.render.font.Font;
import vanta.render.font.Fonts;
import vanta.render.state.Submit;
import vanta.render.state.TextState;
import vanta.util.Colors;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Text {
   private static final Font FONT;
   private static final Font.Bounds CAP_BOUNDS;
   private static final HashMap<String, Float> WIDTHS;

   public static float width(String text, float size) {
      Float cached = (Float)WIDTHS.get(text);
      if (cached == null) {
         float advance = 0.0F;
         int previous = -1;

         Font.Glyph glyph;
         for(int i = 0; i < text.length(); previous = glyph.unicode()) {
            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);
            glyph = FONT.glyph(codepoint);
            advance += FONT.kerning(previous, glyph.unicode()) + glyph.advance();
         }

         if (WIDTHS.size() > 4096) {
            WIDTHS.clear();
         }

         cached = advance;
         WIDTHS.put(text, cached);
      }

      return cached * size;
   }

   public static Ink ink(String text, float size) {
      float advance = 0.0F;
      int previous = -1;
      float left = Float.MAX_VALUE;
      float right = -Float.MAX_VALUE;

      Font.Glyph glyph;
      for(int i = 0; i < text.length(); previous = glyph.unicode()) {
         int codepoint = text.codePointAt(i);
         i += Character.charCount(codepoint);
         glyph = FONT.glyph(codepoint);
         advance += FONT.kerning(previous, glyph.unicode());
         Font.Bounds plane = glyph.planeBounds();
         if (plane != null) {
            left = Math.min(left, advance + plane.left());
            right = Math.max(right, advance + plane.right());
         }

         advance += glyph.advance();
      }

      return left > right ? new Ink(0.0F, 0.0F) : new Ink(left * size, right * size);
   }

   public static float centeredBaseline(String text, float size, float centerY) {
      return centerY - (CAP_BOUNDS.top() + CAP_BOUNDS.bottom()) * size * 0.5F;
   }

   public static void draw(GuiGraphicsExtractor graphics, String text, float x, float baseline, float size, int color) {
      draw(graphics, List.of(new Span(text, color)), x, baseline, size);
   }

   public static void drawCentered(GuiGraphicsExtractor graphics, String text, float x, float centerY, float size, int color) {
      draw(graphics, text, x, centeredBaseline(text, size, centerY), size, color);
   }

   public static void drawCentered(GuiGraphicsExtractor graphics, String text, float x, float centerY, float size, ColorAt color) {
      draw(graphics, List.of(new Span(text, -1)), x, centeredBaseline(text, size, centerY), size, color);
   }

   public static void drawCenteredX(GuiGraphicsExtractor graphics, String text, float centerX, float centerY, float size, int color) {
      drawCentered(graphics, text, centerX - width(text, size) / 2.0F, centerY, size, color);
   }

   public static String fit(String text, float width, float size) {
      if (width(text, size) <= width) {
         return text;
      } else {
         while(!text.isEmpty() && width(text + "...", size) > width) {
            text = text.substring(0, text.offsetByCodePoints(text.length(), -1));
         }

         return text + "...";
      }
   }

   public static List<String> wrap(String text, float width, float size, int maxLines) {
      List<String> lines = new ArrayList();
      StringBuilder line = new StringBuilder();

      for(String word : text.split(" ")) {
         String candidate = line.isEmpty() ? word : String.valueOf(line) + " " + word;
         if (!line.isEmpty() && !(width(candidate, size) <= width)) {
            lines.add(line.toString());
            line.setLength(0);
            line.append(word);
            if (lines.size() == maxLines - 1) {
               break;
            }
         } else {
            line.setLength(0);
            line.append(candidate);
         }
      }

      if (lines.size() == maxLines - 1) {
         String rest = text.substring(Math.min(text.length(), String.join(" ", lines).length() + 1));
         lines.add(fit(rest, width, size));
      } else if (!line.isEmpty()) {
         lines.add(line.toString());
      }

      return lines;
   }

   public static void drawFaded(GuiGraphicsExtractor graphics, String text, float x, float baseline, float size, int color, float transparentX, float opaqueX) {
      float fadeWidth = opaqueX - transparentX;
      if (fadeWidth == 0.0F) {
         fadeWidth = 0.001F;
      }

      draw(graphics, List.of(new Span(text, color)), x, baseline, size, (glyphX) -> Colors.withAlpha(color, Math.clamp((glyphX - transparentX) / fadeWidth, 0.0F, 1.0F)));
   }

   public static void draw(GuiGraphicsExtractor graphics, List<Span> spans, float x, float baseline, float size) {
      draw(graphics, spans, x, baseline, size, (ColorAt)null);
   }

   private static void draw(GuiGraphicsExtractor graphics, List<Span> spans, float x, float baseline, float size, ColorAt colorAt) {
      if (Float.isFinite(size) && !(size <= 0.0F)) {
         List<TextState.Quad> quads = new ArrayList();
         float cursor = x;
         int previous = -1;

         for(Span span : spans) {
            int color = Opacity.apply(span.color());
            String text = span.text();

            Font.Glyph glyph;
            for(int i = 0; i < text.length(); previous = glyph.unicode()) {
               int codepoint = text.codePointAt(i);
               i += Character.charCount(codepoint);
               glyph = FONT.glyph(codepoint);
               cursor += FONT.kerning(previous, glyph.unicode()) * size;
               Font.Bounds plane = glyph.planeBounds();
               if (plane != null && glyph.atlasBounds() != null && !Colors.transparent(color)) {
                  Font.Bounds uv = FONT.uv(glyph);
                  float left = cursor + plane.left() * size;
                  float right = cursor + plane.right() * size;
                  int leftColor = colorAt == null ? color : Opacity.apply(colorAt.at(left));
                  int rightColor = colorAt == null ? color : Opacity.apply(colorAt.at(right));
                  if (!Colors.transparent(leftColor) || !Colors.transparent(rightColor)) {
                     quads.add(new TextState.Quad(left, baseline + plane.top() * size, right, baseline + plane.bottom() * size, uv.left(), uv.top(), uv.right(), uv.bottom(), leftColor, rightColor));
                  }
               }

               cursor += glyph.advance() * size;
            }
         }

         if (!quads.isEmpty()) {
            Submit.submit(graphics, TextState.of(graphics, FONT.texture(), quads));
         }
      }

   }

   public static void drawShadowed(GuiGraphicsExtractor graphics, String text, float x, float baseline, float size, int color, int shadow, float offsetX, float offsetY) {
      if (Float.isFinite(size) && !(size <= 0.0F)) {
         List<TextState.Quad> quads = new ArrayList(text.length() * 2);
         glyphs(quads, text, x + offsetX, baseline + offsetY, size, Opacity.apply(shadow));
         glyphs(quads, text, x, baseline, size, Opacity.apply(color));
         if (!quads.isEmpty()) {
            Submit.submit(graphics, TextState.of(graphics, FONT.texture(), quads));
         }

      }
   }

   private static void glyphs(List<TextState.Quad> quads, String text, float x, float baseline, float size, int color) {
      if (!Colors.transparent(color)) {
         float cursor = x;
         int previous = -1;

         Font.Glyph glyph;
         for(int i = 0; i < text.length(); previous = glyph.unicode()) {
            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);
            glyph = FONT.glyph(codepoint);
            cursor += FONT.kerning(previous, glyph.unicode()) * size;
            Font.Bounds plane = glyph.planeBounds();
            if (plane != null && glyph.atlasBounds() != null) {
               Font.Bounds uv = FONT.uv(glyph);
               quads.add(new TextState.Quad(cursor + plane.left() * size, baseline + plane.top() * size, cursor + plane.right() * size, baseline + plane.bottom() * size, uv.left(), uv.top(), uv.right(), uv.bottom(), color, color));
            }

            cursor += glyph.advance() * size;
         }

      }
   }

   private Text() {
   }

   static {
      FONT = Fonts.INTER_SEMIBOLD;
      CAP_BOUNDS = FONT.glyph(72).planeBounds();
      WIDTHS = new HashMap();
   }

   public static record Ink(float left, float right) {
      public float width() {
         return this.right - this.left;
      }
   }

   public static record Span(String text, int color) {
   }

   @FunctionalInterface
   public interface ColorAt {
      int at(float var1);
   }
}
