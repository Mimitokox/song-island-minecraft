package com.top1.client.island.font;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class FontData {
	private FontData.AtlasData atlas;
	private FontData.MetricsData metrics;
	private List<FontData.GlyphData> glyphs;
	@SerializedName("kerning")
	private List<FontData.KerningData> kernings;

	public FontData.AtlasData atlas() {
		return this.atlas;
	}

	public FontData.MetricsData metrics() {
		return this.metrics;
	}

	public List<FontData.GlyphData> glyphs() {
		return this.glyphs;
	}

	public List<FontData.KerningData> kernings() {
		return this.kernings;
	}

	public static final class AtlasData {
		@SerializedName("distanceRange")
		private float range;
		private float width;
		private float height;

		public float range() {
			return this.range;
		}

		public float width() {
			return this.width;
		}

		public float height() {
			return this.height;
		}
	}

	public static final class BoundsData {
		private float left;
		private float top;
		private float right;
		private float bottom;

		public float left() {
			return this.left;
		}

		public float top() {
			return this.top;
		}

		public float right() {
			return this.right;
		}

		public float bottom() {
			return this.bottom;
		}
	}

	public static final class GlyphData {
		private int unicode;
		private float advance;
		private FontData.BoundsData planeBounds;
		private FontData.BoundsData atlasBounds;

		public int unicode() {
			return this.unicode;
		}

		public float advance() {
			return this.advance;
		}

		public FontData.BoundsData planeBounds() {
			return this.planeBounds;
		}

		public FontData.BoundsData atlasBounds() {
			return this.atlasBounds;
		}
	}

	public static final class KerningData {
		@SerializedName("unicode1")
		private int leftChar;
		@SerializedName("unicode2")
		private int rightChar;
		private float advance;

		public int leftChar() {
			return this.leftChar;
		}

		public int rightChar() {
			return this.rightChar;
		}

		public float advance() {
			return this.advance;
		}
	}

	public static final class MetricsData {
		private float lineHeight;
		private float ascender;
		private float descender;

		public float lineHeight() {
			return this.lineHeight;
		}

		public float ascender() {
			return this.ascender;
		}

		public float descender() {
			return this.descender;
		}
	}
}
