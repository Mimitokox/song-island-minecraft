package com.top1.client.island.font;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public final class MsdfFont {
	private static final Gson GSON = new Gson();
	private final String name;
	private final AbstractTexture texture;
	private final FontData.AtlasData atlas;
	private final FontData.MetricsData metrics;
	private final Map<Integer, MsdfGlyph> glyphs;
	private final Map<Integer, Map<Integer, Float>> kernings;
	private final Map<Long, Float> widthCache = new HashMap<>();

	private MsdfFont(String name, AbstractTexture texture, FontData.AtlasData atlas, FontData.MetricsData metrics,
		Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
		this.name = name;
		this.texture = texture;
		this.atlas = atlas;
		this.metrics = metrics;
		this.glyphs = glyphs;
		this.kernings = kernings;
	}

	public static MsdfFont of(String name) {
		Identifier dataId = Identifier.fromNamespaceAndPath("song-island", "fonts/msdf/" + name + ".json");
		Identifier atlasId = Identifier.fromNamespaceAndPath("song-island", "fonts/msdf/" + name + ".png");
		FontData data;
		try(InputStream in = Minecraft.getInstance().getResourceManager().open(dataId)){
			data = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), FontData.class);
		}catch(Exception e){
			throw new RuntimeException("Failed to read font data: " + dataId, e);
		}
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(atlasId);
		float aWidth = data.atlas().width();
		float aHeight = data.atlas().height();
		Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream()
			.collect(Collectors.toMap(FontData.GlyphData::unicode, g -> new MsdfGlyph(g, aWidth, aHeight)));
		Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
		data.kernings().forEach(k -> kernings
			.computeIfAbsent(k.leftChar(), c -> new HashMap<>())
			.put(k.rightChar(), k.advance()));
		return new MsdfFont(name, texture, data.atlas(), data.metrics(), glyphs, kernings);
	}

	public AbstractTexture getTexture() {
		return this.texture;
	}

	public FontData.AtlasData getAtlas() {
		return this.atlas;
	}

	public FontData.MetricsData getMetrics() {
		return this.metrics;
	}


	public java.util.List<float[]> buildGlyphs(String text, float size, float thickness, float x, float y) {
		return buildGlyphs(text, size, thickness, x, y, null);
	}

	public java.util.List<float[]> buildGlyphs(String text, float size, float thickness, float x, float y,
		java.util.List<Integer> charIndices) {
		java.util.List<float[]> quads = new java.util.ArrayList<>();
		int prevChar = -1;
		boolean skipNext = false;
		for(int i = 0; i < text.length(); i++){
			char c = text.charAt(i);
			if(skipNext){
				skipNext = false;
				continue;
			}
			if(c == 167){
				skipNext = true;
				continue;
			}
			MsdfGlyph glyph = this.glyphs.get((int) c);
			if(glyph == null) continue;
			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if(kerning != null) x += kerning.getOrDefault((int) c, 0.0F) * size;
			quads.add(glyph.quad(size, x, y));
			if(charIndices != null) charIndices.add(i);
			x += glyph.getWidth(size) + thickness;
			prevChar = (int) c;
		}
		return quads;
	}

	public float getWidth(String text, float size) {
		int prevChar = -1;
		float width = 0.0F;
		boolean skipNext = false;
		for(int i = 0; i < text.length(); i++){
			char c = text.charAt(i);
			if(skipNext){
				skipNext = false;
				continue;
			}
			if(c == 167){
				skipNext = true;
				continue;
			}
			MsdfGlyph glyph = this.glyphs.get((int) c);
			if(glyph == null) continue;
			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if(kerning != null) width += kerning.getOrDefault((int) c, 0.0F) * size;
			width += glyph.getWidth(size) + 0.25F;
			prevChar = (int) c;
		}
		return width;
	}

	public static int applyAlpha(int argb, float factor) {
		int a = (int) ((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, factor)));
		return (a << 24) | (argb & 0xFFFFFF);
	}
}
