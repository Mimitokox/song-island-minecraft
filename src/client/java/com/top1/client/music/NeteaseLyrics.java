package com.top1.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NeteaseLyrics {
	private static final Logger LOGGER = LoggerFactory.getLogger("song-island");
	private static final HttpClient HTTP = HttpClient.newHttpClient();

	public static List<LyricsFetcher.LyricLine> fetch(String artist, String title, long durationSeconds) {
		try{
			String query = URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
			JsonObject search = request("https://music.163.com/api/search/get?type=1&limit=10&s=" + query); // ejpiaj do muzyk (te chinskie)
			if(search == null || !search.has("result")) return List.of();
			JsonObject result = search.getAsJsonObject("result");
			if(!result.has("songs")) return List.of();
			JsonArray songs = result.getAsJsonArray("songs");

			long bestId = -1L;
			int bestScore = Integer.MIN_VALUE;
			String bestName = "";
			for(JsonElement element : songs){
				JsonObject song = element.getAsJsonObject();
				String name = song.get("name").getAsString();
				String performer = song.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
				long length = song.has("duration") ? song.get("duration").getAsLong() / 1000L : 0L;

				String normalName = normalize(name);
				String normalTitle = normalize(title);
				int score = 0;
				if(normalName.equals(normalTitle)) score += 8;
				else if(normalName.contains(normalTitle) || normalTitle.contains(normalName)) score += 4;
				else continue;
				if(performer.equalsIgnoreCase(artist)) score += 5;
				else if(performer.toLowerCase(Locale.ROOT).contains(artist.toLowerCase(Locale.ROOT))
					|| artist.toLowerCase(Locale.ROOT).contains(performer.toLowerCase(Locale.ROOT))) score += 3;
				if(durationSeconds > 0 && length > 0){
					long diff = Math.abs(length - durationSeconds);
					if(diff > 30) continue;
					if(diff <= 3) score += 4;
					else if(diff <= 10) score += 2;
				}
				if(score > bestScore){
					bestScore = score;
					bestId = song.get("id").getAsLong();
					bestName = name + " - " + performer;
				}
			}
			if(bestId < 0 || bestScore < 8){
				return List.of();
			}

			JsonObject lyric = request("https://music.163.com/api/song/lyric?id=" + bestId + "&lv=1&kv=1&tv=-1&yv=1");
			if(lyric == null || !lyric.has("lrc")) return List.of();
			JsonObject lrc = lyric.getAsJsonObject("lrc");
			if(!lrc.has("lyric") || lrc.get("lyric").isJsonNull()) return List.of();

			List<LyricsFetcher.LyricLine> lines = LyricsFetcher.parseLrc(clean(lrc.get("lyric").getAsString()));
			if(lines.size() < 4) return List.of();
			return lines;
		}catch(Exception e){
			LOGGER.warn("netease lookup failed: {}", e.toString());
			return List.of();
		}
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private static String clean(String lrc) {
		StringBuilder builder = new StringBuilder();
		for(String line : lrc.split("\\R")){
			String text = line.replaceAll("^\\[[^\\]]*]", "").trim();
			if(text.isEmpty()) continue;
			if(text.matches(".*[\\u4e00-\\u9fff].*[:：].*")) continue;
			builder.append(line).append('\n');
		}
		return builder.toString();
	}

	private static JsonObject request(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
			.header("Referer", "https://music.163.com")
			.GET()
			.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if(response.statusCode() != 200) return null;
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}

	private NeteaseLyrics() {
	}
}
