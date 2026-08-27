package com.top1.client.music;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsFetcher {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("song-island");
	private static final HttpClient HTTP = HttpClient.newHttpClient();
	private static final Pattern LRC_LINE = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)](.*)");

	public record LyricLine(float time, String text) {}

	public record Result(List<LyricLine> lines, boolean synced) {
		public static final Result EMPTY = new Result(List.of(), false);

		public boolean isEmpty() {
			return lines.isEmpty();
		}
	}

	public static Result fetch(String artist, String title, long durationSeconds) {
		if(artist == null || title == null || artist.isBlank() || title.isBlank()) return Result.EMPTY;

		List<String> variants = artistVariants(artist);
		List<LyricLine> plain = List.of();

		for(String candidate : variants){
			Result exact = exact(candidate, title, durationSeconds);
			if(exact.synced()) return exact;
			if(plain.isEmpty() && !exact.isEmpty()) plain = exact.lines();
			if(durationSeconds > 0){
				Result loose = exact(candidate, title, 0L);
				if(loose.synced()) return loose;
				if(plain.isEmpty() && !loose.isEmpty()) plain = loose.lines();
			}
		}

		for(String candidate : variants){
			List<LyricLine> found = search(candidate, title, durationSeconds);
			if(!found.isEmpty()) return new Result(found, true);
		}

		for(String candidate : variants){
			List<LyricLine> found = NeteaseLyrics.fetch(candidate, title, durationSeconds);
			if(!found.isEmpty()) return new Result(found, true);
		}

		if(!plain.isEmpty()){
			return new Result(plain, false);
		}
		return Result.EMPTY;
	}

	private static Result exact(String artist, String title, long durationSeconds) {
		try{
			String q = "artist_name=" + URLEncoder.encode(clean(artist), StandardCharsets.UTF_8)
				+ "&track_name=" + URLEncoder.encode(clean(title), StandardCharsets.UTF_8);
			if(durationSeconds > 0) q += "&duration=" + durationSeconds;
			HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create("https://lrclib.net/api/get?" + q)) //ejpiaj te gorsze ale nie chinskie
				.header("User-Agent", "SongIsland/1.0") //reklama XXDDDDDDDDDDDDDDDDDDDD
				.header("Lrclib-Client", "SongIsland 1.0.0")
				.GET()
				.build();
			HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
			if(resp.statusCode() != 200){
				return Result.EMPTY;
			}
			JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
			String synced = json.has("syncedLyrics") && !json.get("syncedLyrics").isJsonNull()
				? json.get("syncedLyrics").getAsString() : null;
			if(synced != null && !synced.isBlank()){
				List<LyricLine> parsed = parseLrc(synced);
				return new Result(parsed, true);
			}
			String plain = json.has("plainLyrics") && !json.get("plainLyrics").isJsonNull()
				? json.get("plainLyrics").getAsString() : null;
			if(plain != null && !plain.isBlank()){
				List<LyricLine> untimed = parsePlain(plain);
				return untimed.isEmpty() ? Result.EMPTY : new Result(untimed, false);
			}
		}catch(Exception e){
			LOGGER.warn("lyrics get threw: {}", e.toString());
		}
		return Result.EMPTY;
	}

	private static List<LyricLine> search(String artist, String title, long durationSeconds) {
		try{
			String q = "q=" + URLEncoder.encode(clean(artist) + " " + clean(title), StandardCharsets.UTF_8);
			HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create("https://lrclib.net/api/search?" + q))
				.header("User-Agent", "SongIsland/1.0 (https://github.com/top1)")
				.header("Lrclib-Client", "SongIsland 1.0.0 (top1)")
				.GET()
				.build();
			HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
			if(resp.statusCode() != 200) return List.of();
			com.google.gson.JsonArray results = JsonParser.parseString(resp.body()).getAsJsonArray();
			JsonObject best = null;
			long bestScore = Long.MAX_VALUE;
			String wanted = normalize(title);
			for(com.google.gson.JsonElement element : results){
				JsonObject entry = element.getAsJsonObject();
				if(!entry.has("syncedLyrics") || entry.get("syncedLyrics").isJsonNull()) continue;
				String name = entry.has("trackName") ? normalize(entry.get("trackName").getAsString()) : "";
				if(!name.equals(wanted) && !name.contains(wanted) && !wanted.contains(name)) continue;
				long score = 0L;
				if(durationSeconds > 0 && entry.has("duration") && !entry.get("duration").isJsonNull()){
					score = Math.abs(entry.get("duration").getAsLong() - durationSeconds);
				}
				if(score < bestScore){
					bestScore = score;
					best = entry;
				}
			}
			if(best != null && (durationSeconds <= 0 || bestScore <= 30L)){
				List<LyricLine> parsed = parseLrc(best.get("syncedLyrics").getAsString());
				return parsed;
			}
		}catch(Exception e){
			LOGGER.warn("lyrics search threw: {}", e.toString());
		}
		return List.of();
	}

	static String normalize(String value) {
		return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	static List<String> artistVariants(String artist) {
		List<String> variants = new ArrayList<>();
		String full = artist.trim();
		if(!full.isEmpty()) variants.add(full);
		String[] parts = full.split("(?i)\\s*(,|&|;|\\bx\\b|\\bi\\b|\\band\\b|\\bwith\\b|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bvs\\.?\\b)\\s*");
		for(String part : parts){
			String candidate = part.trim();
			if(candidate.length() >= 2 && !variants.contains(candidate)) variants.add(candidate);
		}
		return variants;
	}

	private static String clean(String value) {
		return value
			.replaceAll("(?i)\\s*[\\(\\[][^\\)\\]]*(remaster|remix|live|version|feat\\.?|ft\\.?|official|video|audio|lyrics)[^\\)\\]]*[\\)\\]]", "") // 💀
			.replaceAll("(?i)\\s*-\\s*(remaster|remix|live|official).*$", "")
			.trim();
	}

	public static List<LyricLine> parseLrc(String lrc) {
		List<LyricLine> lines = new ArrayList<>();
		for(String raw : lrc.split("\\R")){
			Matcher m = LRC_LINE.matcher(raw);
			if(m.find()){
				float t = Integer.parseInt(m.group(1)) * 60f + Float.parseFloat(m.group(2));
				String text = m.group(3).trim();
				if(!text.isEmpty()) lines.add(new LyricLine(t, text));
			}
		}
		lines.sort(Comparator.comparingDouble(LyricLine::time));
		return lines;
	}

	static List<LyricLine> parsePlain(String plain) {
		List<LyricLine> lines = new ArrayList<>();
		for(String raw : plain.split("\\R")) lines.add(new LyricLine(-1f, raw.trim()));
		return lines;
	}

	public static int currentIndex(List<LyricLine> lines, float position) {
		int idx = -1;
		for(int i = 0; i < lines.size(); i++){
			if(lines.get(i).time() <= position) idx = i;
			else break;
		}
		return idx;
	}
}
