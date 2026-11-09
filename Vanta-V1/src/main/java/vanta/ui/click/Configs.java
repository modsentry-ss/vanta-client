package vanta.ui.click;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import vanta.VantaClient;
import vanta.config.State;
import vanta.config.StateStore;
import vanta.config.Storage;

final class Configs {
   private static List<String> names;

   private Configs() {
   }

   static Path dir() {
      return Storage.directory().resolve("configs");
   }

   static String clean(String name) {
      return name == null ? "" : name.trim().replaceAll("[^A-Za-z0-9 _.-]", "").replaceAll("\\s+", " ");
   }

   static List<String> list() {
      if (names == null) {
         names = new ArrayList();

         for(Path path : Storage.files(dir())) {
            names.add(Storage.nameOf(path));
         }
      }

      return names;
   }

   static void refresh() {
      names = null;
   }

   static boolean save(String name) {
      name = clean(name);
      if (name.isEmpty()) {
         return false;
      } else {
         State state = new State();
         state.modules = VantaClient.MODULES.snapshot();

         try {
            Files.createDirectories(dir());
         } catch (IOException var3) {
         }

         boolean ok = Storage.write(dir().resolve(name + ".json"), state);
         if (ok) {
            GuiPrefs.activeConfig = name;
            GuiPrefs.changed();
         }

         refresh();
         return ok;
      }
   }

   static boolean load(String name) {
      State state = (State)Storage.read(dir().resolve(name + ".json"), State.class);
      if (state != null && state.modules != null) {
         VantaClient.MODULES.apply(state.modules);
         StateStore.changed();
         GuiPrefs.activeConfig = name;
         GuiPrefs.changed();
         return true;
      } else {
         return false;
      }
   }

   static void delete(String name) {
      Storage.delete(dir().resolve(name + ".json"));
      if (name.equals(GuiPrefs.activeConfig)) {
         GuiPrefs.activeConfig = "";
         GuiPrefs.changed();
      }

      refresh();
   }
}
