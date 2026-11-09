package vanta.module.impl.hud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vanta.VantaClient;
import vanta.event.events.HudRenderEvent;
import vanta.event.events.ModuleToggleEvent;
import vanta.module.HudModule;
import vanta.module.Module;
import vanta.render.Gfx;
import vanta.setting.BoolSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.PositionSetting;
import vanta.setting.SliderSetting;
import vanta.ui.UiScale;
import vanta.visual.Fx;
import vanta.visual.hud.HudElements;
import vanta.visual.hud.HudKit;
import vanta.visual.hud.HudLook;
import vanta.visual.hud.HudSlot;
import net.minecraft.client.Minecraft;

public final class Notifications extends HudModule {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final long SLIDE_OUT_MS = 260L;
   private static final long TOGGLE_MS = 2000L;
   private static final long MESSAGE_MS = 3000L;
   private static final float GAP = 6.0F;
   private static final float HEIGHT = 28.0F;
   private static final float COMPACT_HEIGHT = 18.0F;
   private static final int ENTER_DROP = 1;
   private static final int ENTER_FADE = 2;
   private static final int ENTER_POP = 3;
   private static final int ICON_DOT = 0;
   private static final int ICON_SYMBOL = 1;
   private static final int ICON_STRIPE = 2;
   private static final int BAR_TOP = 1;
   private static final int BAR_EDGE = 2;
   private static final int BAR_NONE = 3;
   private static final int GREEN = -11870592;
   private static final PositionSetting.Position HOME;
   private static final List<Entry> ENTRIES;
   private static final Map<String, Long> COOLDOWNS;
   private final SliderSetting maxNotifications = (SliderSetting)this.add(new SliderSetting("maxCards", 4, 1, 8));
   private final EnumSetting stack = (EnumSetting)this.add(new EnumSetting("stack", 0, new String[]{"Auto", "Up", "Down"}));
   private final EnumSetting entrance = (EnumSetting)this.add(new EnumSetting("entrance", 0, new String[]{"Slide", "Drop", "Fade", "Pop"}));
   private final SliderSetting duration;
   private final BoolSetting compact;
   private final EnumSetting widthMode;
   private final SliderSetting cardWidth;
   private final EnumSetting icon;
   private final EnumSetting statusColors;
   private final EnumSetting timeBar;
   private final SliderSetting textSize;
   private final SliderSetting spacing;
   private final SliderSetting scale;
   private final BoolSetting showToggles;
   private final BoolSetting showMessages;
   private final BoolSetting showErrors;
   private final HudLook look;
   private List<Entry> entries;
   private Card[] cards;
   private int cardCount;
   private final float[] keys;
   private final HudKit.Clock clock;

   public Notifications() {
      super("notifications", HOME.horizontal(), HOME.vertical(), HOME.offsetX(), HOME.offsetY());
      this.duration = (SliderSetting)this.add(new SliderSetting("duration", 100, 25, 400, Measure.PERCENT));
      this.compact = (BoolSetting)this.add(new BoolSetting("compact", false));
      this.widthMode = (EnumSetting)this.add(new EnumSetting("width", 0, new String[]{"Fit", "Fixed"}));
      this.cardWidth = (SliderSetting)this.add(new SliderSetting("cardWidth", 116, 80, 320));
      this.icon = (EnumSetting)this.add(new EnumSetting("icon", 0, new String[]{"Dot", "Symbol", "Stripe", "None"}));
      this.statusColors = (EnumSetting)this.add(new EnumSetting("statusColors", 0, new String[]{"Accent", "Traffic"}));
      this.timeBar = (EnumSetting)this.add(new EnumSetting("timeBar", 0, new String[]{"Bottom", "Top", "Edge", "None"}));
      this.textSize = (SliderSetting)this.add(new SliderSetting("textSize", 100, 70, 160, Measure.PERCENT));
      this.spacing = (SliderSetting)this.add(new SliderSetting("spacing", 5, 0, 16));
      this.scale = (SliderSetting)this.add(new SliderSetting("scale", 100, 25, 1000, Measure.PERCENT));
      this.showToggles = (BoolSetting)this.add(new BoolSetting("showToggles", true));
      this.showMessages = (BoolSetting)this.add(new BoolSetting("showMessages", true));
      this.showErrors = (BoolSetting)this.add(new BoolSetting("showErrors", true));
      this.look = (HudLook)this.add(new HudLook());
      this.entries = List.of();
      this.cards = new Card[8];
      this.keys = new float[4];
      this.clock = new HudKit.Clock();
      this.stack.describe();
      this.entrance.describe();
      this.duration.describe();
      this.widthMode.describe();
      this.icon.describe();
      this.statusColors.describe();
      this.timeBar.describe();
      this.setEnabled(true);
   }

   private static Notifications self() {
      return VantaClient.MODULES == null ? null : (Notifications)VantaClient.MODULES.get(Notifications.class);
   }

   public static void post(String title, String detail, Kind kind) {
      Notifications self = self();
      if (self != null && self.isEnabled()) {
         if (kind != Notifications.Kind.ON && kind != Notifications.Kind.OFF) {
            message(title, detail == null ? "" : detail, kind);
         } else {
            toggle(title, kind == Notifications.Kind.ON);
         }

      }
   }

   private static void message(String title, String detail, Kind kind) {
      long now = System.currentTimeMillis();
      String key = title + ": " + detail;
      synchronized(ENTRIES) {
         Long last = (Long)COOLDOWNS.get(key);
         if (last == null || now - last >= 600L) {
            COOLDOWNS.put(key, now);
            ENTRIES.add(new Entry(title, detail, kind, 3000L));
         }
      }
   }

   private static void toggle(String module, boolean enabled) {
      long now = System.currentTimeMillis();
      String key = module + (enabled ? "+" : "-");
      synchronized(ENTRIES) {
         Long last = (Long)COOLDOWNS.get(key);
         if (last == null || now - last >= 150L) {
            COOLDOWNS.put(key, now);
            ENTRIES.removeIf((entry) -> entry.title.equals(module) && toggle(entry.kind));
            ENTRIES.add(new Entry(module, enabled ? "Enabled" : "Disabled", enabled ? Notifications.Kind.ON : Notifications.Kind.OFF, 2000L));
         }
      }
   }

   protected void onEnable() {
      this.listen(ModuleToggleEvent.class, (event) -> {
         Module module = event.module();
         if (module != this && !(Boolean)module.hideNotificationsSetting().get() && mc.player != null) {
            toggle(module.name(), event.enabled());
         }

      });
      this.listen(HudRenderEvent.class, this::onHudRender);
   }

   protected void onDisable() {
      synchronized(ENTRIES) {
         ENTRIES.clear();
         COOLDOWNS.clear();
      }
   }

   public static String debug() {
      Notifications self = self();
      synchronized(ENTRIES) {
         boolean var10000 = self.isEnabled();
         return "enabled=" + var10000 + " entries=" + ENTRIES.size() + " cards=" + self.cardCount + " at " + self.x() + "," + self.y() + " " + self.width() + "x" + self.height() + " entrance=" + self.entrance.selected();
      }
   }

   private long lifetime(Entry entry) {
      return entry.duration * (long)(Integer)this.duration.get() / 100L;
   }

   private List<Entry> entries(long now) {
      List<Entry> active;
      synchronized(ENTRIES) {
         ENTRIES.removeIf((entry) -> now - entry.start >= this.lifetime(entry));
         COOLDOWNS.values().removeIf((at) -> now - at > 1000L);
         active = ENTRIES.size() == this.entries.size() && ENTRIES.equals(this.entries) ? this.entries : List.copyOf(ENTRIES);
      }

      if (active != this.entries) {
         this.entries = active;
         int kept = 0;

         for(int i = 0; i < this.cardCount; ++i) {
            Card card = this.cards[i];
            if (active.contains(card.entry)) {
               this.cards[i] = this.cards[kept];
               this.cards[kept] = card;
               ++kept;
            } else {
               card.entry = null;
            }
         }

         this.cardCount = kept;
      }

      return this.entries;
   }

   private static boolean toggle(Kind kind) {
      return kind == Notifications.Kind.ON || kind == Notifications.Kind.OFF;
   }

   private Card card(Entry entry) {
      for(int i = 0; i < this.cardCount; ++i) {
         if (this.cards[i].entry == entry) {
            return this.cards[i];
         }
      }

      if (this.cardCount == this.cards.length) {
         this.cards = (Card[])Arrays.copyOf(this.cards, this.cardCount * 2);
      }

      Card card = this.cards[this.cardCount];
      if (card == null) {
         card = new Card();
         this.cards[this.cardCount] = card;
      }

      ++this.cardCount;
      card.entry = entry;
      card.slide.snap(0.0F);
      card.placed = false;
      card.fitRoom = -1.0F;
      return card;
   }

   private boolean wanted(Kind kind) {
      boolean var10000;
      switch (kind.ordinal()) {
         case 0:
         case 1:
            var10000 = (Boolean)this.showToggles.get();
            break;
         case 2:
         default:
            var10000 = (Boolean)this.showMessages.get();
            break;
         case 3:
            var10000 = (Boolean)this.showErrors.get();
      }

      return var10000;
   }

   private void onHudRender(HudRenderEvent event) {
      if (!mc.debugEntries.isOverlayVisible() && mc.player != null) {
         float dt = this.clock.tick();
         long now = System.currentTimeMillis();
         List<Entry> entries = this.entries(now);
         HudKit.sync(this.look.look());
         float scale = UiScale.current().factor() * (float)(Integer)this.scale.get() / 100.0F;
         int screenWidth = mc.getWindow().getGuiScaledWidth();
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         boolean compact = (Boolean)this.compact.get();
         float margin = 6.0F * scale;
         float gap = (float)(Integer)this.spacing.get() * scale;
         float textScale = (float)(Integer)this.textSize.get() / 100.0F;
         float titleSize = 7.5F * scale * textScale;
         float detailSize = 6.5F * scale * textScale;
         float height = compact ? (float)Math.round(Math.max(18.0F * scale, titleSize * 2.4F)) : (float)Math.round(Math.max(28.0F * scale, (titleSize + detailSize) * 1.95F));
         boolean fixed = (Integer)this.widthMode.get() == 1;
         float minWidth = (float)(Integer)this.cardWidth.get() * scale;
         float areaWidth = (float)Math.round(minWidth);
         float areaX = this.left(areaWidth, (float)screenWidth);
         float areaY = this.top(height, (float)screenHeight);
         if (HOME.equals(this.position())) {
            Keystrokes keystrokes = Keystrokes.get();
            if (keystrokes != null && keystrokes.defaultBox(this.keys) && this.keys[0] < areaX + areaWidth && areaX < this.keys[0] + this.keys[2]) {
               areaY = Math.min(areaY, this.keys[1] - 6.0F - height);
            }
         }

         this.placed(areaX, areaY, areaWidth, height);
         boolean placeholder = entries.isEmpty() && HudElements.editing();
         if (!entries.isEmpty() || placeholder) {
            int stack = (Integer)this.stack.get();
            boolean down = stack == 0 ? HudSlot.growsDown(areaY, height, (float)screenHeight) : stack == 2;
            boolean right = HudSlot.growsLeft(areaX, areaWidth, (float)screenWidth);
            float first = areaY;
            float radius = 8.05F * scale;
            int icon = (Integer)this.icon.get();
            int entrance = (Integer)this.entrance.get();
            int bar = (Integer)this.timeBar.get();
            float iconX = (compact ? 11.0F : 14.0F) * scale;
            float var10000;
            switch (icon) {
               case 0:
               case 1:
                  var10000 = (compact ? 20.0F : 26.0F) * scale;
                  break;
               case 2:
                  var10000 = 13.0F * scale;
                  break;
               default:
                  var10000 = 10.0F * scale;
            }

            float textX = var10000;
            float endPad = (bar == 2 ? 16.0F : 12.0F) * scale;
            Gfx.begin(event.graphics());

            try {
               if (placeholder) {
                  float w = (float)Math.round(minWidth);
                  float x = right ? (float)Math.round(areaX + areaWidth - w) : (float)Math.round(areaX);
                  float top = (float)Math.round(first);
                  HudKit.raised(x, top, w, height, radius, scale, 1.0F);
                  HudKit.text("Notifications", x + textX, top + height * 0.5F, titleSize, HudKit.FAINT, true);
               }

               for(int pass = 0; pass < 2; ++pass) {
                  int rendered = 0;
                  float place = 0.0F;

                  for(int i = entries.size() - 1; i >= 0 && rendered < (Integer)this.maxNotifications.get(); --i) {
                     Entry entry = (Entry)entries.get(i);
                     long life = this.lifetime(entry);
                     long left = entry.start + life - now;
                     if (left > 0L && this.wanted(entry.kind)) {
                        Card card = this.card(entry);
                        HudKit.Spring slide = card.slide;
                        if (pass == 0) {
                           boolean leaving = left < 260L;
                           if (leaving) {
                              slide.value = Math.min(slide.value, Fx.smooth((float)left / 260.0F));
                              slide.velocity = 0.0F;
                           } else {
                              HudKit.lively(slide, 1.0F, dt);
                           }

                           if (!card.placed) {
                              card.placed = true;
                              card.place.snap(place);
                           } else {
                              HudKit.smooth(card.place, place, dt);
                           }
                        }

                        float in = slide.value;
                        String title = entry.title;
                        String detail = entry.detail == null ? "" : entry.detail;
                        float w;
                        if (fixed) {
                           w = (float)Math.round(minWidth);
                           this.fit(card, title, detail, w - textX - endPad, titleSize, detailSize, compact, scale);
                           title = card.title;
                           detail = card.detail;
                        } else {
                           float textW = compact ? HudKit.width(title, titleSize) + (detail.isEmpty() ? 0.0F : 5.0F * scale + HudKit.width(detail, detailSize, true)) : Math.max(HudKit.width(title, titleSize), HudKit.width(detail, detailSize, true));
                           w = (float)Math.round(Math.max(minWidth, textX + textW + endPad));
                        }

                        float x = right ? areaX + areaWidth - w : areaX;
                        float top = (float)Math.round(first + (down ? card.place.value : -card.place.value));
                        float pop = 1.0F;
                        float alpha;
                        switch (entrance) {
                           case 1:
                              top += (float)Math.round((1.0F - in) * height * 0.8F * (down ? -1.0F : 1.0F));
                              alpha = Math.clamp(in * 1.4F, 0.0F, 1.0F);
                              break;
                           case 2:
                              alpha = Fx.smooth(Math.clamp(in, 0.0F, 1.0F));
                              break;
                           case 3:
                              pop = 0.82F + 0.18F * in;
                              alpha = Math.clamp(in * 1.6F, 0.0F, 1.0F);
                              break;
                           default:
                              float slideOut = (1.0F - in) * (w + margin);
                              x += right ? slideOut : -slideOut;
                              alpha = Math.clamp(in * 1.4F, 0.0F, 1.0F);
                        }

                        int status = this.statusColor(entry.kind);
                        boolean scaled = pop != 1.0F;
                        if (scaled) {
                           Gfx.push();
                           Gfx.translate(x + w * 0.5F, top + height * 0.5F);
                           Gfx.scale(pop);
                           Gfx.translate(-(x + w * 0.5F), -(top + height * 0.5F));
                        }

                        if (pass == 0) {
                           HudKit.shadow(x, top, w, height, radius, scale, alpha);
                        } else {
                           HudKit.card(x, top, w, height, radius, alpha);
                           float cy = top + height * 0.5F;
                           this.mark(icon, entry.kind, status, x, top, height, x + iconX, cy, compact, scale, alpha);
                           float fraction = Math.clamp((float)left / (float)Math.max(1L, life), 0.0F, 1.0F);
                           this.timeLine(bar, status, fraction, x, top, w, height, radius, scale, alpha);
                           if (compact) {
                              HudKit.text(title, x + textX, cy, titleSize, Fx.alpha(HudKit.TEXT, alpha));
                              if (!detail.isEmpty()) {
                                 HudKit.text(detail, x + textX + HudKit.width(title, titleSize) + 5.0F * scale, cy, detailSize, Fx.alpha(HudKit.MUTED, alpha), true);
                              }
                           } else {
                              HudKit.text(title, x + textX, top + height * 0.36F, titleSize, Fx.alpha(HudKit.TEXT, alpha));
                              HudKit.text(detail, x + textX, top + height * 0.64F, detailSize, Fx.alpha(HudKit.MUTED, alpha), true);
                           }
                        }

                        if (scaled) {
                           Gfx.pop();
                        }

                        place += (height + gap) * Math.min(1.0F, in);
                        ++rendered;
                     }
                  }
               }

               HudKit.flushText();
            } finally {
               HudKit.discardText();
               Gfx.end();
            }

         }
      }
   }

   private void fit(Card card, String title, String detail, float room, float titleSize, float detailSize, boolean compact, float scale) {
      if (card.fitRoom != room || card.fitSize != titleSize || card.fitCompact != compact) {
         card.fitRoom = room;
         card.fitSize = titleSize;
         card.fitCompact = compact;
         card.title = HudKit.fit(title, room, titleSize, false);
         if (compact) {
            float rest = room - HudKit.width(card.title, titleSize) - 5.0F * scale;
            card.detail = !detail.isEmpty() && !(rest < HudKit.width("…", detailSize, true) * 2.0F) ? HudKit.fit(detail, rest, detailSize, true) : "";
         } else {
            card.detail = HudKit.fit(detail, room, detailSize, true);
         }

      }
   }

   private void mark(int icon, Kind kind, int status, float x, float top, float height, float cx, float cy, boolean compact, float scale, float alpha) {
      float k = compact ? 0.75F : 1.0F;
      switch (icon) {
         case 0:
            disc(cx, cy, 6.0F * scale * k, Fx.alpha(status, 0.18F * alpha));
            disc(cx, cy, 2.6F * scale * k, Fx.alpha(status, alpha));
            break;
         case 1:
            disc(cx, cy, 6.0F * scale * k, Fx.alpha(status, 0.18F * alpha));
            symbol(kind, cx, cy, 2.6F * scale * k, Fx.alpha(status, alpha));
            break;
         case 2:
            float p = Gfx.pixel();
            float inset = (compact ? 4.0F : 6.0F) * scale;
            float sw = Math.max(p, (float)Math.round(2.5F * scale / p) * p);
            float sx = (float)Math.round((x + 4.5F * scale) / p) * p;
            float sy = (float)Math.round((top + inset) / p) * p;
            float sh = (float)Math.round((height - inset * 2.0F) / p) * p;
            HudKit.fillV(sx, sy, sw, sh, sw * 0.5F, Fx.alpha(HudKit.brighter(status), alpha), Fx.alpha(HudKit.deeper(status), alpha));
      }

   }

   private static void disc(float cx, float cy, float r, int color) {
      HudKit.Look look = HudKit.look();
      if (look.shape == 0 && look.corners >= 1.0F) {
         Fx.dot(cx, cy, r, color);
      } else {
         HudKit.fill(cx - r, cy - r, r * 2.0F, r * 2.0F, r, color);
      }

   }

   private static void symbol(Kind kind, float cx, float cy, float r, int color) {
      float w = Math.max(Gfx.pixel(), r * 0.5F);
      switch (kind.ordinal()) {
         case 0:
            Gfx.line(cx - r, cy + r * 0.05F, cx - r * 0.3F, cy + r * 0.72F, w, color);
            Gfx.line(cx - r * 0.3F, cy + r * 0.72F, cx + r, cy - r * 0.68F, w, color);
            break;
         case 1:
            float d = r * 0.78F;
            Gfx.line(cx - d, cy - d, cx + d, cy + d, w, color);
            Gfx.line(cx - d, cy + d, cx + d, cy - d, w, color);
            break;
         default:
            boolean error = kind == Notifications.Kind.ERROR;
            float p = Gfx.pixel();
            float sw = Math.max(p, (float)Math.round(w / p) * p);
            float sx = (float)Math.round((cx - sw * 0.5F) / p) * p;
            float stemTop = error ? cy - r : cy - r * 0.25F;
            float stemBottom = error ? cy + r * 0.3F : cy + r;
            float dotY = error ? cy + r * 0.62F : cy - r * 0.95F;
            HudKit.fill(sx, (float)Math.round(stemTop / p) * p, sw, (float)Math.round((stemBottom - stemTop) / p) * p, sw * 0.5F, color);
            HudKit.fill(sx, (float)Math.round(dotY / p) * p, sw, sw, sw * 0.5F, color);
      }

   }

   private void timeLine(int bar, int status, float fraction, float x, float top, float w, float height, float radius, float scale, float alpha) {
      if (bar != 3) {
         float lineH = Math.max(1.0F, 1.5F * scale);
         int from = Fx.alpha(HudKit.deeper(status), 0.85F * alpha);
         int to = Fx.alpha(HudKit.brighter(status), 0.85F * alpha);
         int track = Fx.alpha(HudKit.TRACK, alpha * 0.6F);
         if (bar == 2) {
            float inset = (height < 28.0F * scale ? 3.5F : 5.0F) * scale;
            float lx = x + w - lineH - 3.0F * scale;
            float lineLength = height - inset * 2.0F;
            HudKit.fill(lx, top + inset, lineH, lineLength, lineH * 0.5F, track);
            float filled = Math.max(lineH, lineLength * fraction);
            HudKit.fillV(lx, top + height - inset - filled, lineH, filled, lineH * 0.5F, to, from);
         } else {
            float inset = Math.max(4.0F * scale, HudKit.corner(radius));
            float lineX = x + inset;
            float lineW = w - inset * 2.0F;
            float lineY = bar == 1 ? top + 2.0F * scale : top + height - lineH - 2.0F * scale;
            HudKit.fill(lineX, lineY, lineW, lineH, lineH * 0.5F, track);
            HudKit.fillH(lineX, lineY, Math.max(lineH, lineW * fraction), lineH, lineH * 0.5F, from, to);
         }
      }
   }

   private int statusColor(Kind kind) {
      if (kind != Notifications.Kind.OFF && kind != Notifications.Kind.ERROR) {
         return (Integer)this.statusColors.get() == 1 && kind == Notifications.Kind.ON ? -11870592 : HudKit.ACCENT;
      } else {
         return -45730;
      }
   }

   static {
      HOME = new PositionSetting.Position(PositionSetting.Anchor.END, PositionSetting.Anchor.END, 6.0F, 6.0F);
      ENTRIES = new ArrayList();
      COOLDOWNS = new HashMap();
   }

   public static enum Kind {
      ON,
      OFF,
      INFO,
      ERROR;

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{ON, OFF, INFO, ERROR};
      }
   }

   private static final class Entry {
      final String title;
      final String detail;
      final Kind kind;
      final long start = System.currentTimeMillis();
      final long duration;

      Entry(String title, String detail, Kind kind, long duration) {
         this.title = title;
         this.detail = detail;
         this.kind = kind;
         this.duration = duration;
      }
   }

   private static final class Card {
      Entry entry;
      final HudKit.Spring slide = new HudKit.Spring(0.0F);
      final HudKit.Spring place = new HudKit.Spring(0.0F);
      boolean placed;
      float fitRoom = -1.0F;
      float fitSize;
      boolean fitCompact;
      String title;
      String detail;
   }
}
