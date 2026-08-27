package dev.redstones.mediaplayerinfo;

public interface IMediaSession {
   String getOwner();

   MediaInfo getMedia();

   void play();

   void pause();

   void playPause();

   void stop();

   void next();

   void previous();

   void swapCycle();

   int getCycleType();
}
