package vanta.ui.click;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import vanta.render.Gfx;
import vanta.ui.DraggableHud;
import vanta.visual.hud.HudElements;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
   private static final float HANDLE = 4.0F;
   private static final float SNAP = 4.0F;
   private final Screen previous;
   private DraggableHud dragging;
   private DraggableHud selected;
   private int corner;
   private float offX;
   private float offY;
   private float startScale;
   private float startDist;
   private float anchorX;
   private float anchorY;
   private final long opened;
   private final List<float[]> guides;

   public HudEditorScreen() {
      this((Screen)null);
   }

   public HudEditorScreen(Screen previous) {
      super(Component.literal("HUD"));
      this.corner = -1;
      this.opened = System.nanoTime();
      this.guides = new ArrayList();
      this.previous = previous;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
   }

   private static boolean shown(DraggableHud hud) {
      float[] b = hud.getHudBounds();
      return hud.isHudEnabled() && b != null && b[2] > 0.0F && b[3] > 0.0F;
   }

   private DraggableHud at(double mx, double my) {
      List<DraggableHud> all = HudElements.all();

      for(int i = all.size() - 1; i >= 0; --i) {
         DraggableHud hud = (DraggableHud)all.get(i);
         float[] b = hud.getHudBounds();
         if (shown(hud) && mx >= (double)(b[0] - 2.0F) && my >= (double)(b[1] - 2.0F) && mx < (double)(b[0] + b[2] + 2.0F) && my < (double)(b[1] + b[3] + 2.0F)) {
            return hud;
         }
      }

      return null;
   }

   private static int cornerAt(DraggableHud hud, double mx, double my) {
      if (hud != null && hud.canScaleHud() && shown(hud)) {
         float[] b = hud.getHudBounds();
         float[][] c = corners(b);

         for(int i = 0; i < 4; ++i) {
            if (Math.abs(mx - (double)c[i][0]) <= (double)4.0F && Math.abs(my - (double)c[i][1]) <= (double)4.0F) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private static float[][] corners(float[] b) {
      float x0 = b[0] - 2.0F;
      float y0 = b[1] - 2.0F;
      float x1 = b[0] + b[2] + 2.0F;
      float y1 = b[1] + b[3] + 2.0F;
      return new float[][]{{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
      super.extractRenderState(g, mouseX, mouseY, delta);
      Anim.frame();
      float t = Anim.outCubic((float)(System.nanoTime() - this.opened) / 1.0E9F / 0.25F);
      Paint.g = g;
      Paint.fade = t;
      Gfx.begin(g);

      try {
         int line = 218103807;

         for(int x = 0; x < this.width; x += 20) {
            Gfx.rect((float)x, 0.0F, (float)x + 0.5F, (float)this.height, Paint.a(line, 1.0F));
         }

         for(int y = 0; y < this.height; y += 20) {
            Gfx.rect(0.0F, (float)y, (float)this.width, (float)y + 0.5F, Paint.a(line, 1.0F));
         }

         int ac = Widgets.accent();

         for(float[] guide : this.guides) {
            if (guide[0] == 0.0F) {
               Gfx.rect(guide[1] - 0.4F, 0.0F, guide[1] + 0.4F, (float)this.height, Paint.a(ac & 16777215 | -1073741824, 1.0F));
            } else {
               Gfx.rect(0.0F, guide[1] - 0.4F, (float)this.width, guide[1] + 0.4F, Paint.a(ac & 16777215 | -1073741824, 1.0F));
            }
         }

         DraggableHud hover = this.dragging != null ? this.dragging : this.at((double)mouseX, (double)mouseY);

         for(DraggableHud hud : HudElements.all()) {
            if (shown(hud)) {
               float[] b = hud.getHudBounds();
               boolean active = hud == hover || hud == this.selected;
               float h = Anim.to(hud, 0, active ? 1.0F : 0.0F, 16.0F);
               float x0 = b[0] - 2.0F;
               float y0 = b[1] - 2.0F;
               float w = b[2] + 4.0F;
               float hh = b[3] + 4.0F;
               Gfx.roundedRect(x0, y0, w, hh, 3.0F, Paint.a(806359574, 0.4F + 0.6F * h));
               Gfx.roundedOutline(x0, y0, w, hh, 3.0F, 0.7F, Paint.a(Paint.mix(1351258008, ac, h), 0.5F + 0.5F * h));
               if (h > 0.02F) {
                  if (hud.canScaleHud()) {
                     int hc = cornerAt(hud, (double)mouseX, (double)mouseY);
                     float[][] c = corners(b);

                     for(int i = 0; i < 4; ++i) {
                        float s = i != hc && (i != this.corner || hud != this.dragging) ? 2.4F : 3.2F;
                        Gfx.roundedRect(c[i][0] - s, c[i][1] - s, s * 2.0F, s * 2.0F, 1.0F, Paint.a(-1, h));
                        Gfx.roundedOutline(c[i][0] - s, c[i][1] - s, s * 2.0F, s * 2.0F, 1.0F, 0.6F, Paint.a(ac, h));
                     }
                  }

                  String name = hud.getHudName();
                  if (hud == this.dragging && this.corner >= 0) {
                     name = name + "  " + Math.round(hud.getHudScale() * 100.0F) + "%";
                  }

                  float ty = b[1] - 8.0F < 2.0F ? b[1] + b[3] + 9.0F : b[1] - 8.0F;
                  float nw = Paint.width(name, 6.0F, (byte)0) + 8.0F;
                  Gfx.roundedRect(b[0] - 2.0F, ty - 5.0F, nw, 10.0F, 3.0F, Paint.a(-434430429, h));
                  Paint.text(name, b[0] + 2.0F, ty, 6.0F, Paint.a(-920844, h), (byte)0);
               }
            }
         }

         String hint = "Drag to move  ·  Drag a corner to resize  ·  Scroll to scale  ·  Right click to reset  ·  Esc back";
         float w = Paint.width(hint, 6.0F, (byte)0) + 20.0F;
         float x = ((float)this.width - w) / 2.0F;
         float y = (float)this.height - 30.0F;
         Gfx.shadow(x, y, w, 16.0F, 8.0F, 10.0F, Paint.a(1342177280, 1.0F));
         Gfx.roundedRect(x, y, w, 16.0F, 8.0F, Paint.a(-434430429, 1.0F));
         Gfx.roundedOutline(x, y, w, 16.0F, 8.0F, 0.6F, Paint.a(-11907498, 1.0F));
         Paint.textCenter(hint, (float)this.width / 2.0F, y + 8.0F, 6.0F, Widgets.text(), (byte)0);
         Paint.flush();
      } finally {
         Paint.discard();
         Paint.fade = 1.0F;
         Gfx.end();
      }

   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      DraggableHud hud = this.at(event.x(), event.y());
      int c = cornerAt(this.selected, event.x(), event.y());
      if (c >= 0) {
         hud = this.selected;
      } else {
         for(DraggableHud other : HudElements.all()) {
            int oc = cornerAt(other, event.x(), event.y());
            if (oc >= 0) {
               hud = other;
               c = oc;
               break;
            }
         }
      }

      if (hud == null) {
         this.selected = null;
         return super.mouseClicked(event, doubleClick);
      } else if (event.button() == 1) {
         hud.resetHudPosition();
         if (hud.canScaleHud()) {
            hud.setHudScale(1.0F);
         }

         GuiSounds.tap();
         HudElements.save();
         return true;
      } else {
         float[] b = hud.getHudBounds();
         this.selected = hud;
         this.dragging = hud;
         this.corner = c;
         if (c >= 0) {
            float[][] cs = corners(b);
            float[] opposite = cs[(c + 2) % 4];
            this.anchorX = opposite[0];
            this.anchorY = opposite[1];
            this.startScale = hud.getHudScale();
            this.startDist = (float)Math.hypot(event.x() - (double)this.anchorX, event.y() - (double)this.anchorY);
         } else {
            this.offX = (float)event.x() - b[0];
            this.offY = (float)event.y() - b[1];
         }

         return true;
      }
   }

   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (this.dragging == null) {
         return super.mouseDragged(event, dx, dy);
      } else {
         this.guides.clear();
         if (this.corner >= 0) {
            float dist = (float)Math.hypot(event.x() - (double)this.anchorX, event.y() - (double)this.anchorY);
            float scale = this.startScale * dist / Math.max(1.0F, this.startDist);
            scale = (float)Math.round(scale * 20.0F) / 20.0F;
            scale = Math.max(0.5F, Math.min(3.0F, scale));
            if (Math.abs(scale - this.dragging.getHudScale()) > 0.001F) {
               this.dragging.setHudScale(scale);
               GuiSounds.detent();
            }

            float[] b = this.dragging.getHudBounds();
            if (b != null) {
               float var30;
               switch (this.corner) {
                  case 0:
                  case 3:
                     var30 = this.anchorX - 2.0F - b[2];
                     break;
                  default:
                     var30 = this.anchorX + 2.0F;
               }

               float nx = var30;
               switch (this.corner) {
                  case 0:
                  case 1:
                     var30 = this.anchorY - 2.0F - b[3];
                     break;
                  default:
                     var30 = this.anchorY + 2.0F;
               }

               float ny = var30;
               this.dragging.setHudPosition(Math.max(0.0F, Math.min((float)this.width - b[2], nx)), Math.max(0.0F, Math.min((float)this.height - b[3], ny)));
            }

            return true;
         } else {
            float[] b = this.dragging.getHudBounds();
            float w = b == null ? 0.0F : b[2];
            float h = b == null ? 0.0F : b[3];
            float x = (float)event.x() - this.offX;
            float y = (float)event.y() - this.offY;
            float[] var10000 = new float[]{0.0F, (float)this.width / 2.0F, (float)this.width};
            var10000 = new float[]{0.0F, (float)this.height / 2.0F, (float)this.height};
            List<Float> tx = new ArrayList(List.of(4.0F, (float)this.width / 2.0F, (float)this.width - 4.0F));
            List<Float> ty = new ArrayList(List.of(4.0F, (float)this.height / 2.0F, (float)this.height - 4.0F));

            for(DraggableHud other : HudElements.all()) {
               if (other != this.dragging && shown(other)) {
                  float[] o = other.getHudBounds();
                  tx.add(o[0]);
                  tx.add(o[0] + o[2]);
                  ty.add(o[1]);
                  ty.add(o[1] + o[3]);
               }
            }

            float[] snapX = snap(x, w, tx);
            float[] snapY = snap(y, h, ty);
            if (snapX != null) {
               x = snapX[0];
               this.guides.add(new float[]{0.0F, snapX[1]});
            }

            if (snapY != null) {
               y = snapY[0];
               this.guides.add(new float[]{1.0F, snapY[1]});
            }

            x = Math.max(0.0F, Math.min((float)this.width - w, x));
            y = Math.max(0.0F, Math.min((float)this.height - h, y));
            this.dragging.setHudPosition(x, y);
            return true;
         }
      }
   }

   private static float[] snap(float pos, float size, List<Float> targets) {
      float best = 4.001F;
      float[] result = null;
      float[] edges = new float[]{0.0F, size / 2.0F, size};

      for(float target : targets) {
         for(float edge : edges) {
            float d = Math.abs(pos + edge - target);
            if (d < best) {
               best = d;
               result = new float[]{target - edge, target};
            }
         }
      }

      return result;
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      if (this.dragging != null) {
         this.dragging = null;
         this.corner = -1;
         this.guides.clear();
         HudElements.save();
         return true;
      } else {
         return super.mouseReleased(event);
      }
   }

   public boolean mouseScrolled(double x, double y, double h, double v) {
      DraggableHud hud = this.at(x, y);
      if (hud != null && hud.canScaleHud()) {
         float s = (float)Math.round((hud.getHudScale() + (float)v * 0.05F) * 20.0F) / 20.0F;
         hud.setHudScale(Math.max(0.5F, Math.min(3.0F, s)));
         GuiSounds.detent();
         HudElements.save();
         return true;
      } else {
         return super.mouseScrolled(x, y, h, v);
      }
   }

   public boolean keyPressed(KeyEvent event) {
      if (event.key() == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   public void onClose() {
      HudElements.save();
      this.minecraft.gui.setScreen(this.previous);
      ClickGui.open();
   }

   public void testResize() {
      for(DraggableHud hud : HudElements.all()) {
         if (shown(hud) && hud.canScaleHud()) {
            float[] b = (float[])hud.getHudBounds().clone();
            float[][] c = corners(b);
            double px = (double)c[2][0];
            double py = (double)c[2][1];
            MouseButtonEvent press = new MouseButtonEvent(px, py, new MouseButtonInfo(0, 0));
            this.mouseClicked(press, false);
            MouseButtonEvent drag = new MouseButtonEvent(px + (double)b[2] * 0.4, py + (double)b[3] * 0.4, new MouseButtonInfo(0, 0));
            float before = b[2];
            this.mouseDragged(drag, (double)0.0F, (double)0.0F);
            float[] after = hud.getHudBounds();
            PrintStream var10000 = System.out;
            String var10001 = hud.getHudName();
            var10000.println("[UT] hud resize " + var10001 + " scale=" + hud.getHudScale() + " width " + before + " -> " + (after == null ? -1.0F : after[2]) + (hud.getHudScale() > 1.05F ? " PASS" : " FAIL"));
            return;
         }
      }

      System.out.println("[UT] hud resize: no scalable element shown");
   }
}
