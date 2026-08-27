package dev.redstones.mediaplayerinfo.impl.win;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WindowsMediaPlayerInfo implements MediaPlayerInfo {
   @Override
   public native List<IMediaSession> getMediaSessions();

   public static native void cleanup();

   static {
      try {
         Path tempDir = Files.createTempDirectory("mediaplayerinfo-");
         Path dllFile = tempDir.resolve("MediaPlayerInfo.dll");

         try (InputStream inputStream = WindowsMediaPlayerInfo.class.getResourceAsStream("/mediaplayerinfo/natives/win/MediaPlayerInfo.dll")) {
            if (inputStream == null) {
               throw new IOException("Resource not found: /mediaplayerinfo/natives/win/MediaPlayerInfo.dll");
            }

            Files.write(dllFile, inputStream.readAllBytes());
         }

         System.load(dllFile.toAbsolutePath().toString());

         try {
            Files.deleteIfExists(dllFile);
            Files.deleteIfExists(tempDir);
         } catch (IOException var6) {
            dllFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
         }
      } catch (IOException var8) {
         throw new RuntimeException("Failed to load MediaPlayerInfo.dll", var8);
      }
   }
}
