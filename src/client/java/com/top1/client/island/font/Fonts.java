package com.top1.client.island.font;

public final class Fonts {
	public static MsdfFont MEDIUM;
	public static MsdfFont REGULAR;

	public static void init() {
		MEDIUM = MsdfFont.of("medium");
		REGULAR = MsdfFont.of("regular");
		org.slf4j.LoggerFactory.getLogger("song-island").info("fonts loaded");
	}

	public static Font medium(float size) {
		return new Font(MEDIUM, size);
	}

	public static Font regular(float size) {
		return new Font(REGULAR, size);
	}

	private Fonts() {
	}
}
