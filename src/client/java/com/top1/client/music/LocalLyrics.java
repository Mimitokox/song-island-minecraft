package com.top1.client.music;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocalLyrics {
	private static final Logger LOGGER = LoggerFactory.getLogger("song-island");

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("song-island").resolve("lyrics");
	}

	public static Path fileFor(String artist, String title) {
		return directory().resolve(safe(artist) + " - " + safe(title) + ".lrc");
	}

	public static List<LyricsFetcher.LyricLine> load(String artist, String title) {
		Path file = fileFor(artist, title);
		if(!Files.isRegularFile(file)) return List.of();
		try{
			String content = Files.readString(file, StandardCharsets.UTF_8);
			List<LyricsFetcher.LyricLine> lines = LyricsFetcher.parseLrc(content);
			return lines;
		}catch(IOException e){
			LOGGER.warn("cannot read local lyrics: {}", e.toString());
			return List.of();
		}
	}

	public static boolean save(String artist, String title, List<LyricsFetcher.LyricLine> lines) {
		if(lines.isEmpty()) return false;
		try{
			Files.createDirectories(directory());
			StringBuilder builder = new StringBuilder();
			builder.append("[ar:").append(artist).append("]\n");
			builder.append("[ti:").append(title).append("]\n");
			for(LyricsFetcher.LyricLine line : lines){
				if(line.time() < 0.0F) continue;
				int minutes = (int) (line.time() / 60.0F);
				float seconds = line.time() - minutes * 60.0F;
				builder.append(String.format(Locale.ROOT, "[%02d:%05.2f]%s%n", minutes, seconds, line.text()));
			}
			Path file = fileFor(artist, title);
			Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
			LOGGER.info("saved local lyrics to {}", file);
			return true;
		}catch(IOException e){
			LOGGER.warn("cannot save local lyrics: {}", e.toString());
			return false;
		}
	}

	public static boolean exists(String artist, String title) {
		return Files.isRegularFile(fileFor(artist, title));
	}

	public static boolean delete(String artist, String title) {
		try{
			boolean removed = Files.deleteIfExists(fileFor(artist, title));
			if(removed) LOGGER.info("removed local lyrics for {} - {}", artist, title);
			return removed;
		}catch(IOException e){
			LOGGER.warn("cannot delete local lyrics: {}", e.toString());
			return false;
		}
	}

	public static List<String> plainLines(List<LyricsFetcher.LyricLine> lines) {
		List<String> texts = new ArrayList<>();
		for(LyricsFetcher.LyricLine line : lines){
			if(!line.text().isBlank()) texts.add(line.text());
		}
		return texts;
	}

	private static String safe(String value) {
		return value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
	}

	private LocalLyrics() {
	}
}
