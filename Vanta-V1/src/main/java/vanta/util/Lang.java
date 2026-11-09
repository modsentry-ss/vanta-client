package vanta.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import vanta.ui.GuiState;

public final class Lang {
   private static final Map<Language, Map<String, String>> TRANSLATIONS = new EnumMap(Language.class);
   private static Language current;
   private static int version;
   private static final Map<String, String> RUNTIME;

   public static int version() {
      return version;
   }

   public static Language current() {
      return current;
   }

   public static void set(Language language) {
      current = language;
      ++version;
      GuiState.changed();
   }

   public static String getOrDefault(String key, String fallback) {
      String value = (String)translations(current).get(key);
      if (value == null) {
         value = (String)translations(Lang.Language.ENGLISH).get(key);
      }

      if (value == null) {
         value = (String)RUNTIME.get(key);
      }

      return value == null ? fallback : value;
   }

   public static void putRuntime(String key, String value) {
      if (value == null) {
         RUNTIME.remove(key);
      } else {
         RUNTIME.put(key, value);
      }

      ++version;
   }

   public static void removeRuntime(String prefix) {
      RUNTIME.keySet().removeIf((key) -> key.startsWith(prefix));
      ++version;
   }

   public static String capitalize(String id) {
      StringBuilder out = new StringBuilder(id.length() + 4);

      for(int i = 0; i < id.length(); ++i) {
         char c = id.charAt(i);
         if (i == 0) {
            out.append(Character.toUpperCase(c));
         } else if (Character.isUpperCase(c) && !Character.isUpperCase(id.charAt(i - 1))) {
            out.append(' ').append(c);
         } else {
            out.append(c);
         }
      }

      return out.toString();
   }

   public static String get(String key) {
      String value = (String)translations(current).get(key);
      if (value == null) {
         value = (String)translations(Lang.Language.ENGLISH).get(key);
      }

      if (value == null) {
         value = (String)RUNTIME.get(key);
      }

      return value == null ? key : value;
   }

   private static Map<String, String> translations(Language language) {
      return (Map)TRANSLATIONS.computeIfAbsent(language, Lang::load);
   }

   private static Map<String, String> load(Language language) {
      String path = "/assets/vanta/translations/" + language.code + ".json";

      try {
         InputStream stream = Lang.class.getResourceAsStream(path);

         Object var5;
         try {
            if (stream == null) {
               throw new IllegalStateException("Missing translations: " + path);
            }

            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);

            try {
               Map<String, String> map = new HashMap((Map)(new Gson()).fromJson(reader, (new TypeToken<Map<String, String>>() {
               }).getType()));
               mergeExtra(map, "/assets/vanta/translations/visuals/" + language.code + ".json");
               var5 = map;
            } catch (Throwable var8) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            reader.close();
         } catch (Throwable var9) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }
            }

            throw var9;
         }

         if (stream != null) {
            stream.close();
         }

         return (Map<String, String>)var5;
      } catch (Exception exception) {
         throw new IllegalStateException("Cannot load translations: " + path, exception);
      }
   }

   private static void mergeExtra(Map<String, String> map, String path) {
      try {
         InputStream extra = Lang.class.getResourceAsStream(path);

         try {
            if (extra != null) {
               InputStreamReader reader = new InputStreamReader(extra, StandardCharsets.UTF_8);

               try {
                  map.putAll((Map)(new Gson()).fromJson(reader, (new TypeToken<Map<String, String>>() {
                  }).getType()));
               } catch (Throwable var8) {
                  try {
                     reader.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }

                  throw var8;
               }

               reader.close();
            }
         } catch (Throwable var9) {
            if (extra != null) {
               try {
                  extra.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }
            }

            throw var9;
         }

         if (extra != null) {
            extra.close();
         }
      } catch (Exception var10) {
      }

   }

   private Lang() {
   }

   static {
      current = Lang.Language.ENGLISH;
      RUNTIME = new ConcurrentHashMap();
   }

   public static enum Language {
      ENGLISH("en", "English");

      public static final Language[] ALL = values();
      private final String code;
      private final String displayName;

      private Language(String code, String displayName) {
         this.code = code;
         this.displayName = displayName;
      }

      public String displayName() {
         return this.displayName;
      }

      // $FF: synthetic method
      private static Language[] $values() {
         return new Language[]{ENGLISH};
      }
   }
}
