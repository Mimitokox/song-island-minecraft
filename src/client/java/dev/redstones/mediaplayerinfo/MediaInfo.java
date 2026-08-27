package dev.redstones.mediaplayerinfo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import javax.imageio.ImageIO;

public class MediaInfo implements Serializable {
   private final String title;
   private final String artist;
   private final byte[] artworkPng;
   private final long position;
   private final long duration;
   private final boolean playing;

   public MediaInfo(String title, String artist, byte[] artworkPng, long position, long duration, boolean playing) {
      this.title = title;
      this.artist = artist;
      this.artworkPng = artworkPng;
      this.position = position;
      this.duration = duration;
      this.playing = playing;
   }

   public String getTitle() {
      return this.title;
   }

   public String getArtist() {
      return this.artist;
   }

   public byte[] getArtworkPng() {
      return this.artworkPng;
   }

   public long getPosition() {
      return this.position;
   }

   public long getDuration() {
      return this.duration;
   }

   public boolean isPlaying() {
      return this.playing;
   }

   public BufferedImage getArtwork() {
      try {
         return ImageIO.read(new ByteArrayInputStream(this.artworkPng));
      } catch (Exception var2) {
         return null;
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         MediaInfo mediaInfo = (MediaInfo)o;
         return this.position == mediaInfo.position
            && this.duration == mediaInfo.duration
            && this.playing == mediaInfo.playing
            && Objects.equals(this.title, mediaInfo.title)
            && Objects.equals(this.artist, mediaInfo.artist)
            && Arrays.equals(this.artworkPng, mediaInfo.artworkPng);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = Objects.hash(this.title, this.artist, this.position, this.duration, this.playing);
      return 31 * result + Arrays.hashCode(this.artworkPng);
   }

   @Override
   public String toString() {
      return "MediaInfo{title='"
         + this.title
         + "', artist='"
         + this.artist
         + "', position="
         + this.position
         + ", duration="
         + this.duration
         + ", playing="
         + this.playing
         + "}";
   }
}
