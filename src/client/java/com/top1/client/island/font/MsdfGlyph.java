package com.top1.client.island.font;

public final class MsdfGlyph {
	private final int code;
	private final float minU;
	private final float maxU;
	private final float minV;
	private final float maxV;
	private final float advance;
	private final float topPosition;
	private final float left;
	private final float width;
	private final float height;

	public MsdfGlyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
		this.code = data.unicode();
		this.advance = data.advance();
		FontData.BoundsData atlasBounds = data.atlasBounds();
		if(atlasBounds != null){
			this.minU = atlasBounds.left() / atlasWidth;
			this.maxU = atlasBounds.right() / atlasWidth;
			this.minV = 1.0F - atlasBounds.top() / atlasHeight;
			this.maxV = 1.0F - atlasBounds.bottom() / atlasHeight;
		}else{
			this.minU = this.maxU = this.minV = this.maxV = 0.0F;
		}
		FontData.BoundsData planeBounds = data.planeBounds();
		if(planeBounds != null){
			this.left = planeBounds.left();
			this.width = planeBounds.right() - planeBounds.left();
			this.height = planeBounds.top() - planeBounds.bottom();
			this.topPosition = planeBounds.top();
		}else{
			this.left = 0.0F;
			this.width = this.height = this.topPosition = 0.0F;
		}
	}

	public float[] quad(float size, float x, float y) {
		x += this.left * size;
		float gy = y - this.topPosition * size;
		float w = this.width * size;
		float h = this.height * size;
		return new float[] {
			x, gy, 0, this.minU, this.minV,
			x, gy + h, 0, this.minU, this.maxV,
			x + w, gy + h, 0, this.maxU, this.maxV,
			x + w, gy, 0, this.maxU, this.minV
		};
	}

	public float getWidth(float size) {
		return this.advance * size;
	}

	public int getCharCode() {
		return this.code;
	}
}
