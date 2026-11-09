package vanta.module.impl.hud;

import com.mojang.blaze3d.platform.Window;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import vanta.event.events.HudRenderEvent;
import vanta.module.HudModule;
import vanta.render.Draw;
import vanta.render.Gfx;
import vanta.render.Text;
import vanta.render.UiIcon;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.MultiSetting;
import vanta.setting.PositionSetting;
import vanta.setting.SliderSetting;
import vanta.setting.TextColorSettings;
import vanta.setting.TextSetting;
import vanta.ui.UiScale;
import vanta.visual.Fx;
import vanta.visual.hud.HudKit;
import vanta.visual.hud.HudLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;

public final class Watermark extends HudModule {
   private static final String NAME = "Vanta V1";
   private static final int PILL = 0;
   private static final int CARD = 1;
   private static final int TEXT_ONLY = 2;
   private static final int BADGE = 3;
   private static final int LOGO_SNOW = 0;
   private static final int LOGO_LETTER = 1;
   private static final int SEP_BAR = 0;
   private static final int SEP_DOT = 1;
   private static final int SEP_SLASH = 2;
   private static final int SEP_SPACE = 3;
   private static final String[] INFO = new String[]{"FPS", "Ping", "Time", "Server", "Username", "Version", "Coordinates"};
   private static final int FPS = 0;
   private static final int PING = 1;
   private static final int TIME = 2;
   private static final int SERVER = 3;
   private static final int USERNAME = 4;
   private static final int VERSION = 5;
   private static final float MARK = 1.1111112F;
   private static final float MARK_GAP = 4.0F;
   private static final float INFO_SIZE = 0.82F;
   private static final float CARD_INFO_SIZE = 0.78F;
   private final SliderSetting size;
   private final TextSetting text;
   private final EnumSetting textCase;
   private final EnumSetting layout;
   private final EnumSetting logo;
   private final MultiSetting info;
   private final EnumSetting separator;
   private final EnumSetting clock;
   private final EnumSetting infoColor;
   private final EnumSetting align;
   private final SliderSetting textSize;
   private final SliderSetting height;
   private final SliderSetting padding;
   private final TextColorSettings colors;
   private final HudLook look;
   private String nameFrom;
   private int nameCase;
   private String name;
   private String letter;
   private final String[] infoText;
   private final long[] infoKey;
   private final int[] shown;
   private int shownCount;

   public Watermark() {
      super("watermark", PositionSetting.Anchor.START, PositionSetting.Anchor.START, 4.0F, 4.0F);
      this.size = (SliderSetting)this.add(new SliderSetting("scale", 100, 25, 1000, Measure.PERCENT));
      this.text = (TextSetting)this.add(new TextSetting("text", "Vanta V1", 32));
      this.textCase = (EnumSetting)this.add(new EnumSetting("textCase", 0, new String[]{"As typed", "Upper", "Lower"}));
      this.layout = (EnumSetting)this.add(new EnumSetting("layout", 0, new String[]{"Pill", "Card", "Text", "Badge"}));
      this.logo = (EnumSetting)this.add(new EnumSetting("logo", 0, new String[]{"Vanta V1", "Letter", "None"}));
      this.info = (MultiSetting)this.add(new MultiSetting("info", INFO, new int[0]));
      this.separator = (EnumSetting)this.add(new EnumSetting("separator", 0, new String[]{"Bar", "Dot", "Slash", "Space"}));
      this.clock = (EnumSetting)this.add(new EnumSetting("clock", 0, new String[]{"24 h", "12 h"}));
      this.infoColor = (EnumSetting)this.add(new EnumSetting("infoColor", 0, new String[]{"Muted", "Text", "Accent"}));
      this.align = (EnumSetting)this.add(new EnumSetting("align", 0, new String[]{"Left", "Center", "Right"}));
      this.textSize = (SliderSetting)this.add((new SliderSetting("textSize", 9, 5, 20)).withFormat((value) -> value + " px"));
      this.height = (SliderSetting)this.add((new SliderSetting("height", 18, 10, 40)).withFormat((value) -> value + " px"));
      this.padding = (SliderSetting)this.add((new SliderSetting("padding", 8, 2, 24)).withFormat((value) -> value + " px"));
      this.colors = (TextColorSettings)this.add(new TextColorSettings());
      this.look = (HudLook)this.add(new HudLook(true, false));
      this.nameCase = -1;
      this.name = "Vanta V1";
      this.letter = "Vanta V1".substring(0, 1);
      this.infoText = new String[INFO.length];
      this.infoKey = new long[INFO.length];
      this.shown = new int[INFO.length];
      Arrays.fill(this.infoKey, Long.MIN_VALUE);
      this.separator.visibleWhen(() -> !((Set)this.info.get()).isEmpty());
      this.clock.visibleWhen(() -> this.info.has(2));
      this.infoColor.visibleWhen(() -> !((Set)this.info.get()).isEmpty() || (Integer)this.layout.get() == 1);
      this.align.visibleWhen(() -> (Integer)this.layout.get() == 1);
      this.height.visibleWhen(() -> (Integer)this.layout.get() == 0 || (Integer)this.layout.get() == 3);
      this.padding.visibleWhen(() -> (Integer)this.layout.get() != 2);
      this.text.describe();
      this.layout.describe();
      this.logo.describe();
      this.info.describe();
      this.separator.describe();
      this.align.describe();
   }

   protected void onEnable() {
      this.listen(HudRenderEvent.class, this::onHudRender);
   }

   private String shownName() {
      String typed = (String)this.text.get();
      int textCase = (Integer)this.textCase.get();
      if (typed != this.nameFrom || textCase != this.nameCase) {
         this.nameFrom = typed;
         this.nameCase = textCase;
         String name = typed == null ? "" : typed.strip();
         String var10001;
         switch (textCase) {
            case 1 -> var10001 = name.toUpperCase(Locale.ROOT);
            case 2 -> var10001 = name.toLowerCase(Locale.ROOT);
            default -> var10001 = name;
         }

         this.name = var10001;
         this.letter = name.isEmpty() ? "Vanta V1".substring(0, 1) : name.substring(0, Character.charCount(name.codePointAt(0))).toUpperCase(Locale.ROOT);
      }

      return this.name;
   }

   private void refreshInfo(Minecraft mc) {
      this.shownCount = 0;

      for(int item = 0; item < INFO.length; ++item) {
         if (this.info.has(item)) {
            long key;
            switch (item) {
               case 0 -> key = (long)mc.getFps();
               case 1 -> key = (long)latency(mc);
               case 2 -> key = System.currentTimeMillis() / 60000L * 2L + (long)(Integer)this.clock.get();
               case 3 -> key = mc.isLocalServer() ? -1L : (long)System.identityHashCode(mc.getCurrentServer());
               case 4 -> key = (long)System.identityHashCode(mc.getUser().getName());
               case 5 -> key = 0L;
               default -> key = mc.player == null ? Long.MAX_VALUE : ((long)mc.player.getBlockX() & 2097151L) << 42 | ((long)mc.player.getBlockY() & 2097151L) << 21 | (long)mc.player.getBlockZ() & 2097151L;
            }

            if (key != this.infoKey[item]) {
               this.infoKey[item] = key;
               this.infoText[item] = this.infoText(mc, item);
            }

            if (this.infoText[item] != null) {
               this.shown[this.shownCount++] = item;
            }
         }
      }

   }

   private String infoText(Minecraft mc, int item) {
      String var10000;
      switch (item) {
         case 0:
            var10000 = Fx.number(mc.getFps()) + " fps";
            break;
         case 1:
            int latency = latency(mc);
            var10000 = latency > 0 ? latency + " ms" : null;
            break;
         case 2:
            var10000 = clockText(LocalTime.now(), (Integer)this.clock.get() == 1);
            break;
         case 3:
            if (mc.isLocalServer()) {
               var10000 = "Singleplayer";
            } else {
               ServerData server = mc.getCurrentServer();
               var10000 = server != null && server.ip != null && !server.ip.isEmpty() ? server.ip : "Multiplayer";
            }
            break;
         case 4:
            var10000 = mc.getUser().getName();
            break;
         case 5:
            var10000 = "v1.0";
            break;
         default:
            var10000 = mc.player == null ? null : mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ();
      }

      return var10000;
   }

   private static String clockText(LocalTime time, boolean twelve) {
      int minute = time.getMinute();
      String mm = minute < 10 ? "0" + minute : Integer.toString(minute);
      if (!twelve) {
         int hour = time.getHour();
         String var10000 = hour < 10 ? "0" + hour : Integer.toString(hour);
         return var10000 + ":" + mm;
      } else {
         int hour = time.getHour() % 12;
         return (hour == 0 ? 12 : hour) + ":" + mm + (time.getHour() < 12 ? " AM" : " PM");
      }
   }

   private static int latency(Minecraft mc) {
      if (mc.getConnection() != null && !mc.isLocalServer() && mc.player != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         return info == null ? 0 : info.getLatency();
      } else {
         return 0;
      }
   }

   private void onHudRender(HudRenderEvent event) {
      Minecraft mc = Minecraft.getInstance();
      HudKit.sync(this.look.look());
      this.refreshInfo(mc);
      float s = UiScale.current().factor() * (float)(Integer)this.size.get() / 100.0F;
      String name = this.shownName();
      int logo = (Integer)this.logo.get();
      if (name.isEmpty() && logo == 2) {
         name = "Vanta V1";
      }

      Text.ColorAt color = this.colors.colorAt(s);
      Window window = mc.getWindow();
      Gfx.begin(event.graphics());

      try {
         switch ((Integer)this.layout.get()) {
            case 1 -> this.card(name, logo, color, s, window);
            default -> this.line(name, logo, color, s, window);
         }

         HudKit.flushText();
      } finally {
         HudKit.discardText();
         Gfx.end();
      }

   }

   private void line(String name, int logo, Text.ColorAt color, float s, Window window) {
      int layout = (Integer)this.layout.get();
      float p = Gfx.pixel();
      float nameSize = (float)(Integer)this.textSize.get() * s;
      float infoSize = nameSize * 0.82F;
      boolean hasLogo = logo != 2;
      float mark = hasLogo ? nameSize * 1.1111112F : 0.0F;
      boolean textOnly = layout == 2;
      boolean badge = layout == 3;
      float height = textOnly ? (float)Math.max(1, Math.round(nameSize * 1.5F)) : (float)Math.max(1, Math.round((float)(Integer)this.height.get() * s));
      float padding = textOnly ? 0.0F : (float)(Integer)this.padding.get() * s;
      float inset = badge ? Math.max(p, (float)Math.round(2.0F * s / p) * p) : 0.0F;
      float chipPad = badge ? Math.max(3.0F * s, padding - inset) : 0.0F;
      float markX = textOnly ? 0.0F : (badge ? inset + chipPad - 1.0F * s : padding - 1.5F * s);
      float nameW = HudKit.width(name, nameSize);
      float textX = hasLogo ? markX + mark + (name.isEmpty() ? 0.0F : 4.0F * s) : (badge ? inset + chipPad : padding);
      float nameEnd = textX + nameW;
      boolean items = this.shownCount > 0;
      float sepW = this.separatorWidth(infoSize, s);
      float itemsW = this.itemsWidth(infoSize, sepW);
      float chipW = 0.0F;
      float width;
      float itemsX;
      if (badge) {
         chipW = (float)Math.round((nameEnd + chipPad - inset) / p) * p;
         itemsX = inset + chipW + 7.0F * s;
         width = (float)Math.round(items ? itemsX + itemsW + padding : inset + chipW + inset);
      } else {
         boolean lead = items && !name.isEmpty() || items && hasLogo;
         itemsX = nameEnd + (lead ? sepW : 0.0F);
         width = (float)Math.round(items ? itemsX + itemsW + padding : nameEnd + padding);
      }

      float x = (float)Math.round(this.left(width, (float)window.getGuiScaledWidth()));
      float y = (float)Math.round(this.top(height, (float)window.getGuiScaledHeight()));
      this.placed(x, y, width, height);
      float cy = y + height * 0.5F;
      int markColor = -16777216 | color.at(x + markX);
      int itemColor = this.itemColor();
      boolean drop = textOnly && !HudKit.look().textShadow;
      float dropBy = Math.max(p, (float)Math.round(0.6F * s / p) * p);
      int shadow = HudKit.LIGHT ? -1929379841 : -1946157056;
      if (!textOnly) {
         HudKit.raised(x, y, width, height, height * 0.5F, s, 1.0F);
      }

      int onChip = 0;
      if (badge) {
         float chipH = height - inset * 2.0F;
         float chipX = x + inset;
         HudKit.fillH(chipX, y + inset, chipW, chipH, chipH * 0.5F, -16777216 | color.at(chipX), -16777216 | color.at(chipX + chipW));
         onChip = readableOn(-16777216 | color.at(chipX + chipW * 0.5F));
      }

      if (logo == 1) {
         float side = (float)Math.round(mark / p) * p;
         float tileX = (float)Math.round((x + markX) / p) * p;
         float tileY = (float)Math.round((cy - side * 0.5F) / p) * p;
         if (drop) {
            HudKit.fill(tileX + dropBy, tileY + dropBy, side, side, side * 0.5F, shadow);
         }

         int tile = badge ? onChip : markColor;
         HudKit.fillV(tileX, tileY, side, side, side * 0.5F, HudKit.brighter(tile), HudKit.deeper(tile));
      }

      if (items) {
         float sx = x + itemsX;
         if (!badge && (hasLogo || !name.isEmpty())) {
            this.separatorShape(x + nameEnd + sepW * 0.5F, cy, infoSize, itemColor, drop ? shadow : 0, dropBy, s);
         }

         for(int i = 0; i < this.shownCount; ++i) {
            sx += HudKit.width(this.infoText[this.shown[i]], infoSize, true);
            if (i + 1 < this.shownCount) {
               this.separatorShape(sx + sepW * 0.5F, cy, infoSize, itemColor, drop ? shadow : 0, dropBy, s);
               sx += sepW;
            }
         }
      }

      Gfx.flush();
      if (logo == 0) {
         float my = cy - mark * 0.5F;
         if (drop) {
            Draw.icon(Gfx.graphics(), UiIcon.MACE_SPEAR, x + markX + dropBy, my + dropBy, mark, Fx.alpha(shadow, 0.55F), shadow, shadow);
         }

         int c = badge ? onChip : markColor;
         Draw.icon(Gfx.graphics(), UiIcon.MACE_SPEAR, x + markX, my, mark, Fx.alpha(c, 0.55F), c, Fx.mix(c, -1, 0.35F));
      }

      if (logo == 1) {
         int letterColor = badge ? markColor : readableOn(markColor);
         HudKit.textCentered(this.letter, x + markX + mark * 0.5F, cy, mark * 0.62F, letterColor, false);
      }

      if (!name.isEmpty()) {
         if (drop) {
            HudKit.text(name, x + textX + dropBy * 0.5F, cy + dropBy, nameSize, shadow, false);
         }

         if (badge) {
            HudKit.text(name, x + textX, cy, nameSize, onChip, false);
         } else {
            HudKit.text(name, x + textX, cy, nameSize, color, 1.0F);
         }
      }

      if (items) {
         float sx = x + itemsX;

         for(int i = 0; i < this.shownCount; ++i) {
            String item = this.infoText[this.shown[i]];
            if (drop) {
               HudKit.text(item, sx + dropBy * 0.5F, cy + dropBy, infoSize, shadow, true);
            }

            HudKit.text(item, sx, cy, infoSize, itemColor, true);
            sx += HudKit.width(item, infoSize, true) + sepW;
            if ((Integer)this.separator.get() == 2 && i + 1 < this.shownCount) {
               this.slash(sx - sepW * 0.5F, cy, infoSize, itemColor, drop ? shadow : 0, dropBy);
            }
         }

         if (!badge && (Integer)this.separator.get() == 2 && (hasLogo || !name.isEmpty())) {
            this.slash(x + nameEnd + sepW * 0.5F, cy, infoSize, itemColor, drop ? shadow : 0, dropBy);
         }
      }

   }

   private void card(String name, int logo, Text.ColorAt color, float s, Window window) {
      float nameSize = (float)(Integer)this.textSize.get() * s;
      float infoSize = nameSize * 0.78F;
      float padX = (float)(Integer)this.padding.get() * s;
      float padY = (float)(Integer)this.padding.get() * s * 0.75F;
      float lineGap = 3.0F * s;
      float line1 = padY + nameSize * 0.5F;
      float line2 = line1 + nameSize * 0.5F + lineGap + infoSize * 0.5F;
      float height = (float)Math.round(line2 + infoSize * 0.5F + padY);
      boolean hasLogo = logo != 2;
      float mark = hasLogo ? Math.min(height - padY * 2.0F, nameSize * 2.0F) : 0.0F;
      float textX = hasLogo ? padX + mark + 6.0F * s : padX;
      float sepW = this.separatorWidth(infoSize, s);
      String version = this.shownCount == 0 ? versionText() : null;
      float line2W = version != null ? HudKit.width(version, infoSize, true) : this.itemsWidth(infoSize, sepW);
      float nameW = HudKit.width(name, nameSize);
      float column = Math.max(nameW, line2W);
      float width = (float)Math.round(textX + column + padX);
      float x = (float)Math.round(this.left(width, (float)window.getGuiScaledWidth()));
      float y = (float)Math.round(this.top(height, (float)window.getGuiScaledHeight()));
      this.placed(x, y, width, height);
      float k = (float)(Integer)this.align.get() * 0.5F;
      float nameX = x + textX + (column - nameW) * k;
      float infoX = x + textX + (column - line2W) * k;
      int markColor = -16777216 | color.at(nameX);
      int itemColor = this.itemColor();
      HudKit.raised(x, y, width, height, 7.0F * s, s, 1.0F);
      float p = Gfx.pixel();
      mark = (float)Math.round(mark / p) * p;
      float my = (float)Math.round((y + (height - mark) * 0.5F) / p) * p;
      if (logo == 1) {
         HudKit.fillV((float)Math.round((x + padX) / p) * p, my, mark, mark, 4.5499997F * s, HudKit.brighter(markColor), HudKit.deeper(markColor));
      }

      if (version == null) {
         float sx = infoX;

         for(int i = 0; i + 1 < this.shownCount; ++i) {
            sx += HudKit.width(this.infoText[this.shown[i]], infoSize, true);
            this.separatorShape(sx + sepW * 0.5F, y + line2, infoSize, itemColor, 0, 0.0F, s);
            sx += sepW;
         }
      }

      Gfx.flush();
      if (logo == 0) {
         Draw.icon(Gfx.graphics(), UiIcon.MACE_SPEAR, x + padX, my, mark, Fx.alpha(markColor, 0.55F), markColor, Fx.mix(markColor, -1, 0.35F));
      } else if (logo == 1) {
         HudKit.textCentered(this.letter, x + padX + mark * 0.5F, my + mark * 0.5F, mark * 0.58F, readableOn(markColor), false);
      }

      HudKit.text(name, nameX, y + line1, nameSize, color, 1.0F);
      if (version != null) {
         HudKit.text(version, infoX, y + line2, infoSize, itemColor, true);
      } else {
         float sx = infoX;

         for(int i = 0; i < this.shownCount; ++i) {
            String item = this.infoText[this.shown[i]];
            HudKit.text(item, sx, y + line2, infoSize, itemColor, true);
            sx += HudKit.width(item, infoSize, true) + sepW;
            if ((Integer)this.separator.get() == 2 && i + 1 < this.shownCount) {
               this.slash(sx - sepW * 0.5F, y + line2, infoSize, itemColor, 0, 0.0F);
            }
         }
      }

   }

   private static String versionText() {
      return "v1.0";
   }

   private int itemColor() {
      int var10000;
      switch ((Integer)this.infoColor.get()) {
         case 1 -> var10000 = HudKit.TEXT;
         case 2 -> var10000 = HudKit.ACCENT;
         default -> var10000 = HudKit.MUTED;
      }

      return var10000;
   }

   private float separatorWidth(float size, float s) {
      float var10000;
      switch ((Integer)this.separator.get()) {
         case 2 -> var10000 = HudKit.width("/", size, true) + 7.0F * s;
         case 3 -> var10000 = 6.0F * s;
         default -> var10000 = 11.0F * s;
      }

      return var10000;
   }

   private float itemsWidth(float size, float sepW) {
      float width = 0.0F;

      for(int i = 0; i < this.shownCount; ++i) {
         width += HudKit.width(this.infoText[this.shown[i]], size, true) + (i > 0 ? sepW : 0.0F);
      }

      return width;
   }

   private void separatorShape(float cx, float cy, float size, int color, int shadow, float drop, float s) {
      int style = (Integer)this.separator.get();
      if (style == 0 || style == 1) {
         float p = Gfx.pixel();
         float w;
         float h;
         if (style == 0) {
            w = Math.max(p, (float)Math.round(0.75F * s / p) * p);
            h = Math.max(p, (float)Math.round(size * 0.95F / p) * p);
         } else {
            w = Math.max(p * 2.0F, (float)Math.round(2.2F * s / p) * p);
            h = w;
         }

         float left = (float)Math.round((cx - w * 0.5F) / p) * p;
         float top = (float)Math.round((cy - h * 0.5F) / p) * p;
         float radius = style == 1 ? w * 0.5F : 0.0F;
         if (shadow != 0) {
            HudKit.fill(left + drop, top + drop, w, h, radius, Fx.alpha(shadow, 0.7F));
         }

         HudKit.fill(left, top, w, h, radius, Fx.alpha(color, style == 0 ? 0.45F : 0.6F));
      }
   }

   private void slash(float cx, float cy, float size, int color, int shadow, float drop) {
      if (shadow != 0) {
         HudKit.textCentered("/", cx + drop * 0.5F, cy + drop, size, shadow, true);
      }

      HudKit.textCentered("/", cx, cy, size, Fx.alpha(color, 0.5F), true);
   }

   private static int readableOn(int color) {
      float l = (0.2126F * (float)(color >> 16 & 255) + 0.7152F * (float)(color >> 8 & 255) + 0.0722F * (float)(color & 255)) / 255.0F;
      return l > 0.6F ? -15658476 : -1;
   }
}
