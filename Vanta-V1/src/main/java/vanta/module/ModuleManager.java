package vanta.module;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import vanta.config.State;
import vanta.module.impl.combat.AutoMace;
import vanta.module.impl.combat.BreachSwap;
import vanta.module.impl.combat.PearlCatch;
import vanta.module.impl.combat.PearlGrapple;
import vanta.module.impl.combat.SpearLunge;
import vanta.module.impl.hud.Keystrokes;
import vanta.module.impl.hud.ModuleList;
import vanta.module.impl.hud.NamePlate;
import vanta.module.impl.hud.Notifications;
import vanta.module.impl.hud.Watermark;
import vanta.module.impl.misc.ClientSpoofer;
import vanta.module.impl.movement.MoveFix;
import vanta.module.impl.movement.NoJumpDelay;
import vanta.module.impl.movement.Sprint;
import vanta.module.impl.player.AutoTotem;
import vanta.module.impl.player.FastPlace;
import vanta.module.impl.render.FreeLook;
import vanta.module.impl.render.Freecam;
import vanta.module.impl.render.Perspective;
import vanta.module.impl.render.esp.Esp;
import vanta.setting.Setting;
import net.minecraft.client.Minecraft;

public final class ModuleManager {
   private final List<Module> modules = new ArrayList();
   private final Map<Category, List<Module>> byCategory = new EnumMap(Category.class);
   private final Map<String, Module> byId = new HashMap();
   private final Map<Class<? extends Module>, Module> byType = new HashMap();
   private static final List<String> ORDER = List.of("AutoMace", "BreachSwap", "SpearLunge", "PearlCatch", "PearlGrapple", "Sprint", "NoJumpDelay", "MoveFix", "Esp", "Freecam", "FreeLook", "Perspective", "AutoTotem", "FastPlace", "ModuleList", "Watermark", "Notifications", "Keystrokes", "ClientSpoofer", "ClientSettings");
   private final Map<String, State.ModuleState> unclaimed = new LinkedHashMap();

   public ModuleManager() {
      for(Category category : Category.values()) {
         this.byCategory.put(category, new ArrayList());
      }

      List<Class<? extends Module>> types = discover();
      types.sort(Comparator.comparingInt(ModuleManager::rank).thenComparing(Class::getSimpleName));

      for(Class<? extends Module> type : types) {
         this.register(type);
      }

   }

   private static int rank(Class<? extends Module> type) {
      int index = ORDER.indexOf(type.getSimpleName());
      return index < 0 ? ORDER.size() : index;
   }

   static List<Class<? extends Module>> discover() {
      List<Class<? extends Module>> types = new ArrayList();
      types.add(AutoMace.class);
      types.add(BreachSwap.class);
      types.add(SpearLunge.class);
      types.add(PearlCatch.class);
      types.add(PearlGrapple.class);
      types.add(MoveFix.class);
      types.add(Sprint.class);
      types.add(NoJumpDelay.class);
      types.add(FreeLook.class);
      types.add(Perspective.class);
      types.add(FastPlace.class);
      types.add(ClientSpoofer.class);
      types.add(AutoTotem.class);
      types.add(Esp.class);
      types.add(Freecam.class);
      types.add(ModuleList.class);
      types.add(Watermark.class);
      types.add(Notifications.class);
      types.add(Keystrokes.class);
      types.add(NamePlate.class);
      return types;
   }

   static Category categoryOf(Class<?> type) {
      String prefix = "vanta/module/impl".replace('/', '.') + ".";
      String rest = type.getPackageName().substring(prefix.length());
      int dot = rest.indexOf(46);
      String name = dot < 0 ? rest : rest.substring(0, dot);

      for(Category category : Category.values()) {
         if (category.id().equals(name)) {
            return category;
         }
      }

      return Category.valueOf(name.toUpperCase(Locale.ROOT));
   }

   private void register(Class<? extends Module> type) {
      Module module;
      try {
         module = (Module)type.getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException exception) {
         throw new IllegalStateException("Cannot create module " + type.getName(), exception);
      }

      if (this.byId.containsKey(module.id())) {
         throw new IllegalArgumentException("Duplicate module id: " + module.id());
      } else {
         module.category = categoryOf(type);
         this.modules.add(module);
         ((List)this.byCategory.get(module.category)).add(module);
         this.byId.put(module.id(), module);
         this.byType.put(type, module);
      }
   }

   public void registerExternal(Module module, Category category) {
      if (this.byId.containsKey(module.id())) {
         throw new IllegalArgumentException("Duplicate module id: " + module.id());
      } else {
         module.category = category;
         this.modules.add(module);
         ((List)this.byCategory.get(category)).add(module);
         this.byId.put(module.id(), module);
         ((List)this.byCategory.get(category)).sort(Comparator.comparing((m) -> m.name().toLowerCase(Locale.ROOT)));
      }
   }

   public void unregisterExternal(Module module) {
      if (this.byId.get(module.id()) == module) {
         this.unclaimed.put(module.id(), stateOf(module));
         this.modules.remove(module);
         ((List)this.byCategory.get(module.category)).remove(module);
         this.byId.remove(module.id());
      }
   }

   public void claim(Module module) {
      State.ModuleState state = (State.ModuleState)this.unclaimed.remove(module.id());
      if (state != null && this.byId.get(module.id()) == module) {
         this.apply(Map.of(module.id(), state));
      }

   }

   private static boolean keepsUnclaimed(String id) {
      return id.startsWith("script_");
   }

   public List<Module> all() {
      return Collections.unmodifiableList(this.modules);
   }

   public List<Module> in(Category category) {
      return Collections.unmodifiableList((List)this.byCategory.get(category));
   }

   public List<Module> shown(Category category) {
      return this.in(category);
   }

   public void pressed(InputConstants.Key key) {
      if (key != null && key != InputConstants.UNKNOWN) {
         for(Module module : this.modules) {
            if (key.equals(module.key())) {
               module.pressed();
            }
         }

      }
   }

   public Optional<Module> find(String id) {
      return Optional.ofNullable((Module)this.byId.get(id));
   }

   public Module get(String id) {
      return (Module)this.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown module: " + id));
   }

   public <M extends Module> M get(Class<M> type) {
      Module module = (Module)this.byType.get(type);
      if (module == null) {
         throw new IllegalArgumentException("Unregistered module: " + type.getSimpleName());
      } else {
         return (M)(type.cast(module));
      }
   }

   public boolean isEnabled(Class<? extends Module> type) {
      Module module = (Module)this.byType.get(type);
      return module != null && module.isEnabled();
   }

   public void tickKeybinds(Minecraft client) {
      for(Module module : this.modules) {
         module.tickKeybind(client);
      }

   }

   public boolean activates(InputConstants.Key key) {
      for(Module module : this.modules) {
         if (module.activatable() && module.isEnabled() && module.key().equals(key)) {
            return true;
         }
      }

      return false;
   }

   public Map<String, State.ModuleState> snapshot() {
      Map<String, State.ModuleState> states = new LinkedHashMap();

      for(Module module : this.modules) {
         states.put(module.id(), stateOf(module));
      }

      for(Map.Entry<String, State.ModuleState> entry : this.unclaimed.entrySet()) {
         states.putIfAbsent((String)entry.getKey(), (State.ModuleState)entry.getValue());
      }

      return states;
   }

   private static State.ModuleState stateOf(Module module) {
      State.ModuleState state = new State.ModuleState();
      if (module.bridged()) {
         state.hold = module.hold();
         state.settings = new LinkedHashMap();
         state.settings.put(module.hideNotificationsSetting().id(), module.hideNotificationsSetting().save());
         return state;
      } else {
         state.enabled = module.isEnabled();
         state.key = module.key().getName();
         state.hold = module.hold();
         state.settings = new LinkedHashMap();

         for(Setting<?> setting : module.settings()) {
            state.settings.put(setting.id(), setting.save());
         }

         return state;
      }
   }

   public void apply(Map<String, State.ModuleState> states) {
      for(Map.Entry<String, State.ModuleState> entry : states.entrySet()) {
         if (entry.getValue() != null && keepsUnclaimed((String)entry.getKey()) && !this.byId.containsKey(entry.getKey())) {
            this.unclaimed.put((String)entry.getKey(), (State.ModuleState)entry.getValue());
         }
      }

      for(Module module : List.copyOf(this.modules)) {
         State.ModuleState state = (State.ModuleState)states.get(module.id());
         if (state != null && module.bridged()) {
            module.setHold(state.hold);
            JsonElement hide = state.settings == null ? null : (JsonElement)state.settings.get(module.hideNotificationsSetting().id());
            if (hide != null) {
               module.hideNotificationsSetting().load(hide);
            }
         } else if (state != null) {
            module.setHold(state.hold);
            module.setKey(parseKey(state.key));
            if (state.settings != null) {
               for(Setting<?> setting : module.settings()) {
                  JsonElement json = (JsonElement)state.settings.get(setting.id());
                  if (json != null) {
                     setting.load(json);
                  }
               }
            }

            module.setEnabled(state.enabled);
         }
      }

   }

   public static InputConstants.Key parseKey(String name) {
      if (name == null) {
         return InputConstants.UNKNOWN;
      } else {
         try {
            return InputConstants.getKey(name);
         } catch (IllegalArgumentException var2) {
            return InputConstants.UNKNOWN;
         }
      }
   }
}
