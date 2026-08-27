package dev.redstones.mediaplayerinfo.impl.win;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;

public class WindowsMediaSession implements IMediaSession {
   private final MediaInfo media;
   private final String owner;
   private final int index;
   public static int cycle = -1;

   public WindowsMediaSession(MediaInfo media, String owner, int index) {
      this.media = media;
      this.owner = owner;
      this.index = index;
   }

   @Override
   public MediaInfo getMedia() {
      return this.media;
   }

   @Override
   public String getOwner() {
      return this.owner;
   }

   @Override
   public native void play();

   @Override
   public native void pause();

   @Override
   public native void playPause();

   @Override
   public native void stop();

   @Override
   public native void next();

   @Override
   public native void previous();

   @Override
   public native void swapCycle();

   @Override
   public native int getCycleType();

   public static int getCycle() {
      return cycle;
   }

   public static void setCycle(int cycle) {
      WindowsMediaSession.cycle = cycle;
   }
}
