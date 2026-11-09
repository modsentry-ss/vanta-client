package vanta.render;

import vanta.util.Colors;

public record Style(int fill, int stroke, float strokeWidth, int outline, float outlineWidth, Edges edges) {
   public static final Style EMPTY;

   public static Style fill(int argb) {
      return new Style(argb, 0, 0.0F, 0, 0.0F, Edges.FULL);
   }

   public static Style stroke(int argb, float width) {
      return new Style(0, argb, width, 0, 0.0F, Edges.FULL);
   }

   public Style withFill(int argb) {
      return new Style(argb, this.stroke, this.strokeWidth, this.outline, this.outlineWidth, this.edges);
   }

   public Style withStroke(int argb, float width) {
      return new Style(this.fill, argb, width, this.outline, this.outlineWidth, this.edges);
   }

   public Style withOutline(int argb, float width) {
      return new Style(this.fill, this.stroke, this.strokeWidth, argb, width, this.edges);
   }

   public Style withEdges(Edges style) {
      return new Style(this.fill, this.stroke, this.strokeWidth, this.outline, this.outlineWidth, style);
   }

   public Style scaled(float factor) {
      return new Style(this.fill, this.stroke, this.strokeWidth * factor, this.outline, this.outlineWidth * factor, this.edges);
   }

   public boolean hasFill() {
      return !Colors.transparent(this.fill);
   }

   public boolean hasStroke() {
      return !Colors.transparent(this.stroke) && this.strokeWidth > 0.0F;
   }

   public boolean hasOutline() {
      return this.hasStroke() && !Colors.transparent(this.outline) && this.outlineWidth > this.strokeWidth;
   }

   public float widestStroke() {
      return this.hasOutline() ? this.outlineWidth : (this.hasStroke() ? this.strokeWidth : 0.0F);
   }

   static {
      EMPTY = new Style(0, 0, 0.0F, 0, 0.0F, Edges.FULL);
   }
}
