package com.top1.client.island;

import com.top1.client.music.LyricsFetcher;
import java.util.List;

public final class Karaoke {
	public static final int SUNG = 0xFFFFFFFF;
	public static final int UPCOMING = 0x66FFFFFF;

	public record Word(int start, int end) {
	}

	// nie nawidze kurwa matmy
	public static float lineProgress(List<LyricsFetcher.LyricLine> lines, int index, float position) {
		if(index < 0 || index >= lines.size()) return 0.0F;
		float start = lines.get(index).time();
		if(start < 0.0F) return 1.0F;
		float end = index + 1 < lines.size() ? lines.get(index + 1).time() : start + 4.0F;
		if(end <= start) return 1.0F;
		return Math.max(0.0F, Math.min(1.0F, (position - start) / (end - start)));
	}

	public static float sungChars(String text, float progress) {
		return text.length() * progress;
	}

	public static Word wordAt(String text, float charIndex) {
		if(text.isEmpty()) return new Word(0, 0);
		int i = Math.max(0, Math.min(text.length() - 1, Math.round(charIndex)));
		if(text.charAt(i) == ' '){
			while(i < text.length() && text.charAt(i) == ' ') i++;
			if(i >= text.length()) i = text.length() - 1;
		}
		int start = i;
		while(start > 0 && text.charAt(start - 1) != ' ') start--;
		int end = i;
		while(end < text.length() && text.charAt(end) != ' ') end++;
		return new Word(start, end);
	}

	public static java.util.List<Word> words(String text) {
		java.util.List<Word> words = new java.util.ArrayList<>();
		int i = 0;
		while(i < text.length()){
			while(i < text.length() && text.charAt(i) == ' ') i++;
			if(i >= text.length()) break;
			int start = i;
			while(i < text.length() && text.charAt(i) != ' ') i++;
			words.add(new Word(start, i));
		}
		return words;
	}

	public static int activeWord(java.util.List<Word> words, float sung) {
		for(int i = 0; i < words.size(); i++){
			if(sung < words.get(i).end()) return i;
		}
		return words.size() - 1;
	}

	public static int[] colors(String text, float sung, int sungColor, int upcomingColor) {
		int[] colors = new int[text.length()];
		float softness = 2.2F;
		for(int i = 0; i < text.length(); i++){
			float t = Math.max(0.0F, Math.min(1.0F, (sung - i) / softness + 0.5F));
			t = t * t * (3.0F - 2.0F * t);
			colors[i] = blend(upcomingColor, sungColor, t);
		}
		return colors;
	}

	public static int blend(int from, int to, float t) {
		int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static int withAlpha(int argb, float factor) {
		int a = (int) Math.max(0.0F, Math.min(255.0F, (argb >>> 24) * factor));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	private Karaoke() {
	}
}
