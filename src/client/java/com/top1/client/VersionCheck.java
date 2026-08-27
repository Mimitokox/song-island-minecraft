package com.top1.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VersionCheck {
	public static final String VERSION = "1.0.0";
	private static final String VERSION_URL =
		"https://raw.githubusercontent.com/Mimitokox/song-island-minecraft/refs/heads/main/version.txt";
	private static final String DOWNOLAD_URELEL =
		"https://github.com/Mimitokox/song-island-minecraft/releases";

	private static final Logger LOGGER = LoggerFactory.getLogger("song-island");
	private static volatile String latest;
	private static volatile boolean outdated;

	public static String latest() {
		return latest;
	}

	public static boolean isOutdated() {
		return outdated;
	}

	public static void check() {
		Thread thread = new Thread(() -> {
			try{
				HttpClient client = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(6))
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.build();
				HttpRequest request = HttpRequest.newBuilder(URI.create(VERSION_URL))
					.timeout(Duration.ofSeconds(10))
					.header("User-Agent", "song-island/" + VERSION)
					.GET()
					.build();
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
				if(response.statusCode() != 200){
					return;
				}
				String remote = response.body().trim();
				if(remote.isEmpty()) return;
				latest = remote;
				outdated = !remote.equalsIgnoreCase(VERSION);
			}catch(Exception e){
				LOGGER.info("version check failed: {}", e.toString());
			}
		}, "song-island-version");
		thread.setDaemon(true);
		thread.start();
	}

	public static void notifyPlayer() {
		if(!outdated) return;
		Minecraft mc = Minecraft.getInstance();
		if(mc.player == null) return;

		MutableComponent title = Component.literal("Song Island")
			.withStyle(style -> style.withColor(0xFF8FC7E3).withBold(true));
		MutableComponent header = Component.literal(" - a new version is available")
			.withStyle(style -> style.withColor(ChatFormatting.WHITE));

		MutableComponent versions = Component.literal("You have ")
			.withStyle(style -> style.withColor(ChatFormatting.GRAY))
			.append(Component.literal("v" + VERSION)
				.withStyle(style -> style.withColor(0xFFE38F8F)))
			.append(Component.literal(", newest is ")
				.withStyle(style -> style.withColor(ChatFormatting.GRAY)))
			.append(Component.literal("v" + latest)
				.withStyle(style -> style.withColor(0xFF9BE38F)));

		Style link = Style.EMPTY
			.withColor(0xFF8FC7E3)
			.withUnderlined(true)
			.withClickEvent(new ClickEvent.OpenUrl(URI.create(DOWNOLAD_URELEL)));
		MutableComponent download = Component.literal("Click here to download the update")
			.withStyle(link);

		mc.player.displayClientMessage(Component.empty(), false);
		mc.player.displayClientMessage(title.append(header), false);
		mc.player.displayClientMessage(versions, false);
		mc.player.displayClientMessage(download, false);
		mc.player.displayClientMessage(Component.empty(), false);
	}

	private VersionCheck() {
	}
}
