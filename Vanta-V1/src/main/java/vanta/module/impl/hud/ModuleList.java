package vanta.module.impl.hud;

import com.mojang.blaze3d.platform.Window;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import vanta.VantaClient;
import vanta.event.events.HudRenderEvent;
import vanta.module.Category;
import vanta.module.HudModule;
import vanta.module.Module;
import vanta.render.Gfx;
import vanta.render.Text;
import vanta.setting.BoolSetting;
import vanta.setting.ColorSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.MultiSetting;
import vanta.setting.PositionSetting;
import vanta.setting.SliderSetting;
import vanta.setting.TextColorSettings;
import vanta.ui.UiScale;
import vanta.visual.Fx;
import vanta.visual.hud.HudElements;
import vanta.visual.hud.HudKit;
import vanta.visual.hud.HudLook;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public final class ModuleList extends HudModule {
   private static final String[] INFO_STYLES = new String[]{"Plain", "Brackets", "Parentheses", "Dash", "Divider"};
   private static final int INFO_PLAIN = 0;
   private static final int INFO_BRACKETS = 1;
   private static final int INFO_PARENTHESES = 2;
   private static final int INFO_DASH = 3;
   private static final int INFO_DIVIDER = 4;
   private static final String[] INFO_COLORS = new String[]{"Muted", "Text", "Accent", "Custom"};
   private static final int INFO_TEXT = 1;
   private static final int INFO_ACCENT = 2;
   private static final int INFO_CUSTOM = 3;
   private static final String[] SORTS = new String[]{"Longest", "Shortest", "A-Z", "Z-A", "Category"};
   private static final int SORT_SHORTEST = 1;
   private static final int SORT_AZ = 2;
   private static final int SORT_ZA = 3;
   private static final int SORT_CATEGORY = 4;
   private static final String[] CASES = new String[]{"Normal", "lowercase", "UPPERCASE", "No spaces"};
   private static final int CASE_NORMAL = 0;
   private static final int CASE_LOWER = 1;
   private static final int CASE_UPPER = 2;
   private static final String[] ALIGNS = new String[]{"Auto", "Left", "Right", "Center"};
   private static final int ALIGN_AUTO = 0;
   private static final int ALIGN_LEFT = 1;
   private static final int ALIGN_RIGHT = 2;
   private static final int ALIGN_CENTER = 3;
   private static final String[] BACKDROPS = new String[]{"Per row", "One card", "None"};
   private static final int BACKDROP_ROWS = 0;
   private static final int BACKDROP_CARD = 1;
   private static final String[] MARKERS = new String[]{"None", "Tab", "Rail", "Dot"};
   private static final int MARKER_TAB = 1;
   private static final int MARKER_RAIL = 2;
   private static final int MARKER_DOT = 3;
   private static final String[] ANIMATIONS = new String[]{"Slide", "Fade", "Pop", "None"};
   private static final int ANIM_SLIDE = 0;
   private static final int ANIM_FADE = 1;
   private static final int ANIM_POP = 2;
   private static final int ANIM_NONE = 3;
   private static final String[] FLOWS = new String[]{"Across", "Down"};
   private static final Category[] HIDEABLE = Category.values();
   private static final String[] HIDEABLE_NAMES = new String[]{"Combat", "1.8 PvP", "Vanta V1", "Movement", "Render", "Player", "Scripts"};
   private static final int COLOR_ACCENT = 0;
   private static final int COLOR_GRADIENT = 2;
   private static final float TEXT_SIZE = 7.5F;
   private static final float INFO_GAP = 4.0F;
   private static final float SLIDE = 14.0F;
   private static final float POP_FROM = 0.55F;
   private static final float CARD_INSET = 3.0F;
   private static final long INFO_MILLIS = 100L;
   private final BoolSetting prefix = (BoolSetting)this.add(new BoolSetting("prefix", true));
   private final EnumSetting infoStyle;
   private final EnumSetting infoColor;
   private final ColorSetting infoCustom;
   private final BoolSetting hudModules;
   private final MultiSetting hideCategories;
   private final EnumSetting sort;
   private final EnumSetting letterCase;
   private final SliderSetting maxRows;
   private final EnumSetting align;
   private final EnumSetting backdrop;
   private final EnumSetting marker;
   private final EnumSetting animation;
   private final SliderSetting size;
   private final SliderSetting rowHeight;
   private final SliderSetting spacing;
   private final SliderSetting padding;
   private final SliderSetting textSize;
   private final TextColorSettings colors;
   private final EnumSetting gradientFlow;
   private final HudLook look;
   private final EnumSetting colorMode;
   private final Text.ColorAt accentColor;
   private final Map<Module, Entry> entries;
   private final HudKit.Clock clock;
   private final HudKit.Spring listWidth;
   private boolean listShown;
   private Entry[] rows;
   private int count;
   private final boolean[] hiddenNow;
   private final Entry placeholder;

   public ModuleList() {
      super("arraylist", PositionSetting.Anchor.END, PositionSetting.Anchor.START, 4.0F, 4.0F);
      this.infoStyle = (EnumSetting)this.add(new EnumSetting("infoStyle", 0, INFO_STYLES));
      this.infoColor = (EnumSetting)this.add(new EnumSetting("infoColor", 0, INFO_COLORS));
      this.infoCustom = (ColorSetting)this.add(new ColorSetting("infoCustomColor", 10856106));
      this.hudModules = (BoolSetting)this.add(new BoolSetting("hudModules", false));
      this.hideCategories = (MultiSetting)this.add(new MultiSetting("hideCategories", HIDEABLE_NAMES, new int[0]));
      this.sort = (EnumSetting)this.add(new EnumSetting("sort", 0, SORTS));
      this.letterCase = (EnumSetting)this.add(new EnumSetting("letterCase", 0, CASES));
      this.maxRows = (SliderSetting)this.add((new SliderSetting("maxRows", 30, 1, 30)).unlimitedAtMax());
      this.align = (EnumSetting)this.add(new EnumSetting("align", 0, ALIGNS));
      this.backdrop = (EnumSetting)this.add(new EnumSetting("backdrop", 0, BACKDROPS));
      this.marker = (EnumSetting)this.add(new EnumSetting("marker", 0, MARKERS));
      this.animation = (EnumSetting)this.add(new EnumSetting("animation", 0, ANIMATIONS));
      this.size = (SliderSetting)this.add(new SliderSetting("scale", 100, 25, 1000, Measure.PERCENT));
      this.rowHeight = (SliderSetting)this.add((new SliderSetting("rowHeight", 12, 8, 24)).withFormat((value) -> value + " px"));
      this.spacing = (SliderSetting)this.add((new SliderSetting("spacing", 2, 0, 8)).withFormat((value) -> value + " px"));
      this.padding = (SliderSetting)this.add((new SliderSetting("padding", 6, 2, 14)).withFormat((value) -> value + " px"));
      this.textSize = (SliderSetting)this.add(new SliderSetting("textSize", 100, 60, 160, Measure.PERCENT));
      this.colors = (TextColorSettings)this.add(new TextColorSettings());
      this.gradientFlow = (EnumSetting)this.add(new EnumSetting("gradientFlow", 0, FLOWS));
      this.look = (HudLook)this.add(new HudLook(true, false));
      this.colorMode = (EnumSetting)this.colors.settings().get(0);
      this.accentColor = (x) -> HudKit.ACCENT;
      this.entries = new HashMap();
      this.clock = new HudKit.Clock();
      this.listWidth = new HudKit.Spring(0.0F);
      this.rows = new Entry[16];
      this.hiddenNow = new boolean[Category.values().length];
      this.placeholder = new Entry((Module)null);
      EnumSetting var10000 = this.infoStyle;
      BoolSetting var10001 = this.prefix;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      var10000 = this.infoColor;
      var10001 = this.prefix;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.infoCustom.visibleWhen(() -> (Boolean)this.prefix.get() && (Integer)this.infoColor.get() == 3);
      this.gradientFlow.visibleWhen(() -> (Integer)this.colorMode.get() == 2);
      this.hudModules.describe();
      this.infoStyle.describe();
      this.hideCategories.describe();
      this.sort.describe();
      this.letterCase.describe();
      this.maxRows.describe();
      this.align.describe();
      this.backdrop.describe();
      this.marker.describe();
      this.animation.describe();
      this.gradientFlow.describe();
   }

   protected void onEnable() {
      this.listen(HudRenderEvent.class, this::onHudRender);
   }

   private static String info(Entry entry, long now) {
      if (now - entry.infoAt >= 100L || entry.rawInfo == null) {
         entry.infoAt = now;
         String raw = entry.module.info();
         if (raw == null) {
            raw = "";
         }

         if (!raw.equals(entry.rawInfo)) {
            entry.rawInfo = raw;
            String plain = raw.indexOf(167) >= 0 ? ChatFormatting.stripFormatting(raw) : raw;
            entry.info = plain == null ? "" : plain.trim();
         }
      }

      return entry.info;
   }

   private static String cased(String text, int mode, boolean name) {
      String var10000;
      switch (mode) {
         case 0 -> var10000 = text;
         case 1 -> var10000 = text.toLowerCase(Locale.ROOT);
         case 2 -> var10000 = text.toUpperCase(Locale.ROOT);
         default -> var10000 = name ? text.replace(" ", "") : text;
      }

      return var10000;
   }

   private static String name(Entry entry, String source, int mode) {
      if (mode != entry.nameCase || !source.equals(entry.nameSource)) {
         entry.nameSource = source;
         entry.nameCase = mode;
         entry.name = cased(source, mode, true);
      }

      return entry.name;
   }

   private static String casedInfo(Entry entry, String source, int mode) {
      if (mode != entry.infoCase || !source.equals(entry.infoCasedSource)) {
         entry.infoCasedSource = source;
         entry.infoCase = mode;
         entry.infoCased = source.isEmpty() ? "" : cased(source, mode, false);
      }

      return entry.infoCased;
   }

   private static String open(int style) {
      String var10000;
      switch (style) {
         case 1 -> var10000 = "[";
         case 2 -> var10000 = "(";
         case 3 -> var10000 = "– ";
         default -> var10000 = "";
      }

      return var10000;
   }

   private static String close(int style) {
      String var10000;
      switch (style) {
         case 1 -> var10000 = "]";
         case 2 -> var10000 = ")";
         default -> var10000 = "";
      }

      return var10000;
   }

   private static float infoWidth(String info, float textSize, float infoGap, float line, int style) {
      if (info.isEmpty()) {
         return 0.0F;
      } else {
         float width = infoGap + HudKit.width(info, textSize, true);
         return style == 4 ? width + infoGap + line : width + HudKit.width(open(style), textSize, true) + HudKit.width(close(style), textSize, true);
      }
   }

   private static float pixels(float value, float pixel) {
      return Math.max(pixel, (float)Math.round(value / pixel) * pixel);
   }

   private void add(Entry entry) {
      if (this.count == this.rows.length) {
         this.rows = (Entry[])Arrays.copyOf(this.rows, this.count * 2);
      }

      this.rows[this.count++] = entry;
   }

   private void onHudRender(HudRenderEvent event) {
      float dt = this.clock.tick();
      HudKit.sync(this.look.look());
      Window window = Minecraft.getInstance().getWindow();
      float scale = UiScale.current().factor() * (float)(Integer)this.size.get() / 100.0F;
      float pixel = 1.0F / (float)Math.max((double)1.0F, (double)window.getGuiScale());
      float textSize = 7.5F * scale * (float)(Integer)this.textSize.get() / 100.0F;
      float padding = (float)(Integer)this.padding.get() * scale;
      float infoGap = 4.0F * scale;
      float height = Math.max(1.0F, (float)Math.round((float)(Integer)this.rowHeight.get() * scale));
      int spacing = (Integer)this.spacing.get();
      float step = height + (spacing <= 0 ? 0.0F : Math.max(1.0F, (float)Math.round((float)spacing * scale)));
      int marker = (Integer)this.marker.get();
      int style = (Integer)this.infoStyle.get();
      int letterCase = (Integer)this.letterCase.get();
      int animation = (Integer)this.animation.get();
      int backdrop = (Integer)this.backdrop.get();
      float tab = marker == 1 ? pixels(2.0F * scale, pixel) : 0.0F;
      float tabGap = marker == 1 ? pixels(1.0F * scale, pixel) : 0.0F;
      float rail = marker == 2 ? pixels(1.0F * scale, pixel) : 0.0F;
      float railRoom = marker == 2 ? rail + pixels(2.0F * scale, pixel) : 0.0F;
      float dot = marker == 3 ? pixels(3.0F * scale, pixel) : 0.0F;
      float dotRoom = marker == 3 ? dot + 3.0F * scale : 0.0F;
      float divider = pixel;
      boolean hudModules = (Boolean)this.hudModules.get();
      boolean info = (Boolean)this.prefix.get();

      for(int i = 0; i < HIDEABLE.length; ++i) {
         this.hiddenNow[HIDEABLE[i].ordinal()] = this.hideCategories.has(i);
      }

      long now = System.currentTimeMillis();
      this.count = 0;
      List<Module> modules = VantaClient.MODULES.all();

      for(int i = 0; i < modules.size(); ++i) {
         Module module = (Module)modules.get(i);
         Category category = module.category();
         boolean listed = module.isEnabled() && !module.hidden() && (hudModules || category != Category.HUD) && (category == null || !this.hiddenNow[category.ordinal()]);
         Entry entry = (Entry)this.entries.get(module);
         if (entry == null) {
            if (!listed) {
               continue;
            }

            entry = new Entry(module);
            this.entries.put(module, entry);
         }

         if (!listed && entry.reveal.value <= 0.004F) {
            entry.reveal.snap(0.0F);
            entry.fresh = true;
         } else {
            entry.listed = listed;
            String name = name(entry, module.name(), letterCase);
            entry.shownInfo = info ? casedInfo(entry, info(entry, now), letterCase) : "";
            entry.targetWidth = (float)Math.round(padding * 2.0F + dotRoom + HudKit.width(name, textSize) + infoWidth(entry.shownInfo, textSize, infoGap, divider, style)) + tab + tabGap;
            this.add(entry);
         }
      }

      this.sort(this.count);
      int limit = this.maxRows.unlimited() ? Integer.MAX_VALUE : (Integer)this.maxRows.get();
      int kept = 0;
      int visible = 0;

      for(int i = 0; i < this.count; ++i) {
         Entry row = this.rows[i];
         if (row.listed) {
            if (kept >= limit) {
               row.listed = false;
            } else {
               ++kept;
            }
         }

         float shown;
         if (animation == 3) {
            row.reveal.snap(row.listed ? 1.0F : 0.0F);
            shown = row.reveal.value;
         } else {
            shown = row.listed ? HudKit.lively(row.reveal, 1.0F, dt) : row.reveal.update(0.0F, 340.0F, 37.0F, dt);
         }

         if (!row.listed && shown <= 0.004F) {
            row.reveal.snap(0.0F);
            row.fresh = true;
         } else {
            this.rows[visible++] = row;
         }
      }

      for(int i = visible; i < this.count; ++i) {
         this.rows[i] = null;
      }

      this.count = visible;
      if (this.count == 0 && HudElements.editing()) {
         Entry placeholder = this.placeholder;
         String name = name(placeholder, this.name(), letterCase);
         placeholder.shownInfo = "";
         placeholder.listed = true;
         placeholder.reveal.snap(1.0F);
         placeholder.targetWidth = (float)Math.round(padding * 2.0F + dotRoom + HudKit.width(name, textSize)) + tab + tabGap;
         this.add(placeholder);
      } else {
         this.placeholder.fresh = true;
      }

      int count = this.count;
      if (count == 0) {
         this.listShown = false;
         this.placed(0.0F, 0.0F, 0.0F, 0.0F);
      } else {
         float inset = backdrop == 1 ? (float)Math.round(3.0F * scale) : 0.0F;
         float targetWidth = 0.0F;
         float below = 0.0F;
         float listHeight = 0.0F;

         for(int i = 0; i < count; ++i) {
            Entry row = this.rows[i];
            float shown = Math.clamp(row.reveal.value, 0.0F, 1.0F);
            row.targetY = below;
            below += step * (row.listed ? 1.0F : shown);
            if (row.listed) {
               targetWidth = Math.max(targetWidth, row.targetWidth);
            }

            if (row.fresh) {
               row.fresh = false;
               row.y.snap(row.targetY);
               row.width.snap(row.targetWidth);
            } else {
               HudKit.smooth(row.y, row.targetY, dt);
               HudKit.smooth(row.width, row.targetWidth, dt);
            }

            listHeight = Math.max(listHeight, row.y.value + height * (row.listed ? 1.0F : shown));
         }

         if (targetWidth <= 0.0F) {
            targetWidth = this.rows[0].targetWidth;
         }

         targetWidth += railRoom;
         if (!this.listShown) {
            this.listShown = true;
            this.listWidth.snap(targetWidth);
         }

         float listWidth = Math.max(1.0F, (float)Math.round(HudKit.smooth(this.listWidth, targetWidth, dt)));
         listHeight = Math.max(1.0F, listHeight) + inset * 2.0F;
         int screenWidth = window.getGuiScaledWidth();
         float x = (float)Math.round(this.left(listWidth, (float)screenWidth));
         float y = (float)Math.round(this.top(listHeight, (float)window.getGuiScaledHeight()));
         this.placed(x, y, listWidth, listHeight);
         int align = (Integer)this.align.get();
         if (align == 0) {
            align = this.position().horizontal() != PositionSetting.Anchor.START ? 2 : 1;
         }

         boolean outerRight = align == 2 || align == 3 && x + listWidth * 0.5F > (float)screenWidth * 0.5F;
         float areaX = outerRight ? x : x + railRoom;
         float areaW = listWidth - railRoom;
         float edge = align == 2 ? areaX + areaW : (align == 1 ? areaX : areaX + areaW * 0.5F);
         float travel = align == 2 ? Math.min((float)screenWidth - edge, 14.0F * scale) : (align == 1 ? Math.min(edge, 14.0F * scale) : 14.0F * scale);
         float top = y + inset;
         boolean clip = backdrop == 1 && animation == 0;
         Text.ColorAt color = (Integer)this.colorMode.get() == 0 ? this.accentColor : this.colors.colorAt(scale);
         boolean down = (Integer)this.colorMode.get() == 2 && (Integer)this.gradientFlow.get() == 1;
         int var10000;
         switch ((Integer)this.infoColor.get()) {
            case 1 -> var10000 = HudKit.TEXT;
            case 2 -> var10000 = HudKit.ACCENT;
            case 3 -> var10000 = -16777216 | (Integer)this.infoCustom.get();
            default -> var10000 = HudKit.MUTED;
         }

         int infoColor = var10000;
         Gfx.begin(event.graphics());

         try {
            if (backdrop == 1) {
               HudKit.raised(areaX, y, areaW, listHeight, 7.0F * scale, scale, 1.0F);
            }

            float railX = outerRight ? x + listWidth - rail : x;
            if (marker == 2 && clip) {
               this.rail(railX, y, rail, listHeight, color, down, edge, count);
            }

            if (clip) {
               Gfx.pushScissor(areaX, y, areaW, listHeight);
            }

            try {
               for(int pass = backdrop == 0 ? 0 : 1; pass < 2; ++pass) {
                  for(int i = 0; i < count; ++i) {
                     Entry row = this.rows[i];
                     float alpha = alpha(row, animation);
                     if (!(alpha <= 0.0F)) {
                        float w = Math.max(height, row.width.value);
                        float rx = this.rowX(row, w, edge, travel, align, outerRight, animation);
                        float ry = top + row.y.value;
                        float pillW = w - tab - tabGap;
                        float pillX = outerRight ? rx : rx + tab + tabGap;
                        boolean pop = this.pushPop(row, animation, edge, ry + height * 0.5F);

                        try {
                           if (pass == 0) {
                              HudKit.shadow(pillX, ry, pillW, height, height * 0.5F, scale, alpha);
                           } else {
                              if (backdrop == 0) {
                                 HudKit.pill(pillX, ry, pillW, height, alpha);
                              }

                              if (marker == 1) {
                                 int c = Fx.alpha(this.rowColor(row, color, down, pillX + padding), alpha);
                                 HudKit.fill(outerRight ? rx + w - tab : rx, ry, tab, height, tab * 0.5F, c);
                              } else if (marker == 3) {
                                 int c = Fx.alpha(this.rowColor(row, color, down, pillX + padding), alpha);
                                 HudKit.fill(pillX + padding, (float)Math.round((ry + height * 0.5F - dot * 0.5F) / pixel) * pixel, dot, dot, dot * 0.5F, c);
                              }

                              if (style == 4 && !row.shownInfo.isEmpty()) {
                                 float lineX = pillX + padding + dotRoom + HudKit.width(row.name, textSize) + infoGap;
                                 float lineH = (float)Math.round(Math.min(height - 2.0F * scale, textSize * 1.05F) / pixel) * pixel;
                                 Gfx.rectWH((float)Math.round(lineX / pixel) * pixel, (float)Math.round((ry + (height - lineH) * 0.5F) / pixel) * pixel, divider, lineH, Fx.alpha(HudKit.FAINT, alpha));
                              }
                           }
                        } finally {
                           if (pop) {
                              Gfx.pop();
                           }

                        }
                     }
                  }
               }

               if (marker == 2 && !clip) {
                  this.rail(railX, y, rail, listHeight, color, down, edge, count);
               }

               for(int i = 0; i < count; ++i) {
                  Entry row = this.rows[i];
                  float alpha = alpha(row, animation);
                  if (!(alpha <= 0.0F)) {
                     float w = Math.max(height, row.width.value);
                     float rx = this.rowX(row, w, edge, travel, align, outerRight, animation);
                     float pillX = outerRight ? rx : rx + tab + tabGap;
                     float textX = pillX + padding + dotRoom;
                     float centerY = top + row.y.value + height * 0.5F;
                     boolean pop = this.pushPop(row, animation, edge, centerY);

                     try {
                        if (down) {
                           HudKit.text(row.name, textX, centerY, textSize, Fx.alpha(this.rowColor(row, color, true, textX), alpha));
                        } else {
                           HudKit.text(row.name, textX, centerY, textSize, color, alpha);
                        }

                        if (!row.shownInfo.isEmpty()) {
                           this.info(row, textX + HudKit.width(row.name, textSize) + infoGap, centerY, textSize, infoGap, divider, style, infoColor, alpha);
                        }
                     } finally {
                        if (pop) {
                           Gfx.pop();
                        }

                     }
                  }
               }

               HudKit.flushText();
            } finally {
               if (clip) {
                  Gfx.popScissor();
               }

            }
         } finally {
            HudKit.discardText();
            Gfx.end();
         }

      }
   }

   private void rail(float x, float y, float width, float height, Text.ColorAt color, boolean down, float edge, int count) {
      int from = this.rowColor(this.rows[0], color, down, edge);
      int to = this.rowColor(this.rows[count - 1], color, down, edge);
      HudKit.fillV(x, y, width, height, width * 0.5F, from, to);
   }

   private void info(Entry row, float x, float centerY, float textSize, float infoGap, float divider, int style, int color, float alpha) {
      if (style == 4) {
         x += infoGap + divider;
      }

      String open = open(style);
      if (!open.isEmpty()) {
         HudKit.text(open, x, centerY, textSize, Fx.alpha(HudKit.FAINT, alpha), true);
         x += HudKit.width(open, textSize, true);
      }

      HudKit.text(row.shownInfo, x, centerY, textSize, Fx.alpha(color, alpha), true);
      String close = close(style);
      if (!close.isEmpty()) {
         HudKit.text(close, x + HudKit.width(row.shownInfo, textSize, true), centerY, textSize, Fx.alpha(HudKit.FAINT, alpha), true);
      }

   }

   private int rowColor(Entry row, Text.ColorAt color, boolean down, float textX) {
      return color.at(down ? row.y.value : textX) | -16777216;
   }

   private static float alpha(Entry row, int animation) {
      if (animation == 1) {
         return Fx.smooth(row.reveal.value);
      } else if (animation == 3) {
         return row.reveal.value > 0.5F ? 1.0F : 0.0F;
      } else {
         return Math.clamp(row.reveal.value * 1.6F - 0.1F, 0.0F, 1.0F);
      }
   }

   private float rowX(Entry row, float width, float edge, float travel, int align, boolean outerRight, int animation) {
      float out = animation == 0 ? (1.0F - row.reveal.value) * (width + travel) : 0.0F;
      float var10000;
      switch (align) {
         case 1 -> var10000 = edge - out;
         case 2 -> var10000 = edge - width + out;
         default -> var10000 = edge - width * 0.5F + (outerRight ? out : -out);
      }

      return var10000;
   }

   private boolean pushPop(Entry row, int animation, float edge, float centerY) {
      if (animation == 2 && row.reveal.value != 1.0F) {
         float k = 0.55F + 0.45F * row.reveal.value;
         Gfx.push();
         Gfx.translate(edge, centerY);
         Gfx.scale(Math.max(0.01F, k));
         Gfx.translate(-edge, -centerY);
         return true;
      } else {
         return false;
      }
   }

   private void sort(int count) {
      Entry[] rows = this.rows;
      int order = (Integer)this.sort.get();

      for(int i = 1; i < count; ++i) {
         Entry row = rows[i];

         int j;
         for(j = i - 1; j >= 0 && before(row, rows[j], order); --j) {
            rows[j + 1] = rows[j];
         }

         rows[j + 1] = row;
      }

   }

   private static boolean before(Entry a, Entry b, int order) {
      boolean var10000;
      switch (order) {
         case 1:
            var10000 = a.targetWidth != b.targetWidth ? a.targetWidth < b.targetWidth : a.name.compareTo(b.name) < 0;
            break;
         case 2:
            var10000 = a.name.compareToIgnoreCase(b.name) < 0;
            break;
         case 3:
            var10000 = a.name.compareToIgnoreCase(b.name) > 0;
            break;
         case 4:
            int ca = a.module != null && a.module.category() != null ? a.module.category().ordinal() : 0;
            int cb = b.module != null && b.module.category() != null ? b.module.category().ordinal() : 0;
            var10000 = ca != cb ? ca < cb : longer(a, b);
            break;
         default:
            var10000 = longer(a, b);
      }

      return var10000;
   }

   private static boolean longer(Entry a, Entry b) {
      return a.targetWidth != b.targetWidth ? a.targetWidth > b.targetWidth : a.name.compareTo(b.name) < 0;
   }

   private static final class Entry {
      final Module module;
      String name;
      String nameSource;
      int nameCase = -1;
      String rawInfo;
      String info = "";
      long infoAt;
      String infoCased = "";
      String infoCasedSource;
      int infoCase = -1;
      String shownInfo = "";
      float targetWidth;
      float targetY;
      boolean listed;
      final HudKit.Spring reveal = new HudKit.Spring(0.0F);
      final HudKit.Spring y = new HudKit.Spring(0.0F);
      final HudKit.Spring width = new HudKit.Spring(0.0F);
      boolean fresh = true;

      Entry(Module module) {
         this.module = module;
      }
   }
}
