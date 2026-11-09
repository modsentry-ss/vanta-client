package vanta.visual.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import vanta.ui.DraggableHud;
import vanta.ui.UiScale;
import net.minecraft.client.Minecraft;

public final class HudSlot implements DraggableHud {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final Map<String, HudSlot> SLOTS = new LinkedHashMap();
   private static JsonObject saved;
   private final String id;
   private final String name;
   private final Edge defaultH;
   private final Edge defaultV;
   private final float defaultX;
   private final float defaultY;
   private final BooleanSupplier enabled;
   private Edge h;
   private Edge v;
   private float offsetX;
   private float offsetY;
   private float scale = 1.0F;
   private final boolean scalable;
   private final boolean listed;
   private boolean stored;
   private float[] bounds;

   private HudSlot(String id, String name, Edge h, Edge v, float offsetX, float offsetY, BooleanSupplier enabled, boolean scalable) {
      this.scalable = scalable;
      this.listed = scalable;
      this.id = id;
      this.name = name;
      this.defaultH = h;
      this.defaultV = v;
      this.defaultX = offsetX;
      this.defaultY = offsetY;
      this.enabled = enabled;
      this.resetHudPosition();
      this.load();
   }

   public static synchronized HudSlot of(String id, String name, Edge h, Edge v, float offsetX, float offsetY, BooleanSupplier enabled) {
      return (HudSlot)SLOTS.computeIfAbsent(id, (key) -> new HudSlot(id, name, h, v, offsetX, offsetY, enabled, true));
   }

   public static synchronized HudSlot owned(String id, String name, Edge h, Edge v, float offsetX, float offsetY, BooleanSupplier enabled) {
      return (HudSlot)SLOTS.computeIfAbsent(id, (key) -> new HudSlot(id, name, h, v, offsetX, offsetY, enabled, false));
   }

   public boolean saved() {
      return this.stored;
   }

   public void set(Edge h, Edge v, float offsetX, float offsetY) {
      this.stored = true;
      this.h = h;
      this.v = v;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
   }

   public float scale() {
      return this.scale;
   }

   public static synchronized Collection<HudSlot> all() {
      ArrayList<HudSlot> listed = new ArrayList();

      for(HudSlot slot : SLOTS.values()) {
         if (slot.listed) {
            listed.add(slot);
         }
      }

      return listed;
   }

   public float x(float width, float screenWidth) {
      float var10000;
      switch (this.h.ordinal()) {
         case 0 -> var10000 = this.offsetX * fit();
         case 1 -> var10000 = (screenWidth - width) * 0.5F + this.offsetX * fit();
         case 2 -> var10000 = screenWidth - width - this.offsetX * fit();
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      float x = var10000;
      return Math.clamp(x, 0.0F, Math.max(0.0F, screenWidth - width));
   }

   public float y(float height, float screenHeight) {
      float var10000;
      switch (this.v.ordinal()) {
         case 0 -> var10000 = this.offsetY * fit();
         case 1 -> var10000 = (screenHeight - height) * 0.5F + this.offsetY * fit();
         case 2 -> var10000 = screenHeight - height - this.offsetY * fit();
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      float y = var10000;
      return Math.clamp(y, 0.0F, Math.max(0.0F, screenHeight - height));
   }

   public boolean isDefault() {
      return this.h == this.defaultH && this.v == this.defaultV && this.offsetX == this.defaultX && this.offsetY == this.defaultY && this.scale == 1.0F;
   }

   public boolean fromTop() {
      return this.v == HudSlot.Edge.START;
   }

   public boolean fromRight() {
      return this.h == HudSlot.Edge.END;
   }

   public static boolean growsDown(float y, float height, float screenHeight) {
      return y + height * 0.5F < screenHeight * 0.5F;
   }

   public static boolean growsLeft(float x, float width, float screenWidth) {
      return x + width * 0.5F > screenWidth * 0.5F;
   }

   private static float fit() {
      return UiScale.fit();
   }

   public void placed(float x, float y, float width, float height) {
      float[] bounds = this.bounds;
      if (bounds == null) {
         bounds = new float[4];
         this.bounds = bounds;
      }

      bounds[0] = x;
      bounds[1] = y;
      bounds[2] = width;
      bounds[3] = height;
   }

   public String getHudName() {
      return this.name;
   }

   public float[] getHudBounds() {
      return this.bounds;
   }

   public void setHudPosition(float x, float y) {
      if (this.bounds != null) {
         Minecraft mc = Minecraft.getInstance();
         float sw = (float)mc.getWindow().getGuiScaledWidth();
         float sh = (float)mc.getWindow().getGuiScaledHeight();
         float w = this.bounds[2];
         float hgt = this.bounds[3];
         float cx = x + w * 0.5F;
         float cy = y + hgt * 0.5F;
         this.h = cx < sw / 3.0F ? HudSlot.Edge.START : (cx > sw * 2.0F / 3.0F ? HudSlot.Edge.END : HudSlot.Edge.CENTER);
         this.v = cy < sh / 3.0F ? HudSlot.Edge.START : (cy > sh * 2.0F / 3.0F ? HudSlot.Edge.END : HudSlot.Edge.CENTER);
         float var10001;
         switch (this.h.ordinal()) {
            case 0 -> var10001 = x;
            case 1 -> var10001 = x - (sw - w) * 0.5F;
            case 2 -> var10001 = sw - w - x;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         this.offsetX = var10001 / fit();
         switch (this.v.ordinal()) {
            case 0 -> var10001 = y;
            case 1 -> var10001 = y - (sh - hgt) * 0.5F;
            case 2 -> var10001 = sh - hgt - y;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         this.offsetY = var10001 / fit();
         this.placed(x, y, w, hgt);
      }
   }

   public boolean canScaleHud() {
      return this.scalable;
   }

   public float getHudScale() {
      return this.scale;
   }

   public void setHudScale(float scale) {
      this.scale = Math.max(0.25F, Math.min(10.0F, scale));
   }

   public void resetHudPosition() {
      this.scale = 1.0F;
      this.h = this.defaultH;
      this.v = this.defaultV;
      this.offsetX = this.defaultX;
      this.offsetY = this.defaultY;
   }

   public boolean isHudEnabled() {
      return this.enabled.getAsBoolean();
   }

   private static Path file() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("VantaClient").resolve("hud-slots.json");
   }

   private static synchronized void loadSaved() {
      if (saved == null) {
         saved = new JsonObject();
         Path file = file();
         if (Files.exists(file, new LinkOption[0])) {
            try {
               Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);

               try {
                  JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
                  if (json != null) {
                     saved = json;
                  }
               } catch (Throwable var5) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                     }
                  }

                  throw var5;
               }

               if (reader != null) {
                  reader.close();
               }
            } catch (Exception error) {
               System.err.println("[Vanta V1] Could not read HUD positions: " + String.valueOf(error));
            }
         }

      }
   }

   public static synchronized boolean grid() {
      loadSaved();
      JsonElement grid = saved.get("editorGrid");
      return grid == null || !grid.isJsonPrimitive() || grid.getAsBoolean();
   }

   public static synchronized void grid(boolean on) {
      loadSaved();
      saved.addProperty("editorGrid", on);
      saveAll();
   }

   private void load() {
      try {
         loadSaved();
         JsonObject entry = saved.getAsJsonObject(this.id);
         if (entry != null) {
            this.stored = true;
            this.h = HudSlot.Edge.valueOf(entry.get("h").getAsString());
            this.v = HudSlot.Edge.valueOf(entry.get("v").getAsString());
            this.offsetX = entry.get("x").getAsFloat();
            this.offsetY = entry.get("y").getAsFloat();
            if (entry.has("s")) {
               this.scale = Math.max(0.25F, Math.min(10.0F, entry.get("s").getAsFloat()));
            }
         }
      } catch (Exception var2) {
         this.resetHudPosition();
      }

   }

   public static synchronized void saveAll() {
      try {
         JsonObject json = saved == null ? new JsonObject() : saved;

         for(HudSlot slot : SLOTS.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("h", slot.h.name());
            entry.addProperty("v", slot.v.name());
            entry.addProperty("x", slot.offsetX);
            entry.addProperty("y", slot.offsetY);
            entry.addProperty("s", slot.scale);
            json.add(slot.id, entry);
         }

         saved = json;
         Path file = file();
         Files.createDirectories(file.getParent());
         Path temp = file.resolveSibling("hud-slots.json.tmp");
         Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8);

         try {
            GSON.toJson(json, writer);
         } catch (Throwable var7) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (writer != null) {
            writer.close();
         }

         Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (Exception error) {
         System.err.println("[Vanta V1] Could not save HUD positions: " + String.valueOf(error));
      }

   }

   public static enum Edge {
      START,
      CENTER,
      END;

      // $FF: synthetic method
      private static Edge[] $values() {
         return new Edge[]{START, CENTER, END};
      }
   }
}
