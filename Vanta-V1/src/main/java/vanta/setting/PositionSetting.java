package vanta.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import vanta.ui.UiScale;

public final class PositionSetting extends Setting<Position> {
   public PositionSetting(String id, Position defaultPosition) {
      super(id, defaultPosition);
   }

   public JsonElement save() {
      JsonObject json = new JsonObject();
      json.addProperty("horizontal", ((Position)this.value).horizontal().name().toLowerCase(Locale.ROOT));
      json.addProperty("vertical", ((Position)this.value).vertical().name().toLowerCase(Locale.ROOT));
      json.addProperty("x", ((Position)this.value).offsetX());
      json.addProperty("y", ((Position)this.value).offsetY());
      return json;
   }

   public void load(JsonElement json) {
      if (json != null && json.isJsonObject()) {
         JsonObject object = json.getAsJsonObject();

         try {
            Anchor horizontal = PositionSetting.Anchor.valueOf(object.get("horizontal").getAsString().toUpperCase(Locale.ROOT));
            Anchor vertical = PositionSetting.Anchor.valueOf(object.get("vertical").getAsString().toUpperCase(Locale.ROOT));
            this.value = new Position(horizontal, vertical, object.get("x").getAsFloat(), object.get("y").getAsFloat());
         } catch (RuntimeException var5) {
            this.reset();
         }
      }

   }

   public static enum Anchor {
      START,
      CENTER,
      END;

      // $FF: synthetic method
      private static Anchor[] $values() {
         return new Anchor[]{START, CENTER, END};
      }
   }

   public static record Position(Anchor horizontal, Anchor vertical, float offsetX, float offsetY) {
      public float x(float width, float screenWidth) {
         return along(this.horizontal, this.offsetX * UiScale.fit(), width, screenWidth);
      }

      public float y(float height, float screenHeight) {
         return along(this.vertical, this.offsetY * UiScale.fit(), height, screenHeight);
      }

      public static Position of(float x, float y, float width, float height, float screenWidth, float screenHeight) {
         Anchor horizontal = anchor(x + width * 0.5F, screenWidth);
         Anchor vertical = anchor(y + height * 0.5F, screenHeight);
         float fit = UiScale.fit();
         return new Position(horizontal, vertical, offset(horizontal, x, width, screenWidth) / fit, offset(vertical, y, height, screenHeight) / fit);
      }

      private static float along(Anchor anchor, float offset, float size, float screen) {
         float var10000;
         switch (anchor.ordinal()) {
            case 0 -> var10000 = offset;
            case 1 -> var10000 = (screen - size) * 0.5F + offset;
            case 2 -> var10000 = screen - size - offset;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         float at = var10000;
         return Math.clamp(at, 0.0F, Math.max(0.0F, screen - size));
      }

      private static Anchor anchor(float center, float screen) {
         if (center < screen / 3.0F) {
            return PositionSetting.Anchor.START;
         } else {
            return center < screen * 2.0F / 3.0F ? PositionSetting.Anchor.CENTER : PositionSetting.Anchor.END;
         }
      }

      private static float offset(Anchor anchor, float start, float size, float screen) {
         float var10000;
         switch (anchor.ordinal()) {
            case 0 -> var10000 = start;
            case 1 -> var10000 = start - (screen - size) * 0.5F;
            case 2 -> var10000 = screen - start - size;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }
   }
}
