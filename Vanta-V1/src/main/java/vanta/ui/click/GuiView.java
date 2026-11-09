package vanta.ui.click;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import vanta.VantaClient;
import vanta.config.StateStore;
import vanta.friends.FriendList;
import vanta.module.Category;
import vanta.module.Module;
import vanta.render.Draw;
import vanta.render.Gfx;
import vanta.render.Opacity;
import vanta.render.UiIcon;
import vanta.setting.Setting;
import vanta.setting.TextSetting;
import vanta.ui.DraggableHud;
import vanta.ui.GuiState;
import vanta.visual.hud.HudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;

final class GuiView {
   static final float PANEL_W = 128.0F;
   static final float HEADER_H = 26.0F;
   static final float ROW_H = 22.0F;
   private static final float TOP_Y = 47.0F;
   private final List<Panel> panels = new ArrayList();
   private final List<Hit> hits = new ArrayList();
   private final List<float[]> clips = new ArrayList();
   private final Set<Module> expanded = new HashSet();
   private final StringBuilder query = new StringBuilder();
   private long openedAt = System.nanoTime();
   private long closedAt;
   private boolean closing;
   private int tab;
   private boolean plateDrag;
   private float plateOffX;
   private float plateOffY;
   private float plateX;
   private float plateY;
   private float plateW;
   private float plateH;
   private float scale = 1.0F;
   private int openScale;
   private float vw;
   private float vh;
   private double mouseX;
   private double mouseY;
   private Object hovered;
   private float settingsScroll;
   private float settingsScrollTarget;
   private float settingsMax;
   private float lastOpen;
   static final int PANEL = -233103837;
   static final int PANEL_BORDER = -12762808;
   static final int ROW = -14407892;
   static final int ROW_BORDER = -12170670;
   static final int ROW_ON = -13815754;
   static final int ROW_ON_BORDER = -10328465;
   private boolean glass;
   private static final float ROW_GAP = 4.5F;
   private static final float ROW_PAD = 5.5F;
   private Module pressed;
   private long pressedAt;
   private float pressX;
   private float pressY;
   private final Map<Module, Long> toggledAt;
   private Object tipFor;
   private long tipSince;
   private static final int BTN = -233103837;
   private static final int BTN_BORDER = -12762808;
   private int settingsFrom;
   private Object lastPopup;
   final Map<Object, float[]> rects;
   private boolean searchFocus;
   private static CompletableFuture<GameProfile> ownProfile;
   private static Supplier<PlayerSkin> ownSkin;
   private final TextSetting configName;
   private float configScroll;
   private float configScrollTarget;
   private String pendingDelete;
   private long pendingAt;
   private String friendText;
   private final TextSetting friendName;
   private float friendScroll;
   private float friendScrollTarget;
   final Map<String, float[]> friendRects;

   GuiView() {
      this.openScale = GuiPrefs.scale;
      this.toggledAt = new HashMap();
      this.settingsFrom = 1;
      this.rects = new IdentityHashMap();
      this.configName = new TextSetting("configName", "", 24);
      this.friendText = "";
      this.friendName = (TextSetting)(new TextSetting("friendName", "", 16)).bind(() -> this.friendText, (v) -> this.friendText = v, "Add friend");
      this.friendRects = new HashMap();
      Category[] cats = Category.values();

      for(int i = 0; i < cats.length; ++i) {
         this.panels.add(new Panel(cats[i], i));
      }

      this.layoutDefaults(true);
   }

   private void layoutDefaults(boolean useSaved) {
      Minecraft mc = Minecraft.getInstance();
      this.computeScale(mc);

      for(Panel p : this.panels) {
         float[] saved = useSaved ? GuiPrefs.panel(p.category) : null;
         p.collapsed = saved != null && saved.length >= 3 && saved[2] > 0.5F;
      }

      this.centerPanels();
   }

   private void centerPanels() {
      float gap = 8.0F;
      float total = (float)this.panels.size() * 128.0F + (float)(this.panels.size() - 1) * gap;
      float start = (this.vw - total) / 2.0F;

      for(Panel p : this.panels) {
         p.x = start + (float)p.index * (128.0F + gap);
         p.y = 47.0F;
      }

   }

   private void computeScale(Minecraft mc) {
      float guiScale = (float)mc.getWindow().getGuiScale();
      int gw = mc.getWindow().getGuiScaledWidth();
      int gh = mc.getWindow().getGuiScaledHeight();
      float physH = (float)mc.getWindow().getHeight();
      float s = (float)this.openScale / 100.0F * Math.max(1.0F, physH / 540.0F) / guiScale;
      float need = 848.0F;
      if ((float)gw / s < need) {
         s = (float)gw / need;
      }

      this.scale = s;
      this.vw = (float)gw / s;
      this.vh = (float)gh / s;
   }

   void close() {
      if (!this.closing) {
         this.closing = true;
         this.closedAt = System.nanoTime();
         Widgets.commit();
         Widgets.listening = null;
         Widgets.release();
         GuiSounds.close();
      }

   }

   void reopen() {
      if (this.closing) {
         this.closing = false;
         this.openScale = GuiPrefs.scale;
         float p = this.lastOpen;
         this.openedAt = System.nanoTime() - (long)(p * 0.3F / GuiPrefs.animSpeed() * 1.0E9F);
         GuiSounds.open();
      }

   }

   boolean closing() {
      return this.closing;
   }

   private float openProgress() {
      long now = System.nanoTime();
      float speed = GuiPrefs.animSpeed();
      if (this.closing) {
         float t = (float)(now - this.closedAt) / 1.0E9F / (0.17F / speed);
         return this.lastOpen * (1.0F - Anim.outCubic(t));
      } else {
         return Anim.outCubic((float)(now - this.openedAt) / 1.0E9F / (0.3F / speed));
      }
   }

   void render(GuiGraphicsExtractor g, int guiMouseX, int guiMouseY) {
      Minecraft mc = Minecraft.getInstance();
      Anim.frame();
      this.computeScale(mc);
      this.centerPanels();
      Widgets.dragFrame(this.vw);
      float open = this.openProgress();
      if (!this.closing) {
         this.lastOpen = open;
      } else if (open <= 0.003F) {
         ClickGui.finish();
         return;
      }

      this.mouseX = (double)((float)guiMouseX / this.scale);
      this.mouseY = (double)((float)guiMouseY / this.scale);
      double mx = this.mouseX;
      double my = this.mouseY;
      g.nextStratum();
      this.glass = false;

      try {
         g.blurBeforeThisStratum();
         this.glass = true;
         ClickGui.requestGlassOnly(!GuiPrefs.blur);
      } catch (RuntimeException var25) {
      }

      Paint.g = g;
      Gfx.begin(g);
      g.pose().pushMatrix();
      this.hits.clear();
      this.clips.clear();
      Object hoveredBefore = this.hovered;
      this.hovered = null;

      try {
         Paint.fade = open;
         float dim = (float)GuiPrefs.dim / 100.0F;
         int gw = mc.getWindow().getGuiScaledWidth();
         int gh = mc.getWindow().getGuiScaledHeight();
         Gfx.gradientV(0.0F, 0.0F, (float)gw, (float)gh, Paint.a(0 | (int)(150.0F * dim) << 24, 1.0F), Paint.a((int)(200.0F * dim) << 24, 1.0F));
         Paint.flush();
         g.pose().scale(this.scale, this.scale);
         float zoom = 0.975F + 0.025F * open;
         g.pose().translate(this.vw / 2.0F, this.vh / 2.0F);
         g.pose().scale(zoom, zoom);
         g.pose().translate(-this.vw / 2.0F, -this.vh / 2.0F);
         mx = (mx - (double)(this.vw / 2.0F)) / (double)zoom + (double)(this.vw / 2.0F);
         my = (my - (double)(this.vh / 2.0F)) / (double)zoom + (double)(this.vh / 2.0F);
         this.mouseX = mx;
         this.mouseY = my;
         float setT = Anim.to(this, 0, this.tab == 1 ? 1.0F : 0.0F, 13.0F);
         float cfgT = Anim.to(this, 3, this.tab == 2 ? 1.0F : 0.0F, 13.0F);
         float friT = Anim.to(this, 5, this.tab == 3 ? 1.0F : 0.0F, 13.0F);
         float tabT = Math.min(1.0F, setT + cfgT + friT);
         float elapsed = (float)(System.nanoTime() - this.openedAt) / 1.0E9F * GuiPrefs.animSpeed();
         if (tabT < 0.99F) {
            for(Panel p : this.panels) {
               this.panel(p, elapsed, open, 1.0F - tabT, mx, my);
            }
         }

         if (setT > 0.01F) {
            this.settingsPage(open * setT, setT, mx, my);
         }

         if (cfgT > 0.01F) {
            this.configsPage(open * cfgT, cfgT, mx, my);
         }

         if (friT > 0.01F) {
            this.friendsPage(open * friT, friT, mx, my);
         }

         if (Widgets.popup != null) {
            Paint.flush();
            Paint.fade = open;
            float head = Widgets.popupTitle == null ? 0.0F : 14.0F;
            Widgets.popupX = Math.max(4.0F, Math.min(this.vw - 92.0F - 4.0F, Widgets.popupX));
            Widgets.popupY = Math.max(4.0F, Math.min(this.vh - 96.0F - head - 4.0F, Widgets.popupY));
            if (this.lastPopup != Widgets.popup) {
               this.lastPopup = Widgets.popup;
               Anim.set(Widgets.popup, 21, 0.0F);
            }

            Widgets.drawPopup(Anim.to(Widgets.popup, 21, 1.0F, 12.0F));
            Paint.flush();
         }

         this.tooltip(open, mx, my);
         Paint.fade = open;
         this.topBar(open, elapsed, mx, my);
         this.fpsPill(open * (this.closing ? 1.0F : Anim.outCubic(elapsed / 0.3F)), mx, my);
         this.searchBar(open, elapsed, mx, my);
         this.profileCard(open, elapsed);
         Paint.flush();
      } finally {
         Paint.discard();
         Paint.fade = 1.0F;
         g.pose().popMatrix();
         Gfx.end();
      }

      if (this.hovered != null && this.hovered != hoveredBefore && !this.closing) {
         GuiSounds.hover();
      }

      GuiPrefs.saveIfDirty();
   }

   private float stagger(float elapsed, int index) {
      return this.closing ? 1.0F : Anim.outBack((elapsed - (float)index * 0.045F) / 0.42F);
   }

   private void hit(float x, float y, float w, float h, Action action) {
      for(float[] c : this.clips) {
         float x0 = Math.max(x, c[0]);
         float y0 = Math.max(y, c[1]);
         float x1 = Math.min(x + w, c[0] + c[2]);
         float y1 = Math.min(y + h, c[1] + c[3]);
         x = x0;
         y = y0;
         w = x1 - x0;
         h = y1 - y0;
      }

      if (w > 0.0F && h > 0.0F) {
         this.hits.add(new Hit(x, y, w, h, action));
      }

   }

   private boolean over(double mx, double my, float x, float y, float w, float h) {
      if (!Paint.in(mx, my, x, y, w, h)) {
         return false;
      } else {
         for(float[] c : this.clips) {
            if (!Paint.in(mx, my, c[0], c[1], c[2], c[3])) {
               return false;
            }
         }

         return true;
      }
   }

   private void pushClip(float x, float y, float w, float h) {
      Paint.clip(x, y, w, h);
      this.clips.add(new float[]{x, y, w, h});
   }

   private void popClip() {
      Paint.unclip();
      this.clips.remove(this.clips.size() - 1);
   }

   private void card(float x, float y, float w, float h, float r, int fill, int border) {
      Gfx.shadow(x, y + 3.0F, w, h, r, 12.0F, Paint.a(1342177280, 1.0F));
      boolean g = this.glass && Paint.fade > 0.02F;
      if (g) {
         float fade = Paint.fade;
         Paint.flush();
         Opacity.with(fade, () -> Draw.glass(Paint.g, x, y, w, h, r, 0));
      }

      int a = fill >>> 24;
      int tint = g ? Math.round((float)a * 0.62F) : a;
      Gfx.roundedRect(x, y, w, h, r, Paint.a(tint << 24 | fill & 16777215, 1.0F));
      Gfx.roundedOutline(x, y, w, h, r, 0.6F, Paint.a(9080728 | Math.min(85, border >>> 24) << 24, 1.0F));
   }

   private static UiIcon icon(Category c) {
      UiIcon var10000;
      switch (c) {
         case COMBAT -> var10000 = UiIcon.COMBAT;
         case MOVEMENT -> var10000 = UiIcon.MOVEMENT;
         case RENDER -> var10000 = UiIcon.RENDER;
         case PLAYER -> var10000 = UiIcon.PLAYER;
         case HUD -> var10000 = UiIcon.HUD;
         case MISC -> var10000 = UiIcon.MISC;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static void whiteIcon(UiIcon icon, float x, float y, float size, int color) {
      Paint.flush();
      int c = Paint.a(color, 1.0F);
      Draw.icon(Paint.g, icon, x, y, size, c, c, c);
   }

   private boolean matches(Module m) {
      if (this.query.isEmpty()) {
         return true;
      } else {
         String q = this.query.toString().toLowerCase(Locale.ROOT);
         return m.name().toLowerCase(Locale.ROOT).contains(q) || m.id().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q);
      }
   }

   private List<Module> modules(Category c) {
      List<Module> list = new ArrayList();

      for(Module m : VantaClient.MODULES.in(c)) {
         if (!m.hidden()) {
            list.add(m);
         }
      }

      list.sort(Comparator.comparing((mx) -> !GuiState.isPinned(mx.id())));
      return list;
   }

   private float settingsHeight(Module m, float w) {
      float h = 43.0F;

      for(Setting<?> s : m.settings()) {
         float v = Anim.to(s, 0, s.isVisible() ? 1.0F : 0.0F, 16.0F);
         h += (Widgets.height(s, w) + 1.0F) * v;
      }

      return h + 4.0F;
   }

   private void panel(Panel p, float elapsed, float open, float tabAlpha, double mx, double my) {
      float st = this.stagger(elapsed, p.index);
      float alpha = st * tabAlpha;
      if (!(alpha <= 0.01F)) {
         Paint.fade = open * alpha;
         float dy = (1.0F - st) * -16.0F + (1.0F - tabAlpha) * -8.0F;
         alpha = Math.min(1.0F, alpha);
         Paint.fade = open * alpha;
         float x = p.x;
         float y = p.y + dy;
         List<Module> mods = this.modules(p.category);
         float innerW = 117.0F;
         float setW = innerW - 12.0F;
         float content = 5.0F;
         int shown = 0;

         for(Module m : mods) {
            float vis = Anim.to(m, 0, this.matches(m) ? 1.0F : 0.0F, 15.0F);
            float ex = Anim.smooth(Anim.to(m, 1, this.expanded.contains(m) ? 1.0F : 0.0F, 14.0F));
            content += (26.5F + (ex > 0.001F ? this.settingsHeight(m, setW) * ex : 0.0F)) * vis;
            if (this.matches(m)) {
               ++shown;
            }
         }

         ++content;
         float maxBody = Math.max(40.0F, this.vh - 70.0F - (y + 26.0F));
         float target = !p.collapsed && (shown != 0 || this.query.isEmpty()) ? Math.min(content, maxBody) : 0.0F;
         p.bodyH = Anim.to(p, 0, target, 14.0F);
         p.maxScroll = Math.max(0.0F, content - maxBody);
         p.scrollTarget = Math.max(0.0F, Math.min(p.maxScroll, p.scrollTarget));
         p.scroll = Anim.approach(p.scroll, p.scrollTarget, 18.0F);
         float h = 26.0F + p.bodyH;
         p.lastX = x;
         p.lastY = y;
         p.lastW = 128.0F;
         p.lastH = h;
         this.card(x, y, 128.0F, h, 2.5F, -233103837, -12762808);
         boolean headerHover = this.over(mx, my, x, y, 128.0F, 26.0F);
         float hh = Anim.to(p, 2, headerHover ? 1.0F : 0.0F, 16.0F);
         Paint.text(p.category.label(), x + 21.0F, y + 13.0F, 8.4F, -920844, (byte)0);
         float rot = Anim.spring(p, 10, p.collapsed ? 0.0F : 180.0F, 220.0F, 17.0F);
         Paint.chevron(x + 128.0F - 10.0F, y + 13.0F, 2.6F, rot, 0.95F, Paint.a(-920844, 0.85F + 0.15F * hh));
         float sepA = Math.min(1.0F, p.bodyH / 6.0F);
         if (sepA > 0.01F) {
            float sy = y + 26.0F - 1.0F;
            float sx0 = x + 6.0F;
            float sx1 = x + 128.0F - 6.0F;
            float mid = (sx0 + sx1) / 2.0F;
            int glowC = Paint.mix(-9538692, Widgets.accent(), 0.35F) & 16777215;
            Gfx.gradientH(sx0, sy, mid, sy + 0.8F, Paint.a(805306368 | glowC, sepA), Paint.a(Integer.MIN_VALUE | glowC, sepA));
            Gfx.gradientH(mid, sy, sx1, sy + 0.8F, Paint.a(Integer.MIN_VALUE | glowC, sepA), Paint.a(805306368 | glowC, sepA));
            Gfx.gradientV(sx0, sy + 0.8F, sx1, sy + 3.5F, Paint.a(402653184 | glowC, sepA), 16777215 & glowC);
         }

         Icons.draw(Icons.of(p.category), x + 6.0F, y + 13.0F - 5.5F, 11.0F, Paint.mix(-920844, Widgets.accent(), 0.35F));
         this.hit(x, y, 128.0F, 26.0F, (button, hx, hy) -> {
            if (button == 0) {
               return true;
            } else if (button == 1) {
               this.collapse(p);
               return true;
            } else {
               return false;
            }
         });
         this.hit(x + 128.0F - 20.0F, y, 20.0F, 26.0F, (button, hx, hy) -> {
            if (button != 0 && button != 1) {
               return false;
            } else {
               this.collapse(p);
               return true;
            }
         });
         if (!(p.bodyH < 0.5F)) {
            this.hit(x, y + 26.0F, 128.0F, p.bodyH, (button, hx, hy) -> true);
            this.pushClip(x, y + 26.0F, 128.0F, p.bodyH);
            float ry = y + 26.0F + 5.0F - p.scroll;

            for(Module m : mods) {
               float vis = Anim.to(m, 0, this.matches(m) ? 1.0F : 0.0F, 15.0F);
               if (!(vis < 0.005F)) {
                  float ex = Anim.smooth(Anim.to(m, 1, this.expanded.contains(m) ? 1.0F : 0.0F, 14.0F));
                  float boxH = 22.0F + (ex > 0.001F ? this.settingsHeight(m, setW) * ex : 0.0F);
                  float block = (boxH + 4.5F) * vis;
                  if (ry + block > y + 26.0F - 2.0F && ry < y + 26.0F + p.bodyH + 2.0F) {
                     float saved = Paint.fade;
                     Paint.fade = saved * vis;
                     this.moduleRow(m, x + 5.5F, ry, innerW, boxH * vis, ex, mx, my);
                     Paint.fade = saved;
                  }

                  ry += block;
               }
            }

            if (p.maxScroll > 0.5F) {
               float trackH = p.bodyH - 6.0F;
               float barH = Math.max(14.0F, trackH * p.bodyH / (p.bodyH + p.maxScroll));
               float barY = y + 26.0F + 3.0F + (trackH - barH) * (p.scroll / p.maxScroll);
               Gfx.roundedRect(x + 128.0F - 2.6F, barY, 1.4F, barH, 0.7F, Paint.a(Widgets.accent() & 16777215 | 1879048192, 1.0F));
            }

            this.popClip();
         }
      }
   }

   private void collapse(Panel p) {
      if (System.getenv("MACE_UITEST") != null) {
         (new Throwable("[UT] collapse " + String.valueOf(p.category))).printStackTrace(System.out);
      }

      p.collapsed = !p.collapsed;
      GuiPrefs.panel(p.category, p.x, p.y, p.collapsed);
      GuiSounds.expand(!p.collapsed);
   }

   private void moduleRow(Module m, float x, float y, float w, float boxH, float ex, double mx, double my) {
      boolean hover = this.over(mx, my, x, y, w, 22.0F);
      if (hover) {
         this.hovered = m;
      }

      float hv = Anim.to(m, 2, hover ? 1.0F : 0.0F, 18.0F);
      float on = Anim.to(m, 3, m.isEnabled() ? 1.0F : 0.0F, 9.0F);
      long now = System.nanoTime();
      float pressT = this.pressed == m ? (float)(now - this.pressedAt) / 1.0E9F * GuiPrefs.animSpeed() : 9.0F;
      float squash = pressT < 0.16F ? (float)Math.sin((double)(pressT / 0.16F) * Math.PI) * 0.025F : 0.0F;
      float lift = 1.0F + 0.02F * hv * (1.0F - ex) - squash;
      boolean transform = Math.abs(lift - 1.0F) > 5.0E-4F;
      if (transform) {
         Paint.flush();
         Paint.g.pose().pushMatrix();
         Paint.g.pose().translate(x + w / 2.0F, y + 11.0F);
         Paint.g.pose().scale(lift, lift);
         Paint.g.pose().translate(-(x + w / 2.0F), -(y + 11.0F));
      }

      int ac = Widgets.accent();
      int fill = Paint.mix(-14407892, -13355202, hv * 0.6F);
      int border = Paint.mix(Paint.mix(-12170670, Paint.mix(-10328465, ac, 0.55F), on), Paint.mix(-9802121, ac, 0.3F * on), hv * 0.5F);
      Gfx.roundedRect(x, y, w, boxH, 3.0F, Paint.a(-805306368 | fill & 16777215, 1.0F));
      boolean flatBottom = ex >= 0.01F;
      if (on > 0.005F) {
         float sweep = Anim.smooth(on);
         float fw = Math.max(6.0F, w * sweep);
         int a0 = ac & 16777215;
         boolean full = fw >= w - 0.5F;
         Gfx.roundedGradient(x, y, fw, 22.0F, 3.0F, Paint.a(1543503872 | a0, on), Paint.a(570425344 | a0, on), Paint.a(570425344 | a0, on), Paint.a(1543503872 | a0, on), true, full, full && !flatBottom, !flatBottom);
         float pulse = this.toggledAt.containsKey(m) ? Math.max(0.0F, 1.0F - (float)(now - (Long)this.toggledAt.get(m)) / 4.5E8F) : 0.0F;
         Gfx.shadow(x, y, w, 22.0F, 3.0F, 4.0F + 5.0F * pulse, Paint.a(637534208 | a0, on * (0.7F + pulse)));
         float shimmer = (float)(((double)now / (double)1.0E9F * 0.35 * (double)GuiPrefs.animSpeed() + (double)m.id().hashCode() * 0.013) % (double)1.0F);
         float sx = x - 20.0F + (w + 40.0F) * shimmer;
         this.roundedBand(x, y, w, 22.0F, 3.0F, sx - 18.0F, sx, a0, Paint.a(335544320 | a0, on), !flatBottom);
         this.roundedBand(x, y, w, 22.0F, 3.0F, sx, sx + 18.0F, Paint.a(335544320 | a0, on), a0, !flatBottom);
      }

      if (pressT < 0.45F) {
         float rp = pressT / 0.45F;
         float rr = 4.0F + w * 0.9F * Anim.outCubic(rp);
         float rx0 = Math.max(x, this.pressX - rr);
         float rx1 = Math.min(x + w, this.pressX + rr);
         if (rx1 > rx0) {
            Gfx.roundedRect(rx0, y, rx1 - rx0, 22.0F, Math.min(3.0F, (rx1 - rx0) / 2.0F), Paint.a(637534208 | ac & 16777215, 1.0F - rp));
         }
      }

      Gfx.roundedOutline(x, y, w, boxH, 3.0F, 0.6F, Paint.a(border, 1.0F));
      if (ex > 0.01F) {
         Gfx.rect(x + 6.0F, y + 22.0F - 0.3F, x + w - 6.0F, y + 22.0F + 0.3F, Paint.a(813137029, ex));
      }

      int nameColor = Paint.mix(Paint.mix(Paint.mix(-5657164, -2762531, hv), -920844, ex), Paint.mix(-920844, ac, 0.38F), on);
      if (GuiState.isPinned(m.id())) {
         Gfx.circle(x + w - 15.5F, y + 11.0F, 1.5F, Paint.a(ac, 1.0F));
      }

      String name = Paint.fit(m.name(), w - 34.0F, 7.6F, (byte)0);
      Paint.text(name, x + 7.5F, y + 11.0F, 7.6F, nameColor, (byte)0);
      if (m.key() != null && m.key() != InputConstants.UNKNOWN) {
         String key = Paint.fit(m.key().getDisplayName().getString(), 30.0F, 5.6F, (byte)0);
         Paint.textRight(key, x + w - (GuiState.isPinned(m.id()) ? 20.0F : 16.0F), y + 11.0F, 5.6F, Paint.a(-8354419, 1.0F), (byte)0);
      }

      float chev = Anim.spring(m, 20, this.expanded.contains(m) ? 180.0F : 0.0F, 220.0F, 17.0F);
      Paint.chevron(x + w - 8.0F, y + 11.0F, 2.3F, chev, 0.9F, Paint.a(Paint.mix(-2762531, ac, 0.4F * on), 0.85F + 0.15F * hv));
      if (transform) {
         Paint.flush();
         Paint.g.pose().popMatrix();
      }

      this.hit(x, y, w, 22.0F, (button, hx, hy) -> {
         this.pressed = m;
         this.pressedAt = System.nanoTime();
         this.pressX = (float)hx;
         this.pressY = (float)hy;
         if (button > 1) {
            return false;
         } else if (button == 1 && Minecraft.getInstance().hasShiftDown()) {
            GuiState.togglePinned(m.id());
            GuiSounds.tap();
            return true;
         } else if (button == 1 && Minecraft.getInstance().hasControlDown()) {
            for(Setting<?> s : m.settings()) {
               s.reset();
            }

            StateStore.changed();
            GuiSounds.bind();
            return true;
         } else if (button == 0 && hx < (double)(x + w - 15.0F)) {
            m.toggle();
            this.toggledAt.put(m, System.nanoTime());
            GuiSounds.toggle(m.isEnabled());
            return true;
         } else if (button != 1 && button != 0) {
            return false;
         } else {
            this.toggleExpand(m);
            return true;
         }
      });
      if (ex > 0.01F) {
         float sx = x + 6.0F;
         float sw = w - 12.0F;
         float sy = y + 22.0F + 2.0F;
         float sh = boxH - 22.0F - 2.0F;
         this.pushClip(x, sy, w, Math.max(0.0F, sh));
         float saved = Paint.fade;
         Paint.fade = saved * Anim.smooth(ex * 1.4F - 0.2F);
         this.moduleSettings(m, sx, sy, sw, mx, my);
         Paint.fade = saved;
         this.popClip();
      }

   }

   private void roundedBand(float x, float y, float w, float h, float r, float b0, float b1, int c0, int c1, boolean roundBottom) {
      float x0 = Math.max(x, b0);
      float x1 = Math.min(x + w, b1);
      if (!(x1 - x0 < 0.2F)) {
         float span = b1 - b0;
         int l = Paint.mix(c0, c1, (x0 - b0) / span);
         int rr = Paint.mix(c0, c1, (x1 - b0) / span);
         boolean left = x0 <= x + r;
         boolean right = x1 >= x + w - r;
         Gfx.roundedGradient(x0, y, x1 - x0, h, Math.min(r, (x1 - x0) / 2.0F), l, rr, rr, l, left, right, right && roundBottom, left && roundBottom);
      }
   }

   private void tooltip(float open, double mx, double my) {
      Object h = this.hovered;
      if (h != this.tipFor) {
         this.tipFor = h;
         this.tipSince = System.nanoTime();
      }

      String var10000;
      if (h instanceof Module m) {
         var10000 = m.description();
      } else if (h instanceof Setting<?> st) {
         var10000 = st.description();
      } else {
         var10000 = "";
      }

      String text = var10000;
      if (text != null && !text.isEmpty() && !this.plateDrag && Widgets.dragging == null) {
         float t = Anim.smooth(((float)(System.nanoTime() - this.tipSince) / 1.0E9F - 0.45F) / 0.15F);
         if (!(t <= 0.01F)) {
            Paint.fade = open * t;
            float max = 150.0F;
            List<String> lines = new ArrayList();
            StringBuilder line = new StringBuilder();

            for(String word : text.split(" ")) {
               String next = line.isEmpty() ? word : String.valueOf(line) + " " + word;
               if (Paint.width(next, 6.0F, (byte)0) > max && !line.isEmpty()) {
                  lines.add(line.toString());
                  line = new StringBuilder(word);
               } else {
                  line = new StringBuilder(next);
               }
            }

            if (!line.isEmpty()) {
               lines.add(line.toString());
            }

            float w = 0.0F;

            for(String l : lines) {
               w = Math.max(w, Paint.width(l, 6.0F, (byte)0));
            }

            w += 12.0F;
            float hgt = (float)lines.size() * 8.5F + 7.0F;
            float x = (float)Math.min(mx + (double)10.0F, (double)(this.vw - w - 4.0F));
            float y = (float)Math.min(my + (double)12.0F, (double)(this.vh - hgt - 4.0F));
            Paint.flush();
            this.card(x, y + (1.0F - t) * 3.0F, w, hgt, 3.0F, -233103837, -11907498);
            float ly = y + (1.0F - t) * 3.0F + 7.75F;

            for(String l : lines) {
               Paint.text(l, x + 6.0F, ly, 6.0F, Widgets.text(), (byte)0);
               ly += 8.5F;
            }

            Paint.flush();
         }
      }
   }

   private void toggleExpand(Module m) {
      if (!this.expanded.remove(m)) {
         this.expanded.add(m);
         GuiSounds.expand(true);
      } else {
         GuiSounds.expand(false);
      }

   }

   private void moduleSettings(Module m, float x, float y, float w, double mx, double my) {
      float cy = y + 10.0F;
      Paint.text("Bind", x + 2.0F, cy, 6.9F, Widgets.text(), (byte)0);
      boolean bound = m.key() != null && m.key() != InputConstants.UNKNOWN;
      Widgets.keyChip(m, bound ? m.key().getDisplayName().getString() : "None", x + w, cy);
      this.hit(x, y, w, 20.0F, (button, hx, hy) -> {
         if (button == 1) {
            Widgets.listening = m;
            Widgets.bindTo(InputConstants.UNKNOWN);
            return true;
         } else {
            Widgets.listening = Widgets.listening == m ? null : m;
            GuiSounds.tap();
            return true;
         }
      });
      y += 20.0F;
      cy = y + 10.0F;
      Paint.text("Mode", x + 2.0F, cy, 6.9F, Widgets.text(), (byte)0);
      this.segmented(m, new String[]{"Toggle", "Hold"}, m.hold() ? 1 : 0, x + w, cy, mx, my, (indexx) -> m.setHold(indexx == 1));
      y += 21.0F;
      float ex = Anim.to(m, 1, this.expanded.contains(m) ? 1.0F : 0.0F, 14.0F);
      int index = 0;

      for(Setting<?> s : m.settings()) {
         float v = Anim.to(s, 0, s.isVisible() ? 1.0F : 0.0F, 16.0F);
         if (!(v < 0.01F)) {
            float h = Widgets.height(s, w);
            float saved = Paint.fade;
            float child = Anim.smooth(ex * 1.8F - (float)(index++) * 0.06F);
            Paint.fade = saved * v * child;
            this.setting(s, x, y, w, h * v, mx, my);
            Paint.fade = saved;
            y += (h + 1.0F) * v;
         }
      }

   }

   private void segmented(Object key, String[] labels, int selected, float right, float cy, double mx, double my, IntConsumer pick) {
      float h = 11.0F;
      float half = 0.0F;

      for(String label : labels) {
         half = Math.max(half, Paint.width(label, 6.3F, (byte)0) + 10.0F);
      }

      float w = half * (float)labels.length;
      float x = right - w;
      float y = cy - h / 2.0F;
      Widgets.pill(x, y, w, h, -14736857, -11710119);
      float pos = Anim.spring(key, 30, (float)selected, 240.0F, 20.0F);
      float inset = 1.1F;
      float hx = x + inset + pos * half;
      float hw = half - inset * 2.0F + (selected == labels.length - 1 ? 0.0F : 0.0F);
      int ac = Widgets.accent();
      Gfx.roundedRect(hx, y + inset, hw, h - inset * 2.0F, (h - inset * 2.0F) / 2.0F, Paint.a(Paint.mix(-12039084, ac, 0.35F), 1.0F));

      for(int i = 0; i < labels.length; ++i) {
         float sx = x + (float)i * half;
         boolean hover = this.over(mx, my, sx, y, half, h);
         float hv = Anim.to(labels[i] + key.hashCode(), 0, hover && i != selected ? 1.0F : 0.0F, 18.0F);
         if (hv > 0.01F) {
            Gfx.roundedRect(sx + inset, y + inset, half - inset * 2.0F, h - inset * 2.0F, (h - inset * 2.0F) / 2.0F, Paint.a(446341290, hv));
         }

         float sel = Math.max(0.0F, 1.0F - Math.abs(pos - (float)i));
         Paint.textCenter(labels[i], sx + half / 2.0F, cy, 6.3F, Paint.mix(Paint.mix(Widgets.muted(), Widgets.text(), hv), -920844, sel), (byte)0);
         this.hit(sx, y, half, h, (button, px, py) -> {
            if (i != selected) {
               pick.accept(i);
               GuiSounds.tap();
            }

            return true;
         });
      }

   }

   private void setting(Setting<?> s, float x, float y, float w, float h, double mx, double my) {
      boolean partial = h < Widgets.height(s, w) - 0.5F;
      if (partial) {
         this.pushClip(x - 3.0F, y, w + 6.0F, h);
      }

      Widgets.draw(s, x, y, w, mx, my);
      this.rects.put(s, new float[]{x, y, w, h});
      if (this.over(mx, my, x, y, w, h)) {
         this.hovered = s;
      }

      if (s.isVisible()) {
         this.hit(x, y, w, h, (button, hx, hy) -> button <= 1 && (button == 1 && Widgets.resetOnRightClick(s) || Widgets.click(s, x, y, w, hx, hy, button)));
      }

      if (partial) {
         this.popClip();
      }

   }

   private boolean button(float x, float y, float w, float h, boolean active, Object key, double mx, double my) {
      boolean hover = this.over(mx, my, x, y, w, h);
      if (hover) {
         this.hovered = key;
      }

      float hv = Anim.to(key, 0, hover ? 1.0F : 0.0F, 18.0F);
      float sel = Anim.to(key, 1, active ? 1.0F : 0.0F, 16.0F);
      int fill = Paint.mix(Paint.mix(-233103837, -232314320, hv), -231327167, sel);
      int border = Paint.mix(Paint.mix(-12762808, -10854810, hv), Paint.mix(-7696746, Widgets.accent(), 0.55F), sel);
      this.card(x, y, w, h, 2.5F, fill, border);
      return hover;
   }

   private void topBar(float open, float elapsed, double mx, double my) {
      float st = this.closing ? 1.0F : Anim.outCubic(elapsed / 0.3F);
      Paint.fade = open * st;
      float h = 19.0F;
      float gap = 5.0F;
      float modW = Paint.width("Modules", 7.0F, (byte)0) + 26.0F;
      float total = modW + 5.0F * (h + gap);
      float x = (this.vw - total) / 2.0F;
      float y = 14.0F - (1.0F - st) * 8.0F;
      this.button(x, y, modW, h, this.tab == 0, "tab.modules", mx, my);
      float modSel = Anim.to("tab.modules", 1, this.tab == 0 ? 1.0F : 0.0F, 16.0F);
      Paint.text("Modules", x + 19.0F, y + h / 2.0F, 7.0F, -920844, (byte)0);
      Icons.draw(Icons.Kind.SWORDS, x + 5.5F, y + h / 2.0F - 5.0F, 10.0F, Paint.mix(-920844, Widgets.accent(), 0.6F * modSel));
      this.hit(x, y, modW, h, (b, hx, hy) -> {
         this.setTab(0);
         return true;
      });
      float bx = x + modW + gap;
      String[] keys = new String[]{"tab.configs", "tab.friends", "tab.settings", "tab.hud", "tab.themes"};

      for(int i = 0; i < 5; ++i) {
         boolean active = i == 0 && this.tab == 2 || i == 1 && this.tab == 3 || i == 2 && this.tab == 1 || i == 4 && Widgets.popup == GuiPrefs.ACCENT;
         this.button(bx, y, h, h, active, keys[i], mx, my);
         float cx = bx + h / 2.0F;
         float cy = y + h / 2.0F;
         int c = Paint.a(Paint.mix(-920844, Widgets.accent(), 0.6F * Anim.to(keys[i], 1, active ? 1.0F : 0.0F, 16.0F)), 1.0F);
         Icons.Kind[] kinds = new Icons.Kind[]{Icons.Kind.DOCUMENT, Icons.Kind.FRIENDS, Icons.Kind.GEAR, Icons.Kind.MOVE, Icons.Kind.SPARKLE};
         Icons.draw(kinds[i], cx - 5.0F, cy - 5.0F, 10.0F, c);
         this.hit(bx, y, h, h, (b, hx, hy) -> {
            if (b != 0) {
               return true;
            } else {
               if (i == 3) {
                  ClickGui.openHudEditor();
               } else if (i == 4) {
                  if (Widgets.popup == GuiPrefs.ACCENT) {
                     Widgets.popup = null;
                  } else {
                     Widgets.popup = GuiPrefs.ACCENT;
                     Widgets.popupTitle = "Accent";
                     Widgets.popupX = hx > (double)(this.vw - 92.0F - 4.0F) ? this.vw - 92.0F - 4.0F : bx0(i, x, modW, h, gap) + h / 2.0F - 46.0F;
                     Widgets.popupY = y + h + 5.0F;
                  }

                  GuiSounds.expand(Widgets.popup != null);
               } else if (i == 0) {
                  Configs.refresh();
                  this.setTab(this.tab == 2 ? 0 : 2);
               } else if (i == 1) {
                  this.setTab(this.tab == 3 ? 0 : 3);
               } else {
                  this.setTab(this.tab == 1 ? 0 : 1);
               }

               return true;
            }
         });
         bx += h + gap;
      }

   }

   double[] toGui(double vx, double vy) {
      float zoom = 0.975F + 0.025F * this.lastOpen;
      double x = ((vx - (double)(this.vw / 2.0F)) * (double)zoom + (double)(this.vw / 2.0F)) * (double)this.scale;
      double y = ((vy - (double)(this.vh / 2.0F)) * (double)zoom + (double)(this.vh / 2.0F)) * (double)this.scale;
      return new double[]{x, y};
   }

   private static float bx0(int index, float x, float modW, float h, float gap) {
      return x + modW + gap + (float)index * (h + gap);
   }

   private static void sparkle(float cx, float cy, float r, int color) {
      float k = r * 0.28F;
      Gfx.polyline(new float[]{cx, cy - r, cx + k, cy - k, cx + r, cy, cx + k, cy + k, cx, cy + r, cx - k, cy + k, cx - r, cy, cx - k, cy - k}, 0.75F, color, true);
   }

   private void fpsPill(float open, double mx, double my) {
      Minecraft mc = Minecraft.getInstance();
      Paint.fade = open;
      String fps = String.valueOf(mc.getFps());
      int ping = 0;
      if (mc.getConnection() != null && mc.player != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         if (info != null) {
            ping = info.getLatency();
         }
      }

      String ms = ping + " ms";
      float s = 6.4F;
      float w1 = Paint.width(fps + " FPS", s, (byte)0);
      float w2 = Paint.width(ms, s, (byte)0);
      float w = w1 + w2 + 22.0F;
      float h = 15.0F;
      float x = 10.0F;
      float y = 16.0F;
      this.card(x, y, w, h, h / 2.0F, -232773074, -12761269);
      Paint.text(fps, x + 8.0F, y + h / 2.0F, s, -10497139, (byte)0);
      Paint.text(" FPS", x + 8.0F + Paint.width(fps, s, (byte)0), y + h / 2.0F, s, Widgets.text(), (byte)0);
      Gfx.rect(x + 11.0F + w1, y + 4.0F, x + 11.6F + w1, y + h - 4.0F, Paint.a(1358954495, 1.0F));
      Paint.text(ms, x + 14.0F + w1, y + h / 2.0F, s, Widgets.text(), (byte)0);
   }

   private void profileCard(float open, float elapsed) {
      Minecraft mc = Minecraft.getInstance();
      float st = this.closing ? 1.0F : Anim.outCubic((elapsed - 0.1F) / 0.3F);
      Paint.fade = open * st;
      String name = mc.getUser().getName();
      float w = plateWidth(name);
      float h = 34.0F;
      float freeW = Math.max(0.0F, this.vw - w);
      float freeH = Math.max(0.0F, this.vh - h);
      float x;
      float y;
      if (this.plateDrag) {
         x = this.plateX;
         y = this.plateY;
      } else if (GuiPrefs.plate != null) {
         x = GuiPrefs.plate[0] * freeW;
         y = GuiPrefs.plate[1] * freeH;
      } else {
         x = 10.0F;
         y = this.vh - h - 10.0F;
      }

      x = Math.clamp(x, 0.0F, freeW);
      y = Math.clamp(y, 0.0F, freeH);
      this.plateX = x;
      this.plateY = y;
      this.plateW = w;
      this.plateH = h;
      y += (1.0F - st) * 8.0F;
      float lift = Anim.to("plate", 1, this.plateDrag ? 1.0F : 0.0F, 14.0F);
      if (lift > 0.01F) {
         Gfx.shadow(x, y + 6.0F, w, h, 4.0F, 20.0F, Paint.a(1610612736, lift));
      }

      this.card(x, y, w, h, 2.5F, -233103837, -12762808);
      this.hit(x, y, w, h, (button, hx, hy) -> {
         if (button != 0) {
            return true;
         } else {
            this.plateDrag = true;
            this.plateOffX = (float)hx - this.plateX;
            this.plateOffY = (float)hy - this.plateY;
            return true;
         }
      });
      plateContent(mc, name, x, y, h);
   }

   static float plateWidth(String name) {
      return Math.max(120.0F, Paint.width(name, 7.4F, (byte)0) + 72.0F);
   }

   static float scaleFor(Minecraft mc, int scalePref) {
      float guiScale = (float)mc.getWindow().getGuiScale();
      int gw = mc.getWindow().getGuiScaledWidth();
      float s = (float)scalePref / 100.0F * Math.max(1.0F, (float)mc.getWindow().getHeight() / 540.0F) / guiScale;
      float need = 848.0F;
      return (float)gw / s < need ? (float)gw / need : s;
   }

   static void drawPlateOnHud(GuiGraphicsExtractor g) {
      Minecraft mc = Minecraft.getInstance();
      float s = scaleFor(mc, GuiPrefs.scale);
      float vw = (float)mc.getWindow().getGuiScaledWidth() / s;
      float vh = (float)mc.getWindow().getGuiScaledHeight() / s;
      String name = mc.getUser().getName();
      float w = plateWidth(name);
      float h = 34.0F;
      float freeW = Math.max(0.0F, vw - w);
      float freeH = Math.max(0.0F, vh - h);
      float x = GuiPrefs.plate != null ? GuiPrefs.plate[0] * freeW : 10.0F;
      float y = GuiPrefs.plate != null ? GuiPrefs.plate[1] * freeH : vh - h - 10.0F;
      x = Math.clamp(x, 0.0F, freeW);
      y = Math.clamp(y, 0.0F, freeH);
      Paint.g = g;
      Paint.fade = 1.0F;
      g.pose().pushMatrix();

      try {
         g.pose().scale(s, s);
         Gfx.shadow(x, y + 3.0F, w, h, 2.5F, 12.0F, Paint.a(1342177280, 1.0F));
         Gfx.roundedRect(x, y, w, h, 2.5F, Paint.a(-233103837, 1.0F));
         Gfx.roundedOutline(x, y, w, h, 2.5F, 0.6F, Paint.a(1435144088, 1.0F));
         plateContent(mc, name, x, y, h);
         Paint.flush();
      } finally {
         Paint.discard();
         g.pose().popMatrix();
      }

   }

   private static void plateContent(Minecraft mc, String name, float x, float y, float h) {
      float a = 24.0F;
      float ax = x + 6.0F;
      float ay = y + (h - a) / 2.0F;
      PlayerSkin skin = plateSkin(mc);
      if (skin != null && Paint.fade > 0.6F) {
         Gfx.roundedRect(ax, ay, a, a, 3.0F, Paint.a(-13947340, 1.0F));
         Paint.flush();
         Paint.g.pose().pushMatrix();
         Paint.g.pose().translate(ax + 2.0F, ay + 2.0F);
         Paint.g.pose().scale((a - 4.0F) / 16.0F, (a - 4.0F) / 16.0F);
         PlayerFaceExtractor.extractRenderState(Paint.g, skin, 0, 0, 16);
         Paint.g.pose().popMatrix();
      } else {
         Gfx.roundedRect(ax, ay, a, a, 3.0F, Paint.a(-13947340, 1.0F));
      }

      float tx = ax + a + 8.0F;
      Paint.text(name, tx, y + 11.5F, 7.4F, -920844, (byte)0);
      float bw = Paint.width("MACE", 4.8F, (byte)0) + 7.0F;
      float bx = tx + Paint.width(name, 7.4F, (byte)0) + 4.0F;
      Gfx.roundedRect(bx, y + 8.0F, bw, 7.0F, 2.0F, Paint.a(-11183517, 1.0F));
      Paint.textCenter("MACE", bx + bw / 2.0F, y + 11.5F, 4.8F, -1841687, (byte)0);
      Gfx.circle(tx + 1.8F, y + 23.5F, 1.6F, Paint.a(-11547778, 1.0F));
      Paint.text("v1.0", tx + 6.0F, y + 23.5F, 5.6F, Widgets.muted(), (byte)0);
   }

   private static PlayerSkin plateSkin(Minecraft mc) {
      if (mc.player != null) {
         return mc.player.getSkin();
      } else {
         if (ownSkin == null) {
            if (ownProfile == null) {
               Objects.requireNonNull(mc);
               ownProfile = CompletableFuture.supplyAsync(mc::getGameProfile);
            }

            GameProfile profile = ownProfile.isDone() && !ownProfile.isCompletedExceptionally() ? (GameProfile)ownProfile.join() : null;
            if (profile == null) {
               return DefaultPlayerSkin.get(mc.getUser().getProfileId());
            }

            ownSkin = mc.getSkinManager().createLookup(profile, false);
         }

         return (PlayerSkin)ownSkin.get();
      }
   }

   private float clearOfHud(float x, float y, float w, float h, boolean vertical) {
      for(int pass = 0; pass < 4; ++pass) {
         boolean moved = false;

         for(DraggableHud hud : HudElements.all()) {
            float[] b = hud.isHudEnabled() ? hud.getHudBounds() : null;
            if (b != null && !(b[2] <= 0.0F) && !(b[3] <= 0.0F)) {
               float bx = b[0] / this.scale;
               float by = b[1] / this.scale;
               float bw = b[2] / this.scale;
               float bh = b[3] / this.scale;
               if (x < bx + bw && x + w > bx && y < by + bh && y + h > by) {
                  if (vertical) {
                     y = by + bh + 4.0F;
                  } else {
                     x = bx + bw + 6.0F;
                  }

                  moved = true;
               }
            }
         }

         if (!moved) {
            break;
         }
      }

      return vertical ? y : x;
   }

   private void setTab(int tab) {
      if (this.tab != tab) {
         this.tab = tab;
         GuiSounds.tap();
      }

   }

   private void searchBar(float open, float elapsed, double mx, double my) {
      float st = this.closing ? 1.0F : Anim.outCubic((elapsed - 0.12F) / 0.3F);
      Paint.fade = open * st;
      boolean active = !this.query.isEmpty();
      float t = Anim.to(this.query, 0, this.searchFocus ? 1.0F : 0.0F, 14.0F);
      float grow = Anim.spring(this.query, 2, this.searchFocus ? 1.0F : (active ? 0.55F : 0.0F), 200.0F, 18.0F);
      float w = 130.0F + 80.0F * grow;
      float h = 18.0F;
      float x = (this.vw - w) / 2.0F;
      float y = this.vh - 46.0F + (1.0F - st) * 10.0F;
      boolean hoverS = this.over(mx, my, x, y, w, h);
      float hs = Anim.to(this.query, 4, hoverS ? 1.0F : 0.0F, 18.0F);
      this.card(x, y, w, h, h / 2.0F, -233103837, Paint.mix(Paint.mix(-11907498, -10262673, hs), Paint.mix(-8749434, Widgets.accent(), 0.8F), t));
      if (t > 0.01F) {
         Gfx.shadow(x, y, w, h, h / 2.0F, 6.0F, Paint.a(Widgets.accent() & 16777215 | 1073741824, t));
      }

      float ix = x + 11.0F;
      float iy = y + h / 2.0F - 0.6F;
      int ic = Paint.a(-2762531, 1.0F);
      Gfx.circleOutline(ix, iy, 2.9F, 0.9F, ic);
      Gfx.line(ix + 2.1F, iy + 2.1F, ix + 4.2F, iy + 4.2F, 1.0F, ic);
      float tx = x + 20.0F;
      String q = Paint.fit(this.query.toString(), Math.max(10.0F, w - 70.0F), 6.8F, (byte)0);
      if (!active) {
         Paint.text(this.searchFocus ? "" : "Search...", tx, y + h / 2.0F, 6.8F, Paint.a(-8354419, 1.0F), (byte)0);
      } else {
         Paint.text(q, tx, y + h / 2.0F, 6.8F, -920844, (byte)0);
      }

      if (this.searchFocus) {
         float cx = tx + (active ? Paint.width(q, 6.8F, (byte)0) + 0.8F : 0.0F);
         float blink = 0.55F + 0.45F * (float)Math.cos((double)System.nanoTime() / (double)1.0E9F * Math.PI * (double)2.0F);
         Gfx.rect(cx, y + h / 2.0F - 4.0F, cx + 0.6F, y + h / 2.0F + 4.0F, Paint.a(Paint.mix(-920844, Widgets.accent(), 0.6F), blink));
      }

      if (active) {
         int found = 0;

         for(Module m : VantaClient.MODULES.all()) {
            if (!m.hidden() && this.matches(m)) {
               ++found;
            }
         }

         Paint.textRight(found + (found == 1 ? " result" : " results"), x + w - 20.0F, y + h / 2.0F, 5.6F, Paint.mix(Widgets.muted(), Widgets.accent(), 0.7F), (byte)0);
         float bx = x + w - 11.0F;
         float by = y + h / 2.0F;
         boolean xh = this.over(mx, my, bx - 4.0F, by - 4.0F, 8.0F, 8.0F);
         Gfx.circle(bx, by, 3.6F, Paint.a(xh ? -11907498 : -13420996, 1.0F));
         int xc = Paint.a(-2762531, 1.0F);
         Gfx.line(bx - 1.4F, by - 1.4F, bx + 1.4F, by + 1.4F, 0.7F, xc);
         Gfx.line(bx - 1.4F, by + 1.4F, bx + 1.4F, by - 1.4F, 0.7F, xc);
      }

      this.hit(x, y, w, h, (button, hx, hy) -> {
         if (active && hx > (double)(x + w - 16.0F)) {
            this.query.setLength(0);
            this.onQuery();
            GuiSounds.tap();
            return true;
         } else {
            if (button == 1) {
               this.query.setLength(0);
            }

            if (!this.searchFocus) {
               GuiSounds.tap();
            }

            this.searchFocus = true;
            return true;
         }
      });
      String[][] hints = new String[][]{{GuiPrefs.keyName(), "close"}, {"Drag plate", "move it"}, {"Scroll", "long lists"}, {"Right click", "settings"}};
      float s = 5.6F;
      float total = 6.0F;

      for(String[] hint : hints) {
         total += Paint.width(hint[0], s, (byte)0) + 8.0F + 4.0F + Paint.width(hint[1], s, (byte)0) + 8.0F;
      }

      float hx = (this.vw - total) / 2.0F;
      float hy = y + h + 5.0F;
      float hh = 13.0F;
      this.card(hx, hy, total, hh, hh / 2.0F, -434430429, -13354946);
      float cx = hx + 4.0F;

      for(String[] hint : hints) {
         float kw = Paint.width(hint[0], s, (byte)0) + 8.0F;
         Gfx.roundedRect(cx, hy + 2.5F, kw, hh - 5.0F, 2.5F, Paint.a(-13881547, 1.0F));
         Paint.textCenter(hint[0], cx + kw / 2.0F, hy + hh / 2.0F, s, Widgets.text(), (byte)0);
         cx += kw + 4.0F;
         Paint.text(hint[1], cx, hy + hh / 2.0F, s, Widgets.text(), (byte)0);
         cx += Paint.width(hint[1], s, (byte)0) + 8.0F;
      }

   }

   private void settingsPage(float alpha, float tabT, double mx, double my) {
      Paint.fade = alpha;
      float w = 260.0F;
      float x = (this.vw - w) / 2.0F;
      float y = 47.0F + (1.0F - Anim.outCubic(tabT)) * 12.0F;
      float innerW = w - 28.0F;
      float content = 30.0F;

      for(GuiPrefs.Section section : GuiPrefs.sections()) {
         content += 16.0F;

         for(Setting<?> s : section.settings()) {
            content += (Widgets.height(s, innerW) + 1.0F) * Anim.to(s, 0, s.isVisible() ? 1.0F : 0.0F, 16.0F);
         }

         content += 6.0F;
      }

      float maxH = this.vh - 62.0F - y;
      float h = Math.min(content, maxH);
      this.settingsMax = Math.max(0.0F, content - maxH);
      this.settingsScrollTarget = Math.max(0.0F, Math.min(this.settingsMax, this.settingsScrollTarget));
      this.settingsScroll = Anim.approach(this.settingsScroll, this.settingsScrollTarget, 18.0F);
      this.card(x, y, w, h, 3.0F, -233103837, -12762808);
      this.hit(x, y, w, h, (button, hx, hy) -> true);
      this.pushClip(x, y, w, h);
      float cy = y + 16.0F - this.settingsScroll;
      Paint.text("Client settings", x + 14.0F, cy, 8.4F, -920844, (byte)0);
      Paint.textRight("Saved automatically", x + w - 14.0F, cy, 5.6F, Paint.a(Widgets.muted(), 0.7F), (byte)0);
      cy += 14.0F;

      for(GuiPrefs.Section section : GuiPrefs.sections()) {
         Paint.text(section.title().toUpperCase(Locale.ROOT), x + 14.0F, cy + 8.0F, 5.4F, Widgets.muted(), (byte)0);
         Gfx.rect(x + 14.0F + Paint.width(section.title().toUpperCase(Locale.ROOT), 5.4F, (byte)1) + 6.0F, cy + 8.0F, x + w - 14.0F, cy + 8.6F, Paint.a(419430399, 1.0F));
         cy += 16.0F;

         for(Setting<?> s : section.settings()) {
            float v = Anim.to(s, 0, s.isVisible() ? 1.0F : 0.0F, 16.0F);
            if (!(v < 0.01F)) {
               float sh = Widgets.height(s, innerW);
               float saved = Paint.fade;
               Paint.fade = saved * v;
               this.setting(s, x + 14.0F, cy, innerW, sh * v, mx, my);
               Paint.fade = saved;
               cy += (sh + 1.0F) * v;
            }
         }

         cy += 6.0F;
      }

      this.popClip();
   }

   private boolean chip(String label, float right, float cy, int fill, int border, int color, double mx, double my, Runnable action) {
      float w = Paint.width(label, 5.8F, (byte)0) + 10.0F;
      float x = right - w;
      boolean hover = this.over(mx, my, x, cy - 5.5F, w, 11.0F);
      Widgets.pill(x, cy - 5.5F, w, 11.0F, hover ? Paint.mix(fill, -10854810, 0.25F) : fill, border);
      Paint.textCenter(label, x + w / 2.0F, cy, 5.8F, color, (byte)0);
      this.hit(x, cy - 5.5F, w, 11.0F, (b, hx, hy) -> {
         if (b == 0) {
            action.run();
         }

         return true;
      });
      return hover;
   }

   private void configsPage(float alpha, float tabT, double mx, double my) {
      Paint.fade = alpha;
      float w = 260.0F;
      float x = (this.vw - w) / 2.0F;
      float y = 47.0F + (1.0F - Anim.outCubic(tabT)) * 12.0F;
      float innerW = w - 28.0F;
      List<String> names = Configs.list();
      float content = 80.0F + (float)Math.max(1, names.size()) * 20.0F + 10.0F;
      float maxH = this.vh - 70.0F - y;
      float h = Math.min(content, maxH);
      float max = Math.max(0.0F, content - maxH);
      this.configScrollTarget = Math.max(0.0F, Math.min(max, this.configScrollTarget));
      this.configScroll = Anim.approach(this.configScroll, this.configScrollTarget, 18.0F);
      this.card(x, y, w, h, 3.0F, -233103837, -12762808);
      this.hit(x, y, w, h, (button, hx, hy) -> true);
      this.pushClip(x, y, w, h);
      float cy = y + 16.0F - this.configScroll;
      float ix = x + 14.0F;
      Icons.draw(Icons.Kind.DOCUMENT, ix, cy - 5.0F, 10.0F, -920844);
      Paint.text("Configs", ix + 14.0F, cy, 8.4F, -920844, (byte)0);
      String active = GuiPrefs.activeConfig.isEmpty() ? "No config loaded" : "Active: " + GuiPrefs.activeConfig;
      Paint.textRight(active, x + w - 14.0F, cy, 5.8F, GuiPrefs.activeConfig.isEmpty() ? Widgets.muted() : Paint.mix(Widgets.accent(), -1, 0.3F), (byte)0);
      cy += 12.0F;
      float fieldW = innerW - 52.0F;
      this.setting(this.configName, ix, cy, fieldW, Widgets.height(this.configName, fieldW), mx, my);
      float sy = cy + 21.25F;
      this.chip("Save", ix + innerW, sy, -12039084, -9802121, -920844, mx, my, () -> {
         String name = Configs.clean(Widgets.typing == this.configName ? Widgets.buffer.toString() : (String)this.configName.get());
         if (name.isEmpty()) {
            int var10000 = Configs.list().size();
            name = "config-" + (var10000 + 1);
         }

         if (Configs.save(name)) {
            this.configName.set("");
            GuiSounds.bind();
         }

      });
      cy += 34.0F;
      Paint.text("SAVED", ix, cy + 6.0F, 5.4F, Widgets.muted(), (byte)0);
      Gfx.rect(ix + Paint.width("SAVED", 5.4F, (byte)0) + 6.0F, cy + 6.0F, x + w - 14.0F, cy + 6.6F, Paint.a(419430399, 1.0F));
      cy += 14.0F;
      if (names.isEmpty()) {
         Paint.text("No saved configs yet. Name one above and press Save.", ix, cy + 8.0F, 6.0F, Widgets.muted(), (byte)0);
      }

      if (this.pendingDelete != null && System.currentTimeMillis() - this.pendingAt > 3000L) {
         this.pendingDelete = null;
      }

      for(String name : names) {
         boolean isActive = name.equals(GuiPrefs.activeConfig);
         boolean hover = this.over(mx, my, ix - 4.0F, cy, innerW + 8.0F, 18.0F);
         float hv = Anim.to(name, 0, hover ? 1.0F : 0.0F, 18.0F);
         Gfx.roundedRect(ix - 4.0F, cy, innerW + 8.0F, 18.0F, 3.0F, Paint.a(Paint.mix(-14407892, -13815754, hv), 1.0F));
         Gfx.roundedOutline(ix - 4.0F, cy, innerW + 8.0F, 18.0F, 3.0F, 0.6F, Paint.a(isActive ? -9802121 : -12170670, 1.0F));
         float rcy = cy + 9.0F;
         if (isActive) {
            Gfx.circle(ix + 1.5F, rcy, 1.6F, Paint.a(Widgets.accent(), 1.0F));
         }

         Paint.text(Paint.fit(name, innerW - 120.0F, 6.8F, (byte)0), ix + (isActive ? 6.0F : 2.0F), rcy, 6.8F, isActive ? -920844 : Widgets.text(), (byte)0);
         float right = ix + innerW;
         boolean confirm = name.equals(this.pendingDelete);
         String del = confirm ? "Confirm?" : "Delete";
         this.chip(del, right, rcy, confirm ? -10868176 : -13947340, confirm ? -5222308 : -11710119, confirm ? -10534 : -920844, mx, my, () -> {
            if (name.equals(this.pendingDelete)) {
               this.pendingDelete = null;
               Configs.delete(name);
               GuiSounds.toggle(false);
            } else {
               this.pendingDelete = name;
               this.pendingAt = System.currentTimeMillis();
               GuiSounds.tap();
            }

         });
         right -= Paint.width(del, 5.8F, (byte)0) + 14.0F;
         this.chip("Overwrite", right, rcy, -13947340, -11710119, -920844, mx, my, () -> {
            if (Configs.save(name)) {
               GuiSounds.bind();
            }

         });
         right -= Paint.width("Overwrite", 5.8F, (byte)0) + 14.0F;
         this.chip("Load", right, rcy, -12039084, -9802121, -920844, mx, my, () -> {
            if (Configs.load(name)) {
               GuiSounds.toggle(true);
            }

         });
         cy += 20.0F;
      }

      this.popClip();
   }

   static String cleanName(String raw) {
      StringBuilder out = new StringBuilder();

      for(char ch : raw.trim().toCharArray()) {
         if ((ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' || ch == '_') && out.length() < 16) {
            out.append(ch);
         }
      }

      return out.toString();
   }

   private void addFriend(String raw) {
      String name = cleanName(raw);
      if (!name.isEmpty() && !FriendList.contains(name)) {
         FriendList.add(name);
         GuiSounds.toggle(true);
      } else {
         GuiSounds.tap();
      }
   }

   private static List<String> onlineNotFriends(Minecraft mc) {
      List<String> out = new ArrayList();
      String self = mc.player != null ? mc.player.getGameProfile().name() : mc.getUser().getName();
      Consumer<String> offer = (name) -> {
         if (name != null && !name.isBlank() && !name.equalsIgnoreCase(self) && !FriendList.contains(name)) {
            Stream var10000 = out.stream();
            Objects.requireNonNull(name);
            if (var10000.noneMatch(name::equalsIgnoreCase)) {
               out.add(name);
            }
         }

      };
      if (mc.getConnection() != null) {
         for(PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            offer.accept(info.getProfile().name());
         }
      }

      if (mc.level != null) {
         for(Player p : mc.level.players()) {
            offer.accept(p.getGameProfile().name());
         }
      }

      out.sort(String.CASE_INSENSITIVE_ORDER);
      return out;
   }

   private static PlayerSkin skinFor(Minecraft mc, String name) {
      if (mc.getConnection() != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfoIgnoreCase(name);
         if (info != null) {
            return info.getSkin();
         }
      }

      if (mc.level != null) {
         for(Player p : mc.level.players()) {
            if (p instanceof AbstractClientPlayer) {
               AbstractClientPlayer cp = (AbstractClientPlayer)p;
               if (p.getGameProfile().name().equalsIgnoreCase(name)) {
                  return cp.getSkin();
               }
            }
         }
      }

      return DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name));
   }

   private static boolean isOnline(Minecraft mc, String name) {
      if (mc.getConnection() != null && mc.getConnection().getPlayerInfoIgnoreCase(name) != null) {
         return true;
      } else {
         return mc.level != null && mc.level.players().stream().anyMatch((p) -> p.getGameProfile().name().equalsIgnoreCase(name));
      }
   }

   private static void face(Minecraft mc, String name, float x, float y, float a) {
      Gfx.roundedRect(x, y, a, a, 2.5F, Paint.a(-13947340, 1.0F));
      if (Paint.fade > 0.6F) {
         Paint.flush();
         Paint.g.pose().pushMatrix();
         Paint.g.pose().translate(x + 1.5F, y + 1.5F);
         Paint.g.pose().scale((a - 3.0F) / 16.0F, (a - 3.0F) / 16.0F);
         PlayerFaceExtractor.extractRenderState(Paint.g, skinFor(mc, name), 0, 0, 16);
         Paint.g.pose().popMatrix();
      }

   }

   private void friendsPage(float alpha, float tabT, double mx, double my) {
      Minecraft mc = Minecraft.getInstance();
      Paint.fade = alpha;
      float w = 260.0F;
      float x = (this.vw - w) / 2.0F;
      float y = 47.0F + (1.0F - Anim.outCubic(tabT)) * 12.0F;
      float innerW = w - 28.0F;
      List<String> friends = FriendList.all();
      List<String> online = onlineNotFriends(mc);
      float content = 78.0F + (float)Math.max(1, friends.size()) * 20.0F + 8.0F + 14.0F + (float)Math.max(1, online.size()) * 20.0F + 10.0F;
      float maxH = this.vh - 70.0F - y;
      float h = Math.min(content, maxH);
      float max = Math.max(0.0F, content - maxH);
      this.friendScrollTarget = Math.max(0.0F, Math.min(max, this.friendScrollTarget));
      this.friendScroll = Anim.approach(this.friendScroll, this.friendScrollTarget, 18.0F);
      this.friendRects.clear();
      this.card(x, y, w, h, 3.0F, -233103837, -12762808);
      this.hit(x, y, w, h, (button, hx, hy) -> true);
      this.pushClip(x, y, w, h);
      float cy = y + 16.0F - this.friendScroll;
      float ix = x + 14.0F;
      Icons.draw(Icons.Kind.FRIENDS, ix, cy - 5.0F, 10.0F, -920844);
      Paint.text("Friends", ix + 14.0F, cy, 8.4F, -920844, (byte)0);
      Paint.textRight("Combat modules never target friends", x + w - 14.0F, cy, 5.6F, Paint.a(Widgets.muted(), 0.7F), (byte)0);
      cy += 12.0F;
      float fieldW = innerW - 52.0F;
      this.setting(this.friendName, ix, cy, fieldW, Widgets.height(this.friendName, fieldW), mx, my);
      float sy = cy + 21.25F;
      this.chip("Add", ix + innerW, sy, -12039084, -9802121, -920844, mx, my, () -> {
         this.addFriend(Widgets.typing == this.friendName ? Widgets.buffer.toString() : (String)this.friendName.get());
         this.friendName.set("");
      });
      cy += 34.0F;
      String head = friends.isEmpty() ? "FRIENDS" : "FRIENDS  " + friends.size();
      Paint.text(head, ix, cy + 6.0F, 5.4F, Widgets.muted(), (byte)0);
      Gfx.rect(ix + Paint.width(head, 5.4F, (byte)0) + 6.0F, cy + 6.0F, x + w - 14.0F, cy + 6.6F, Paint.a(419430399, 1.0F));
      cy += 14.0F;
      if (friends.isEmpty()) {
         Paint.text("No friends yet. Type a name above and press Enter.", ix, cy + 8.0F, 6.0F, Widgets.muted(), (byte)0);
         cy += 20.0F;
      }

      for(String name : friends) {
         String var10000 = name.toLowerCase(Locale.ROOT);
         Object key = "friend.row:" + var10000;
         boolean hover = this.over(mx, my, ix - 4.0F, cy, innerW + 8.0F, 18.0F);
         float hv = Anim.to(key, 0, hover ? 1.0F : 0.0F, 18.0F);
         Gfx.roundedRect(ix - 4.0F, cy, innerW + 8.0F, 18.0F, 3.0F, Paint.a(Paint.mix(-14407892, -13815754, hv), 1.0F));
         Gfx.roundedOutline(ix - 4.0F, cy, innerW + 8.0F, 18.0F, 3.0F, 0.6F, Paint.a(-12170670, 1.0F));
         float rcy = cy + 9.0F;
         face(mc, name, ix - 1.0F, cy + 3.0F, 12.0F);
         String shown = Paint.fit(name, innerW - 60.0F, 6.8F, (byte)0);
         Paint.text(shown, ix + 16.0F, rcy, 6.8F, -920844, (byte)0);
         if (isOnline(mc, name)) {
            float nx = ix + 16.0F + Paint.width(shown, 6.8F, (byte)0) + 6.0F;
            Gfx.circle(nx + 1.6F, rcy, 1.6F, Paint.a(-11547778, 1.0F));
            Paint.text("online", nx + 6.0F, rcy, 5.6F, Widgets.muted(), (byte)0);
         }

         float bs = 11.0F;
         float bx = ix + innerW - bs;
         float by = rcy - bs / 2.0F;
         boolean xh = this.over(mx, my, bx, by, bs, bs);
         if (xh) {
            this.hovered = key;
         }

         float xt = Anim.to(key, 1, xh ? 1.0F : 0.0F, 18.0F);
         Widgets.pill(bx, by, bs, bs, Paint.mix(-13947340, -10868176, xt), Paint.mix(-11710119, -5222308, xt));
         int xc = Paint.a(Paint.mix(-920844, -10534, xt), 1.0F);
         float xr = 2.0F;
         Gfx.line(bx + bs / 2.0F - xr, rcy - xr, bx + bs / 2.0F + xr, rcy + xr, 0.75F, xc);
         Gfx.line(bx + bs / 2.0F - xr, rcy + xr, bx + bs / 2.0F + xr, rcy - xr, 0.75F, xc);
         this.friendRects.put("remove:" + name.toLowerCase(Locale.ROOT), new float[]{bx, by, bs, bs});
         this.hit(bx, by, bs, bs, (b, hx, hy) -> {
            if (b == 0) {
               FriendList.remove(name);
               GuiSounds.toggle(false);
            }

            return true;
         });
         cy += 20.0F;
      }

      cy += 8.0F;
      Paint.text("ADD PLAYERS ONLINE", ix, cy + 6.0F, 5.4F, Widgets.muted(), (byte)0);
      Gfx.rect(ix + Paint.width("ADD PLAYERS ONLINE", 5.4F, (byte)0) + 6.0F, cy + 6.0F, x + w - 14.0F, cy + 6.6F, Paint.a(419430399, 1.0F));
      cy += 14.0F;
      if (online.isEmpty()) {
         Paint.text(mc.level == null ? "Join a world or server to pick players from the tab list." : "No other players online.", ix, cy + 8.0F, 6.0F, Widgets.muted(), (byte)0);
      }

      for(String name : online) {
         Object key = "friend.online:" + name.toLowerCase(Locale.ROOT);
         boolean hover = this.over(mx, my, ix - 4.0F, cy, innerW + 8.0F, 18.0F);
         if (hover) {
            this.hovered = key;
         }

         float hv = Anim.to(key, 0, hover ? 1.0F : 0.0F, 18.0F);
         Gfx.roundedRect(ix - 4.0F, cy, innerW + 8.0F, 18.0F, 3.0F, Paint.a(Paint.mix(-14407892, -13815754, hv), 1.0F));
         Gfx.roundedOutline(ix - 4.0F, cy, innerW + 8.0F, 18.0F, 3.0F, 0.6F, Paint.a(Paint.mix(-12170670, -10328465, hv), 1.0F));
         float rcy = cy + 9.0F;
         face(mc, name, ix - 1.0F, cy + 3.0F, 12.0F);
         Paint.text(Paint.fit(name, innerW - 60.0F, 6.8F, (byte)0), ix + 16.0F, rcy, 6.8F, Widgets.text(), (byte)0);
         float aw = Paint.width("+ Add", 5.8F, (byte)0) + 10.0F;
         Widgets.pill(ix + innerW - aw, rcy - 5.5F, aw, 11.0F, Paint.mix(-13947340, -12039084, hv), Paint.mix(-11710119, -9802121, hv));
         Paint.textCenter("+ Add", ix + innerW - aw / 2.0F, rcy, 5.8F, -920844, (byte)0);
         this.friendRects.put("online:" + name.toLowerCase(Locale.ROOT), new float[]{ix - 4.0F, cy, innerW + 8.0F, 18.0F});
         this.hit(ix - 4.0F, cy, innerW + 8.0F, 18.0F, (b, hx, hy) -> {
            if (b == 0) {
               this.addFriend(name);
            }

            return true;
         });
         cy += 20.0F;
      }

      this.popClip();
   }

   void testConfigs() {
      Configs.save("pvp");
      Configs.save("legit");
      Configs.load("pvp");
      this.configName.set("my setup");
      this.tab = 2;
   }

   boolean covers(double gx, double gy) {
      double mx = gx / (double)this.scale;
      double my = gy / (double)this.scale;

      for(Hit h : this.hits) {
         if (Paint.in(mx, my, h.x, h.y, h.w, h.h)) {
            return true;
         }
      }

      return false;
   }

   private double[] virt(double gx, double gy) {
      double mx = gx / (double)this.scale;
      double my = gy / (double)this.scale;
      float zoom = 0.975F + 0.025F * this.lastOpen;
      mx = (mx - (double)(this.vw / 2.0F)) / (double)zoom + (double)(this.vw / 2.0F);
      my = (my - (double)(this.vh / 2.0F)) / (double)zoom + (double)(this.vh / 2.0F);
      return new double[]{mx, my};
   }

   boolean mouseClicked(double gx, double gy, int button) {
      if (this.closing) {
         return false;
      } else {
         double[] v = this.virt(gx, gy);
         if (Widgets.listening != null && button > 1) {
            Widgets.bindTo(Type.MOUSE.getOrCreate(button));
            return true;
         } else {
            boolean wasFocus = this.searchFocus;
            this.searchFocus = false;
            if (Widgets.popupClick(v[0], v[1])) {
               return true;
            } else {
               for(int i = this.hits.size() - 1; i >= 0; --i) {
                  Hit h = (Hit)this.hits.get(i);
                  if (Paint.in(v[0], v[1], h.x, h.y, h.w, h.h)) {
                     if (Widgets.typing != null) {
                        Widgets.commit();
                        Widgets.typing = null;
                     }

                     if (Widgets.listening != null && h.action != null) {
                        Widgets.listening = null;
                     }

                     return h.action.run(button, v[0], v[1]);
                  }
               }

               if (Widgets.typing != null) {
                  Widgets.commit();
                  Widgets.typing = null;
               }

               Widgets.listening = null;
               return false;
            }
         }
      }
   }

   boolean mouseDragged(double gx, double gy) {
      double[] v = this.virt(gx, gy);
      if (this.plateDrag) {
         this.plateX = Math.clamp((float)v[0] - this.plateOffX, 0.0F, Math.max(0.0F, this.vw - this.plateW));
         this.plateY = Math.clamp((float)v[1] - this.plateOffY, 0.0F, Math.max(0.0F, this.vh - this.plateH));
         return true;
      } else if (Widgets.dragging != null) {
         Widgets.drag(v[0], v[1]);
         return true;
      } else {
         return false;
      }
   }

   boolean mouseReleased() {
      boolean used = false;
      if (this.plateDrag) {
         this.plateDrag = false;
         float freeW = Math.max(1.0F, this.vw - this.plateW);
         float freeH = Math.max(1.0F, this.vh - this.plateH);
         GuiPrefs.plate = new float[]{Math.clamp(this.plateX / freeW, 0.0F, 1.0F), Math.clamp(this.plateY / freeH, 0.0F, 1.0F)};
         GuiPrefs.changed();
         used = true;
      }

      if (Widgets.dragging != null) {
         Widgets.release();
         used = true;
      }

      return used;
   }

   boolean mouseScrolled(double gx, double gy, double amount) {
      double[] v = this.virt(gx, gy);
      if (this.tab == 1) {
         this.settingsScrollTarget -= (float)amount * 24.0F;
         return true;
      } else if (this.tab == 2) {
         this.configScrollTarget -= (float)amount * 24.0F;
         return true;
      } else if (this.tab == 3) {
         this.friendScrollTarget -= (float)amount * 24.0F;
         return true;
      } else {
         for(int i = this.panels.size() - 1; i >= 0; --i) {
            Panel p = (Panel)this.panels.get(i);
            if (Paint.in(v[0], v[1], p.lastX, p.lastY, p.lastW, p.lastH)) {
               p.scrollTarget -= (float)amount * 26.0F;
               return true;
            }
         }

         return false;
      }
   }

   boolean keyPressed(KeyEvent event) {
      int k = event.key();
      if (k >= 290 && k <= 314) {
         return false;
      } else if (this.closing) {
         return false;
      } else if (Widgets.typing != this.friendName || k != 257 && k != 335) {
         if (Widgets.busy()) {
            return Widgets.key(event);
         } else {
            boolean ctrl = (event.modifiers() & 2) != 0;
            if (k == 70 && ctrl) {
               this.searchFocus = true;
               return true;
            } else if (k == 256) {
               if (this.searchFocus) {
                  this.searchFocus = false;
                  return true;
               } else if (Widgets.popup != null) {
                  Widgets.popup = null;
                  return true;
               } else {
                  ClickGui.close();
                  return true;
               }
            } else if (!this.searchFocus || k != 257 && k != 335) {
               if (GuiPrefs.bound() && GuiPrefs.key.getType() == Type.KEYSYM && GuiPrefs.key.getValue() == k) {
                  ClickGui.close();
                  return true;
               } else {
                  if (this.searchFocus && k == 259 && !this.query.isEmpty()) {
                     if ((event.modifiers() & 2) != 0) {
                        this.query.setLength(0);
                     } else {
                        this.query.deleteCharAt(this.query.length() - 1);
                     }

                     this.onQuery();
                  }

                  return true;
               }
            } else {
               this.searchFocus = false;
               return true;
            }
         }
      } else {
         this.addFriend(Widgets.buffer.toString());
         Widgets.buffer = new StringBuilder();
         return true;
      }
   }

   boolean charTyped(int codepoint) {
      if (this.closing) {
         return false;
      } else if (Widgets.chr(codepoint)) {
         return true;
      } else if (!this.searchFocus) {
         return true;
      } else {
         if (codepoint >= 32 && codepoint != 127 && this.query.length() < 32) {
            if (this.query.isEmpty() && codepoint == 32) {
               return true;
            }

            this.query.appendCodePoint(codepoint);
            this.onQuery();
         }

         return true;
      }
   }

   private void onQuery() {
      if (this.tab != 0) {
         this.tab = 0;
      }

      for(Panel p : this.panels) {
         p.scrollTarget = 0.0F;
      }

   }

   void testExpand(String id) {
      Optional var10000 = VantaClient.MODULES.find(id);
      Set var10001 = this.expanded;
      Objects.requireNonNull(var10001);
      var10000.ifPresent(var10001::add);
   }

   void testSearch(String text) {
      this.searchFocus = !text.isEmpty();
      this.query.setLength(0);
      this.query.append(text);
      this.onQuery();
   }

   void testTab(int tab) {
      this.tab = tab;
   }

   int tabForTest() {
      return this.tab;
   }

   float vwForTest() {
      return this.vw;
   }

   TextSetting friendFieldForTest() {
      return this.friendName;
   }

   float testScale() {
      return this.scale;
   }

   void resetLayout() {
      GuiPrefs.PANELS.clear();
      GuiPrefs.plate = null;
      this.layoutDefaults(false);
   }

   float[] testDragPlate(float dx, float dy) {
      float zoom = 0.975F + 0.025F * this.lastOpen;
      float vx = this.plateX + this.plateW / 2.0F;
      float vy = this.plateY + this.plateH / 2.0F;
      double gx = (double)(((vx - this.vw / 2.0F) * zoom + this.vw / 2.0F) * this.scale);
      double gy = (double)(((vy - this.vh / 2.0F) * zoom + this.vh / 2.0F) * this.scale);
      double ex = (double)(((vx + dx - this.vw / 2.0F) * zoom + this.vw / 2.0F) * this.scale);
      double ey = (double)(((vy + dy - this.vh / 2.0F) * zoom + this.vh / 2.0F) * this.scale);
      this.mouseClicked(gx, gy, 0);

      for(int i = 1; i <= 8; ++i) {
         this.mouseDragged(gx + (ex - gx) * (double)i / (double)8.0F, gy + (ey - gy) * (double)i / (double)8.0F);
      }

      this.mouseReleased();
      return new float[]{this.plateX, this.plateY};
   }

   private static record Hit(float x, float y, float w, float h, Action action) {
   }

   private final class Panel {
      final Category category;
      final int index;
      float x;
      float y;
      boolean collapsed;
      float scroll;
      float scrollTarget;
      float maxScroll;
      float bodyH;
      float lastX;
      float lastY;
      float lastW;
      float lastH;

      Panel(Category category, int index) {
         Objects.requireNonNull(GuiView.this);
         super();
         this.category = category;
         this.index = index;
      }
   }

   interface Action {
      boolean run(int var1, double var2, double var4);
   }
}
