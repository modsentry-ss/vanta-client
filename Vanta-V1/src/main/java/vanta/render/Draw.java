package vanta.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import vanta.render.font.Font;
import vanta.render.font.Fonts;
import vanta.render.glass.GlassRenderer;
import vanta.render.state.FadedTextureState;
import vanta.render.state.Pipelines;
import vanta.render.state.RectState;
import vanta.render.state.StairsState;
import vanta.render.state.Submit;
import vanta.render.state.TextState;
import vanta.ui.Chrome;
import vanta.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class Draw {
   public static final int TOP_LEFT = 1;
   public static final int TOP_RIGHT = 2;
   public static final int BOTTOM_RIGHT = 4;
   public static final int BOTTOM_LEFT = 8;
   public static final int ALL_CORNERS = 15;
   public static final int TILED = 32;

   public static void rect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
      rect(graphics, x, y, width, height, radius, 15, color, Pipelines.RECT);
   }

   public static void rect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int corners, int color) {
      rect(graphics, x, y, width, height, radius, corners, color, Pipelines.RECT);
   }

   public static void rect(GuiGraphicsExtractor graphics, float x, float y, float width, int height, int radius, int color) {
      if (!(width <= 0.0F) && height > 0) {
         graphics.pose().pushMatrix();

         try {
            int vertexWidth = (int)Math.ceil((double)width);
            graphics.pose().translate(x, y).scale(width / (float)vertexWidth, 1.0F);
            rect(graphics, 0, 0, vertexWidth, height, radius, color);
         } finally {
            graphics.pose().popMatrix();
         }
      }

   }

   public static void stairs(GuiGraphicsExtractor graphics, StairsState.Row row, int color) {
      color = Opacity.apply(color);
      if (row.width() > 0 && row.height() > 0 && !Colors.transparent(color)) {
         Submit.submit(graphics, new StairsState(new Matrix3x2f(graphics.pose()), row, color));
      }

   }

   public static void gradient(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int corners, int top, int bottom) {
      rect(graphics, x, y, width, height, radius, corners, top, bottom, Pipelines.RECT);
   }

   public static void bordered(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int fill, int border) {
      int r = Math.max(0, Math.round(radius));
      int h = Math.max(1, Math.round(height));
      rect(graphics, x - 1.0F, y - 1.0F, width + 2.0F, h + 2, r + 1, border);
      rect(graphics, x, y, width, h, r, fill);
   }

   public static void hueBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius) {
      rect(graphics, x, y, width, height, radius, 15, -1, Pipelines.HUE_BAR);
   }

   public static void saturationValue(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int hueColor) {
      rect(graphics, x, y, width, height, radius, 15, Colors.opaque(hueColor), Pipelines.SATURATION_VALUE);
   }

   public static void icon(GuiGraphicsExtractor graphics, int codepoint, float x, float y, float size, int color) {
      color = Opacity.apply(color);
      if (!(size <= 0.0F) && Float.isFinite(size) && !Colors.transparent(color)) {
         Font font = Fonts.MATERIAL_ICONS;
         Font.Glyph glyph = font.glyph(codepoint);
         Font.Bounds plane = glyph.planeBounds();
         if (plane != null && glyph.atlasBounds() != null) {
            float centerX = x + size * 0.5F;
            float centerY = y + size * 0.5F;
            float halfWidth = (plane.right() - plane.left()) * size * 0.5F;
            float halfHeight = (plane.bottom() - plane.top()) * size * 0.5F;
            Font.Bounds uv = font.uv(glyph);
            TextState.Quad quad = new TextState.Quad(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight, uv.left(), uv.top(), uv.right(), uv.bottom(), color);
            Submit.submit(graphics, TextState.of(graphics, font.texture(), List.of(quad)));
         }
      }

   }

   public static void fadedTexture(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float width, float height, float u0, float fade) {
      int color = Opacity.apply(-1);
      if (!(width <= 0.0F) && !(height <= 0.0F) && !Colors.transparent(color)) {
         TextureSetup setup = TextState.setup(Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView());
         Matrix3x2f pose = (new Matrix3x2f(graphics.pose())).translate(x, y).scale(width, height);
         Submit.submit(graphics, new FadedTextureState(pose, setup, u0, fade, color, Scissor.current()));
      }

   }

   public static void logo(GuiGraphicsExtractor graphics, float x, float y, float size, int starColor) {
      Font font = atlas(size, Fonts.LOGO_SMALL, Fonts.LOGO);
      List<TextState.Quad> quads = layers(font, new int[]{2, 1, 3}, new int[]{-6052957, starColor, -1381654}, x, y, size);
      if (!quads.isEmpty()) {
         Submit.submit(graphics, TextState.of(graphics, font.texture(), quads));
      }

   }

   public static void icon(GuiGraphicsExtractor graphics, UiIcon icon, float x, float y, float size, int back, int hero, int front, int backdrop) {
      if (icon == UiIcon.MACE_SPEAR) {
         maceSpear(graphics, x, y, size, back, hero, front);
      } else if (icon == UiIcon.SCRIPTS) {
         scripts(graphics, x, y, size, back, hero, front);
      } else if (icon == UiIcon.PVP_18) {
         pvp18(graphics, x, y, size, hero, front);
      } else {
         Font font = atlas(size, Fonts.UI_ICONS_SMALL, Fonts.UI_ICONS);
         List<TextState.Quad> quads = iconLayers(font, icon, x, y, size, back, hero, front);
         if (!quads.isEmpty()) {
            Submit.submit(graphics, TextState.icon(graphics, font.texture(), quads, backdrop));
         }

      }
   }

   public static void icon(GuiGraphicsExtractor graphics, UiIcon icon, float x, float y, float size, int back, int hero, int front) {
      if (icon == UiIcon.MACE_SPEAR) {
         maceSpear(graphics, x, y, size, back, hero, front);
      } else if (icon == UiIcon.SCRIPTS) {
         scripts(graphics, x, y, size, back, hero, front);
      } else if (icon == UiIcon.PVP_18) {
         pvp18(graphics, x, y, size, hero, front);
      } else {
         Font font = atlas(size, Fonts.UI_ICONS_SMALL, Fonts.UI_ICONS);
         List<TextState.Quad> quads = iconLayers(font, icon, x, y, size, back, hero, front);
         if (!quads.isEmpty()) {
            Submit.submit(graphics, TextState.of(graphics, font.texture(), quads));
         }

      }
   }

   private static void maceSpear(GuiGraphicsExtractor graphics, float x, float y, float size, int back, int hero, int front) {
      if (size > 0.0F && Float.isFinite(size)) {
         Chrome.styled(graphics, () -> {
            float bx = x + 0.62F * size;
            float by = y + 0.38F * size;
            float tx = x + 0.9F * size;
            float ty = y + 0.1F * size;
            Gfx.line(x + 0.12F * size, y + 0.88F * size, bx, by, 0.085F * size, back);
            float dx = tx - bx;
            float dy = ty - by;
            float length = (float)Math.sqrt((double)(dx * dx + dy * dy));
            float nx = -dy / length;
            float ny = dx / length;
            float mx = bx + dx * 0.36F;
            float my = by + dy * 0.36F;
            float half = 0.095F * size;
            Gfx.fillConvex(new float[]{bx, by, mx + nx * half, my + ny * half, tx, ty, mx - nx * half, my - ny * half}, front);
            Gfx.line(bx + nx * 0.08F * size, by + ny * 0.08F * size, bx - nx * 0.08F * size, by - ny * 0.08F * size, 0.06F * size, front);
            float cx = x + 0.33F * size;
            float cy = y + 0.33F * size;
            float r = 0.15F * size;
            Gfx.line(x + 0.84F * size, y + 0.84F * size, cx, cy, 0.1F * size, back);
            Gfx.circle(x + 0.86F * size, y + 0.86F * size, 0.06F * size, back);

            for(int angle : new int[]{135, 180, 225, 270, 315}) {
               double a = Math.toRadians((double)angle);
               double side = Math.toRadians((double)24.0F);
               float ox = (float)Math.cos(a);
               float oy = (float)Math.sin(a);
               Gfx.fillConvex(new float[]{cx + (float)Math.cos(a - side) * r * 0.9F, cy + (float)Math.sin(a - side) * r * 0.9F, cx + ox * r * 1.62F, cy + oy * r * 1.62F, cx + (float)Math.cos(a + side) * r * 0.9F, cy + (float)Math.sin(a + side) * r * 0.9F}, hero);
            }

            Gfx.circle(cx, cy, r, hero);
         });
      }
   }

   private static void scripts(GuiGraphicsExtractor graphics, float x, float y, float size, int back, int hero, int front) {
      if (size > 0.0F && Float.isFinite(size)) {
         Chrome.styled(graphics, () -> {
            float w = 0.11F * size;
            Gfx.polyline(new float[]{x + 0.34F * size, y + 0.24F * size, x + 0.1F * size, y + 0.5F * size, x + 0.34F * size, y + 0.76F * size}, w, hero, false);
            Gfx.polyline(new float[]{x + 0.66F * size, y + 0.24F * size, x + 0.9F * size, y + 0.5F * size, x + 0.66F * size, y + 0.76F * size}, w, hero, false);
            Gfx.line(x + 0.58F * size, y + 0.14F * size, x + 0.42F * size, y + 0.86F * size, w * 0.9F, front);
         });
      }
   }

   private static void pvp18(GuiGraphicsExtractor graphics, float x, float y, float size, int hero, int front) {
      if (size > 0.0F && Float.isFinite(size)) {
         float textSize = Math.min(0.62F * size, 0.96F * size / Math.max(0.01F, Text.width("1.8", 1.0F)));
         Text.drawCenteredX(graphics, "1.8", x + 0.5F * size, y + 0.44F * size, textSize, hero);
         Chrome.styled(graphics, () -> Gfx.line(x + 0.22F * size, y + 0.84F * size, x + 0.78F * size, y + 0.84F * size, 0.09F * size, front));
      }
   }

   private static List<TextState.Quad> iconLayers(Font font, UiIcon icon, float x, float y, float size, int back, int hero, int front) {
      int first = icon.ordinal() * 3 + 1;
      return layers(font, new int[]{first, first + 1, first + 2}, new int[]{back, hero, front}, x, y, size);
   }

   public static void glass(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int variant) {
      glass(graphics, x, y, width, height, radius, variant, 0, -0.33F, -0.94F, 0.0F);
   }

   public static void glass(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int variant, int tint, float lightX, float lightY, float energy) {
      if (!(width <= 0.0F) && !(height <= 0.0F)) {
         Vector2f start = graphics.pose().transformPosition(x, y, new Vector2f());
         Vector2f end = graphics.pose().transformPosition(x + width, y + height, new Vector2f());
         float stretch = Math.abs(end.y - start.y) / height;
         float opacity = (float)Colors.alpha(Opacity.apply(-1)) / 255.0F;
         GlassRenderer.submit(new GlassRenderer.Pane(Math.min(start.x, end.x), Math.min(start.y, end.y), Math.abs(end.x - start.x), Math.abs(end.y - start.y), radius * stretch, variant, tint, lightX, lightY, energy, opacity, Scissor.current()));
      }

   }

   private static Font atlas(float size, Font small, Font large) {
      return size * (float)Minecraft.getInstance().getWindow().getGuiScale() <= 40.0F ? small : large;
   }

   private static List<TextState.Quad> layers(Font font, int[] codepoints, int[] colors, float x, float y, float size) {
      List<TextState.Quad> quads = new ArrayList();
      if (!(size <= 0.0F) && Float.isFinite(size)) {
         for(int i = 0; i < codepoints.length; ++i) {
            layer(quads, font, codepoints[i], x, y, size, colors[i]);
         }

         return quads;
      } else {
         return quads;
      }
   }

   private static void layer(List<TextState.Quad> quads, Font font, int codepoint, float x, float y, float size, int color) {
      color = Opacity.apply(color);
      if (!Colors.transparent(color)) {
         Font.Glyph glyph = font.glyph(codepoint);
         Font.Bounds plane = glyph.planeBounds();
         Font.Bounds uv = font.uv(glyph);
         quads.add(new TextState.Quad(x + plane.left() * size, y + plane.top() * size, x + plane.right() * size, y + plane.bottom() * size, uv.left(), uv.top(), uv.right(), uv.bottom(), color));
      }

   }

   private static void rect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int shape, int color, RenderPipeline pipeline) {
      rect(graphics, x, y, width, height, radius, shape, color, color, pipeline);
   }

   private static void rect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int shape, int color, int bottomColor, RenderPipeline pipeline) {
      color = Opacity.apply(color);
      bottomColor = Opacity.apply(bottomColor);
      if (width > 0 && height > 0 && (!Colors.transparent(color) || !Colors.transparent(bottomColor))) {
         if (width > 32767 || height > 32767) {
            throw new IllegalArgumentException("Rectangle dimensions exceed the vertex format's range");
         }

         int limit = (shape & 16) != 0 ? Math.max(width, height) : Math.min(width, height) / 2;
         Submit.submit(graphics, new RectState(new Matrix3x2f(graphics.pose()), x, y, width, height, Math.clamp((long)radius, 0, limit), shape, color, bottomColor, pipeline));
      }

   }

   private Draw() {
   }
}
