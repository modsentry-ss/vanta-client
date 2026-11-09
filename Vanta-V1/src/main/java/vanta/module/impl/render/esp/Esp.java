package vanta.module.impl.render.esp;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import vanta.event.events.EntityRenderStateEvent;
import vanta.event.events.FrameEvent;
import vanta.event.events.HandRenderEvent;
import vanta.event.events.WorldRenderEvent;
import vanta.friends.FriendList;
import vanta.module.Module;
import vanta.render.Rect;
import vanta.render.Style;
import vanta.render.entity.EntityFills;
import vanta.render.entity.EntityOutline;
import vanta.render.entity.EntityOutlines;
import vanta.render.entity.Filled;
import vanta.render.screen.Projection;
import vanta.setting.BoolSetting;
import vanta.setting.ColorSetting;
import vanta.setting.EntitySetting;
import vanta.setting.EnumSetting;
import vanta.setting.Measure;
import vanta.setting.MultiSetting;
import vanta.setting.Setting;
import vanta.setting.SliderSetting;
import vanta.util.Colors;
import vanta.util.Entities;
import vanta.visual.Fx;
import vanta.visual.Scene;
import vanta.visual.Visuals;
import vanta.visual.style.TargetDraw;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;

public final class Esp extends Module {
   private static final String[] COLOR_BY = new String[]{"Single", "Team", "Health", "Distance", "Type"};
   private static final String[] MODES = new String[]{"3D", "2D", "Shader", "Outline"};
   private static final String[] TARGETS = new String[]{"Self", "Players", "Entities", "Hand"};
   private static final int HAND = 3;
   private final EnumSetting mode;
   private final SliderSetting distance;
   private final MultiSetting targets;
   private final EntitySetting entities;
   private final EnumSetting colorBy;
   private final ColorSetting farColor;
   private final ColorSetting hostileColor;
   private final ColorSetting passiveColor;
   private final ColorSetting itemColor;
   private final BoolSetting friends;
   private final ColorSetting friendColor;
   private final BoolSetting hurtFlash;
   private final BoolSetting distanceFade;
   private final SliderSetting lineWidth;
   private final SliderSetting outlineWidth;
   private final Box box;
   private final Health health;
   private final Outline outline;
   private final Shader shader;
   private final Runnable fxDrawer;
   private Entity[] fxEntity;
   private float[] fxRect;
   private int[] fxColor;
   private float[] fxFade;
   private double[] fxDistance;
   private int[] fxOrder;
   private final double[] fxPos;
   private final float[] fxAt;
   private final float[] fxCorners;
   private final float[] fxHull;
   private final float[] fxBar;
   private final float[] fxRectNow;
   private final Int2ObjectOpenHashMap<float[]> fxEase;
   private long fxLastFrame;
   private int fxFrame;

   public Esp() {
      super("esp2d");
      this.mode = (EnumSetting)this.add(new EnumSetting("mode", 1, MODES));
      this.distance = (SliderSetting)this.add((new SliderSetting("maxDistance", 1000, 8, 1000, Measure.DISTANCE)).unlimitedAtMax());
      this.targets = (MultiSetting)this.add(new MultiSetting("targets", TARGETS, new int[]{1, 2}));
      this.entities = (EntitySetting)this.add(new EntitySetting("entities"));
      this.colorBy = (EnumSetting)this.add(new EnumSetting("colorBy", 1, COLOR_BY));
      this.farColor = (ColorSetting)this.add(new ColorSetting("farColor", 5929983));
      this.hostileColor = (ColorSetting)this.add(new ColorSetting("hostileColor", 16734810));
      this.passiveColor = (ColorSetting)this.add(new ColorSetting("passiveColor", 7069834));
      this.itemColor = (ColorSetting)this.add(new ColorSetting("itemColor", 16765770));
      this.friends = (BoolSetting)this.add(new BoolSetting("highlightFriends", true));
      this.friendColor = (ColorSetting)this.add(new ColorSetting("friendColor", 7073992));
      this.hurtFlash = (BoolSetting)this.add(new BoolSetting("hurtFlash", true));
      this.distanceFade = (BoolSetting)this.add(new BoolSetting("distanceFade", false));
      this.lineWidth = (SliderSetting)this.add((new SliderSetting("lineWidth", 1, 1, 10)).withFormat(Esp::pixels));
      this.outlineWidth = (SliderSetting)this.add((new SliderSetting("outlineWidth", 2, 1, 10)).withFormat(Esp::pixels));
      this.box = new Box(this);
      this.health = new Health(this, this.box);
      this.outline = new Outline(this, this.box);
      this.shader = new Shader(this);
      this.fxDrawer = this::drawFx;
      this.fxEntity = new Entity[16];
      this.fxRect = new float[64];
      this.fxColor = new int[16];
      this.fxFade = new float[16];
      this.fxDistance = new double[16];
      this.fxOrder = new int[16];
      this.fxPos = new double[3];
      this.fxAt = new float[3];
      this.fxCorners = new float[16];
      this.fxHull = new float[16];
      this.fxBar = new float[4];
      this.fxRectNow = new float[4];
      this.fxEase = new Int2ObjectOpenHashMap();
      this.mode.set(1);
      this.mode.visibleWhen(() -> false);
      this.entities.visibleWhen(() -> ((Set)this.targets.get()).contains(2));
      this.targets.optionVisibleWhen((option) -> option != 3 || this.shaded() || this.outlined());
      this.farColor.visibleWhen(() -> (Integer)this.colorBy.get() == 3);
      this.hostileColor.visibleWhen(() -> (Integer)this.colorBy.get() == 4);
      this.passiveColor.visibleWhen(() -> (Integer)this.colorBy.get() == 4);
      this.itemColor.visibleWhen(() -> (Integer)this.colorBy.get() == 4);
      ColorSetting var10000 = this.friendColor;
      BoolSetting var10001 = this.friends;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.lineWidth.visibleWhen(() -> this.boxed() || this.flat());
      this.outlineWidth.visibleWhen(this::outlined);
      this.lineWidth.describe();
      this.outlineWidth.describe();
   }

   public String info() {
      return "";
   }

   float weightPixels() {
      return (float)Math.clamp((long)(Integer)this.lineWidth.get(), 1, 10);
   }

   float outlinePixels() {
      return (float)Math.clamp((long)(Integer)this.outlineWidth.get(), 1, 10);
   }

   static String pixels(int value) {
      return value + " px";
   }

   boolean coloredByHealth() {
      return (Integer)this.colorBy.get() == 2;
   }

   private int tint(Entity entity, double distanceSq) {
      int rgb = 0;
      if ((Boolean)this.friends.get() && entity instanceof Player && FriendList.protects(entity)) {
         rgb = -16777216 | (Integer)this.friendColor.get();
      } else {
         switch ((Integer)this.colorBy.get()) {
            case 1:
               rgb = this.teamRgb(entity);
               break;
            case 2:
               if (entity instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)entity;
                  if (living.getMaxHealth() > 0.0F) {
                     rgb = this.health.color(Math.clamp(living.getHealth() / living.getMaxHealth(), 0.0F, 1.0F));
                  }
               }
               break;
            case 3:
               double range = this.distance.unlimited() ? (double)96.0F : (double)(Integer)this.distance.get();
               float k = (float)Math.clamp(Math.sqrt(distanceSq) / range, (double)0.0F, (double)1.0F);
               rgb = Colors.lerp(-16777216 | this.box.rgb(), -16777216 | (Integer)this.farColor.get(), k);
               break;
            case 4:
               if (entity instanceof Enemy) {
                  rgb = -16777216 | (Integer)this.hostileColor.get();
               } else if (entity instanceof ItemEntity) {
                  rgb = -16777216 | (Integer)this.itemColor.get();
               } else if (entity instanceof Mob) {
                  rgb = -16777216 | (Integer)this.passiveColor.get();
               }
         }
      }

      if ((Boolean)this.hurtFlash.get() && entity instanceof LivingEntity living) {
         if (living.hurtTime > 0) {
            int base = rgb != 0 ? rgb : -16777216 | this.box.rgb();
            float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            float flash = Math.clamp(((float)living.hurtTime - partial) / Math.max(1.0F, (float)living.hurtDuration), 0.0F, 1.0F);
            rgb = Colors.lerp(base, -1, flash * 0.85F);
         }
      }

      return rgb;
   }

   private float fade(double distanceSq) {
      if (!(Boolean)this.distanceFade.get()) {
         return 1.0F;
      } else {
         double range = this.distance.unlimited() ? (double)128.0F : (double)(Integer)this.distance.get();
         double d = Math.sqrt(distanceSq) / range;
         return (float)Math.clamp((double)1.0F - Math.max((double)0.0F, d - 0.4) / 0.6, 0.15, (double)1.0F);
      }
   }

   private int boxColor(Entity entity, double distanceSq) {
      int tint = this.tint(entity, distanceSq);
      int rgb = tint != 0 ? tint : -16777216 | this.box.rgb();
      return Colors.withAlpha(rgb, this.fade(distanceSq));
   }

   <S extends Setting<?>> S setting(S setting) {
      return (S)this.add(setting);
   }

   boolean flat() {
      return true;
   }

   boolean boxed() {
      return false;
   }

   boolean shaded() {
      return false;
   }

   boolean outlined() {
      return false;
   }

   boolean handTargeted() {
      return this.targets.has(3);
   }

   boolean outlineFilled() {
      return this.outline.filled();
   }

   protected void onEnable() {
      this.listen(WorldRenderEvent.class, this::onWorldRender);
      this.listen(EntityRenderStateEvent.class, this::onEntityRenderState);
      this.listen(HandRenderEvent.class, this::onHandRender);
      this.listen(Visuals.WorldLayer.class, (event) -> Visuals.drawWorld(event, this.fxDrawer));
   }

   protected void onDisable() {
      EntityOutlines.configure((EntityOutline)null);
      EntityOutlines.configureHand((EntityOutline)null);
      this.health.clear();
   }

   private void onWorldRender(WorldRenderEvent event) {
      this.outline.endFrame();
      EntityOutlines.configure(this.outlined() ? this.outline.spec() : null);
      EntityOutlines.configureHand(this.outlined() && this.handTargeted() ? this.outline.spec() : null);
      if (this.boxed()) {
         Style style = this.box.worldStyle();
         if (style.hasFill() || style.hasStroke()) {
            for(Target target : this.targets(event)) {
               event.buffer().box(target.bounds(), this.box.worldStyle(this.boxColor(target.entity(), target.distanceSq())));
            }
         }
      }

   }

   private void ease2d(Entity entity, float[] r, float dt) {
      float cx = (r[0] + r[2]) * 0.5F;
      float cy = (r[1] + r[3]) * 0.5F;
      float hw = (r[2] - r[0]) * 0.5F;
      float hh = (r[3] - r[1]) * 0.5F;
      float[] e = (float[])this.fxEase.get(entity.getId());
      if (e != null && this.fxFrame - (int)e[2] <= 2) {
         float k = 1.0F - (float)Math.exp((double)(-dt * 16.0F));
         e[0] += (hw - e[0]) * k;
         e[1] += (hh - e[1]) * k;
      } else {
         e = e == null ? new float[3] : e;
         e[0] = hw;
         e[1] = hh;
         this.fxEase.put(entity.getId(), e);
      }

      e[2] = (float)this.fxFrame;
      r[0] = cx - e[0];
      r[2] = cx + e[0];
      r[1] = cy - e[1];
      r[3] = cy + e[1];
   }

   private void drawFx() {
      boolean flat = this.flat();
      if (flat || this.boxed()) {
         boolean boxes = flat && this.box.shown();
         boolean glow = this.box.glows();
         int side = this.health.sideIndex();
         if (boxes || glow || side >= 0) {
            Minecraft mc = Minecraft.getInstance();
            Camera camera = mc.gameRenderer.mainCamera();
            Vec3 origin = camera.position();
            double range = this.range();
            if (side >= 0) {
               this.health.beginFrame(Visuals.replaying());
            }

            float intensity = this.outline.haloIntensity();
            float radius = this.outline.haloRadius();
            float[] rect = this.fxRectNow;
            float p = TargetDraw.px();
            float line = this.weightPixels() * p;
            boolean outlined = this.box.outlined();
            float edge = this.box.edgePixels() * p;
            int count = 0;
            long now = System.nanoTime();
            float dt = this.fxLastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.fxLastFrame) / 1.0E9F);
            this.fxLastFrame = now;
            if (++this.fxFrame % 200 == 0) {
               int frame = this.fxFrame;
               this.fxEase.values().removeIf((e) -> frame - (int)e[2] > 20);
            }

            for(Entity entity : mc.level.entitiesForRendering()) {
               if (this.targeted(entity, camera)) {
                  double distanceSq = entity.distanceToSqr(origin);
                  if (!(distanceSq > range)) {
                     int hull = this.project(entity, flat, glow && !flat, rect);
                     if (hull >= 0) {
                        int color = this.boxColor(entity, distanceSq);
                        if (flat) {
                           this.ease2d(entity, rect, dt);
                           fit(rect, line, p);
                        }

                        if (glow) {
                           float strength = Math.min(1.0F, 0.3F * intensity) * ((float)(color >>> 24) / 255.0F);
                           int halo = Math.round(strength * 255.0F) << 24 | color & 16777215;
                           if (hull >= 3) {
                              Halo.convex(this.fxHull, hull, radius, halo);
                           } else {
                              float out = outlined ? edge : 0.0F;
                              Halo.rect(rect[0] - out, rect[1] - out, rect[2] + out, rect[3] + out, radius, halo, this.fxHull);
                           }
                        }

                        if (boxes || side >= 0 && entity instanceof LivingEntity) {
                           if (count == this.fxEntity.length) {
                              int grown = count * 2;
                              this.fxEntity = (Entity[])Arrays.copyOf(this.fxEntity, grown);
                              this.fxRect = Arrays.copyOf(this.fxRect, grown * 4);
                              this.fxColor = Arrays.copyOf(this.fxColor, grown);
                              this.fxFade = Arrays.copyOf(this.fxFade, grown);
                              this.fxDistance = Arrays.copyOf(this.fxDistance, grown);
                              this.fxOrder = Arrays.copyOf(this.fxOrder, grown);
                           }

                           this.fxEntity[count] = entity;
                           System.arraycopy(rect, 0, this.fxRect, count * 4, 4);
                           this.fxColor[count] = color;
                           this.fxFade[count] = this.fade(distanceSq);
                           this.fxDistance[count] = distanceSq;
                           ++count;
                        }
                     }
                  }
               }
            }

            if (count != 0) {
               int[] order = this.fxOrder;

               int j;
               for(int i = 0; i < count; order[j + 1] = i++) {
                  for(j = i - 1; j >= 0 && this.fxDistance[order[j]] < this.fxDistance[i]; --j) {
                     order[j + 1] = order[j];
                  }
               }

               boolean corners = this.box.cornered();
               float fillOpacity = this.box.filled() ? this.box.fillOpacity() : 0.0F;
               float gap = (boxes && outlined ? edge : 0.0F) + p * 2.0F;
               float thick = this.health.widthPixels() * p;
               boolean numbers = side >= 0 && this.health.numbered();
               float[] bar = this.fxBar;

               for(int k = 0; k < count; ++k) {
                  int i = order[k];
                  int o = i * 4;
                  float x0 = this.fxRect[o];
                  float y0 = this.fxRect[o + 1];
                  float x1 = this.fxRect[o + 2];
                  float y1 = this.fxRect[o + 3];
                  int color = this.fxColor[i];
                  if (boxes) {
                     int fill = fillOpacity > 0.0F ? Colors.withAlpha(color, fillOpacity) : 0;
                     TargetDraw.box(x0, y0, x1, y1, line, outlined ? edge : 0.0F, corners, color, fill);
                  }

                  if (side >= 0) {
                     Entity var37 = this.fxEntity[i];
                     if (var37 instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity)var37;
                        Health.Bar state = this.health.bar(living);
                        float fade = this.fxFade[i];
                        TargetDraw.healthBar(x0, y0, x1, y1, side, gap, thick, state.shown, state.trail, this.health.color(state.shown), fade, bar);
                        if (numbers) {
                           this.healthNumber(living, side, state.shown, bar, fade);
                        }
                     }
                  }

                  this.fxEntity[i] = null;
               }

               if (numbers) {
                  Fx.flushText();
               }

            }
         }
      }
   }

   private static void fit(float[] r, float line, float p) {
      float l = (float)Math.round(r[0] / p) * p;
      float t = (float)Math.round(r[1] / p) * p;
      float rr = (float)Math.round(r[2] / p) * p;
      float b = (float)Math.round(r[3] / p) * p;
      float least = line * 2.0F + p * 2.0F;
      if (rr - l < least) {
         l = (float)Math.round(((l + rr) * 0.5F - least * 0.5F) / p) * p;
         rr = l + least;
      }

      if (b - t < least) {
         t = (float)Math.round(((t + b) * 0.5F - least * 0.5F) / p) * p;
         b = t + least;
      }

      r[0] = l;
      r[1] = t;
      r[2] = rr;
      r[3] = b;
   }

   private void healthNumber(LivingEntity living, int side, float fraction, float[] bar, float fade) {
      String text = Fx.number(Math.max(0, Math.round(living.getHealth())));
      float p = TargetDraw.px();
      float width = TargetDraw.textWidth(text);
      float half = 1.98F;
      int white = Fx.alpha(-1, fade);
      if (side != 0 && side != 1) {
         float left = bar[0] + p;
         float right = bar[2] - p;
         float x = Math.clamp(left + (right - left) * fraction - width * 0.5F, left, Math.max(left, right - width));
         float y = side == 2 ? bar[1] - p * 2.0F - half : bar[3] + p * 2.0F + half;
         TargetDraw.text(text, x, y, white, true);
      } else {
         float top = bar[1] + p;
         float bottom = bar[3] - p;
         float y = Math.clamp(bottom - (bottom - top) * fraction, top + half, Math.max(top + half, bottom - half));
         float x = side == 0 ? bar[0] - p * 2.0F - width : bar[2] + p * 2.0F;
         TargetDraw.text(text, x, y, white, true);
      }

   }

   private int project(Entity entity, boolean flat, boolean outline, float[] out) {
      double[] pos = this.fxPos;
      Visuals.position(entity, pos);
      AABB b = entity.getBoundingBox();
      double ox = pos[0] - entity.getX();
      double oy = pos[1] - entity.getY();
      double oz = pos[2] - entity.getZ();
      double x0 = b.minX + ox;
      double y0 = b.minY + oy;
      double z0 = b.minZ + oz;
      double x1 = b.maxX + ox;
      double y1 = b.maxY + oy;
      double z1 = b.maxZ + oz;
      if (flat && entity instanceof Player) {
         x0 -= 0.12;
         z0 -= 0.12;
         x1 += 0.12;
         z1 += 0.12;
         y1 += 0.06;
      }

      float[] corners = this.fxCorners;
      float[] at = this.fxAt;
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      boolean behind = false;

      for(int i = 0; i < 8; ++i) {
         if (!Scene.toScreen((i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1, at)) {
            behind = true;
            break;
         }

         corners[i * 2] = at[0];
         corners[i * 2 + 1] = at[1];
         minX = Math.min(minX, at[0]);
         minY = Math.min(minY, at[1]);
         maxX = Math.max(maxX, at[0]);
         maxY = Math.max(maxY, at[1]);
      }

      int hull = 0;
      if (behind) {
         Rect rect = Scene.projection().bounds(new AABB(x0, y0, z0, x1, y1, z1));
         if (rect == null) {
            return -1;
         }

         minX = rect.minX();
         minY = rect.minY();
         maxX = rect.maxX();
         maxY = rect.maxY();
      } else if (outline) {
         hull = Halo.hull(corners, 8, this.fxHull);
      }

      Projection view = Scene.projection();
      if (!(maxX < -32.0F) && !(maxY < -32.0F) && !(minX > view.width() + 32.0F) && !(minY > view.height() + 32.0F)) {
         out[0] = minX;
         out[1] = minY;
         out[2] = maxX;
         out[3] = maxY;
         return hull;
      } else {
         return -1;
      }
   }

   private void onEntityRenderState(EntityRenderStateEvent event) {
      EntityFills.fillHeldItems = this.shader.heldItems();
      if (this.shaded() || this.outlined()) {
         Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
         Entity entity = event.entity();
         double distanceSq = entity.distanceToSqr(camera.position());
         boolean targeted = this.targeted(entity, camera) && distanceSq <= this.range();
         if (this.outlined()) {
            if (targeted) {
               int tint = this.tint(entity, distanceSq);
               event.state().outlineColor = tint != 0 ? tint : this.outline.color();
            }

            if (targeted || entity.isCurrentlyGlowing()) {
               this.outline.include(event);
            }
         } else if (targeted) {
            int tint = this.tint(entity, distanceSq);
            ((Filled)event.state()).client$setFill(tint != 0 ? this.shader.teamFill(tint, event.renderPosition(), camera) : this.shader.entityFill(event.renderPosition(), camera, event.state()));
         }
      }

   }

   private void onHandRender(HandRenderEvent event) {
      if (this.shaded() && this.handTargeted()) {
         event.setFill(this.shader.handFill());
      }

   }

   private int teamRgb(Entity entity) {
      if (!(entity instanceof Player player)) {
         return 0;
      } else {
         TeamColor color = teamColor(player.getTeam());
         return color != null && color != TeamColor.WHITE ? -16777216 | color.rgb() : 0;
      }
   }

   public static TeamColor teamColor(PlayerTeam team) {
      if (team == null) {
         return null;
      } else {
         TeamColor color = (TeamColor)team.getColor().orElse((Object)null);
         if (color != null && color != TeamColor.WHITE && color != TeamColor.GRAY) {
            return color;
         } else {
            TextColor[] last = new TextColor[1];
            team.getPlayerPrefix().visit((style, text) -> {
               if (style.getColor() != null && !text.isBlank()) {
                  last[0] = style.getColor();
               }

               return Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
            if (last[0] != null) {
               for(TeamColor candidate : TeamColor.values()) {
                  if (candidate.textColor().getValue() == last[0].getValue()) {
                     return candidate;
                  }
               }
            }

            return color;
         }
      }
   }

   private double range() {
      double limit = this.distance.limitValue();
      return limit * limit;
   }

   private List<Target> targets(FrameEvent event) {
      Vec3 origin = event.camera().position();
      double range = this.range();
      List<Target> found = new ArrayList();

      for(Entity entity : event.level().entitiesForRendering()) {
         if (this.targeted(entity, event.camera())) {
            double distanceSq = entity.distanceToSqr(origin);
            if (distanceSq <= range) {
               found.add(new Target(entity, event.interpolatedBounds(entity), distanceSq));
            }
         }
      }

      return found;
   }

   public boolean drawsWith(Entity entity) {
      if (!this.shaded() && !this.outlined()) {
         return false;
      } else {
         Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
         return this.targeted(entity, camera) && entity.distanceToSqr(camera.position()) <= this.range();
      }
   }

   private boolean targeted(Entity entity, Camera camera) {
      if (!entity.isRemoved() && !entity.isSpectator()) {
         if (entity == camera.entity() && !camera.isDetached()) {
            return false;
         } else if (entity instanceof LocalPlayer) {
            return ((Set)this.targets.get()).contains(0);
         } else {
            return entity instanceof Player ? ((Set)this.targets.get()).contains(1) : ((Set)this.targets.get()).contains(2) && ((Set)this.entities.get()).contains(Entities.id(entity));
         }
      } else {
         return false;
      }
   }

   static record Target(Entity entity, AABB bounds, double distanceSq) {
   }
}
