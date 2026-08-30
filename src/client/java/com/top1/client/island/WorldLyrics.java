package com.top1.client.island;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.top1.client.island.font.Font;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.font.MsdfFont;
import com.top1.client.island.render.IslandRender;
import com.top1.client.music.LyricsFetcher;
import com.top1.client.music.MusicTracker;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class WorldLyrics {
	private static final float DISTANCE = 5.0F;
	private static final float HEIGHT = 2.0F;
	private static final float SCALE = 0.035F;
	private static final long FADE = 420L;
	private static final long HOLD = 10000L;
	private static final long OUT = 450L;

	private static final java.util.Random RANDOM = new java.util.Random();
	private static Matrix4f modelView;
	private static GpuBufferSlice projection;
	private static Vec3 cameraPos;
	private static float cameraYaw;

	private static Placed current;
	private static Placed previous;

	private static final class Placed {
		final String text;
		final Vec3 anchor;
		final float rightX;
		final float rightZ;
		final long placedAt;
		long leftAt;

		Placed(String text, Vec3 anchor, float rightX, float rightZ) {
			this.text = text;
			this.anchor = anchor;
			this.rightX = rightX;
			this.rightZ = rightZ;
			this.placedAt = System.currentTimeMillis();
		}
	}
		// jebać matme 
	public static void capture() {
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		if(!camera.isInitialized()) return;
		modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
		projection = RenderSystem.getProjectionMatrixBuffer();
		cameraPos = camera.position();
		cameraYaw = camera.yRot();
	}

	public static void reset() {
		current = null;
		previous = null;
	}

	public static void render(MusicTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		if(modelView == null || projection == null || cameraPos == null) return;
		if(mc.player == null || mc.options.hideGui) return;

		List<LyricsFetcher.LyricLine> lines = tracker.getLyrics();
		float position = tracker.smoothPosition();
		int index = lines.isEmpty() ? -1 : LyricsFetcher.currentIndex(lines, position);
		String line = index >= 0 ? lines.get(index).text() : "";

		if(!line.isEmpty() && (current == null || !current.text.equals(line))){
			retire(current);
			current = place(mc, line);
		}else if(line.isEmpty() && current != null){
			retire(current);
			current = null;
		}else if(current != null && System.currentTimeMillis() - current.placedAt > HOLD){
			retire(current);
			current = null;
		}

		Font font = Fonts.medium(10.0F);
		if(previous != null){
			float gone = (System.currentTimeMillis() - previous.leftAt) / (float) OUT;
			float fade = 1.0F - Math.min(1.0F, Math.max(0.0F, gone));
			fade = fade * fade;
			if(fade <= 0.02F){
				previous = null;
			}else{
				draw(font, previous, tracker, lines, (int) (200.0F * fade), false, (1.0F - fade) * 0.9F);
			}
		}
		if(current != null){
			float appear = Math.min(1.0F, (System.currentTimeMillis() - current.placedAt) / (float) FADE);
			appear = 1.0F - (1.0F - appear) * (1.0F - appear) * (1.0F - appear);
			draw(font, current, tracker, lines, (int) (255.0F * appear), true, (1.0F - appear) * -0.5F);
		}
	}

	private static void retire(Placed placed) {
		if(placed == null) return;
		placed.leftAt = System.currentTimeMillis();
		previous = placed;
	}

	private static Placed place(Minecraft mc, String line) {
		boolean first = mc.options.getCameraType().isFirstPerson();
		float yaw = (float) Math.toRadians(first ? mc.player.getYRot() : cameraYaw);
		float lookX = -(float) Math.sin(yaw);
		float lookZ = (float) Math.cos(yaw);
		float rx = -(float) Math.cos(yaw);
		float rz = -(float) Math.sin(yaw);

		float dist = DISTANCE;
		float side = 0.0F;
		float up = HEIGHT;
		Vec3 from;

		if(first){
			from = mc.player.getEyePosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
		}else{
			from = cameraPos;
			dist = 4.0F + (float) (RANDOM.nextDouble() * 3.0);
			side = (float) ((RANDOM.nextDouble() - 0.5) * 4.5);
			up = (float) (RANDOM.nextDouble() * 2.4 - 0.6);
		}

		Vec3 anchor = new Vec3(
			from.x + lookX * dist + rx * side,
			from.y + up,
			from.z + lookZ * dist + rz * side);
		return new Placed(line, anchor, rx, rz);
	}

	private static void draw(Font font, Placed placed, MusicTracker tracker,
		List<LyricsFetcher.LyricLine> lines, int alpha, boolean karaoke, float lift) {
		if(alpha <= 3 || placed.text.isEmpty()) return;
		MsdfFont msdf = font.getFont();
		float size = font.getSize();
		String text = placed.text;
		float width = font.width(text);
		List<Integer> idx = new java.util.ArrayList<>();
		List<float[]> glyphs = msdf.buildGlyphs(text, size, 0.05F * 0.5F * size, -width / 2.0F, 0.0F, idx);
		if(glyphs.isEmpty()) return;

		int litColor = (Math.min(255, alpha) << 24) | 0xFFFFFF;
		int grayColor = (Math.min(255, (int) (alpha * 0.35F)) << 24) | 0xFFFFFF;
		int[] cols = null;
		if(karaoke && !lines.isEmpty() && lines.get(0).time() >= 0.0F){
			int index = LyricsFetcher.currentIndex(lines, tracker.smoothPosition());
			if(index >= 0){
				float progress = Karaoke.lineProgress(lines, index, tracker.smoothPosition());
				cols = Karaoke.colors(text, Karaoke.sungChars(text, progress), litColor, grayColor);
			}
		}

		// mirror the billboard when seen from behind so the line still reads left-to-right
		float flip = (placed.anchor.x - cameraPos.x) * placed.rightZ
			- (placed.anchor.z - cameraPos.z) * placed.rightX < 0.0 ? -1.0F : 1.0F;

		double originX = placed.anchor.x - cameraPos.x;
		double originY = placed.anchor.y - cameraPos.y + lift;
		double originZ = placed.anchor.z - cameraPos.z;

		float[] verts = new float[glyphs.size() * 20];
		int[] colors = new int[glyphs.size() * 4];
		int o = 0;
		for(int g = 0; g < glyphs.size(); g++){
			float[] quad = glyphs.get(g);
			int color = litColor;
			if(cols != null){
				int charIndex = idx.get(g);
				if(charIndex >= 0 && charIndex < cols.length) color = cols[charIndex];
			}
			for(int v = 0; v < 4; v++){
				float offsetX = quad[v * 5] * SCALE * flip;
				float offsetY = -(quad[v * 5 + 1] * SCALE);
				verts[o] = (float) (originX + placed.rightX * offsetX);
				verts[o + 1] = (float) (originY + offsetY);
				verts[o + 2] = (float) (originZ + placed.rightZ * offsetX);
				verts[o + 3] = quad[v * 5 + 3];
				verts[o + 4] = quad[v * 5 + 4];
				colors[o / 5] = color;
				o += 5;
			}
		}
		AbstractTexture texture = msdf.getTexture();
		IslandRender.submitWorld(texture, verts, colors, modelView, projection);
	}

	private WorldLyrics() {
	}
}
