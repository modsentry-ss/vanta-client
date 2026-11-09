package vanta.module.impl.hud;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import vanta.VantaClient;
import vanta.event.events.HudRenderEvent;
import vanta.event.events.TickEvent;
import vanta.module.HudModule;
import vanta.render.Gfx;
import vanta.setting.BoolSetting;
import vanta.setting.ColorSetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.PositionSetting;
import vanta.setting.Setting;
import vanta.setting.SliderSetting;
import vanta.ui.Theme;
import vanta.ui.UiScale;
import vanta.util.Clicks;
import vanta.util.Colors;
import vanta.util.KeyBindUtil;
import vanta.visual.Fx;
import vanta.visual.hud.HudKit;
import vanta.visual.hud.HudLook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Input;

public final class Keystrokes extends HudModule {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final int KEY_W = 0;
   private static final int KEY_A = 1;
   private static final int KEY_S = 2;
   private static final int KEY_D = 3;
   private static final int KEY_LMB = 4;
   private static final int KEY_RMB = 5;
   private static final int KEY_SPACE = 6;
   private static final int KEY_SNEAK = 7;
   private static final int KEY_COUNT = 8;
   private static final int STYLE_CLASSIC = 1;
   private static final int LAYOUT_MOUSE_TOP = 1;
   private static final int LAYOUT_SIDE = 2;
   private static final int LABELS_BOUND = 1;
   private static final int LABELS_ARROWS = 2;
   private static final int CPS_INSTEAD = 1;
   private static final int PRESS_FILL = 0;
   private static final int PRESS_GROW = 1;
   private static final int PRESS_OUTLINE = 2;
   private static final int SPACE_LINE = 0;
   private static final int SPACE_LABEL = 1;
   private static final int MODE_CUSTOM = 0;
   private static final int MODE_HUD = 1;
   private static final PositionSetting.Position HOME;
   private final EnumSetting style = (EnumSetting)this.add(new EnumSetting("style", 0, new String[]{"Flat", "Classic"}));
   private final EnumSetting layout = (EnumSetting)this.add(new EnumSetting("layout", 0, new String[]{"Standard", "Mouse-Top", "Side"}));
   private final EnumSetting colorMode = (EnumSetting)this.add(new EnumSetting("colorMode", 1, new String[]{"Custom", "HUD Theme"}));
   private final ColorSetting color = (ColorSetting)this.add(new ColorSetting("color", 5221630));
   private final SliderSetting alpha = (SliderSetting)this.add(new SliderSetting("alpha", 255, 40, 255));
   private final SliderSetting scale;
   private final SliderSetting size;
   private final SliderSetting gap;
   private final SliderSetting radius;
   private final SliderSetting idleAlpha;
   private final EnumSetting keyLabels;
   private final SliderSetting labelSize;
   private final EnumSetting labelColor;
   private final ColorSetting customLabelColor;
   private final EnumSetting pressStyle;
   private final SliderSetting animationSpeed;
   private final BoolSetting pressGlow;
   private final BoolSetting pressShrink;
   private final BoolSetting movementKeys;
   private final BoolSetting showMouse;
   private final BoolSetting showCps;
   private final EnumSetting cpsStyle;
   private final BoolSetting showSpace;
   private final EnumSetting spaceMark;
   private final BoolSetting showSneak;
   private final HudLook look;
   private final float[] press;
   private final float[] pulse;
   private final boolean[] wasDown;
   private final Deque<Long> leftClicks;
   private final Deque<Long> rightClicks;
   private final long[] lastFrame;
   private final boolean[] driven;
   public static volatile boolean[] testHeld;
   private final boolean[] held;
   private final Key[] keys;
   private int keyCount;
   private final float[] box;
   private final float[] blockSize;
   private final float[] blockAt;
   private final int[] cpsShown;
   private final float[] cpsRoom;
   private final float[] cpsSize;
   private final String[] cpsText;
   private final int[] boundCode;
   private final String[] boundName;

   public Keystrokes() {
      super("keystrokes", HOME.horizontal(), HOME.vertical(), HOME.offsetX(), HOME.offsetY());
      this.scale = (SliderSetting)this.add(new SliderSetting("scale", 100, 25, 1000, Measure.PERCENT));
      this.size = (SliderSetting)this.add(new SliderSetting("keySize", 20, 12, 32));
      this.gap = (SliderSetting)this.add(new SliderSetting("gap", 2, 0, 8));
      this.radius = (SliderSetting)this.add(new SliderSetting("radius", 5, 0, 10));
      this.idleAlpha = (SliderSetting)this.add(new SliderSetting("idleAlpha", 80, 0, 255));
      this.keyLabels = (EnumSetting)this.add(new EnumSetting("keyLabels", 0, new String[]{"Letters", "Bound", "Arrows"}));
      this.labelSize = (SliderSetting)this.add(new SliderSetting("labelSize", 100, 50, 150, Measure.PERCENT));
      this.labelColor = (EnumSetting)this.add(new EnumSetting("labelColor", 0, new String[]{"Theme", "Custom"}));
      this.customLabelColor = (ColorSetting)this.add(new ColorSetting("customLabelColor", 15922165));
      this.pressStyle = (EnumSetting)this.add(new EnumSetting("pressStyle", 0, new String[]{"Fill", "Grow", "Outline"}));
      this.animationSpeed = (SliderSetting)this.add(new SliderSetting("animationSpeed", 14, 2, 30));
      this.pressGlow = (BoolSetting)this.add(new BoolSetting("pressGlow", false));
      this.pressShrink = (BoolSetting)this.add(new BoolSetting("pressShrink", false));
      this.movementKeys = (BoolSetting)this.add(new BoolSetting("movementKeys", true));
      this.showMouse = (BoolSetting)this.add(new BoolSetting("mouseKeys", true));
      this.showCps = (BoolSetting)this.add(new BoolSetting("showCps", true));
      this.cpsStyle = (EnumSetting)this.add(new EnumSetting("cpsStyle", 0, new String[]{"Below", "Instead"}));
      this.showSpace = (BoolSetting)this.add(new BoolSetting("spaceBar", true));
      this.spaceMark = (EnumSetting)this.add(new EnumSetting("spaceMark", 0, new String[]{"Line", "Label", "None"}));
      this.showSneak = (BoolSetting)this.add(new BoolSetting("sneakKey", false));
      this.press = new float[8];
      this.pulse = new float[8];
      this.wasDown = new boolean[8];
      this.leftClicks = new ArrayDeque();
      this.rightClicks = new ArrayDeque();
      this.lastFrame = new long[1];
      this.driven = new boolean[8];
      this.held = new boolean[8];
      this.keys = new Key[8];
      this.box = new float[4];
      this.blockSize = new float[2];
      this.blockAt = new float[2];
      this.cpsShown = new int[]{-1, -1};
      this.cpsRoom = new float[]{-1.0F, -1.0F};
      this.cpsSize = new float[]{-1.0F, -1.0F};
      this.cpsText = new String[]{"", ""};
      this.boundCode = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
      this.boundName = new String[8];

      for(int i = 0; i < 8; ++i) {
         this.keys[i] = new Key();
      }

      HudLook look = new HudLook(false, false);
      look.settings().removeIf((settingx) -> settingx.id().equals("backgroundOpacity"));

      for(Setting<?> setting : look.settings()) {
         if (setting.id().equals("shadow") && setting instanceof EnumSetting shadow) {
            shadow.set(3);
         }
      }

      this.look = (HudLook)this.add(look);
      this.layout.visibleWhen(() -> (Boolean)this.showMouse.get() && (Boolean)this.movementKeys.get());
      this.color.visibleWhen(() -> (Integer)this.colorMode.get() == 0);
      this.radius.visibleWhen(() -> !this.look.squared());
      this.customLabelColor.visibleWhen(() -> (Integer)this.labelColor.get() == 1);
      this.pressStyle.visibleWhen(() -> (Integer)this.style.get() != 1);
      this.animationSpeed.visibleWhen(() -> this.look.look().animate);
      this.pressGlow.visibleWhen(() -> (Integer)this.style.get() != 1);
      this.pressShrink.visibleWhen(() -> (Integer)this.style.get() != 1);
      BoolSetting var10000 = this.showCps;
      BoolSetting var10001 = this.showMouse;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.cpsStyle.visibleWhen(() -> (Boolean)this.showMouse.get() && (Boolean)this.showCps.get());
      EnumSetting var6 = this.spaceMark;
      var10001 = this.showSpace;
      Objects.requireNonNull(var10001);
      var6.visibleWhen(var10001::get);
      this.style.describe();
      this.layout.describe();
      this.keyLabels.describe();
      this.pressStyle.describe();
      this.cpsStyle.describe();
   }

   protected void onEnable() {
      Arrays.fill(this.press, 0.0F);
      Arrays.fill(this.pulse, 0.0F);
      Arrays.fill(this.wasDown, false);
      this.leftClicks.clear();
      this.rightClicks.clear();

      for(int button = 0; button < 2; ++button) {
         Clicks.clicks(button, false);
         Clicks.clicks(button, true);
      }

      Arrays.fill(this.driven, false);
      this.lastFrame[0] = 0L;
      this.listen(TickEvent.class, (event) -> this.sampleTick());
      this.listen(HudRenderEvent.class, this::onHud);
   }

   protected void onDisable() {
      this.leftClicks.clear();
      this.rightClicks.clear();
   }

   private void sampleTick() {
      if (mc.player != null) {
         Input used = mc.player.input.keyPresses;
         boolean[] held = this.driven;
         held[0] = used.forward() && !down(mc.options.keyUp);
         held[1] = used.left() && !down(mc.options.keyLeft);
         held[2] = used.backward() && !down(mc.options.keyDown);
         held[3] = used.right() && !down(mc.options.keyRight);
         held[6] = used.jump() && !down(mc.options.keyJump);
      }
   }

   private static boolean sneaking() {
      return mc.player.isShiftKeyDown() && mc.player.getPose() == Pose.CROUCHING;
   }

   private static boolean down(KeyMapping binding) {
      int code = KeyBindUtil.getKeyCode(binding);
      return code != 0 && KeyBindUtil.isKeyDown(code);
   }

   private static float approach(float current, float target, float speed, float deltaSeconds) {
      float factor = 1.0F - (float)Math.exp((double)(-speed * deltaSeconds));
      return current + (target - current) * Math.max(0.0F, Math.min(1.0F, factor));
   }

   private static float delta(long[] lastFrameHolder) {
      long now = System.nanoTime();
      long last = lastFrameHolder[0];
      lastFrameHolder[0] = now;
      return last == 0L ? 0.0F : Math.min(0.1F, Math.max(0.0F, (float)(now - last) / 1.0E9F));
   }

   private float barHeight() {
      return (Integer)this.style.get() == 1 ? (float)(Integer)this.size.get() * 0.7F : Math.max(8.0F, (float)(Integer)this.size.get() * 0.55F);
   }

   private Key key(int index, float x, float y, float width, float height) {
      Key key = this.keys[this.keyCount++];
      key.set(index, x, y, width, height);
      return key;
   }

   private float[] arrange() {
      float keySize = (float)(Integer)this.size.get();
      float gap = (float)(Integer)this.gap.get();
      float bar = this.barHeight();
      boolean movement = (Boolean)this.movementKeys.get();
      boolean mouse = (Boolean)this.showMouse.get();
      int layout = (Integer)this.layout.get();
      boolean side = movement && mouse && layout == 2;
      float mouseWidth = side ? (float)Math.round(keySize * 1.6F) : 0.0F;
      float rowWidth = keySize * 3.0F + gap * 2.0F + (side ? gap + mouseWidth : 0.0F);
      this.keyCount = 0;
      float cursorY = 0.0F;
      if (mouse && !side && layout == 1) {
         cursorY = this.mouseRow(cursorY, rowWidth, keySize, gap);
      }

      if (movement) {
         this.key(0, keySize + gap, cursorY, keySize, keySize);
         if (side) {
            this.key(4, rowWidth - mouseWidth, cursorY, mouseWidth, keySize);
         }

         cursorY += keySize + gap;
         this.key(1, 0.0F, cursorY, keySize, keySize);
         this.key(2, keySize + gap, cursorY, keySize, keySize);
         this.key(3, (keySize + gap) * 2.0F, cursorY, keySize, keySize);
         if (side) {
            this.key(5, rowWidth - mouseWidth, cursorY, mouseWidth, keySize);
         }

         cursorY += keySize + gap;
      }

      if (mouse && !side && layout != 1) {
         cursorY = this.mouseRow(cursorY, rowWidth, keySize, gap);
      }

      if ((Boolean)this.showSpace.get()) {
         this.key(6, 0.0F, cursorY, rowWidth, bar);
         cursorY += bar + gap;
      }

      if ((Boolean)this.showSneak.get()) {
         this.key(7, 0.0F, cursorY, rowWidth, bar);
         cursorY += bar + gap;
      }

      this.blockSize[0] = this.keyCount == 0 ? 0.0F : rowWidth;
      this.blockSize[1] = Math.max(0.0F, cursorY - gap);
      return this.blockSize;
   }

   private float mouseRow(float y, float rowWidth, float keySize, float gap) {
      float half = (rowWidth - gap) / 2.0F;
      this.key(4, 0.0F, y, half, keySize);
      this.key(5, half + gap, y, half, keySize);
      return y + keySize + gap;
   }

   private float[] position(float width, float height) {
      float scale = this.shownScale();
      float screenWidth = (float)mc.getWindow().getGuiScaledWidth();
      float screenHeight = (float)mc.getWindow().getGuiScaledHeight();
      float shown = width * scale;
      this.blockAt[0] = this.left(shown, screenWidth);
      this.blockAt[1] = this.top(height * scale, screenHeight);
      this.placed(this.blockAt[0], this.blockAt[1], shown, height * scale);
      return this.blockAt;
   }

   private float shownScale() {
      return (float)(Integer)this.scale.get() / 100.0F * UiScale.fit();
   }

   public boolean defaultBox(float[] out) {
      if (this.isEnabled() && HOME.equals(this.position())) {
         float[] size = this.arrange();
         float scale = this.shownScale();
         float[] at = this.position(size[0], size[1]);
         out[0] = at[0];
         out[1] = at[1];
         out[2] = size[0] * scale;
         out[3] = size[1] * scale;
         return true;
      } else {
         return false;
      }
   }

   private String cpsLabel(int key, int count, float width, float size) {
      if (count != this.cpsShown[key] || width != this.cpsRoom[key] || size != this.cpsSize[key]) {
         this.cpsShown[key] = count;
         this.cpsRoom[key] = width;
         this.cpsSize[key] = size;
         String full = count + " CPS";
         this.cpsText[key] = HudKit.width(full, size, true) <= width - 3.0F ? full : Fx.number(count);
      }

      return this.cpsText[key];
   }

   private static int cps(Deque<Long> clicks, long now) {
      while(!clicks.isEmpty() && now - (Long)clicks.peekFirst() > 1000L) {
         clicks.pollFirst();
      }

      return clicks.size();
   }

   private String name(int index) {
      int labels = (Integer)this.keyLabels.get();
      if (labels == 1) {
         return this.bound(index);
      } else {
         String var10000;
         switch (index) {
            case 0 -> var10000 = labels == 2 ? null : "W";
            case 1 -> var10000 = labels == 2 ? null : "A";
            case 2 -> var10000 = labels == 2 ? null : "S";
            case 3 -> var10000 = labels == 2 ? null : "D";
            case 4 -> var10000 = "LMB";
            case 5 -> var10000 = "RMB";
            case 6 -> var10000 = "Space";
            default -> var10000 = "Sneak";
         }

         return var10000;
      }
   }

   private String bound(int index) {
      KeyMapping var10000;
      switch (index) {
         case 0 -> var10000 = mc.options.keyUp;
         case 1 -> var10000 = mc.options.keyLeft;
         case 2 -> var10000 = mc.options.keyDown;
         case 3 -> var10000 = mc.options.keyRight;
         case 4 -> var10000 = mc.options.keyAttack;
         case 5 -> var10000 = mc.options.keyUse;
         case 6 -> var10000 = mc.options.keyJump;
         default -> var10000 = mc.options.keyShift;
      }

      KeyMapping binding = var10000;
      int code = KeyBindUtil.getKeyCode(binding);
      if (code != this.boundCode[index]) {
         this.boundCode[index] = code;
         this.boundName[index] = code == 0 ? "-" : pretty(KeyBindUtil.getKeyName(code));
      }

      return this.boundName[index];
   }

   private static String pretty(String name) {
      if (name.length() > 1 && !name.equals("LMB") && !name.equals("RMB") && !name.equals("MMB")) {
         if (name.startsWith("MOUSE")) {
            return "M" + name.substring(5);
         } else if (name.length() <= 3 || name.charAt(0) != 'L' && name.charAt(0) != 'R' || !name.endsWith("SHIFT") && !name.endsWith("CTRL") && !name.endsWith("ALT")) {
            return capitalize(name);
         } else {
            char var10000 = name.charAt(0);
            return var10000 + capitalize(name.substring(1));
         }
      } else {
         return name;
      }
   }

   private static String capitalize(String word) {
      char var10000 = word.charAt(0);
      return var10000 + word.substring(1).toLowerCase(Locale.ROOT);
   }

   private void onHud(HudRenderEvent event) {
      if (mc.player != null && !mc.debugEntries.isOverlayVisible()) {
         long now = System.currentTimeMillis();
         float delta = delta(this.lastFrame);
         boolean animate = this.look.look().animate;
         float speed = animate ? (float)(Integer)this.animationSpeed.get() : 10000.0F;
         boolean[] held = this.held;
         boolean[] test = testHeld;
         if (test != null) {
            Arrays.fill(held, false);
            System.arraycopy(test, 0, held, 0, Math.min(test.length, 8));
         } else {
            held[0] = down(mc.options.keyUp);
            held[1] = down(mc.options.keyLeft);
            held[2] = down(mc.options.keyDown);
            held[3] = down(mc.options.keyRight);
            held[4] = Clicks.shown(mc.options.keyAttack);
            held[5] = Clicks.shown(mc.options.keyUse);
            held[6] = down(mc.options.keyJump);
            held[7] = sneaking();
         }

         held[4] |= Clicks.simulated(mc.options.keyAttack);
         held[5] |= Clicks.simulated(mc.options.keyUse);

         for(int i = 0; i < 8; ++i) {
            held[i] |= this.driven[i];
         }

         for(int button = 0; button < 2; ++button) {
            Deque<Long> clicks = button == 0 ? this.leftClicks : this.rightClicks;
            int count = Clicks.clicks(button, false) + Clicks.clicks(button, true);

            for(int i = 0; i < count; ++i) {
               clicks.addLast(now);
            }

            if (count > 0) {
               this.pulse[button == 0 ? 4 : 5] = 1.0F;
            }
         }

         boolean classic = (Integer)this.style.get() == 1;

         for(int i = 0; i < 8; ++i) {
            if (held[i] && !this.wasDown[i] && i != 4 && i != 5) {
               this.pulse[i] = 1.0F;
            }

            this.wasDown[i] = held[i];
            this.press[i] = approach(this.press[i], held[i] ? 1.0F : 0.0F, speed, delta);
            this.pulse[i] = approach(this.pulse[i], 0.0F, speed * (classic ? 1.6F : 0.55F), delta);
         }

         cps(this.leftClicks, now);
         cps(this.rightClicks, now);
         float[] size = this.arrange();
         float rowWidth = size[0];
         float blockHeight = size[1];
         if (this.keyCount == 0) {
            this.placed(0.0F, 0.0F, 0.0F, 0.0F);
         } else {
            float labelScale = (float)(Integer)this.labelSize.get() / 100.0F;

            for(int i = 0; i < this.keyCount; ++i) {
               Key key = this.keys[i];
               key.label = this.name(key.index);
               if (key.index == 6 && (Integer)this.spaceMark.get() != 1) {
                  key.label = null;
               }

               if ((key.index == 4 || key.index == 5) && (Boolean)this.showCps.get()) {
                  int count = cps(key.index == 4 ? this.leftClicks : this.rightClicks, now);
                  int slot = key.index == 4 ? 0 : 1;
                  if ((Integer)this.cpsStyle.get() == 1) {
                     key.label = this.cpsLabel(slot, count, key.width, labelSize(key.height, false) * labelScale);
                  } else {
                     key.subLabel = this.cpsLabel(slot, count, key.width, labelSize(key.height, true) * labelScale);
                  }
               }
            }

            float scale = this.shownScale();
            float[] pos = this.position(rowWidth, blockHeight);
            HudKit.sync(this.look.look());
            GuiGraphicsExtractor graphics = event.graphics();
            graphics.pose().pushMatrix();
            graphics.pose().translate(pos[0], pos[1]);
            graphics.pose().scale(scale, scale);
            Gfx.begin(graphics);

            try {
               if (HudKit.look().shadow != 3) {
                  for(int i = 0; i < this.keyCount; ++i) {
                     this.shadow(this.keys[i], classic);
                  }
               }

               for(int i = 0; i < this.keyCount; ++i) {
                  if (classic) {
                     this.classicPane(this.keys[i]);
                  } else {
                     this.pane(this.keys[i]);
                  }
               }

               for(int i = 0; i < this.keyCount; ++i) {
                  if (classic) {
                     this.classicLabels(this.keys[i]);
                  } else {
                     this.labels(this.keys[i]);
                  }
               }

               HudKit.flushText();
            } finally {
               HudKit.discardText();
               Gfx.end();
            }

            graphics.pose().popMatrix();
         }
      }
   }

   private static float labelSize(float height, boolean twoLines) {
      float big = Math.min(height * 0.4F, 9.5F);
      return twoLines ? big * 0.78F : big;
   }

   private float opacity() {
      return (float)Math.max(0, Math.min(255, (Integer)this.alpha.get())) / 255.0F;
   }

   private int accent() {
      int rgb = (Integer)this.colorMode.get() == 0 ? (Integer)this.color.get() : Theme.accentRgb();
      return -16777216 | rgb & 16777215;
   }

   private float lit(int index) {
      return Math.max(0.0F, Math.min(1.0F, this.press[index] + this.pulse[index] * 0.45F));
   }

   private static int onColor(int rgb) {
      float luma = (0.2126F * (float)(rgb >> 16 & 255) + 0.7152F * (float)(rgb >> 8 & 255) + 0.0722F * (float)(rgb & 255)) / 255.0F;
      return luma > 0.62F ? -15592165 : -1;
   }

   private float[] box(Key key) {
      float sink = (Boolean)this.pressShrink.get() ? 1.0F - 0.06F * this.press[key.index] : 1.0F;
      float width = key.width * sink;
      float height = key.height * sink;
      this.box[0] = key.x + (key.width - width) / 2.0F;
      this.box[1] = key.y + (key.height - height) / 2.0F;
      this.box[2] = width;
      this.box[3] = height;
      return this.box;
   }

   private static void spaceMark(float x, float y, float width, float height, int color) {
      float lineWidth = Math.min(width * 0.28F, 34.0F);
      float lineHeight = Math.max(1.4F, height * 0.13F);
      HudKit.fill(x + (width - lineWidth) / 2.0F, y + (height - lineHeight) / 2.0F, lineWidth, lineHeight, lineHeight / 2.0F, color);
   }

   private void arrow(Key key, float x, float y, float width, float height, int color) {
      if ((Integer)this.keyLabels.get() == 2 && key.index <= 3) {
         float size = labelSize(key.height, false) * (float)(Integer)this.labelSize.get() / 100.0F * (width / key.width);
         float across = size * 0.5F;
         float along = size * 0.36F;
         float cx = x + width / 2.0F;
         float cy = y + height / 2.0F;
         switch (key.index) {
            case 0 -> Fx.triangle(cx - across, cy + along, cx + across, cy + along, cx, cy - along, color);
            case 1 -> Fx.triangle(cx + along, cy - across, cx + along, cy + across, cx - along, cy, color);
            case 2 -> Fx.triangle(cx - across, cy - along, cx + across, cy - along, cx, cy + along, color);
            default -> Fx.triangle(cx - along, cy - across, cx - along, cy + across, cx + along, cy, color);
         }

      }
   }

   private float radius(float width, float height) {
      return Math.min((float)(Integer)this.radius.get(), Math.min(width, height) / 2.0F);
   }

   private void shadow(Key key, boolean classic) {
      float x = classic ? key.x : this.box(key)[0];
      float y = classic ? key.y : this.box[1];
      float width = classic ? key.width : this.box[2];
      float height = classic ? key.height : this.box[3];
      float solid = (float)Math.max(0, Math.min(255, (Integer)this.idleAlpha.get())) / 255.0F;
      float filled = classic ? this.classicLit(key.index) : this.lit(key.index);
      HudKit.shadow(x, y, width, height, this.radius(width, height), 0.7F, this.opacity() * Math.max(classic ? Math.max(solid, 0.55F) : solid, filled));
   }

   private void border(float x, float y, float width, float height, float radius, float filled, float solid, boolean classic) {
      HudKit.Look look = HudKit.look();
      float opacity = this.opacity();
      switch (look.border) {
         case 1:
            break;
         case 2:
            HudKit.ring(x, y, width, height, radius, (float)look.borderWidth * Gfx.pixel(), Fx.alpha(-16777216 | look.borderColor, opacity));
            break;
         case 3:
            HudKit.ring(x, y, width, height, radius, (float)look.borderWidth * Gfx.pixel(), Fx.alpha(Fx.mix(this.accent(), HudKit.brighter(this.accent()), filled), opacity));
            break;
         default:
            if (!classic) {
               float rimAlpha = opacity * (0.55F + 0.45F * solid);
               HudKit.rim(x, y, width, height, radius, Fx.alpha(Fx.mix(HudKit.RIM_TOP, 1509949439, filled), rimAlpha), Fx.alpha(Fx.mix(HudKit.RIM_BOTTOM, 637534208, filled), rimAlpha));
            }
      }

   }

   private void accentBar(float x, float y, float width, float height, float radius, float filled) {
      HudKit.Look look = HudKit.look();
      if (look.accentBar != 0) {
         float p = Gfx.pixel();
         float t = Math.max(p, (float)Math.round(1.5F / p) * p);
         float r = Math.min(HudKit.corner(radius), Math.min(width, height) * 0.5F);
         float across = (float)Math.round(r * 0.25F / p) * p;
         float along = Math.max(r * 0.6F, Math.min(width, height) * 0.18F);
         float round = look.shape == 0 ? t * 0.5F : 0.0F;
         int accent = this.accent();
         float opacity = this.opacity() * 0.9F;
         int bright = Fx.alpha(Fx.mix(HudKit.brighter(accent), onColor(accent), filled * 0.85F), opacity);
         int deep = Fx.alpha(Fx.mix(HudKit.deeper(accent), onColor(accent), filled * 0.85F), opacity);
         switch (look.accentBar) {
            case 1:
               HudKit.fillV(x + across, y + along, t, height - along * 2.0F, round, bright, deep);
               break;
            case 2:
               HudKit.fillH(x + along, y + across, width - along * 2.0F, t, round, deep, bright);
               break;
            case 3:
            default:
               HudKit.fillH(x + along, y + height - across - t, width - along * 2.0F, t, round, deep, bright);
               break;
            case 4:
               HudKit.fillV(x + width - across - t, y + along, t, height - along * 2.0F, round, bright, deep);
         }

      }
   }

   private static void fill(float x, float y, float width, float height, float radius, int top, int bottom) {
      if (HudKit.look().background == 3) {
         HudKit.fillH(x, y, width, height, radius, top, bottom);
      } else {
         HudKit.fillV(x, y, width, height, radius, top, bottom);
      }

   }

   private void pane(Key key) {
      float[] b = this.box(key);
      float x = b[0];
      float y = b[1];
      float width = b[2];
      float height = b[3];
      float opacity = this.opacity();
      float lit = this.lit(key.index);
      float solid = (float)Math.max(0, Math.min(255, (Integer)this.idleAlpha.get())) / 255.0F;
      float radius = this.radius(width, height);
      int accent = this.accent();
      int press = (Integer)this.pressStyle.get();
      float idleAlpha = opacity * (0.3F + 0.7F * solid);
      float rest = press == 0 ? idleAlpha * (1.0F - lit) : idleAlpha;
      if (rest > 0.001F) {
         fill(x, y, width, height, radius, Fx.withAlpha(HudKit.FILL_TOP, Math.round(255.0F * rest)), Fx.withAlpha(HudKit.FILL_BOTTOM, Math.round(255.0F * rest)));
      }

      switch (press) {
         case 1:
            grow(x, y, width, height, radius, lit, accent, opacity);
         case 2:
            break;
         default:
            if (lit > 0.001F) {
               HudKit.fillV(x, y, width, height, radius, Fx.alpha(HudKit.brighter(accent), opacity * lit), Fx.alpha(HudKit.deeper(accent), opacity * lit));
            }
      }

      this.accentBar(x, y, width, height, radius, press == 2 ? 0.0F : lit);
      this.border(x, y, width, height, radius, press == 2 ? 0.0F : lit, solid, false);
      if (press == 2) {
         float line = Math.max(Gfx.pixel() * 2.0F, (float)Math.round(1.4F / Gfx.pixel()) * Gfx.pixel());
         if (lit > 0.001F) {
            HudKit.ring(x, y, width, height, radius, line, Fx.alpha(HudKit.brighter(accent), opacity * lit));
         }
      }

      if ((Boolean)this.pressGlow.get()) {
         this.glow(x, y, width, height, radius, this.pulse[key.index], accent, opacity);
      }

      int text = Fx.alpha(this.textColor(lit), opacity);
      if (key.index == 6 && (Integer)this.spaceMark.get() == 0) {
         spaceMark(x, y, width, height, text);
      }

      this.arrow(key, x, y, width, height, text);
   }

   private static void grow(float x, float y, float width, float height, float radius, float amount, int color, float opacity) {
      if (!(amount <= 0.001F)) {
         float k = Fx.smooth(amount);
         float w = width * k;
         float h = height * k;
         float a = opacity * Math.min(1.0F, amount * 2.5F);
         HudKit.fillV(x + (width - w) / 2.0F, y + (height - h) / 2.0F, w, h, radius * k, Fx.alpha(HudKit.brighter(color), a), Fx.alpha(HudKit.deeper(color), a));
      }
   }

   private void glow(float x, float y, float width, float height, float radius, float flash, int color, float opacity) {
      if (flash > 0.01F) {
         float reach = Math.max(width, height) * (0.45F + 0.35F * (1.0F - flash));
         int c = Fx.alpha(Fx.lighten(color, 0.55F), opacity * 0.42F * flash);
         if (HudKit.squared()) {
            HudKit.halo(x, y, width, height, radius, reach * 0.45F, c);
         } else {
            Gfx.glow(x + width / 2.0F, y + height / 2.0F, reach, c);
         }
      }

   }

   private int labelBase(boolean classic) {
      if ((Integer)this.labelColor.get() == 1) {
         return -16777216 | (Integer)this.customLabelColor.get();
      } else {
         return classic ? Theme.TEXT : Fx.TEXT;
      }
   }

   private int textColor(float lit) {
      int base = this.labelBase(false);
      return (Integer)this.pressStyle.get() == 2 ? Fx.mix(base, HudKit.brighter(this.accent()), lit) : Fx.mix(base, onColor(this.accent()), lit);
   }

   private int mutedText(boolean classic) {
      if ((Integer)this.labelColor.get() == 1) {
         int back = classic ? Theme.MAIN : -16777216 | HudKit.FILL_BOTTOM;
         return Fx.mix(this.labelBase(classic), -16777216 | back, 0.38F);
      } else {
         return classic ? Theme.DIM : HudKit.MUTED;
      }
   }

   private static float fitting(String text, float size, float width, boolean medium) {
      float at = HudKit.width(text, size, medium);
      float room = width - 3.0F;
      return at > room && at > 0.0F ? size * Math.max(0.5F, room / at) : size;
   }

   private void labels(Key key) {
      if (key.label != null || key.subLabel != null) {
         float[] b = this.box(key);
         float x = b[0];
         float y = b[1];
         float width = b[2];
         float height = b[3];
         float lit = this.lit(key.index);
         float opacity = this.opacity();
         float centerX = x + width / 2.0F;
         float labelScale = (float)(Integer)this.labelSize.get() / 100.0F;
         int text = Fx.alpha(this.textColor(lit), opacity);
         if (key.label != null && key.subLabel != null) {
            float small = labelSize(key.height, true) * labelScale * (width / key.width);
            HudKit.textCentered(key.label, centerX, y + height * 0.35F, fitting(key.label, small, width, false), text, false);
            int dim = Fx.alpha(Fx.mix(this.mutedText(false), this.textColor(lit), lit), opacity * (0.85F + 0.15F * lit));
            HudKit.textCentered(key.subLabel, centerX, y + height * 0.7F, small * 0.92F, dim, true);
         } else {
            String shown = key.label != null ? key.label : key.subLabel;
            float size = (key.index != 7 && key.index != 6 ? labelSize(key.height, false) : this.barLabelSize(key.height)) * labelScale * (width / key.width);
            size = fitting(shown, size, width, false);
            HudKit.textCentered(shown, centerX, y + height / 2.0F, size, text, false);
         }

      }
   }

   private float barLabelSize(float barHeight) {
      return Math.min(labelSize((float)(Integer)this.size.get(), true), barHeight * 0.62F);
   }

   private float classicLit(int index) {
      return Math.max(0.0F, Math.min(1.0F, this.press[index] + this.pulse[index] * 0.6F));
   }

   private int classicText(float lit) {
      return Colors.lerp(this.labelBase(true), Theme.ON_ACCENT, lit);
   }

   private void classicPane(Key key) {
      float lit = this.classicLit(key.index);
      float opacity = this.opacity();
      float idle = (float)Math.max(0, Math.min(255, (Integer)this.idleAlpha.get())) / 255.0F;
      float radius = this.radius(key.width, key.height);
      int background = HudKit.look().background;
      if (background == 0) {
         int panel = Colors.withAlpha(Theme.MAIN, Math.max(idle, 0.55F));
         int fill = Colors.lerp(panel, -16777216 | this.accent(), lit);
         HudKit.fill(key.x, key.y, key.width, key.height, radius, Fx.alpha(fill, opacity));
      } else {
         float rest = opacity * Math.max(idle, 0.55F);
         fill(key.x, key.y, key.width, key.height, radius, Fx.withAlpha(HudKit.FILL_TOP, Math.round(255.0F * rest)), Fx.withAlpha(HudKit.FILL_BOTTOM, Math.round(255.0F * rest)));
         if (lit > 0.001F) {
            HudKit.fill(key.x, key.y, key.width, key.height, radius, Fx.alpha(-16777216 | this.accent(), opacity * lit));
         }
      }

      this.accentBar(key.x, key.y, key.width, key.height, radius, lit);
      this.border(key.x, key.y, key.width, key.height, radius, lit, idle, true);
      int text = Fx.alpha(this.classicText(lit), opacity);
      if (key.index == 6 && (Integer)this.spaceMark.get() == 0) {
         spaceMark(key.x, key.y, key.width, key.height, text);
      }

      this.arrow(key, key.x, key.y, key.width, key.height, text);
   }

   private void classicLabels(Key key) {
      if (key.label != null || key.subLabel != null) {
         float x = key.x;
         float y = key.y;
         float width = key.width;
         float height = key.height;
         float lit = this.classicLit(key.index);
         float opacity = this.opacity();
         int text = Fx.alpha(this.classicText(lit), opacity);
         float centerX = x + width / 2.0F;
         float labelScale = (float)(Integer)this.labelSize.get() / 100.0F;
         float big = Math.min(height * 0.42F, 9.0F) * labelScale;
         float small = big * 0.8F;
         if (key.label != null && key.subLabel != null) {
            HudKit.textCentered(key.label, centerX, y + height * 0.34F, fitting(key.label, small, width, false), text, false);
            int dim = Colors.lerp(this.mutedText(true), Theme.ON_ACCENT, lit);
            HudKit.textCentered(key.subLabel, centerX, y + height * 0.7F, small, Fx.alpha(dim, opacity), false);
         } else {
            String shown = key.label != null ? key.label : key.subLabel;
            float size = key.index != 7 && key.index != 6 ? big : this.barLabelSize(height) * labelScale;
            size = fitting(shown, size, width, false);
            HudKit.textCentered(shown, centerX, y + height / 2.0F, size, text, false);
         }

      }
   }

   static Keystrokes get() {
      return VantaClient.MODULES == null ? null : (Keystrokes)VantaClient.MODULES.get(Keystrokes.class);
   }

   static {
      HOME = new PositionSetting.Position(PositionSetting.Anchor.END, PositionSetting.Anchor.END, 4.0F, 4.0F);
   }

   private static final class Key {
      int index;
      float x;
      float y;
      float width;
      float height;
      String label;
      String subLabel;

      void set(int index, float x, float y, float width, float height) {
         this.index = index;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.label = null;
         this.subLabel = null;
      }
   }
}
