package vanta.render.font;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import vanta.VantaCore;
import net.minecraft.resources.Identifier;

public final class Font {
   private final Identifier texture;
   private final int fallbackCodepoint;
   private final Atlas atlas;
   private final Map<Integer, Glyph> glyphs = new HashMap();
   private final Map<Long, Float> kerning = new HashMap();
   private static final int TABLE = 1024;
   private static final int PAIRS = 256;
   private final Glyph[] table = new Glyph[1024];
   private final Map<Glyph, Bounds> uvs = new IdentityHashMap();
   private final float[] pairs = new float[65536];
   private Glyph fallback;

   private Font(Metrics metrics, Identifier texture, int fallbackCodepoint) {
      this.texture = texture;
      this.fallbackCodepoint = fallbackCodepoint;
      this.atlas = metrics.atlas;
      if ("msdf".equals(this.atlas.type) && "top".equals(this.atlas.yOrigin)) {
         for(Glyph glyph : metrics.glyphs) {
            this.glyphs.put(glyph.unicode, glyph);
         }

         if (!this.glyphs.containsKey(fallbackCodepoint)) {
            throw new IllegalStateException("Missing fallback glyph in " + String.valueOf(texture));
         } else {
            this.fallback = (Glyph)this.glyphs.get(fallbackCodepoint);

            for(int c = 0; c < 1024; ++c) {
               this.table[c] = (Glyph)this.glyphs.getOrDefault(c, this.fallback);
            }

            if (metrics.kerning != null) {
               for(KerningPair pair : metrics.kerning) {
                  this.kerning.put(pairKey(pair.unicode1, pair.unicode2), pair.advance);
                  if (pair.unicode1 >= 0 && pair.unicode1 < 256 && pair.unicode2 >= 0 && pair.unicode2 < 256) {
                     this.pairs[pair.unicode1 * 256 + pair.unicode2] = pair.advance;
                  }
               }
            }

         }
      } else {
         throw new IllegalStateException("Expected a top-origin MSDF font atlas for " + String.valueOf(texture));
      }
   }

   public static Font load(String name, int fallbackCodepoint) {
      String path = "/assets/vanta/" + name + ".json";
      Identifier texture = VantaCore.id(name + ".png");

      try {
         InputStream stream = Font.class.getResourceAsStream(path);

         Font var6;
         try {
            if (stream == null) {
               throw new IllegalStateException("Missing font metrics: " + path);
            }

            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);

            try {
               var6 = new Font((Metrics)(new Gson()).fromJson(reader, Metrics.class), texture, fallbackCodepoint);
            } catch (Throwable var10) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }

               throw var10;
            }

            reader.close();
         } catch (Throwable var11) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (stream != null) {
            stream.close();
         }

         return var6;
      } catch (Exception exception) {
         throw new IllegalStateException("Cannot load font metrics: " + path, exception);
      }
   }

   public Identifier texture() {
      return this.texture;
   }

   public float distanceRange() {
      return this.atlas.distanceRange;
   }

   public Glyph glyph(int codepoint) {
      return codepoint >= 0 && codepoint < 1024 ? this.table[codepoint] : (Glyph)this.glyphs.getOrDefault(codepoint, this.fallback);
   }

   public float kerning(int previous, int current) {
      if (previous >= 0 && previous < 256 && current >= 0 && current < 256) {
         return this.pairs[previous * 256 + current];
      } else {
         return previous < 0 ? 0.0F : (Float)this.kerning.getOrDefault(pairKey(previous, current), 0.0F);
      }
   }

   public Bounds uv(Glyph glyph) {
      Bounds cached = (Bounds)this.uvs.get(glyph);
      if (cached == null) {
         Bounds area = glyph.atlasBounds();
         cached = new Bounds(area.left() / (float)this.atlas.width, area.top() / (float)this.atlas.height, area.right() / (float)this.atlas.width, area.bottom() / (float)this.atlas.height);
         this.uvs.put(glyph, cached);
      }

      return cached;
   }

   private static long pairKey(int first, int second) {
      return (long)first << 32 | (long)second & 4294967295L;
   }

   private static record Atlas(String type, float distanceRange, int width, int height, String yOrigin) {
   }

   public static record Bounds(float left, float top, float right, float bottom) {
   }

   public static record Glyph(int unicode, float advance, Bounds planeBounds, Bounds atlasBounds) {
   }

   private static record KerningPair(int unicode1, int unicode2, float advance) {
   }

   private static record Metrics(Atlas atlas, Glyph[] glyphs, KerningPair[] kerning) {
   }
}
