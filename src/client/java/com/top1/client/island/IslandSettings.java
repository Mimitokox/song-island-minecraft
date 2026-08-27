package com.top1.client.island;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IslandSettings {
	private static final Logger LOGGER = LoggerFactory.getLogger("song-island");

	public static boolean worldText;
	public static boolean hideBossbar;
	public static float posX = -1.0F;
	public static float posY = -1.0F;

	public static boolean isMoved() {
		return posX >= 0.0F && posY >= 0.0F;
	}

	public static void resetPosition() {
		posX = -1.0F;
		posY = -1.0F;
		save();
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("song-island").resolve("settings.json");
	}

	public static void load() {
		Path path = file();
		if(!Files.isRegularFile(path)) return;
		try{
			JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			if(json.has("worldText")) worldText = json.get("worldText").getAsBoolean();
			if(json.has("hideBossbar")) hideBossbar = json.get("hideBossbar").getAsBoolean();
			if(json.has("posX")) posX = json.get("posX").getAsFloat();
			if(json.has("posY")) posY = json.get("posY").getAsFloat();
		}catch(Exception e){
			LOGGER.warn("cannot read settings: {}", e.toString());
		}
	}

	public static void save() {
		try{
			Path path = file();
			Files.createDirectories(path.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("worldText", worldText);
			json.addProperty("hideBossbar", hideBossbar);
			json.addProperty("posX", posX);
			json.addProperty("posY", posY);
			Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
		}catch(IOException e){
			LOGGER.warn("cannot save settings: {}", e.toString());
		}
	}

	private IslandSettings() {
	}
}
