package vanta.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class GfxTexture {
   private static final AtomicInteger COUNTER = new AtomicInteger();
   private final Identifier id;
   private final boolean linear;
   private final int width;
   private final int height;
   private final boolean owned;
   private NativeImage pixels;
   private DynamicTexture texture;
   private Supplier<GpuTextureView> externalView;
   private Supplier<GpuSampler> externalSampler;
   private volatile boolean dirty;
   private boolean closed;

   private GfxTexture(Identifier id, boolean linear, int width, int height, boolean owned, NativeImage pixels) {
      this.id = id;
      this.linear = linear;
      this.width = width;
      this.height = height;
      this.owned = owned;
      this.pixels = pixels;
      this.dirty = owned;
   }

   public static GfxTexture upload(String name, BufferedImage image, boolean linear) {
      GfxTexture texture = blank(name, image.getWidth(), image.getHeight(), linear);
      texture.write(image, 0, 0, image.getWidth(), image.getHeight());
      return texture;
   }

   public static GfxTexture blank(String name, int width, int height, boolean linear) {
      String var10001 = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
      Identifier id = Identifier.fromNamespaceAndPath("vanta", "dynamic/" + var10001 + "_" + COUNTER.incrementAndGet());
      return new GfxTexture(id, linear, width, height, true, new NativeImage(width, height, true));
   }

   public static GfxTexture external(String name, Supplier<GpuTextureView> view, Supplier<GpuSampler> sampler) {
      GfxTexture texture = new GfxTexture(Identifier.fromNamespaceAndPath("vanta", "external/" + name.toLowerCase(Locale.ROOT)), true, 0, 0, false, (NativeImage)null);
      texture.externalView = view;
      texture.externalSampler = sampler;
      return texture;
   }

   public static GfxTexture of(Identifier id, boolean linear) {
      return new GfxTexture(id, linear, 0, 0, false, (NativeImage)null);
   }

   public synchronized void write(BufferedImage image, int x, int y, int w, int h) {
      if (this.owned && !this.closed && w > 0 && h > 0) {
         int[] argb = image.getRGB(x, y, w, h, (int[])null, 0, w);

         for(int row = 0; row < h; ++row) {
            for(int col = 0; col < w; ++col) {
               this.pixels.setPixel(x + col, y + row, argb[row * w + col]);
            }
         }

         this.dirty = true;
      }
   }

   public Identifier id() {
      return this.id;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   private synchronized AbstractTexture handle() {
      if (!this.owned) {
         return Minecraft.getInstance().getTextureManager().getTexture(this.id);
      } else if (this.closed) {
         throw new IllegalStateException("Texture " + String.valueOf(this.id) + " was closed");
      } else {
         if (this.texture == null) {
            NativeImage copy = new NativeImage(this.width, this.height, false);
            copy.copyFrom(this.pixels);
            Identifier var10003 = this.id;
            Objects.requireNonNull(var10003);
            this.texture = new FilteredTexture(var10003::toString, copy, this.linear);
            Minecraft.getInstance().getTextureManager().register(this.id, this.texture);
            this.dirty = false;
         } else if (this.dirty) {
            this.texture.getPixels().copyFrom(this.pixels);
            this.texture.upload();
            this.dirty = false;
         }

         return this.texture;
      }
   }

   GpuTextureView view() {
      return this.externalView != null ? (GpuTextureView)this.externalView.get() : this.handle().getTextureView();
   }

   GpuSampler sampler() {
      if (this.externalSampler != null) {
         return (GpuSampler)this.externalSampler.get();
      } else {
         return this.owned ? this.handle().getSampler() : RenderSystem.getSamplerCache().getClampToEdge(this.linear ? FilterMode.LINEAR : FilterMode.NEAREST);
      }
   }

   public synchronized void close() {
      if (this.owned && !this.closed) {
         this.closed = true;
         if (this.texture != null) {
            Minecraft.getInstance().getTextureManager().release(this.id);
            this.texture = null;
         }

         this.pixels.close();
      }
   }

   private static final class FilteredTexture extends DynamicTexture {
      FilteredTexture(Supplier<String> label, NativeImage image, boolean linear) {
         super(label, image);
         this.sampler = RenderSystem.getSamplerCache().getClampToEdge(linear ? FilterMode.LINEAR : FilterMode.NEAREST);
      }
   }
}
