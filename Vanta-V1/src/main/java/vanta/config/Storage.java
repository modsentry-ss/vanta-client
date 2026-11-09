package vanta.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import vanta.VantaCore;
import net.minecraft.client.Minecraft;

public final class Storage {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

   public static Path directory() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("VantaClient");
   }

   public static Path file(String name) {
      return directory().resolve(name + ".json");
   }

   public static List<Path> files(Path directory) {
      if (!Files.isDirectory(directory, new LinkOption[0])) {
         return List.of();
      } else {
         try {
            Stream<Path> stream = Files.list(directory);

            ArrayList var2;
            try {
               var2 = new ArrayList(stream.filter((path) -> path.toString().endsWith(".json")).sorted().toList());
            } catch (Throwable var5) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (stream != null) {
               stream.close();
            }

            return var2;
         } catch (IOException exception) {
            VantaCore.LOGGER.warn("Cannot list {}", directory, exception);
            return List.of();
         }
      }
   }

   public static String nameOf(Path path) {
      String file = path.getFileName().toString();
      return file.endsWith(".json") ? file.substring(0, file.length() - ".json".length()) : file;
   }

   public static <T> T read(Path path, Class<T> type) {
      if (!Files.isRegularFile(path, new LinkOption[0])) {
         return null;
      } else {
         try {
            Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);

            Object var3;
            try {
               var3 = GSON.fromJson(reader, type);
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return (T)var3;
         } catch (RuntimeException | IOException exception) {
            VantaCore.LOGGER.warn("Cannot read {}", path, exception);
            return null;
         }
      }
   }

   static String json(Object value) {
      return GSON.toJson(value);
   }

   static <T> T parse(String json, Class<T> type) {
      try {
         return (T)GSON.fromJson(json, type);
      } catch (RuntimeException exception) {
         VantaCore.LOGGER.warn("Cannot parse a {}", type.getSimpleName(), exception);
         return null;
      }
   }

   public static boolean write(Path path, Object value) {
      Path temporary = path.resolveSibling(String.valueOf(path.getFileName()) + ".tmp");

      try {
         Files.createDirectories(path.getParent());
         Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8);

         try {
            GSON.toJson(value, writer);
         } catch (Throwable var7) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (writer != null) {
            writer.close();
         }

         move(temporary, path);
         return true;
      } catch (RuntimeException | IOException exception) {
         VantaCore.LOGGER.warn("Cannot write {}", path, exception);
         deleteQuietly(temporary);
         return false;
      }
   }

   private static void move(Path temporary, Path path) throws IOException {
      try {
         Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException var3) {
         Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
      }

   }

   public static boolean delete(Path path) {
      try {
         return Files.deleteIfExists(path);
      } catch (IOException exception) {
         VantaCore.LOGGER.warn("Cannot delete {}", path, exception);
         return false;
      }
   }

   private static void deleteQuietly(Path path) {
      try {
         Files.deleteIfExists(path);
      } catch (IOException var2) {
      }

   }

   private Storage() {
   }
}
