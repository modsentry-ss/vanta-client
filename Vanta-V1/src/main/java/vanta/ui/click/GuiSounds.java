package vanta.ui.click;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

final class GuiSounds {
   private static long lastHover;
   private static long lastDetent;
   private static long lastTap;

   private GuiSounds() {
   }

   private static void play(String name, float gain, float pitchSpread) {
      if (GuiPrefs.sounds) {
         float volume = gain * (float)GuiPrefs.volume / 100.0F;
         if (!(volume <= 0.002F)) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            float pitch = 1.0F + (r.nextFloat() * 2.0F - 1.0F) * pitchSpread;

            try {
               Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(Identifier.fromNamespaceAndPath("vanta", "ui." + name), SoundSource.MASTER, volume, pitch, RandomSource.create(), false, 0, Attenuation.NONE, (double)0.0F, (double)0.0F, (double)0.0F, true));
            } catch (RuntimeException var7) {
            }

         }
      }
   }

   static void open() {
      play("open", 0.9F, 0.02F);
   }

   static void close() {
      play("close", 0.85F, 0.02F);
   }

   static void toggle(boolean on) {
      lastTap = System.nanoTime();
      play(on ? "on" : "off", 1.0F, 0.03F);
   }

   static void tap() {
      long now = System.nanoTime();
      if (now - lastTap > 40000000L) {
         lastTap = now;
         play("detent", 0.8F, 0.04F);
      }

   }

   static void hover() {
      if (GuiPrefs.hoverSounds) {
         long now = System.nanoTime();
         if (now - lastHover > 90000000L) {
            lastHover = now;
            play("hover", 0.6F, 0.05F);
         }

      }
   }

   static void expand(boolean open) {
      play(open ? "expand" : "collapse", 0.9F, 0.02F);
   }

   static void detent() {
      long now = System.nanoTime();
      if (now - lastDetent > 55000000L) {
         lastDetent = now;
         play("detent", 0.7F, 0.05F);
      }

   }

   static void bind() {
      play("bind", 0.9F, 0.0F);
   }
}
