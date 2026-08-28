package com.top1.client.island;

import java.util.List;

public final class WordScales {
	private String line = "";
	private List<Karaoke.Word> words = List.of();
	private float[] scales = new float[0];
	private long lastFrame;
	private long lineChanged = -10000L;

	public List<Karaoke.Word> words(String text) {
		if(!text.equals(line)){
			boolean first = line.isEmpty();
			line = text;
			words = Karaoke.words(text);
			scales = new float[words.size()];
			if(!first) lineChanged = System.currentTimeMillis();
		}
		return words;
	}

	/** 0 tuz po zmianie linijki, 1 gdy wejscie sie skonczylo. */
	public float entry() {
		float t = (System.currentTimeMillis() - lineChanged) / 260.0F;
		if(t >= 1.0F) return 1.0F;
		if(t <= 0.0F) return 0.0F;
		return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
	}

	public void update(int active) {
		long now = System.nanoTime();
		float dt = lastFrame == 0L ? 0.016F : Math.min(0.12F, (now - lastFrame) / 1.0E9F);
		lastFrame = now;
		float rate = Math.min(1.0F, dt * 9.0F);
		for(int i = 0; i < scales.length; i++){
			float target = i == active ? 1.0F : 0.0F;
			scales[i] += (target - scales[i]) * rate;
		}
	}

	public float scale(int index) {
		return index >= 0 && index < scales.length ? scales[index] : 0.0F;
	}
}
