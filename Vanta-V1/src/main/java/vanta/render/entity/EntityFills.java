package vanta.render.entity;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import vanta.VantaCore;
import vanta.mixin.accessor.RenderSetupAccessor;
import vanta.mixin.accessor.RenderTypeAccessor;
import vanta.mixin.accessor.TextureBindingAccessor;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class EntityFills {
   public static final int EFFECTS = 6;
   private static final ColorTargetState TRANSLUCENT;
   private static final RenderPipeline[] OCCLUDED;
   private static final RenderPipeline[] VISIBLE;
   private static final RenderPipeline[] HAND;
   private static final RenderPipeline SILHOUETTE;
   private static final OutputTarget OUTLINE;
   private static final Set<RenderPipeline> ALL;
   private static final Map<Key, RenderType> VISIBLE_TYPES;
   private static final Map<Key, RenderType> OCCLUDED_TYPES;
   private static final Map<Key, RenderType> HAND_TYPES;
   private static final Map<Identifier, RenderType> SILHOUETTE_TYPES;
   private static EntityFill hand;
   private static boolean handPass;
   private static EntityFill entity;
   public static boolean fillHeldItems;

   public static void beginEntity(EntityFill fill) {
      entity = fillHeldItems ? fill : null;
   }

   public static void endEntity() {
      entity = null;
   }

   public static EntityFill entity() {
      return entity;
   }

   public static void beginHand(EntityFill fill) {
      hand = fill;
      handPass = true;
   }

   public static void endHand() {
      hand = null;
      handPass = false;
   }

   public static EntityFill hand() {
      return hand;
   }

   public static boolean inHandPass() {
      return handPass;
   }

   public static RenderType visible(Identifier texture, int effect) {
      return (RenderType)VISIBLE_TYPES.computeIfAbsent(new Key(texture, effect), (key) -> create("visible", VISIBLE[key.effect()], key));
   }

   public static RenderType visibleHand(Identifier texture, int effect) {
      return (RenderType)HAND_TYPES.computeIfAbsent(new Key(texture, effect), (key) -> create("hand", HAND[key.effect()], key));
   }

   public static RenderType silhouette(Identifier texture) {
      return (RenderType)SILHOUETTE_TYPES.computeIfAbsent(texture, (key) -> RenderType.create("vanta:entity_silhouette/" + String.valueOf(key), RenderSetup.builder(SILHOUETTE).withTexture("Sampler0", key).setOutputTarget(OUTLINE).createRenderSetup()));
   }

   public static RenderType occluded(Identifier texture, int effect) {
      return (RenderType)OCCLUDED_TYPES.computeIfAbsent(new Key(texture, effect), (key) -> create("occluded", OCCLUDED[key.effect()], key));
   }

   public static boolean isFill(RenderType type) {
      return ALL.contains(type.pipeline());
   }

   public static Identifier texture(RenderType type) {
      RenderSetup setup = ((RenderTypeAccessor)type).client$getSetup();
      Object binding = ((RenderSetupAccessor)setup).client$getTextures().get("Sampler0");
      return binding == null ? null : ((TextureBindingAccessor)binding).client$getLocation();
   }

   private static RenderType create(String name, RenderPipeline pipeline, Key key) {
      return RenderType.create("vanta:entity_fill_" + name + key.effect() + "/" + String.valueOf(key.texture()), RenderSetup.builder(pipeline).withTexture("Sampler0", key.texture()).createRenderSetup());
   }

   private static RenderPipeline[] pipelines(String name, ColorTargetState color, DepthStencilState depth, boolean hand) {
      RenderPipeline[] pipelines = new RenderPipeline[7];

      for(int effect = 0; effect < pipelines.length; ++effect) {
         pipelines[effect] = pipeline(name + effect, effect, color, depth, hand);
      }

      return pipelines;
   }

   private static RenderPipeline pipeline(String name, int effect, ColorTargetState color, DepthStencilState depth, boolean hand) {
      return RenderPipeline.builder(new RenderPipeline.Snippet[0]).withLocation(VantaCore.id("pipeline/fill_" + name)).withVertexShader(VantaCore.id("core/fill")).withFragmentShader(VantaCore.id("core/fill")).withShaderDefine("EFFECT", effect).withShaderDefine("HAND", hand ? 1 : 0).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.SAMPLER0).withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS).withColorTargetState(color).withDepthStencilState(depth).withCull(false).build();
   }

   private EntityFills() {
   }

   static {
      TRANSLUCENT = new ColorTargetState(BlendFunction.TRANSLUCENT);
      OCCLUDED = pipelines("occluded", ColorTargetState.DEFAULT, new DepthStencilState(CompareOp.LESS_THAN, false), false);
      VISIBLE = pipelines("visible", TRANSLUCENT, DepthStencilState.DEFAULT, false);
      HAND = pipelines("hand", TRANSLUCENT, DepthStencilState.DEFAULT, true);
      SILHOUETTE = pipeline("silhouette", 0, ColorTargetState.DEFAULT, new DepthStencilState(CompareOp.ALWAYS_PASS, false), true);
      OUTLINE = new OutputTarget("vanta:outline", EntityOutlines::mask);
      ALL = new HashSet();
      VISIBLE_TYPES = new HashMap();
      OCCLUDED_TYPES = new HashMap();
      HAND_TYPES = new HashMap();
      SILHOUETTE_TYPES = new HashMap();

      for(RenderPipeline pipeline : OCCLUDED) {
         ALL.add(pipeline);
      }

      for(RenderPipeline pipeline : VISIBLE) {
         ALL.add(pipeline);
      }

      for(RenderPipeline pipeline : HAND) {
         ALL.add(pipeline);
      }

      ALL.add(SILHOUETTE);
   }

   private static record Key(Identifier texture, int effect) {
   }
}
