package vanta.module;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import vanta.VantaClient;
import vanta.config.StateStore;
import vanta.event.Event;
import vanta.event.Listener;
import vanta.event.Subscription;
import vanta.event.events.HudRenderEvent;
import vanta.event.events.ModuleToggleEvent;
import vanta.setting.BoolSetting;
import vanta.setting.Setting;
import vanta.setting.SettingGroup;
import vanta.ui.GuiState;
import vanta.util.Lang;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public abstract class Module {
   public static final Minecraft mc = Minecraft.getInstance();
   private final String id;
   Category category;
   private final List<Setting<?>> settings = new ArrayList();
   private final List<Subscription> subscriptions = new ArrayList();
   private boolean enabled;
   private InputConstants.Key key;
   private boolean hold;
   private boolean keyWasDown;
   private boolean pressedSinceTick;
   private final BoolSetting hideInList;
   private final BoolSetting hideNotifications;
   private int commonSettings;
   private String cachedName;
   private int cachedNameVersion;

   public String name() {
      if (this.cachedNameVersion != Lang.version()) {
         this.cachedName = Lang.getOrDefault("module." + this.id, Lang.capitalize(this.id));
         this.cachedNameVersion = Lang.version();
      }

      return this.cachedName;
   }

   public String info() {
      return "";
   }

   public boolean bridged() {
      return false;
   }

   public boolean hidden() {
      return false;
   }

   public BoolSetting hideInListSetting() {
      return this.hideInList;
   }

   public boolean hidesNotifications() {
      return false;
   }

   public BoolSetting hideNotificationsSetting() {
      return this.hideNotifications;
   }

   public String description() {
      return Lang.getOrDefault("module." + this.id + ".description", "");
   }

   protected <S extends Setting<?>> S add(S setting) {
      for(Setting<?> existing : this.settings) {
         if (existing.id().equals(setting.id())) {
            String var10002 = this.id;
            throw new IllegalArgumentException("Duplicate setting id: " + var10002 + "." + setting.id());
         }
      }

      setting.attach(this.id);
      this.settings.add(this.settings.size() - this.commonSettings, setting);
      return setting;
   }

   protected <G extends SettingGroup> G add(G group) {
      for(Setting<?> setting : group.settings()) {
         this.add(setting);
      }

      return group;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (enabled) {
            this.onEnable();
         } else {
            for(Subscription subscription : this.subscriptions) {
               subscription.cancel();
            }

            this.subscriptions.clear();
            this.onDisable();
         }

         VantaClient.EVENTS.post(new ModuleToggleEvent(this, enabled));
      }

   }

   public void toggle() {
      this.setEnabled(!this.isEnabled());
   }

   public void setKey(InputConstants.Key key) {
      this.key = key == null ? InputConstants.UNKNOWN : key;
      StateStore.changed();
   }

   public void setHold(boolean hold) {
      this.hold = hold;
      StateStore.changed();
   }

   public boolean activatable() {
      return false;
   }

   protected void onActivate() {
   }

   protected void onHold() {
   }

   protected void onRelease() {
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   protected <E extends Event> void listen(Class<E> type, Listener<E> listener) {
      this.listen(type, 0, listener);
   }

   protected <E extends Event> void listen(Class<E> type, int priority, Listener<E> listener) {
      if (this instanceof HudModule && type == HudRenderEvent.class) {
         listener = (event) -> {
            if (!GuiState.hudHidden()) {
               listener.on(event);
            }

         };
      }

      this.subscriptions.add(VantaClient.EVENTS.subscribe(type, priority, listener));
   }

   void pressed() {
      this.pressedSinceTick = true;
   }

   void tickKeybind(Minecraft client) {
      boolean pressed = this.pressedSinceTick;
      this.pressedSinceTick = false;
      if (!this.bridged() || this.hold) {
         InputConstants.Key bound = this.key();
         boolean held = bound != null && bound != InputConstants.UNKNOWN && client.gui.screen() == null && isKeyDown(bound, client.getWindow().handle());
         boolean down = held || pressed && !this.keyWasDown;
         if (this.activatable()) {
            if (this.isEnabled()) {
               if (down && !this.keyWasDown) {
                  this.onActivate();
               }

               if (down) {
                  this.onHold();
               }

               if (!down && this.keyWasDown) {
                  this.onRelease();
               }
            }
         } else if (this.hold) {
            if (down != this.keyWasDown) {
               this.setEnabled(down);
            }
         } else if (down && !this.keyWasDown) {
            this.toggle();
         }

         this.keyWasDown = down;
      }
   }

   private static boolean isKeyDown(InputConstants.Key key, long window) {
      boolean var10000;
      switch (key.getType()) {
         case KEYSYM -> var10000 = GLFW.glfwGetKey(window, key.getValue()) == 1;
         case MOUSE -> var10000 = GLFW.glfwGetMouseButton(window, key.getValue()) == 1;
         default -> var10000 = false;
      }

      return var10000;
   }

   protected Module(String id) {
      this.key = InputConstants.UNKNOWN;
      this.cachedNameVersion = -1;
      this.id = id;
      this.hideInList = (BoolSetting)this.add(new BoolSetting("hideInList", false));
      this.hideNotifications = (BoolSetting)this.add(new BoolSetting("hideNotifications", false));
      this.commonSettings = 2;
      this.hideInList.describe();
      this.hideNotifications.describe();
      this.hideInList.visibleWhen(() -> false);
      this.hideNotifications.visibleWhen(() -> false);
   }

   public String id() {
      return this.id;
   }

   public Category category() {
      return this.category;
   }

   public List<Setting<?>> settings() {
      return this.settings;
   }

   public InputConstants.Key key() {
      return this.key;
   }

   public boolean hold() {
      return this.hold;
   }
}
