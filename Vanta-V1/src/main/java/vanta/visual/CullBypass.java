package vanta.visual;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import vanta.VantaClient;
import vanta.module.impl.render.esp.Esp;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public final class CullBypass {
   private static final Minecraft mc = Minecraft.getInstance();
   private static MethodHandle keepVisible;
   private static Class<?> cullable;
   private static boolean looked;

   private CullBypass() {
   }

   public static void init() {
      ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> tick());
   }

   public static boolean wanted(Entity entity) {
      if (entity instanceof Player && entity != mc.player) {
         return true;
      } else if (!(entity instanceof LivingEntity) && !(entity instanceof ItemEntity)) {
         return false;
      } else if (VantaClient.MODULES == null) {
         return false;
      } else {
         Esp esp = (Esp)VantaClient.MODULES.get(Esp.class);
         return esp != null && esp.isEnabled() && esp.drawsWith(entity);
      }
   }

   private static void tick() {
      if (mc.level != null && mc.player != null) {
         MethodHandle handle = keepVisible();
         if (handle != null) {
            for(Entity entity : mc.level.entitiesForRendering()) {
               if (cullable.isInstance(entity) && wanted(entity)) {
                  try {
                     handle.invoke(entity);
                  } catch (Throwable var4) {
                  }
               }
            }

         }
      }
   }

   private static MethodHandle keepVisible() {
      if (!looked) {
         looked = true;

         for(String name : new String[]{"dev.tr7zw.entityculling.access.Cullable", "dev.tr7zw.entityculling.versionless.access.Cullable"}) {
            try {
               cullable = Class.forName(name);
               keepVisible = MethodHandles.publicLookup().findVirtual(cullable, "setTimeout", MethodType.methodType(Void.TYPE)).asType(MethodType.methodType(Void.TYPE, Object.class));
               break;
            } catch (LinkageError | ReflectiveOperationException var5) {
               cullable = null;
               keepVisible = null;
            }
         }
      }

      return keepVisible;
   }
}
