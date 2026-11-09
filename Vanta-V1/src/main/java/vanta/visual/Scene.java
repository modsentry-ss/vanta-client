package vanta.visual;

import vanta.render.Gfx;
import vanta.render.Point;
import vanta.render.screen.Projection;
import vanta.render.world.WorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

public final class Scene {
   private static final float NEAR = 0.05F;
   private static Projection projection;
   private static Projection captured;
   private static long capturedFrame = -1L;
   private static float m00;
   private static float m01;
   private static float m02;
   private static float m03;
   private static float m10;
   private static float m11;
   private static float m12;
   private static float m13;
   private static float m30;
   private static float m31;
   private static float m32;
   private static float m33;
   private static double ox;
   private static double oy;
   private static double oz;
   private static float screenW;
   private static float screenH;
   private static final float[] SCALE = new float[6];
   private static float[] px = new float[16];
   private static float[] py = new float[16];
   private static float[] pw = new float[16];
   private static int[] pc = new int[16];
   private static float[] qx = new float[17];
   private static float[] qy = new float[17];
   private static float[] qw = new float[17];
   private static int[] qc = new int[17];
   private static float[] sx = new float[17];
   private static float[] sy = new float[17];
   public static final int SHADE = 1;
   public static final int EDGES = 2;
   private static final int[] FACES = new int[]{0, 1, 5, 4, 2, 3, 7, 6, 0, 1, 3, 2, 4, 5, 7, 6, 0, 2, 6, 4, 1, 3, 7, 5};
   private static final float[] SHADES = new float[]{0.7F, 1.3F, 0.85F, 0.85F, 1.0F, 1.0F};
   private static final int[] EDGE = new int[]{0, 1, 0, 2, 2, 3, 1, 2, 4, 5, 0, 3, 6, 7, 1, 3, 0, 2, 4, 2, 1, 3, 5, 2, 4, 6, 4, 3, 5, 7, 5, 3, 0, 4, 4, 0, 1, 5, 5, 0, 2, 6, 4, 1, 3, 7, 5, 1};
   private static final float[] BX = new float[8];
   private static final float[] BY = new float[8];
   private static final float[] BW = new float[8];
   private static final boolean[] SHOWN = new boolean[6];
   private static final int[] SIL = new int[12];
   private static final int[] CHAIN = new int[12];
   private static final float[] SIL_XY = new float[12];
   private static float[] miterX = new float[16];
   private static float[] miterY = new float[16];
   private static final int RING_MAX = 128;
   private static final float[] RIX = new float[129];
   private static final float[] RIY = new float[129];
   private static final float[] RIW = new float[129];
   private static final float[] ROX = new float[129];
   private static final float[] ROY = new float[129];
   private static final float[] ROW = new float[129];
   private static final float[] NX = new float[129];
   private static final float[] NY = new float[129];

   private Scene() {
   }

   public static Projection begin() {
      captured = Projection.capture();
      capturedFrame = WorldRenderer.frame();
      load(captured);
      return captured;
   }

   public static Projection frame() {
      if (captured != null && capturedFrame == WorldRenderer.frame()) {
         if (projection != captured) {
            load(captured);
         }

         return captured;
      } else {
         return begin();
      }
   }

   public static Projection projection() {
      return projection;
   }

   public static void use(Projection view) {
      if (view != projection) {
         load(view);
      }

   }

   private static void load(Projection view) {
      projection = view;
      Vec3 o = view.origin();
      ox = o.x;
      oy = o.y;
      oz = o.z;
      screenW = view.width();
      screenH = view.height();
      Vector4f t = view.clipPoint(ox, oy, oz);
      Vector4f a = view.clipPoint(ox + (double)1.0F, oy, oz);
      Vector4f b = view.clipPoint(ox, oy + (double)1.0F, oz);
      Vector4f c = view.clipPoint(ox, oy, oz + (double)1.0F);
      m00 = a.x - t.x;
      m01 = b.x - t.x;
      m02 = c.x - t.x;
      m03 = t.x;
      m10 = a.y - t.y;
      m11 = b.y - t.y;
      m12 = c.y - t.y;
      m13 = t.y;
      m30 = a.w - t.w;
      m31 = b.w - t.w;
      m32 = c.w - t.w;
      m33 = t.w;
   }

   public static Vector4f clip(double x, double y, double z) {
      return projection.clipPoint(x, y, z);
   }

   public static Point project(double x, double y, double z) {
      return projection.project(x, y, z);
   }

   public static Point project(Vec3 v) {
      return projection.project(v.x, v.y, v.z);
   }

   public static boolean toScreen(double x, double y, double z, float[] out) {
      float rx = (float)(x - ox);
      float ry = (float)(y - oy);
      float rz = (float)(z - oz);
      float w = m30 * rx + m31 * ry + m32 * rz + m33;
      if (w <= 0.05F) {
         return false;
      } else {
         float cx = m00 * rx + m01 * ry + m02 * rz + m03;
         float cy = m10 * rx + m11 * ry + m12 * rz + m13;
         out[0] = (cx / w + 1.0F) * 0.5F * screenW;
         out[1] = (1.0F - cy / w) * 0.5F * screenH;
         out[2] = w;
         return true;
      }
   }

   public static void direction(double x, double y, double z, float[] out) {
      float rx = (float)(x - ox);
      float ry = (float)(y - oy);
      float rz = (float)(z - oz);
      out[0] = (m00 * rx + m01 * ry + m02 * rz + m03) * screenW * 0.5F;
      out[1] = -(m10 * rx + m11 * ry + m12 * rz + m13) * screenH * 0.5F;
   }

   public static float scaleAt(double x, double y, double z) {
      float[] s = SCALE;
      if (!toScreen(x, y, z, s)) {
         return 0.0F;
      } else {
         float yaw = (float)Math.toRadians((double)Minecraft.getInstance().gameRenderer.mainCamera().yRot());
         s[3] = s[0];
         s[4] = s[1];
         if (!toScreen(x + Math.cos((double)yaw), y, z + Math.sin((double)yaw), s)) {
            return 0.0F;
         } else {
            float dx = s[0] - s[3];
            float dy = s[1] - s[4];
            return (float)Math.sqrt((double)(dx * dx + dy * dy));
         }
      }
   }

   public static boolean onScreen(Point p, float margin) {
      return p != null && onScreen(p.x(), p.y(), margin);
   }

   public static boolean onScreen(float x, float y, float margin) {
      return x > -margin && y > -margin && x < screenW + margin && y < screenH + margin;
   }

   private static void ensure(int n) {
      if (px.length < n) {
         px = new float[n];
         py = new float[n];
         pw = new float[n];
         pc = new int[n];
         qx = new float[n + 1];
         qy = new float[n + 1];
         qw = new float[n + 1];
         qc = new int[n + 1];
         sx = new float[n + 1];
         sy = new float[n + 1];
      }

   }

   private static void corner(int i, double x, double y, double z, int color) {
      float rx = (float)(x - ox);
      float ry = (float)(y - oy);
      float rz = (float)(z - oz);
      px[i] = m00 * rx + m01 * ry + m02 * rz + m03;
      py[i] = m10 * rx + m11 * ry + m12 * rz + m13;
      pw[i] = m30 * rx + m31 * ry + m32 * rz + m33;
      pc[i] = color;
   }

   public static void quad(double ax, double ay, double az, double bx, double by, double bz, double cx, double cy, double cz, double dx, double dy, double dz, int ca, int cb, int cc, int cd) {
      if ((ca | cb | cc | cd) >>> 24 != 0) {
         corner(0, ax, ay, az, ca);
         corner(1, bx, by, bz, cb);
         corner(2, cx, cy, cz, cc);
         corner(3, dx, dy, dz, cd);
         fill(4, screenW, screenH);
      }
   }

   public static void line(double ax, double ay, double az, double bx, double by, double bz, float width, int color) {
      if (color >>> 24 != 0) {
         corner(0, ax, ay, az, color);
         corner(1, bx, by, bz, color);
         stroke(0, 1, screenW, screenH, color, width);
      }
   }

   public static void glowLine(double ax, double ay, double az, double bx, double by, double bz, float width, int color) {
      line(ax, ay, az, bx, by, bz, width, color);
   }

   public static void segment(Projection projection, Vector4f a, Vector4f b, int color, float width) {
      px[0] = a.x;
      py[0] = a.y;
      pw[0] = a.w;
      px[1] = b.x;
      py[1] = b.y;
      pw[1] = b.w;
      stroke(0, 1, projection.width(), projection.height(), color, width);
   }

   public static void polygon(Projection projection, Vector4f[] points, int[] colors) {
      int n = points.length;
      ensure(n);

      for(int i = 0; i < n; ++i) {
         px[i] = points[i].x;
         py[i] = points[i].y;
         pw[i] = points[i].w;
         pc[i] = colors[i];
      }

      fill(n, projection.width(), projection.height());
   }

   private static void stroke(int i, int j, float width, float height, int color, float lineWidth) {
      float ax = px[i];
      float ay = py[i];
      float aw = pw[i];
      float bx = px[j];
      float by = py[j];
      float bw = pw[j];
      boolean av = aw > 0.05F;
      boolean bv = bw > 0.05F;
      if (av || bv) {
         if (!av) {
            float t = (0.05F - aw) / (bw - aw);
            ax += (bx - ax) * t;
            ay += (by - ay) * t;
            aw = 0.05F;
         } else if (!bv) {
            float t = (0.05F - bw) / (aw - bw);
            bx += (ax - bx) * t;
            by += (ay - by) * t;
            bw = 0.05F;
         }

         Gfx.line((ax / aw + 1.0F) * 0.5F * width, (1.0F - ay / aw) * 0.5F * height, (bx / bw + 1.0F) * 0.5F * width, (1.0F - by / bw) * 0.5F * height, lineWidth, color);
      }
   }

   private static void fill(int n, float width, float height) {
      boolean front = true;

      for(int i = 0; i < n; ++i) {
         if (pw[i] <= 0.05F) {
            front = false;
            break;
         }
      }

      float[] xs = px;
      float[] ys = py;
      float[] ws = pw;
      int[] cs = pc;
      int m = n;
      if (!front) {
         m = 0;

         for(int i = 0; i < n; ++i) {
            int j = i + 1 == n ? 0 : i + 1;
            boolean iv = pw[i] > 0.05F;
            boolean jv = pw[j] > 0.05F;
            if (iv) {
               qx[m] = px[i];
               qy[m] = py[i];
               qw[m] = pw[i];
               qc[m] = pc[i];
               ++m;
            }

            if (iv != jv) {
               float t = (0.05F - pw[i]) / (pw[j] - pw[i]);
               qx[m] = px[i] + (px[j] - px[i]) * t;
               qy[m] = py[i] + (py[j] - py[i]) * t;
               qw[m] = 0.05F;
               qc[m] = Gfx.lerpColor(pc[i], pc[j], t);
               ++m;
            }
         }

         if (m < 3) {
            return;
         }

         xs = qx;
         ys = qy;
         ws = qw;
         cs = qc;
      }

      for(int i = 0; i < m; ++i) {
         sx[i] = (xs[i] / ws[i] + 1.0F) * 0.5F * width;
         sy[i] = (1.0F - ys[i] / ws[i]) * 0.5F * height;
      }

      if (m == 4) {
         Gfx.quadColors(sx[0], sy[0], cs[0], sx[1], sy[1], cs[1], sx[2], sy[2], cs[2], sx[3], sy[3], cs[3]);
      } else {
         for(int i = 1; i + 1 < m; ++i) {
            Gfx.triangleColors(sx[0], sy[0], cs[0], sx[i], sy[i], cs[i], sx[i + 1], sy[i + 1], cs[i + 1]);
         }

      }
   }

   public static void box(AABB box, int color, float fillAlpha, float edgeAlpha, float width, int flags) {
      box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color, fillAlpha, edgeAlpha, width, flags, 0);
   }

   public static void box(double x0, double y0, double z0, double x1, double y1, double z1, int color, float fillAlpha, float edgeAlpha, float width, int flags, int covered) {
      if (projection != null && color >>> 24 != 0) {
         boolean[] shown = SHOWN;
         shown[0] = oy < y0;
         shown[1] = oy > y1;
         shown[2] = oz < z0;
         shown[3] = oz > z1;
         shown[4] = ox < x0;
         shown[5] = ox > x1;
         boolean any = false;

         for(int f = 0; f < 6; ++f) {
            shown[f] &= (covered & 1 << f) == 0;
            any |= shown[f];
         }

         if (any) {
            int left = 0;
            int right = 0;
            int below = 0;
            int above = 0;
            int behind = 0;

            for(int i = 0; i < 8; ++i) {
               float rx = (float)(((i & 1) == 0 ? x0 : x1) - ox);
               float ry = (float)(((i & 2) == 0 ? y0 : y1) - oy);
               float rz = (float)(((i & 4) == 0 ? z0 : z1) - oz);
               float x = m00 * rx + m01 * ry + m02 * rz + m03;
               float y = m10 * rx + m11 * ry + m12 * rz + m13;
               float w = m30 * rx + m31 * ry + m32 * rz + m33;
               BX[i] = x;
               BY[i] = y;
               BW[i] = w;
               if (w <= 0.05F) {
                  ++behind;
               }

               if (x < -w) {
                  ++left;
               }

               if (x > w) {
                  ++right;
               }

               if (y < -w) {
                  ++below;
               }

               if (y > w) {
                  ++above;
               }
            }

            if (behind != 8 && left != 8 && right != 8 && below != 8 && above != 8) {
               if (fillAlpha > 0.0F) {
                  for(int f = 0; f < 6; ++f) {
                     if (shown[f]) {
                        int fill = Fx.alpha(color, (flags & 1) != 0 ? fillAlpha * SHADES[f] : fillAlpha);

                        for(int k = 0; k < 4; ++k) {
                           int c = FACES[f * 4 + k];
                           px[k] = BX[c];
                           py[k] = BY[c];
                           pw[k] = BW[c];
                           pc[k] = fill;
                        }

                        fill(4, screenW, screenH);
                     }
                  }
               }

               boolean edges = edgeAlpha > 0.0F && width > 0.0F;
               if (edges || !(fillAlpha <= 0.0F)) {
                  int edge = Fx.alpha(color, edgeAlpha);
                  int silhouette = 0;

                  for(int e = 0; e < EDGE.length; e += 4) {
                     boolean a = shown[EDGE[e + 2]];
                     boolean b = shown[EDGE[e + 3]];
                     if (a != b) {
                        SIL[silhouette * 2] = EDGE[e];
                        SIL[silhouette * 2 + 1] = EDGE[e + 1];
                        ++silhouette;
                     } else if (a && edges && (flags & 2) != 0) {
                        segment(EDGE[e], EDGE[e + 1], edge, width);
                     }
                  }

                  int loop = behind == 0 ? chain(silhouette) : 0;
                  if (loop > 0) {
                     for(int k = 0; k < loop; ++k) {
                        int c = SIL[k];
                        SIL_XY[k * 2] = (BX[c] / BW[c] + 1.0F) * 0.5F * screenW;
                        SIL_XY[k * 2 + 1] = (1.0F - BY[c] / BW[c]) * 0.5F * screenH;
                     }

                     if (edges) {
                        outline(SIL_XY, loop, width, edge);
                     } else {
                        featherOutside(SIL_XY, loop, Fx.alpha(color, fillAlpha));
                     }
                  } else if (edges) {
                     for(int s = 0; s < silhouette; ++s) {
                        segment(SIL[s * 2], SIL[s * 2 + 1], edge, width);
                     }
                  }

               }
            }
         }
      }
   }

   private static void segment(int i, int j, int color, float width) {
      px[0] = BX[i];
      py[0] = BY[i];
      pw[0] = BW[i];
      px[1] = BX[j];
      py[1] = BY[j];
      pw[1] = BW[j];
      stroke(0, 1, screenW, screenH, color, width);
   }

   private static int chain(int count) {
      if (count < 3) {
         return 0;
      } else {
         int first = SIL[0];
         int at = SIL[1];
         int used = 1;
         int length = 1;

         int next;
         for(CHAIN[0] = first; at != first; at = next) {
            if (length >= count) {
               return 0;
            }

            CHAIN[length++] = at;
            next = -1;

            for(int s = 1; s < count && next < 0; ++s) {
               if ((used & 1 << s) == 0) {
                  if (SIL[s * 2] == at) {
                     next = SIL[s * 2 + 1];
                  } else {
                     if (SIL[s * 2 + 1] != at) {
                        continue;
                     }

                     next = SIL[s * 2];
                  }

                  used |= 1 << s;
               }
            }

            if (next < 0) {
               return 0;
            }
         }

         if (length != count) {
            return 0;
         } else {
            System.arraycopy(CHAIN, 0, SIL, 0, length);
            return length;
         }
      }
   }

   public static void outline(float[] xy, int n, float width, int color) {
      if (n >= 3 && color >>> 24 != 0) {
         miters(xy, n);
         float p = Gfx.pixel();
         float half = Math.max(width, p) * 0.5F;
         float in = Math.max(half - p * 0.5F, 0.0F);
         float out = half + p * 0.5F;
         int clear = color & 16777215;

         for(int i = 0; i < n; ++i) {
            int j = i + 1 == n ? 0 : i + 1;
            band(xy, i, j, -in, in, color, color);
            band(xy, i, j, in, out, color, clear);
            band(xy, i, j, -out, -in, clear, color);
         }

      }
   }

   private static void featherOutside(float[] xy, int n, int color) {
      if (n >= 3 && color >>> 24 != 0) {
         miters(xy, n);
         float cx = 0.0F;
         float cy = 0.0F;

         for(int i = 0; i < n; ++i) {
            cx += xy[i * 2];
            cy += xy[i * 2 + 1];
         }

         cx /= (float)n;
         cy /= (float)n;
         float p = miterX[0] * (xy[0] - cx) + miterY[0] * (xy[1] - cy) >= 0.0F ? Gfx.pixel() : -Gfx.pixel();
         int clear = color & 16777215;

         for(int i = 0; i < n; ++i) {
            band(xy, i, i + 1 == n ? 0 : i + 1, 0.0F, p, color, clear);
         }

      }
   }

   private static void miters(float[] xy, int n) {
      if (miterX.length < n) {
         miterX = new float[Math.max(n, miterX.length * 2)];
         miterY = new float[miterX.length];
      }

      for(int i = 0; i < n; ++i) {
         int prev = i == 0 ? n - 1 : i - 1;
         int next = i + 1 == n ? 0 : i + 1;
         float ex = xy[i * 2] - xy[prev * 2];
         float ey = xy[i * 2 + 1] - xy[prev * 2 + 1];
         float fx = xy[next * 2] - xy[i * 2];
         float fy = xy[next * 2 + 1] - xy[i * 2 + 1];
         float el = (float)Math.sqrt((double)(ex * ex + ey * ey));
         float fl = (float)Math.sqrt((double)(fx * fx + fy * fy));
         if (el < 1.0E-5F && fl < 1.0E-5F) {
            miterX[i] = 0.0F;
            miterY[i] = 0.0F;
         } else {
            float n1x = el < 1.0E-5F ? -fy / fl : -ey / el;
            float n1y = el < 1.0E-5F ? fx / fl : ex / el;
            float n2x = fl < 1.0E-5F ? n1x : -fy / fl;
            float n2y = fl < 1.0E-5F ? n1y : fx / fl;
            float mx = n1x + n2x;
            float my = n1y + n2y;
            float ml = (float)Math.sqrt((double)(mx * mx + my * my));
            if (ml < 1.0E-4F) {
               mx = n2x;
               my = n2y;
               ml = 1.0F;
            }

            mx /= ml;
            my /= ml;
            float k = 1.0F / Math.max(mx * n2x + my * n2y, 0.35F);
            miterX[i] = mx * k;
            miterY[i] = my * k;
         }
      }

   }

   private static void band(float[] xy, int i, int j, float from, float to, int fromColor, int toColor) {
      float xi = xy[i * 2];
      float yi = xy[i * 2 + 1];
      float xj = xy[j * 2];
      float yj = xy[j * 2 + 1];
      float mxi = miterX[i];
      float myi = miterY[i];
      float mxj = miterX[j];
      float myj = miterY[j];
      Gfx.quadColors(xi + mxi * from, yi + myi * from, fromColor, xj + mxj * from, yj + myj * from, fromColor, xj + mxj * to, yj + myj * to, toColor, xi + mxi * to, yi + myi * to, toColor);
   }

   public static void groundRing(double x, double y, double z, double radius, double width, int color) {
      ring(x, y, z, (double)1.0F, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, (double)1.0F, radius, width, color);
   }

   public static void ring(double x, double y, double z, double ux, double uy, double uz, double vx, double vy, double vz, double radius, double width, int color) {
      if (projection != null && color >>> 24 != 0 && !(radius <= (double)0.0F) && !(width <= (double)0.0F)) {
         float scale = scaleAt(x, y, z);
         int n = scale <= 0.0F ? 64 : Math.clamp((long)((int)((Math.PI * 2D) * radius * (double)scale / (double)5.0F)), 24, 128);
         double inner = Math.max((double)0.0F, radius - width * (double)0.5F);
         double outer = radius + width * (double)0.5F;
         boolean front = true;

         for(int i = 0; i <= n; ++i) {
            double a = i == n ? (double)0.0F : (Math.PI * 2D) * (double)i / (double)n;
            double c = Math.cos(a);
            double s = Math.sin(a);
            double dx = ux * c + vx * s;
            double dy = uy * c + vy * s;
            double dz = uz * c + vz * s;
            float rx = (float)(x + dx * inner - ox);
            float ry = (float)(y + dy * inner - oy);
            float rz = (float)(z + dz * inner - oz);
            RIX[i] = m00 * rx + m01 * ry + m02 * rz + m03;
            RIY[i] = m10 * rx + m11 * ry + m12 * rz + m13;
            RIW[i] = m30 * rx + m31 * ry + m32 * rz + m33;
            rx = (float)(x + dx * outer - ox);
            ry = (float)(y + dy * outer - oy);
            rz = (float)(z + dz * outer - oz);
            ROX[i] = m00 * rx + m01 * ry + m02 * rz + m03;
            ROY[i] = m10 * rx + m11 * ry + m12 * rz + m13;
            ROW[i] = m30 * rx + m31 * ry + m32 * rz + m33;
            front &= RIW[i] > 0.05F && ROW[i] > 0.05F;
         }

         if (!front) {
            for(int i = 0; i < n; ++i) {
               bandCorner(0, RIX, RIY, RIW, i, color);
               bandCorner(1, RIX, RIY, RIW, i + 1, color);
               bandCorner(2, ROX, ROY, ROW, i + 1, color);
               bandCorner(3, ROX, ROY, ROW, i, color);
               fill(4, screenW, screenH);
            }

         } else {
            for(int i = 0; i <= n; ++i) {
               RIX[i] = (RIX[i] / RIW[i] + 1.0F) * 0.5F * screenW;
               RIY[i] = (1.0F - RIY[i] / RIW[i]) * 0.5F * screenH;
               ROX[i] = (ROX[i] / ROW[i] + 1.0F) * 0.5F * screenW;
               ROY[i] = (1.0F - ROY[i] / ROW[i]) * 0.5F * screenH;
            }

            for(int i = 0; i <= n; ++i) {
               float nx = ROX[i] - RIX[i];
               float ny = ROY[i] - RIY[i];
               float len = (float)Math.sqrt((double)(nx * nx + ny * ny));
               if (len < 0.001F) {
                  int a = i == 0 ? n - 1 : i - 1;
                  int b = i == n ? 1 : i + 1;
                  nx = -(RIY[b] - RIY[a]);
                  ny = RIX[b] - RIX[a];
                  len = (float)Math.sqrt((double)(nx * nx + ny * ny));
                  if (len < 1.0E-6F) {
                     nx = 0.0F;
                     ny = 1.0F;
                     len = 1.0F;
                  }
               }

               NX[i] = nx / len;
               NY[i] = ny / len;
            }

            float px1 = Gfx.pixel();
            int clear = color & 16777215;

            for(int i = 0; i < n; ++i) {
               int j = i + 1;
               Gfx.quadColors(RIX[i], RIY[i], color, RIX[j], RIY[j], color, ROX[j], ROY[j], color, ROX[i], ROY[i], color);
               Gfx.quadColors(ROX[i], ROY[i], color, ROX[j], ROY[j], color, ROX[j] + NX[j] * px1, ROY[j] + NY[j] * px1, clear, ROX[i] + NX[i] * px1, ROY[i] + NY[i] * px1, clear);
               Gfx.quadColors(RIX[i] - NX[i] * px1, RIY[i] - NY[i] * px1, clear, RIX[j] - NX[j] * px1, RIY[j] - NY[j] * px1, clear, RIX[j], RIY[j], color, RIX[i], RIY[i], color);
            }

         }
      }
   }

   private static void bandCorner(int k, float[] xs, float[] ys, float[] ws, int i, int color) {
      px[k] = xs[i];
      py[k] = ys[i];
      pw[k] = ws[i];
      pc[k] = color;
   }

   public static void loop(double[] xs, double[] ys, double[] zs, int n, float width, int color) {
      if (projection != null && n >= 3 && color >>> 24 != 0) {
         n = Math.min(n, 128);
         float[] out = SCALE;

         for(int i = 0; i < n; ++i) {
            if (!toScreen(xs[i], ys[i], zs[i], out)) {
               for(int k = 0; k < n; ++k) {
                  int l = k + 1 == n ? 0 : k + 1;
                  line(xs[k], ys[k], zs[k], xs[l], ys[l], zs[l], width, color);
               }

               return;
            }

            RIX[i] = out[0];
            RIY[i] = out[1];
         }

         for(int i = 0; i < n; ++i) {
            int a = i == 0 ? n - 1 : i - 1;
            int b = i + 1 == n ? 0 : i + 1;
            float tx = RIX[b] - RIX[a];
            float ty = RIY[b] - RIY[a];
            float len = (float)Math.sqrt((double)(tx * tx + ty * ty));
            if (len < 1.0E-5F) {
               NX[i] = 0.0F;
               NY[i] = 1.0F;
            } else {
               NX[i] = -ty / len;
               NY[i] = tx / len;
            }
         }

         float px1 = Gfx.pixel();
         float half = Math.max(width, px1) * 0.5F;
         float in = Math.max(half - px1 * 0.5F, 0.0F);
         float out1 = half + px1 * 0.5F;
         int clear = color & 16777215;

         for(int i = 0; i < n; ++i) {
            int j = i + 1 == n ? 0 : i + 1;
            float ax = RIX[i];
            float ay = RIY[i];
            float bx = RIX[j];
            float by = RIY[j];
            float anx = NX[i];
            float any = NY[i];
            float bnx = NX[j];
            float bny = NY[j];
            Gfx.quadColors(ax - anx * in, ay - any * in, color, bx - bnx * in, by - bny * in, color, bx + bnx * in, by + bny * in, color, ax + anx * in, ay + any * in, color);
            Gfx.quadColors(ax + anx * in, ay + any * in, color, bx + bnx * in, by + bny * in, color, bx + bnx * out1, by + bny * out1, clear, ax + anx * out1, ay + any * out1, clear);
            Gfx.quadColors(ax - anx * out1, ay - any * out1, clear, bx - bnx * out1, by - bny * out1, clear, bx - bnx * in, by - bny * in, color, ax - anx * in, ay - any * in, color);
         }

      }
   }
}
