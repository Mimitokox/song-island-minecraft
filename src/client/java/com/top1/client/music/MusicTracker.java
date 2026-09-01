package com.top1.client.music;

import com.top1.client.SongIslandClient;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import com.top1.client.music.LocalLyrics;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class MusicTracker {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("song-island");
	private IMediaSession session;
	private List<LyricsFetcher.LyricLine> lyrics = List.of();
	private int mediaColor = 0xFFFFFFFF;
	private final Map<Integer, Identifier> textureCache = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> colorCache = new ConcurrentHashMap<>();
	private String lastTrack = "";
	private boolean playing;
	private volatile float peak;
	private volatile float lyricsOffset;
	private volatile boolean lyricsSynced;

	private long retryAt;
	private int tries;
	private static final Random RANDOM = new Random();

	private int ticks;
	private long logAt;
	private String currentKey = "";
	private float clock = -1.0F;
	private long clockStamp;

	private static String key(IMediaSession session) {
		return session.getOwner() + "|" + session.getMedia().getArtist() + " - " + session.getMedia().getTitle();
	}

	private volatile boolean running = true;
	private Thread thread;

	public MusicTracker() {
		thread = new Thread(() -> {
			while(running){
				try{
					Thread.sleep(25L);
					float value = AudioLevel.peak();
					if(value >= 0.0F) peak = value;
					if(++ticks % 4 == 0) poll();
				}catch(InterruptedException e){
					Thread.currentThread().interrupt();
					return;
				}
			}
		});
		thread.setDaemon(true);
		thread.setName("song-island-media");
		thread.start();
	}

	private void poll() {
		try{
			List<IMediaSession> sessions = MediaPlayerInfo.INSTANCE.getMediaSessions();
			List<IMediaSession> usable = sessions.stream()
				.filter(s -> !s.getMedia().getArtist().isEmpty() && !s.getMedia().getTitle().isEmpty())
				.toList();

			IMediaSession found = null;
			if(!currentKey.isEmpty()){
				for(IMediaSession candidate : usable){
					if(key(candidate).equals(currentKey) && candidate.getMedia().isPlaying()){
						found = candidate;
						break;
					}
				}
			}
			if(found == null){
				for(IMediaSession candidate : usable){
					if(candidate.getMedia().isPlaying()){
						found = candidate;
						break;
					}
				}
			}
			if(found == null && !currentKey.isEmpty()){
				for(IMediaSession candidate : usable){
					if(key(candidate).equals(currentKey)){
						found = candidate;
						break;
					}
				}
			}
			if(found == null && !usable.isEmpty()) found = usable.get(0);
			if(found != null){
				String foundKey = key(found);
				if(!foundKey.equals(currentKey)){
					currentKey = foundKey;
					clock = -1.0F;
				}
			}
			if(found == null){
				session = null;
				return;
			}
			session = found;
			MediaInfo info = found.getMedia();
			playing = info.isPlaying();
			long raw = info.getPosition();
			long now = System.currentTimeMillis();
			if(clockStamp != 0L && playing) clock += (now - clockStamp) / 1000.0F;
			clockStamp = now;

			float reported = raw + 0.5F;
			if(clock < 0.0F){
				clock = reported;
			}else{
				float diff = reported - clock;
				if(Math.abs(diff) > 3.0F){
					clock = reported;
				}else{
					clock += diff * 0.12F;
				}
			}
			if(info.getDuration() > 0L) clock = Math.min(clock, info.getDuration());

			if(System.currentTimeMillis() - logAt > 10000L){
				logAt = System.currentTimeMillis();
			}
			String trackId = found.getMedia().getArtist() + " - " + found.getMedia().getTitle();
			if(!trackId.equals(lastTrack)){
				lastTrack = trackId;
				clock = -1.0F;
				lyricsOffset = LyricDelays.get(info.getArtist(), info.getTitle());
				lyrics = List.of();
				lyricsSynced = false;
				tries = 0;
				retryAt = 0L;
			}
			if(lyrics.isEmpty() && tries < 5 && System.currentTimeMillis() >= retryAt){
				tries++;
				retryAt = System.currentTimeMillis() + 4000L;
				List<LyricsFetcher.LyricLine> local = LocalLyrics.load(info.getArtist(), info.getTitle());
				if(!local.isEmpty()){
					lyrics = local;
					lyricsSynced = true;
					return;
				}
				LyricsFetcher.Result fetched = LyricsFetcher.fetch(
					info.getArtist(), info.getTitle(), info.getDuration());
				if(!fetched.isEmpty()){
					lyrics = fetched.lines();
					lyricsSynced = fetched.synced();
				}else{
				}
			}
		}catch(Throwable ignored){
		}
	}

	public IMediaSession getSession() {
		return session;
	}

	public void shutdown() {
		running = false;
		if(thread != null) thread.interrupt();
		try{
			dev.redstones.mediaplayerinfo.impl.win.WindowsMediaPlayerInfo.cleanup();
		}catch(Throwable e){
		}
	}

	public float getPeak() {
		return peak;
	}

	public float rawPosition() {
		float value = clock;
		if(playing && clockStamp != 0L) value += (System.currentTimeMillis() - clockStamp) / 1000.0F;
		return Math.max(0.0F, value);
	}

	public float smoothPosition() {
		float value = clock;
		if(playing && clockStamp != 0L) value += (System.currentTimeMillis() - clockStamp) / 1000.0F;
		return Math.max(0.0F, value) + lyricsOffset;
	}

	public float getLyricsOffset() {
		return lyricsOffset;
	}

	public void adjustLyricsOffset(float delta) {
		lyricsOffset = Math.max(-10.0F, Math.min(10.0F, lyricsOffset + delta));
		IMediaSession current = session;
		if(current != null){
			LyricDelays.set(current.getMedia().getArtist(), current.getMedia().getTitle(), lyricsOffset);
		}
	}

	public boolean hasSession() {
		return session != null;
	}

	public List<LyricsFetcher.LyricLine> getLyrics() {
		return lyricsSynced ? lyrics : List.of();
	}

	public List<LyricsFetcher.LyricLine> getRawLyrics() {
		return lyrics;
	}

	public boolean hasLocalMapping() {
		IMediaSession current = session;
		return current != null
			&& LocalLyrics.exists(current.getMedia().getArtist(), current.getMedia().getTitle());
	}

	public void forgetMapping() {
		IMediaSession current = session;
		if(current == null) return;
		LocalLyrics.delete(current.getMedia().getArtist(), current.getMedia().getTitle());
		lyrics = List.of();
		lyricsSynced = false;
		tries = 0;
		retryAt = 0L;
	}

	public void reloadLocal() {
		IMediaSession current = session;
		if(current == null) return;
		List<LyricsFetcher.LyricLine> local = LocalLyrics.load(
			current.getMedia().getArtist(), current.getMedia().getTitle());
		if(!local.isEmpty()){
			lyrics = local;
			lyricsSynced = true;
		}
	}

	public boolean isSynced() {
		return lyricsSynced;
	}

	public boolean needsMapping() {
		return !lyrics.isEmpty() && !lyricsSynced;
	}

	public int getMediaColor() {
		return mediaColor;
	}

	public Identifier getImage() {
		try{
			if(textureCache.size() > 10){
				textureCache.clear();
				colorCache.clear();
			}
			byte[] data = session.getMedia().getArtworkPng();
			if(data == null || data.length == 0) return null;
			int hash = Arrays.hashCode(data);
			Integer cachedColor = colorCache.get(hash);
			if(cachedColor != null){
				mediaColor = cachedColor;
				return textureCache.get(hash);
			}
			Identifier id = com.top1.client.island.render.IslandRender.id("temp/" + randomString());
			DynamicTexture texture = new DynamicTexture(() -> "song island cover", NativeImage.read(toPng(data)));
			mediaColor = averageColor(texture.getPixels(), 2);
			Minecraft.getInstance().getTextureManager().register(id, texture);
			textureCache.put(hash, id);
			colorCache.put(hash, mediaColor);
			return id;
		}catch(Exception e){
			return null;
		}
	}

	// Apple Music wystawia JPEG, NativeImage czyta tylko PNG
	private static byte[] toPng(byte[] data) throws Exception {
		java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
		if(image == null) throw new java.io.IOException("unsupported artwork format");
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	public static int averageColor(com.mojang.blaze3d.platform.NativeImage image, int step) {
		int width = image.getWidth();
		int height = image.getHeight();
		long r = 0, g = 0, b = 0;
		int count = 0;
		for(int y = 0; y < height; y += step){
			for(int x = 0; x < width; x += step){
				int argb = image.getPixel(x, y);
				r += argb >> 16 & 0xFF;
				g += argb >> 8 & 0xFF;
				b += argb & 0xFF;
				count++;
			}
		}
		if(count == 0) return 0xFFFFFFFF;
		int add = 50;
		int rr = Math.min(255, (int)(r / count) + add);
		int gg = Math.min(255, (int)(g / count) + add);
		int bb = Math.min(255, (int)(b / count) + add);
		return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
	}

	private static String randomString() {
		StringBuilder sb = new StringBuilder(32);
		for(int i = 0; i < 32; i++) sb.append((char)(97 + RANDOM.nextInt(26)));
		return sb.toString();
	}

	public static String formatTime(long totalSeconds) {
		return String.format("%d:%02d", totalSeconds / 60L, totalSeconds % 60L);
	}
}
