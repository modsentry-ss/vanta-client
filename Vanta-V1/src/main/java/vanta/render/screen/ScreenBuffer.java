package vanta.render.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vanta.render.Point;
import vanta.render.Rect;
import vanta.render.Style;

public final class ScreenBuffer {
   private final List<Shape> shapes = new ArrayList();
   private final List<Line> lines = new ArrayList();
   private final List<Quad> quads = new ArrayList();

   public void rect(Rect bounds, Style style) {
      this.shapes.add(new Shape(bounds, style));
   }

   public void line(float x0, float y0, float x1, float y1, int color, float width) {
      this.lines.add(new Line(x0, y0, x1, y1, color, width));
   }

   public void line(Point from, Point to, int color, float width) {
      this.line(from.x(), from.y(), to.x(), to.y(), color, width);
   }

   public void quad(Point a, Point b, Point c, Point d, int color) {
      this.quads.add(new Quad(a, b, c, d, color));
   }

   public List<Shape> shapes() {
      return Collections.unmodifiableList(this.shapes);
   }

   public List<Line> lines() {
      return Collections.unmodifiableList(this.lines);
   }

   public List<Quad> quads() {
      return Collections.unmodifiableList(this.quads);
   }

   public boolean isEmpty() {
      return this.shapes.isEmpty() && this.lines.isEmpty() && this.quads.isEmpty();
   }

   public static record Line(float x0, float y0, float x1, float y1, int color, float width) {
   }

   public static record Quad(Point a, Point b, Point c, Point d, int color) {
   }

   public static record Shape(Rect bounds, Style style) {
   }
}
