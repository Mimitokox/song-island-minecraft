package dev.redstones.mediaplayerinfo;

import dev.redstones.mediaplayerinfo.impl.DummyMediaPlayerInfo;
import dev.redstones.mediaplayerinfo.impl.win.WindowsMediaPlayerInfo;
import java.util.List;

public interface MediaPlayerInfo {
   MediaPlayerInfo INSTANCE = MediaPlayerInfo.SystemMediaPlayerInfo.getInstance();

   List<IMediaSession> getMediaSessions();

   public static class SystemMediaPlayerInfo {
      private static final MediaPlayerInfo instance = (MediaPlayerInfo)(System.getProperty("os.name").toLowerCase().startsWith("windows")
         ? new WindowsMediaPlayerInfo()
         : new DummyMediaPlayerInfo());

      public static MediaPlayerInfo getInstance() {
         return instance;
      }
   }
}
