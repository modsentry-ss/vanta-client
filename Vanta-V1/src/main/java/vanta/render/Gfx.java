package vanta.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

public final class Gfx {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final int STRIDE = 5;
   private static @Nullable GuiGraphicsExtractor graphics;
   private static final Deque<GuiGraphicsExtractor> outer = new ArrayDeque();
   private static float[] data = new float[20480];
   private static int floats;
   private static @Nullable GfxTexture batchTexture;
   private static @Nullable ScreenRectangle batchScissor;
   private static float minX;
   private static float minY;
   private static float maxX;
   private static float maxY;
   private static int discardDepth;
   private static int legacyScissorDepth;
   private static float legacyScale = 1.0F;
   private static float alphaScale = 1.0F;
   public static boolean screenPass;
   public static long emitted;
   private static final float[][][] CORNER_TRIG = new float[4][161][];
   private static final float[][] CIRCLE_TRIG = new float[361][];

   public static float alphaScale() {
      return alphaScale;
   }

   public static void setAlphaScale(float scale) {
      alphaScale = Math.max(0.0F, Math.min(1.0F, scale));
   }

   private Gfx() {
   }

   public static void begin(GuiGraphicsExtractor g) {
      if (graphics != null) {
         flush();
         outer.push(graphics);
      }

      graphics = g;
   }

   public static void end() {
      flush();

      while(legacyScissorDepth > 0) {
         --legacyScissorDepth;
         graphics.disableScissor();
      }

      graphics = outer.isEmpty() ? null : (GuiGraphicsExtractor)outer.pop();
   }

   public static boolean active() {
      return graphics != null;
   }

   public static GuiGraphicsExtractor graphics() {
      if (graphics == null) {
         throw new IllegalStateException("Gfx used outside of a GUI frame");
      } else {
         return graphics;
      }
   }

   public static void newFrame() {
   }

   public static Matrix3x2fStack pose() {
      return graphics().pose();
   }

   public static void push() {
      pose().pushMatrix();
   }

   public static void pop() {
      pose().popMatrix();
   }

   public static void translate(float x, float y) {
      pose().translate(x, y);
   }

   public static void scale(float x, float y) {
      pose().scale(x, y);
   }

   public static void scale(float s) {
      pose().scale(s, s);
   }

   public static void rotate(float degrees) {
      pose().rotate((float)Math.toRadians((double)degrees));
   }

   public static void pushScissor(float x, float y, float width, float height) {
      flush();
      graphics().enableScissor((int)Math.floor((double)x), (int)Math.floor((double)y), (int)Math.ceil((double)(x + Math.max(0.0F, width))), (int)Math.ceil((double)(y + Math.max(0.0F, height))));
   }

   public static void popScissor() {
      flush();
      graphics().disableScissor();
   }

   public static void legacyScissor(double x, double y, double width, double height) {
      releaseLegacyScissor();
      flush();
      double k = (double)legacyScale;
      int x0 = (int)Math.floor(x * k);
      int y0 = (int)Math.floor(y * k);
      int x1 = (int)Math.ceil((x + Math.max((double)0.0F, width)) * k);
      int y1 = (int)Math.ceil((y + Math.max((double)0.0F, height)) * k);
      graphics().scissorStack.push(new ScreenRectangle(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0)));
      ++legacyScissorDepth;
   }

   public static void setLegacyScale(float scale) {
      legacyScale = scale;
   }

   public static void releaseLegacyScissor() {
      if (legacyScissorDepth > 0) {
         --legacyScissorDepth;
         popScissor();
      }

   }

   public static void beginDiscard() {
      ++discardDepth;
   }

   public static void endDiscard() {
      if (discardDepth > 0) {
         --discardDepth;
      }

   }

   public static void item(ItemStack stack, int x, int y, boolean decorations) {
      if (discardDepth <= 0 && stack != null && !stack.isEmpty()) {
         flush();
         graphics().item(stack, x, y);
         if (decorations) {
            graphics().itemDecorations(mc.font, stack, x, y);
         }

      }
   }

   public static void entity(LivingEntity entity, float x, float y, int size, float lookX, float lookY) {
      if (discardDepth <= 0 && entity != null) {
         flush();
         Matrix3x2f m = graphics().pose();
         float scale = (float)Math.sqrt((double)Math.abs(m.m00() * m.m11() - m.m01() * m.m10()));
         float sx = m.m00() * x + m.m10() * y + m.m20();
         float sy = m.m01() * x + m.m11() * y + m.m21();
         float s = (float)size * scale;
         int x0 = Math.round(sx - s);
         int x1 = Math.round(sx + s);
         int y0 = Math.round(sy - 2.1F * s);
         int y1 = Math.round(sy + 0.3F * s);
         float centerX = (float)(x0 + x1) / 2.0F;
         float centerY = (float)(y0 + y1) / 2.0F;
         InventoryScreen.extractEntityInInventoryFollowsMouse(graphics(), x0, y0, x1, y1, Math.round(s), 0.0625F, centerX - lookX * scale, centerY - lookY * scale, entity);
      }
   }

   public static void vanillaText(String text, float x, float y, int color, boolean shadow) {
      if (discardDepth <= 0 && text != null && !text.isEmpty()) {
         flush();
         int ix = (int)Math.floor((double)x);
         int iy = (int)Math.floor((double)y);
         float fx = x - (float)ix;
         float fy = y - (float)iy;
         boolean shifted = fx != 0.0F || fy != 0.0F;
         if (shifted) {
            push();
            translate(fx, fy);
         }

         graphics().text(mc.font, text, ix, iy, legacyTextColor(color), shadow);
         if (shifted) {
            pop();
         }

      }
   }

   public static int legacyTextColor(int color) {
      return (color & -67108864) == 0 ? color | -16777216 : color;
   }

   public static int vanillaWidth(String text) {
      return mc.font.width(text);
   }

   public static int drawString(String text, float x, float y, int color, boolean shadow) {
      vanillaText(text, x, y, color, shadow);
      return text == null ? (int)x : (int)x + mc.font.width(text) + (shadow ? 1 : 0);
   }

   public static int drawString(String text, float x, float y, int color) {
      return drawString(text, x, y, color, false);
   }

   public static int drawStringWithShadow(String text, float x, float y, int color) {
      return drawString(text, x, y, color, true);
   }

   public static void rect(float x1, float y1, float x2, float y2, int color) {
      quad4(x1, y1, x2, y2, color, color, color, color);
   }

   public static void rectWH(float x, float y, float width, float height, int color) {
      quad4(x, y, x + width, y + height, color, color, color, color);
   }

   public static void gradientV(float x1, float y1, float x2, float y2, int top, int bottom) {
      quad4(x1, y1, x2, y2, top, top, bottom, bottom);
   }

   public static void gradientH(float x1, float y1, float x2, float y2, int left, int right) {
      quad4(x1, y1, x2, y2, left, right, right, left);
   }

   public static void quad4(float x1, float y1, float x2, float y2, int tl, int tr, int br, int bl) {
      if (x1 > x2) {
         float t = x1;
         x1 = x2;
         x2 = t;
         int c = tl;
         tl = tr;
         tr = c;
         c = bl;
         bl = br;
         br = c;
      }

      if (y1 > y2) {
         float t = y1;
         y1 = y2;
         y2 = t;
         int c = tl;
         tl = bl;
         bl = c;
         c = tr;
         tr = br;
         br = c;
      }

      if (x1 != x2 && y1 != y2 && (tl | tr | br | bl) >>> 24 != 0) {
         shapes();
         vquad(x1, y1, tl, x1, y2, bl, x2, y2, br, x2, y1, tr);
      }
   }

   public static void rectOutline(float x, float y, float width, float height, float lineWidth, int color) {
      rectWH(x, y, width, lineWidth, color);
      rectWH(x, y + height - lineWidth, width, lineWidth, color);
      rectWH(x, y + lineWidth, lineWidth, height - lineWidth * 2.0F, color);
      rectWH(x + width - lineWidth, y + lineWidth, lineWidth, height - lineWidth * 2.0F, color);
   }

   public static void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
      fillConvex(new float[]{x1, y1, x2, y2, x3, y3}, color);
   }

   public static void triangleColors(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
      if ((c1 | c2 | c3) >>> 24 != 0) {
         shapes();
         vtri(x1, y1, c1, x2, y2, c2, x3, y3, c3);
      }
   }

   public static void quadColors(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3, float x4, float y4, int c4) {
      if ((c1 | c2 | c3 | c4) >>> 24 != 0) {
         shapes();
         vquad(x1, y1, c1, x2, y2, c2, x3, y3, c3, x4, y4, c4);
      }
   }

   public static void line(float x1, float y1, float x2, float y2, float width, int color) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(len < 1.0E-4F) && color >>> 24 != 0) {
         shapes();
         float px = pixel();
         float half = Math.max(width, px) * 0.5F;
         float nx = -dy / len;
         float ny = dx / len;
         int clear = color & 16777215;
         float inner = Math.max(half - px * 0.5F, 0.0F);
         float outerR = half + px * 0.5F;
         vquad(x1 + nx * inner, y1 + ny * inner, color, x2 + nx * inner, y2 + ny * inner, color, x2 - nx * inner, y2 - ny * inner, color, x1 - nx * inner, y1 - ny * inner, color);
         vquad(x1 + nx * outerR, y1 + ny * outerR, clear, x2 + nx * outerR, y2 + ny * outerR, clear, x2 + nx * inner, y2 + ny * inner, color, x1 + nx * inner, y1 + ny * inner, color);
         vquad(x1 - nx * inner, y1 - ny * inner, color, x2 - nx * inner, y2 - ny * inner, color, x2 - nx * outerR, y2 - ny * outerR, clear, x1 - nx * outerR, y1 - ny * outerR, clear);
      }
   }

   public static void polyline(float[] points, float width, int color, boolean closed) {
      int n = points.length / 2;

      for(int i = 0; i + 1 < n; ++i) {
         line(points[i * 2], points[i * 2 + 1], points[i * 2 + 2], points[i * 2 + 3], width, color);
      }

      if (closed && n > 2) {
         line(points[n * 2 - 2], points[n * 2 - 1], points[0], points[1], width, color);
      }

   }

   public static void fillConvex(float[] points, int color) {
      int n = points.length / 2;
      if (n >= 3 && color >>> 24 != 0) {
         float[] dirs = new float[n * 2];
         float area = 0.0F;

         for(int i = 0; i < n; ++i) {
            int j = (i + 1) % n;
            area += points[i * 2] * points[j * 2 + 1] - points[j * 2] * points[i * 2 + 1];
         }

         float sign = area >= 0.0F ? 1.0F : -1.0F;

         for(int i = 0; i < n; ++i) {
            int prev = (i + n - 1) % n;
            int next = (i + 1) % n;
            float[] a = edgeNormal(points, prev, i, sign);
            float[] b = edgeNormal(points, i, next, sign);
            float mx = a[0] + b[0];
            float my = a[1] + b[1];
            float dot = mx * a[0] + my * a[1];
            if (dot < 0.001F) {
               mx = a[0];
               my = a[1];
               dot = 1.0F;
            }

            float miter = Math.min(1.0F / dot, 4.0F);
            dirs[i * 2] = mx * miter;
            dirs[i * 2 + 1] = my * miter;
         }

         int[] colors = new int[n];
         Arrays.fill(colors, color);
         fillContour(points, dirs, colors);
      }
   }

   private static float[] edgeNormal(float[] p, int a, int b, float sign) {
      float dx = p[b * 2] - p[a * 2];
      float dy = p[b * 2 + 1] - p[a * 2 + 1];
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      return len < 1.0E-6F ? new float[]{0.0F, 0.0F} : new float[]{dy / len * sign, -dx / len * sign};
   }

   public static void circle(float cx, float cy, float radius, int color) {
      if (!(radius <= 0.0F) && color >>> 24 != 0) {
         int segments = segments(radius, 360.0F);
         float[] pts = new float[segments * 2];
         float[] dirs = new float[segments * 2];
         int[] colors = new int[segments];
         float[] trig = circleTrig(segments);

         for(int i = 0; i < segments; ++i) {
            float c = trig[i * 2];
            float s = trig[i * 2 + 1];
            pts[i * 2] = cx + c * radius;
            pts[i * 2 + 1] = cy + s * radius;
            dirs[i * 2] = c;
            dirs[i * 2 + 1] = s;
            colors[i] = color;
         }

         fillContour(pts, dirs, colors);
      }
   }

   public static void circleOutline(float cx, float cy, float radius, float lineWidth, int color) {
      arc(cx, cy, radius, lineWidth, 0.0F, 360.0F, color);
   }

   public static void arc(float cx, float cy, float radius, float lineWidth, float startDeg, float endDeg, int color) {
      if (!(radius <= 0.0F) && color >>> 24 != 0 && endDeg != startDeg) {
         float sweep = endDeg - startDeg;
         int segments = segments(radius, Math.abs(sweep));
         boolean full = Math.abs(sweep) >= 360.0F;
         int count = full ? segments : segments + 1;
         float[] pts = new float[count * 2];
         float[] dirs = new float[count * 2];

         for(int i = 0; i < count; ++i) {
            double a = Math.toRadians((double)(startDeg + sweep * (float)i / (float)segments));
            float c = (float)Math.cos(a);
            float s = (float)Math.sin(a);
            pts[i * 2] = cx + c * radius;
            pts[i * 2 + 1] = cy + s * radius;
            dirs[i * 2] = c;
            dirs[i * 2 + 1] = s;
         }

         strokeContour(pts, dirs, lineWidth, color, full);
      }
   }

   private static int segments(float radius, float degrees) {
      float physical = radius * scaleFactor();
      double step = (double)2.0F * Math.acos(Math.max((double)0.0F, (double)1.0F - 0.2 / (double)Math.max(0.2F, physical)));
      int full = Math.max(12, Math.min(160, (int)Math.ceil((Math.PI * 2D) / step)));
      return Math.max(2, (int)Math.ceil((double)((float)full * degrees / 360.0F)));
   }

   public static void roundedRect(float x, float y, float width, float height, float radius, int color) {
      roundedRect(x, y, width, height, radius, color, true, true, true, true);
   }

   public static void roundedRect(float x, float y, float width, float height, float radius, int color, boolean tl, boolean tr, boolean br, boolean bl) {
      roundedGradient(x, y, width, height, radius, color, color, color, color, tl, tr, br, bl);
   }

   public static void roundedGradient(float x, float y, float width, float height, float radius, int cTL, int cTR, int cBR, int cBL, boolean tl, boolean tr, boolean br, boolean bl) {
      if (!(width <= 0.0F) && !(height <= 0.0F) && (cTL | cTR | cBR | cBL) >>> 24 != 0) {
         radius = Math.max(0.0F, Math.min(radius, Math.min(width, height) / 2.0F));
         if (radius <= 0.0F) {
            quad4(x, y, x + width, y + height, cTL, cTR, cBR, cBL);
         } else {
            float[][] contour = roundedContour(x, y, width, height, radius, tl, tr, br, bl);
            float[] pts = contour[0];
            int n = pts.length / 2;
            int[] colors = new int[n];

            for(int i = 0; i < n; ++i) {
               float u = (pts[i * 2] - x) / width;
               float v = (pts[i * 2 + 1] - y) / height;
               colors[i] = bilinear(cTL, cTR, cBR, cBL, u, v);
            }

            fillContour(pts, contour[1], colors);
         }
      }
   }

   public static void roundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
      roundedOutline(x, y, width, height, radius, lineWidth, color, true, true, true, true);
   }

   public static void roundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color, boolean tl, boolean tr, boolean br, boolean bl) {
      if (!(width <= 0.0F) && !(height <= 0.0F) && color >>> 24 != 0) {
         radius = Math.max(0.0F, Math.min(radius, Math.min(width, height) / 2.0F));
         float[][] contour = roundedContour(x, y, width, height, radius, tl, tr, br, bl);
         strokeContour(contour[0], contour[1], lineWidth, color, true);
      }
   }

   public static void shadow(float x, float y, float width, float height, float radius, float softness, int color) {
      if (!(width <= 0.0F) && !(height <= 0.0F) && !(softness <= 0.0F) && color >>> 24 != 0) {
         float r = Math.max(0.0F, Math.min(radius, Math.min(width, height) / 2.0F));
         int level = Math.round(r / (r + softness) * 63.0F);
         effects();
         float u0 = Gfx.FxAtlas.tileU(level, 0.0F);
         float u1 = Gfx.FxAtlas.tileU(level, 1.0F);
         float v0 = Gfx.FxAtlas.tileV(level, 0.0F);
         float v1 = Gfx.FxAtlas.tileV(level, 1.0F);
         float xo0 = x - softness;
         float xi0 = x + r;
         float xi1 = x + width - r;
         float xo1 = x + width + softness;
         float yo0 = y - softness;
         float yi0 = y + r;
         float yi1 = y + height - r;
         float yo1 = y + height + softness;
         uvRect(xo0, yo0, xi0, yi0, u0, v0, u1, v1, color);
         uvRect(xi1, yo0, xo1, yi0, u1, v0, u0, v1, color);
         uvRect(xi1, yi1, xo1, yo1, u1, v1, u0, v0, color);
         uvRect(xo0, yi1, xi0, yo1, u0, v1, u1, v0, color);
         uvRect(xi0, yo0, xi1, yi0, u1, v0, u1, v1, color);
         uvRect(xi1, yi0, xo1, yi1, u1, v1, u0, v1, color);
         uvRect(xi0, yi1, xi1, yo1, u1, v1, u1, v0, color);
         uvRect(xo0, yi0, xi0, yi1, u0, v1, u1, v1, color);
         uvRect(xi0, yi0, xi1, yi1, u1, v1, u1, v1, color);
      }
   }

   public static void glow(float cx, float cy, float radius, int color) {
      if (!(radius <= 0.0F) && color >>> 24 != 0) {
         effects();
         uvRect(cx - radius, cy - radius, cx + radius, cy + radius, 0.50097656F, 0.001953125F, 0.74902344F, 0.49804688F, color);
      }
   }

   private static void uvRect(float x0, float y0, float x1, float y1, float uLeft, float vTop, float uRight, float vBottom, int color) {
      if (!(x1 <= x0) && !(y1 <= y0)) {
         vquadUv(x0, y0, uLeft, vTop, x0, y1, uLeft, vBottom, x1, y1, uRight, vBottom, x1, y0, uRight, vTop, color);
      }
   }

   private static float falloff(float t) {
      float k = 1.0F - t;
      return k * k * (3.0F - 2.0F * k) * k;
   }

   private static float[][] roundedContour(float x, float y, float w, float h, float r, boolean tl, boolean tr, boolean br, boolean bl) {
      int seg = r > 0.0F ? segments(r, 90.0F) : 0;
      float[] pts = new float[(seg + 1) * 8];
      float[] dirs = new float[(seg + 1) * 8];
      int[] idx = new int[]{0};
      corner(pts, dirs, idx, x + r, y + r, r, 180.0F, tl, x, y, -1.0F, -1.0F, seg);
      corner(pts, dirs, idx, x + w - r, y + r, r, 270.0F, tr, x + w, y, 1.0F, -1.0F, seg);
      corner(pts, dirs, idx, x + w - r, y + h - r, r, 0.0F, br, x + w, y + h, 1.0F, 1.0F, seg);
      corner(pts, dirs, idx, x + r, y + h - r, r, 90.0F, bl, x, y + h, -1.0F, 1.0F, seg);
      int n = idx[0];
      return new float[][]{Arrays.copyOf(pts, n * 2), Arrays.copyOf(dirs, n * 2)};
   }

   private static void corner(float[] pts, float[] dirs, int[] idx, float cx, float cy, float r, float start, boolean round, float sx, float sy, float dx, float dy, int seg) {
      if (round && !(r <= 0.0F) && seg != 0) {
         float[] trig = cornerTrig(seg, start);

         for(int s = 0; s <= seg; ++s) {
            float c = trig[s * 2];
            float sn = trig[s * 2 + 1];
            int var20 = idx[0];
            int var19 = idx[0];
            idx[0] = var20 + 1;
            int i = var19;
            pts[i * 2] = cx + c * r;
            pts[i * 2 + 1] = cy + sn * r;
            dirs[i * 2] = c;
            dirs[i * 2 + 1] = sn;
         }

      } else {
         int var10003 = idx[0];
         int var10000 = idx[0];
         idx[0] = var10003 + 1;
         int i = var10000;
         pts[i * 2] = sx;
         pts[i * 2 + 1] = sy;
         dirs[i * 2] = dx;
         dirs[i * 2 + 1] = dy;
      }
   }

   private static float[] cornerTrig(int seg, float start) {
      int quadrant = Math.floorMod((int)(start / 90.0F), 4);
      float[] cached = seg < 161 ? CORNER_TRIG[quadrant][seg] : null;
      if (cached != null) {
         return cached;
      } else {
         float[] trig = new float[(seg + 1) * 2];

         for(int s = 0; s <= seg; ++s) {
            double a = Math.toRadians((double)start + (double)90.0F * (double)s / (double)seg);
            trig[s * 2] = (float)Math.cos(a);
            trig[s * 2 + 1] = (float)Math.sin(a);
         }

         if (seg < 161 && start == (float)quadrant * 90.0F) {
            CORNER_TRIG[quadrant][seg] = trig;
         }

         return trig;
      }
   }

   private static float[] circleTrig(int segments) {
      float[] cached = segments < 361 ? CIRCLE_TRIG[segments] : null;
      if (cached != null) {
         return cached;
      } else {
         float[] trig = new float[segments * 2];

         for(int i = 0; i < segments; ++i) {
            double a = (Math.PI * 2D) * (double)i / (double)segments;
            trig[i * 2] = (float)Math.cos(a);
            trig[i * 2 + 1] = (float)Math.sin(a);
         }

         if (segments < 361) {
            CIRCLE_TRIG[segments] = trig;
         }

         return trig;
      }
   }

   private static int bilinear(int tl, int tr, int br, int bl, float u, float v) {
      u = Math.max(0.0F, Math.min(1.0F, u));
      v = Math.max(0.0F, Math.min(1.0F, v));
      return lerpColor(lerpColor(tl, tr, u), lerpColor(bl, br, u), v);
   }

   public static int lerpColor(int a, int b, float t) {
      if (a == b) {
         return a;
      } else {
         int aa = a >>> 24;
         int ar = a >> 16 & 255;
         int ag = a >> 8 & 255;
         int ab = a & 255;
         int ba = b >>> 24;
         int br = b >> 16 & 255;
         int bg = b >> 8 & 255;
         int bb = b & 255;
         return Math.round((float)aa + (float)(ba - aa) * t) << 24 | Math.round((float)ar + (float)(br - ar) * t) << 16 | Math.round((float)ag + (float)(bg - ag) * t) << 8 | Math.round((float)ab + (float)(bb - ab) * t);
      }
   }

   private static void fillContour(float[] pts, float[] dirs, int[] colors) {
      int n = pts.length / 2;
      if (n >= 3) {
         shapes();
         float half = pixel() * 0.5F;
         float cx = 0.0F;
         float cy = 0.0F;

         for(int i = 0; i < n; ++i) {
            cx += pts[i * 2];
            cy += pts[i * 2 + 1];
         }

         cx /= (float)n;
         cy /= (float)n;
         int center = averageColor(colors);

         for(int i = 0; i < n; ++i) {
            int j = (i + 1) % n;
            float ix = pts[i * 2] - dirs[i * 2] * half;
            float iy = pts[i * 2 + 1] - dirs[i * 2 + 1] * half;
            float jx = pts[j * 2] - dirs[j * 2] * half;
            float jy = pts[j * 2 + 1] - dirs[j * 2 + 1] * half;
            vtri(cx, cy, center, ix, iy, colors[i], jx, jy, colors[j]);
            vquad(ix, iy, colors[i], jx, jy, colors[j], pts[j * 2] + dirs[j * 2] * half, pts[j * 2 + 1] + dirs[j * 2 + 1] * half, colors[j] & 16777215, pts[i * 2] + dirs[i * 2] * half, pts[i * 2 + 1] + dirs[i * 2 + 1] * half, colors[i] & 16777215);
         }

      }
   }

   private static void strokeContour(float[] pts, float[] dirs, float lineWidth, int color, boolean closed) {
      int n = pts.length / 2;
      if (n >= 2) {
         shapes();
         float px = pixel();
         float half = Math.max(lineWidth, px) * 0.5F;
         float inner = Math.max(half - px * 0.5F, 0.0F);
         float outerR = half + px * 0.5F;
         int clear = color & 16777215;
         int last = closed ? n : n - 1;

         for(int i = 0; i < last; ++i) {
            int j = (i + 1) % n;
            ring(pts, dirs, i, j, inner, -inner, color, color);
            ring(pts, dirs, i, j, outerR, inner, clear, color);
            ring(pts, dirs, i, j, -inner, -outerR, color, clear);
         }

      }
   }

   private static void ring(float[] p, float[] d, int i, int j, float o0, float o1, int c0, int c1) {
      vquad(p[i * 2] + d[i * 2] * o0, p[i * 2 + 1] + d[i * 2 + 1] * o0, c0, p[j * 2] + d[j * 2] * o0, p[j * 2 + 1] + d[j * 2 + 1] * o0, c0, p[j * 2] + d[j * 2] * o1, p[j * 2 + 1] + d[j * 2 + 1] * o1, c1, p[i * 2] + d[i * 2] * o1, p[i * 2 + 1] + d[i * 2 + 1] * o1, c1);
   }

   private static int averageColor(int[] colors) {
      long a = 0L;
      long r = 0L;
      long g = 0L;
      long b = 0L;

      for(int c : colors) {
         a += (long)(c >>> 24);
         r += (long)(c >> 16 & 255);
         g += (long)(c >> 8 & 255);
         b += (long)(c & 255);
      }

      int n = colors.length;
      return (int)(a / (long)n) << 24 | (int)(r / (long)n) << 16 | (int)(g / (long)n) << 8 | (int)(b / (long)n);
   }

   public static void texture(GfxTexture texture, float x, float y, float width, float height, float u0, float v0, float u1, float v1, int color) {
      if (width != 0.0F && height != 0.0F && color >>> 24 != 0) {
         textured(texture);
         vquadUv(x, y, u0, v0, x, y + height, u0, v1, x + width, y + height, u1, v1, x + width, y, u1, v0, color);
      }
   }

   public static void texture(GfxTexture texture, float x, float y, float width, float height, int color) {
      texture(texture, x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F, color);
   }

   public static float pixel() {
      return 1.0F / scaleFactor();
   }

   private static float scaleFactor() {
      float gui = mc.getWindow() == null ? 1.0F : (float)Math.max(1, mc.getWindow().getGuiScale());
      if (graphics == null) {
         return gui;
      } else {
         Matrix3x2f m = graphics.pose();
         float det = Math.abs(m.m00() * m.m11() - m.m01() * m.m10());
         return gui * (float)Math.sqrt((double)Math.max(det, 1.0E-6F));
      }
   }

   private static void shapes() {
      prepare((GfxTexture)null);
   }

   private static void textured(GfxTexture texture) {
      prepare(texture);
   }

   private static void effects() {
      prepare(Gfx.FxAtlas.get());
   }

   private static void prepare(@Nullable GfxTexture texture) {
      ScreenRectangle scissor = graphics().scissorStack.peek();
      if (floats > 0 && (texture != batchTexture || !Objects.equals(scissor, batchScissor))) {
         flush();
      }

      batchTexture = texture;
      batchScissor = scissor;
   }

   public static void flush() {
      if (floats != 0 && graphics != null) {
         int x0 = (int)Math.floor((double)minX);
         int y0 = (int)Math.floor((double)minY);
         int x1 = (int)Math.ceil((double)maxX);
         int y1 = (int)Math.ceil((double)maxY);
         ScreenRectangle bounds = new ScreenRectangle(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
         if (batchScissor != null) {
            bounds = batchScissor.intersection(bounds);
         }

         if (bounds != null) {
            GfxTexture tex = batchTexture;
            RenderPipeline pipeline = tex == null ? RenderPipelines.GUI : RenderPipelines.GUI_TEXTURED;
            TextureSetup setup = tex == null ? TextureSetup.noTexture() : TextureSetup.singleTexture(tex.view(), tex.sampler());
            graphics.guiRenderState.addGuiElement(new Batch(pipeline, setup, Arrays.copyOf(data, floats), tex != null, batchScissor, bounds));
            emitted += (long)(floats / 5);
         }

         floats = 0;
      } else {
         floats = 0;
      }
   }

   private static void ensure(int more) {
      if (floats + more > data.length) {
         data = Arrays.copyOf(data, Math.max(data.length * 2, floats + more));
      }

   }

   private static void vertex(float x, float y, float u, float v, int color) {
      if (alphaScale < 1.0F) {
         color = Math.round((float)(color >>> 24) * alphaScale) << 24 | color & 16777215;
      }

      Matrix3x2f m = graphics.pose();
      float tx = m.m00() * x + m.m10() * y + m.m20();
      float ty = m.m01() * x + m.m11() * y + m.m21();
      if (floats == 0) {
         maxX = tx;
         minX = tx;
         maxY = ty;
         minY = ty;
      } else {
         minX = Math.min(minX, tx);
         minY = Math.min(minY, ty);
         maxX = Math.max(maxX, tx);
         maxY = Math.max(maxY, ty);
      }

      data[floats++] = tx;
      data[floats++] = ty;
      data[floats++] = u;
      data[floats++] = v;
      data[floats++] = Float.intBitsToFloat(color);
   }

   private static float screenCross(float ax, float ay, float bx, float by, float cx, float cy) {
      Matrix3x2f m = graphics.pose();
      float det = m.m00() * m.m11() - m.m01() * m.m10();
      float cross = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
      return det >= 0.0F ? cross : -cross;
   }

   private static void vtri(float ax, float ay, int ac, float bx, float by, int bc, float cx, float cy, int cc) {
      if (discardDepth <= 0) {
         ensure(20);
         if (screenCross(ax, ay, bx, by, cx, cy) > 0.0F) {
            vertex(ax, ay, 0.0F, 0.0F, ac);
            vertex(cx, cy, 0.0F, 0.0F, cc);
            vertex(bx, by, 0.0F, 0.0F, bc);
            vertex(bx, by, 0.0F, 0.0F, bc);
         } else {
            vertex(ax, ay, 0.0F, 0.0F, ac);
            vertex(bx, by, 0.0F, 0.0F, bc);
            vertex(cx, cy, 0.0F, 0.0F, cc);
            vertex(cx, cy, 0.0F, 0.0F, cc);
         }

      }
   }

   private static void vquad(float ax, float ay, int ac, float bx, float by, int bc, float cx, float cy, int cc, float dx, float dy, int dc) {
      if (discardDepth <= 0) {
         ensure(20);
         float cross = screenCross(ax, ay, bx, by, cx, cy);
         if (cross == 0.0F) {
            cross = screenCross(ax, ay, cx, cy, dx, dy);
         }

         if (cross > 0.0F) {
            vertex(ax, ay, 0.0F, 0.0F, ac);
            vertex(dx, dy, 0.0F, 0.0F, dc);
            vertex(cx, cy, 0.0F, 0.0F, cc);
            vertex(bx, by, 0.0F, 0.0F, bc);
         } else {
            vertex(ax, ay, 0.0F, 0.0F, ac);
            vertex(bx, by, 0.0F, 0.0F, bc);
            vertex(cx, cy, 0.0F, 0.0F, cc);
            vertex(dx, dy, 0.0F, 0.0F, dc);
         }

      }
   }

   private static void vquadUv(float ax, float ay, float au, float av, float bx, float by, float bu, float bv, float cx, float cy, float cu, float cv, float dx, float dy, float du, float dv, int color) {
      if (discardDepth <= 0) {
         ensure(20);
         if (screenCross(ax, ay, bx, by, cx, cy) > 0.0F) {
            vertex(ax, ay, au, av, color);
            vertex(dx, dy, du, dv, color);
            vertex(cx, cy, cu, cv, color);
            vertex(bx, by, bu, bv, color);
         } else {
            vertex(ax, ay, au, av, color);
            vertex(bx, by, bu, bv, color);
            vertex(cx, cy, cu, cv, color);
            vertex(dx, dy, du, dv, color);
         }

      }
   }

   private static final class FxAtlas {
      static final int WIDTH = 512;
      static final int HEIGHT = 256;
      static final int TILE = 32;
      static final int TILES = 8;
      static final int LEVELS = 64;
      static final int GLOW_X = 256;
      static final int GLOW_SIZE = 128;
      static final float GLOW_U0 = 0.50097656F;
      static final float GLOW_U1 = 0.74902344F;
      static final float GLOW_V0 = 0.001953125F;
      static final float GLOW_V1 = 0.49804688F;
      private static final float[] GLOW_STOPS = new float[]{0.0F, 0.18F, 0.42F, 0.7F, 1.0F};
      private static final float[] GLOW_WEIGHTS = new float[]{1.0F, 0.72F, 0.34F, 0.1F, 0.0F};
      private static @Nullable GfxTexture texture;

      static GfxTexture get() {
         if (texture == null) {
            texture = GfxTexture.upload("gfx_effects", build(), true);
         }

         return texture;
      }

      static float tileU(int level, float t) {
         return ((float)(level % 8 * 32) + 0.5F + t * 31.0F) / 512.0F;
      }

      static float tileV(int level, float t) {
         return ((float)(level / 8 * 32) + 0.5F + t * 31.0F) / 256.0F;
      }

      private static BufferedImage build() {
         BufferedImage image = new BufferedImage(512, 256, 2);

         for(int level = 0; level < 64; ++level) {
            float q = Math.min((float)level / 63.0F, 0.999F);
            int ox = level % 8 * 32;
            int oy = level / 8 * 32;

            for(int j = 0; j < 32; ++j) {
               for(int i = 0; i < 32; ++i) {
                  float dx = 1.0F - (float)i / 31.0F;
                  float dy = 1.0F - (float)j / 31.0F;
                  float t = ((float)Math.sqrt((double)(dx * dx + dy * dy)) - q) / (1.0F - q);
                  image.setRGB(ox + i, oy + j, white(t <= 0.0F ? 1.0F : Gfx.falloff(Math.min(t, 1.0F))));
               }
            }
         }

         for(int j = 0; j < 128; ++j) {
            for(int i = 0; i < 128; ++i) {
               float dx = (float)i / 127.0F * 2.0F - 1.0F;
               float dy = (float)j / 127.0F * 2.0F - 1.0F;
               image.setRGB(256 + i, j, white(glowAt((float)Math.sqrt((double)(dx * dx + dy * dy)))));
            }
         }

         return image;
      }

      private static float glowAt(float d) {
         for(int k = 0; k + 1 < GLOW_STOPS.length; ++k) {
            if (d <= GLOW_STOPS[k + 1]) {
               float f = (d - GLOW_STOPS[k]) / (GLOW_STOPS[k + 1] - GLOW_STOPS[k]);
               return GLOW_WEIGHTS[k] + (GLOW_WEIGHTS[k + 1] - GLOW_WEIGHTS[k]) * f;
            }
         }

         return 0.0F;
      }

      private static int white(float alpha) {
         return Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | 16777215;
      }
   }

   private static record Batch(RenderPipeline pipeline, TextureSetup textureSetup, float[] vertices, boolean textured, @Nullable ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
      public void buildVertices(VertexConsumer consumer) {
         float[] v = this.vertices;

         for(int i = 0; i < v.length; i += 5) {
            VertexConsumer vc = consumer.addVertex(v[i], v[i + 1], 0.0F);
            if (this.textured) {
               vc.setUv(v[i + 2], v[i + 3]);
            }

            vc.setColor(Float.floatToRawIntBits(v[i + 4]));
         }

      }
   }
}
