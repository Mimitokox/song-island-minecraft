package com.top1.client.island.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.top1.client.island.font.Font;
import com.top1.client.island.font.MsdfFont;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IslandRender {

	private static final int VERTEX_SIZE = 24;
	private static final int PARAM_FLOATS = 12;
	private static final float SMOOTHNESS = 0.5F;

	private static final List<Cmd> QUEUE = new ArrayList<>();
	private static final List<Cmd> WORLD_QUEUE = new ArrayList<>();
	private static Matrix4f worldModelView;
	private static GpuBufferSlice worldProjection;
	private static final List<float[]> SCISSORS = new ArrayList<>();

	private static GpuBuffer vertexBuffer;
	private static int vertexBufferQuads;
	private static GpuBuffer uniformBuffer;
	private static int uniformSlots;
	private static int uniformAlign;
	private static GpuTexture snapshot;
	private static GpuTextureView snapshotView;
	private static GpuSampler snapshotSampler;
	private static boolean needsSnapshot;
	private static GpuBuffer projectionBuffer;
	private static float projectionWidth = -1.0F;
	private static float projectionHeight = -1.0F;
	private static final Logger LOGGER = LoggerFactory.getLogger("song-island");
	private static boolean loggedError;

	private record Cmd(RenderPipeline pipeline, GpuTextureView texture, GpuSampler sampler,
		float[] verts, int[] colors, float[] params, float[] scissor) {
	}

	public static void submitWorld(AbstractTexture texture, float[] verts, int[] colors,
		Matrix4f modelView, GpuBufferSlice projection) {
		worldModelView = modelView;
		worldProjection = projection;
		WORLD_QUEUE.add(new Cmd(IslandPipelines.MSDF, texture.getTextureView(), texture.getSampler(),
			verts, colors, params(texture == null ? 10.0F : msdfRange(), 0.05F, 0.5F, 0.0F,
				0, 0, 0, 0, 0, 0, 0, 0), null));
	}

	private static float msdfRange() {
		return com.top1.client.island.font.Fonts.MEDIUM != null
			? com.top1.client.island.font.Fonts.MEDIUM.getAtlas().range() : 10.0F;
	}

	public static void pushScissor(float x, float y, float width, float height) {
		SCISSORS.add(new float[] { x, y, width, height });
	}

	public static void popScissor() {
		if(!SCISSORS.isEmpty()) SCISSORS.remove(SCISSORS.size() - 1);
	}

	private static float[] scissor() {
		return SCISSORS.isEmpty() ? null : SCISSORS.get(SCISSORS.size() - 1);
	}

	private static void submit(RenderPipeline pipeline, GpuTextureView tex, GpuSampler sampler,
		float[] verts, int[] colors, float[] params) {
		QUEUE.add(new Cmd(pipeline, tex, sampler, verts, colors, params, scissor()));
	}

	private static int[] fill(int color, int vertices) {
		int[] colors = new int[vertices];
		java.util.Arrays.fill(colors, color);
		return colors;
	}

	public static void drawRoundedRect(float x, float y, float width, float height,
		float tl, float bl, float tr, float br, int argb) {
		float hp = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
		float vp = SMOOTHNESS / 2.0F + SMOOTHNESS;
		submit(IslandPipelines.SHAPE, null, null,
			quad(x - hp / 2.0F, y - vp / 2.0F, width + hp, height + vp), fill(argb, 4),
			params(width, height, SMOOTHNESS, 1.0F, tl, bl, tr, br, 0, 0, 0, 0));
	}

	public static void drawSquircle(float x, float y, float width, float height, float squirt,
		float tl, float bl, float tr, float br, int argb) {
		float hp = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
		float vp = SMOOTHNESS / 2.0F + SMOOTHNESS;
		submit(IslandPipelines.SHAPE, null, null,
			quad(x - hp / 2.0F, y - vp / 2.0F, width + hp, height + vp), fill(argb, 4),
			params(width, height, SMOOTHNESS, squirt,
				tl * squirt / 2.0F, bl * squirt / 2.0F, tr * squirt / 2.0F, br * squirt / 2.0F, 0, 0, 0, 0));
	}

	public static void drawTexture(Identifier id, float x, float y, float width, float height, int argb) {
		AbstractTexture tex = texture(id);
		if(tex == null) return;
		submit(IslandPipelines.TEXTURE, tex.getTextureView(), tex.getSampler(),
			quad(x, y, width, height), fill(argb, 4),
			params(width, height, SMOOTHNESS, 1.0F, 0, 0, 0, 0, 0, 0, 0, 0));
	}

	public static void drawRoundedTexture(Identifier id, float x, float y, float width, float height,
		float tl, float bl, float tr, float br, int argb) {
		AbstractTexture tex = texture(id);
		if(tex == null) return;
		float hp = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
		float vp = SMOOTHNESS / 2.0F + SMOOTHNESS;
		submit(IslandPipelines.TEXTURE, tex.getTextureView(), tex.getSampler(),
			quad(x - hp / 2.0F, y - vp / 2.0F, width + hp, height + vp), fill(argb, 4),
			params(width, height, SMOOTHNESS, 1.0F, tl, bl, tr, br, 0, 0, 0, 0));
	}

	public static void drawBlur(float x, float y, float width, float height, float blurRadius, float squirt,
		float tl, float bl, float tr, float br, int argb) {
		blurRadius /= 22.5F;
		if(blurRadius <= 0.0F) return;
		needsSnapshot = true;
		Minecraft mc = Minecraft.getInstance();
		float sw = mc.getWindow().getGuiScaledWidth();
		float sh = mc.getWindow().getGuiScaledHeight();
		float u = x / sw;
		float v = (sh - y - height) / sh;
		float tw = width / sw;
		float th = height / sh;
		float[] verts = new float[] {
			x, y, 0, u, v + th,
			x, y + height, 0, u, v,
			x + width, y + height, 0, u + tw, v,
			x + width, y, 0, u + tw, v + th
		};
		submit(IslandPipelines.BLUR, null, null, verts, fill(argb, 4),
			params(width, height, 0.1F, squirt,
				tl * squirt / 2.0F, bl * squirt / 2.0F, tr * squirt / 2.0F, br * squirt / 2.0F,
				blurRadius, 0, 0, 0));
	}

	public static void drawText(Font font, String text, float x, float y, int argb) {
		text(font, text, x, y, argb, null, false, 0.0F, 0.0F, 0.0F, 0.0F);
	}

	public static void drawFadeoutText(Font font, String text, float x, float y, int argb,
		float fadeStart, float fadeEnd, float maxWidth) {
		drawWindowedText(font, text, x, y, argb, x, x + maxWidth, 7.0F, 0.0F);
	}

	public static void drawWindowedText(Font font, String text, float x, float y, int argb,
		float windowStart, float windowEnd, float fadeRight, float fadeLeft) {
		text(font, text, x, y, argb, null, true, windowStart, windowEnd, fadeRight, fadeLeft);
	}

	public static void drawKaraokeText(Font font, String text, float x, float y, int[] charColors,
		float windowStart, float windowEnd, float fadeRight, float fadeLeft) {
		text(font, text, x, y, 0xFFFFFFFF, charColors, true, windowStart, windowEnd, fadeRight, fadeLeft);
	}

	public static void drawCenteredText(Font font, String text, float x, float y, int argb) {
		text(font, text, x - font.width(text) / 2.0F, y, argb, null, false, 0.0F, 0.0F, 0.0F, 0.0F);
	}

	private static void text(Font font, String text, float x, float y, int argb, int[] charColors,
		boolean windowed, float windowStart, float windowEnd, float fadeRight, float fadeLeft) {
		if(text == null || text.isEmpty()) return;
		MsdfFont msdf = font.getFont();
		float size = font.getSize();
		float thickness = 0.05F;
		List<Integer> charIndices = charColors != null ? new ArrayList<>() : null;
		List<float[]> glyphs = msdf.buildGlyphs(text, size, thickness * 0.5F * size,
			x - 0.75F, y + size * 0.7F, charIndices);
		if(glyphs.isEmpty()) return;
		float[] verts = new float[glyphs.size() * 20];
		int o = 0;
		for(float[] q : glyphs){
			System.arraycopy(q, 0, verts, o, 20);
			o += 20;
		}
		int[] colors = new int[glyphs.size() * 4];
		for(int i = 0; i < glyphs.size(); i++){
			int color = argb;
			if(charColors != null){
				int index = charIndices.get(i);
				if(index >= 0 && index < charColors.length) color = charColors[index];
			}
			colors[i * 4] = color;
			colors[i * 4 + 1] = color;
			colors[i * 4 + 2] = color;
			colors[i * 4 + 3] = color;
		}
		AbstractTexture tex = msdf.getTexture();
		submit(IslandPipelines.MSDF, tex.getTextureView(), tex.getSampler(), verts, colors,
			params(msdf.getAtlas().range(), thickness, 0.5F, 0.0F,
				windowStart, windowEnd, fadeRight, fadeLeft,
				0, 0, 0, windowed ? 1.0F : 0.0F));
	}

	private static AbstractTexture texture(Identifier id) {
		try{
			return Minecraft.getInstance().getTextureManager().getTexture(id);
		}catch(Exception e){
			return null;
		}
	}

	private static float[] params(float a, float b, float c, float d,
		float r0, float r1, float r2, float r3,
		float e0, float e1, float e2, float e3) {
		return new float[] { a, b, c, d, r0, r1, r2, r3, e0, e1, e2, e3 };
	}

	private static float[] quad(float x, float y, float w, float h) {
		return new float[] {
			x, y, 0, 0, 0,
			x, y + h, 0, 0, 1,
			x + w, y + h, 0, 1, 1,
			x + w, y, 0, 1, 0
		};
	}

	private static boolean loggedOnce;

	public static void flush() {
		if(!WORLD_QUEUE.isEmpty()){
			try{
				executeWorld();
			}catch(Throwable e){
				if(!loggedError){
					loggedError = true;
					LOGGER.error("world lyrics render failed", e);
				}
			} finally {
				WORLD_QUEUE.clear();
			}
		}
		if(!loggedOnce && !QUEUE.isEmpty()){
			loggedOnce = true;
		}
		if(QUEUE.isEmpty()){
			needsSnapshot = false;
			return;
		}
		try{
			execute();
		}catch(Throwable e){
			if(!loggedError){
				loggedError = true;
				LOGGER.error("island render failed", e);
			}
		} finally {
			QUEUE.clear();
			SCISSORS.clear();
			needsSnapshot = false;
		}
	}

	private static void executeWorld() {
		if(worldModelView == null || worldProjection == null) return;
		Minecraft mc = Minecraft.getInstance();
		GpuDevice device = RenderSystem.getDevice();
		CommandEncoder encoder = device.createCommandEncoder();
		RenderTarget target = mc.getMainRenderTarget();

		int quadCount = 0;
		for(Cmd cmd : WORLD_QUEUE) quadCount += cmd.verts().length / 20;
		if(quadCount == 0) return;
		ensureVertexBuffer(device, quadCount);
		writeVertices(encoder, WORLD_QUEUE, quadCount);

		ensureUniformBuffer(device, WORLD_QUEUE.size());
		writeUniforms(encoder, WORLD_QUEUE);

		RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer ibuf = indices.getBuffer(quadCount * 6);
		VertexFormat.IndexType itype = indices.type();

		GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(
			worldModelView, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

		drawWorldInto(encoder, target.getColorTextureView(), OptionalInt.empty(),
			transform, ibuf, itype);
	}

	private static void drawWorldInto(CommandEncoder encoder, GpuTextureView view, OptionalInt clear,
		GpuBufferSlice transform, GpuBuffer ibuf, VertexFormat.IndexType itype) {
		int baseVertex = 0;
		try(RenderPass pass = encoder.createRenderPass(() -> "song island world",
			view, clear, null, OptionalDouble.empty())) {
			pass.setUniform("Projection", worldProjection);
			pass.setUniform("DynamicTransforms", transform);
			pass.setVertexBuffer(0, vertexBuffer);
			pass.setIndexBuffer(ibuf, itype);
			for(int i = 0; i < WORLD_QUEUE.size(); i++){
				Cmd cmd = WORLD_QUEUE.get(i);
				int quads = cmd.verts().length / 20;
				pass.setPipeline(cmd.pipeline());
				pass.setUniform("IslandParams", uniformBuffer.slice(i * uniformAlign, uniformAlign));
				if(cmd.texture() != null) pass.bindTexture("Sampler0", cmd.texture(), cmd.sampler());
				pass.drawIndexed(baseVertex, 0, quads * 6, 1);
				baseVertex += quads * 4;
			}
		}
	}

	private static void writeVertices(CommandEncoder encoder, List<Cmd> queue, int quadCount) {
		ByteBuffer vbuf = MemoryUtil.memAlloc(quadCount * 4 * VERTEX_SIZE);
		vbuf.order(ByteOrder.nativeOrder());
		for(Cmd cmd : queue){
			float[] v = cmd.verts();
			int[] colors = cmd.colors();
			for(int i = 0, vertex = 0; i < v.length; i += 5, vertex++){
				vbuf.putFloat(v[i]).putFloat(v[i + 1]).putFloat(v[i + 2]);
				vbuf.putFloat(v[i + 3]).putFloat(v[i + 4]);
				int c = colors[vertex];
				vbuf.put((byte) (c >> 16 & 0xFF)).put((byte) (c >> 8 & 0xFF))
					.put((byte) (c & 0xFF)).put((byte) (c >>> 24 & 0xFF));
			}
		}
		vbuf.flip();
		encoder.writeToBuffer(vertexBuffer.slice(0, vbuf.remaining()), vbuf);
		MemoryUtil.memFree(vbuf);
	}

	private static void writeUniforms(CommandEncoder encoder, List<Cmd> queue) {
		ByteBuffer pbuf = MemoryUtil.memAlloc(uniformAlign);
		pbuf.order(ByteOrder.nativeOrder());
		for(int i = 0; i < queue.size(); i++){
			pbuf.clear();
			for(float f : queue.get(i).params()) pbuf.putFloat(f);
			while(pbuf.hasRemaining()) pbuf.putFloat(0.0F);
			pbuf.flip();
			encoder.writeToBuffer(uniformBuffer.slice(i * uniformAlign, uniformAlign), pbuf);
		}
		MemoryUtil.memFree(pbuf);
	}

	private static void execute() {
		Minecraft mc = Minecraft.getInstance();
		GpuDevice device = RenderSystem.getDevice();
		CommandEncoder encoder = device.createCommandEncoder();
		RenderTarget target = mc.getMainRenderTarget();

		if(needsSnapshot) updateSnapshot(device, encoder, target);

		int quadCount = 0;
		for(Cmd cmd : QUEUE) quadCount += cmd.verts().length / 20;
		ensureVertexBuffer(device, quadCount);
		ensureUniformBuffer(device, QUEUE.size());
		writeVertices(encoder, QUEUE, quadCount);
		writeUniforms(encoder, QUEUE);

		RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer ibuf = indices.getBuffer(quadCount * 6);
		VertexFormat.IndexType itype = indices.type();

		GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(
			new Matrix4f(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

		float guiWidth = mc.getWindow().getGuiScaledWidth();
		float guiHeight = mc.getWindow().getGuiScaledHeight();
		GpuBufferSlice proj = projectionSlice(guiWidth, guiHeight);

		int guiScale = mc.getWindow().getGuiScale();
		int screenHeight = mc.getWindow().getHeight();
		int baseVertex = 0;
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(proj, ProjectionType.ORTHOGRAPHIC);
		try(RenderPass pass = encoder.createRenderPass(() -> "song island",
			target.getColorTextureView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transform);
			pass.setVertexBuffer(0, vertexBuffer);
			pass.setIndexBuffer(ibuf, itype);
			for(int i = 0; i < QUEUE.size(); i++){
				Cmd cmd = QUEUE.get(i);
				int quads = cmd.verts().length / 20;
				pass.setPipeline(cmd.pipeline());
				pass.setUniform("IslandParams", uniformBuffer.slice(i * uniformAlign, uniformAlign));
				if(cmd.pipeline() == IslandPipelines.BLUR){
					pass.bindTexture("Sampler0", snapshotView, snapshotSampler);
				}else if(cmd.texture() != null){
					pass.bindTexture("Sampler0", cmd.texture(), cmd.sampler());
				}
				float[] sc = cmd.scissor();
				if(sc != null){
					int sx = (int) (sc[0] * guiScale);
					int sw = (int) (sc[2] * guiScale);
					int sh = (int) (sc[3] * guiScale);
					int sy = screenHeight - (int) ((sc[1] + sc[3]) * guiScale);
					pass.enableScissor(sx, sy, Math.max(0, sw), Math.max(0, sh));
				}else{
					pass.disableScissor();
				}
				pass.drawIndexed(baseVertex, 0, quads * 6, 1);
				baseVertex += quads * 4;
			}
		} finally {
			RenderSystem.restoreProjectionMatrix();
		}
	}

	private static GpuBufferSlice projectionSlice(float width, float height) {
		GpuDevice device = RenderSystem.getDevice();
		if(projectionBuffer == null){
			projectionBuffer = device.createBuffer(() -> "song island projection",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
			projectionWidth = -1.0F;
		}
		if(width != projectionWidth || height != projectionHeight){
			projectionWidth = width;
			projectionHeight = height;
			Matrix4f ortho = new Matrix4f().setOrtho(0.0F, width, height, 0.0F, -1000.0F, 1000.0F);
			try(MemoryStack stack = MemoryStack.stackPush()){
				ByteBuffer data = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
					.putMat4f(ortho).get();
				device.createCommandEncoder().writeToBuffer(projectionBuffer.slice(), data);
			}
		}
		return projectionBuffer.slice();
	}

	private static void updateSnapshot(GpuDevice device, CommandEncoder encoder, RenderTarget target) {
		int w = target.width;
		int h = target.height;
		if(snapshot == null || snapshot.getWidth(0) != w || snapshot.getHeight(0) != h){
			if(snapshotView != null) snapshotView.close();
			if(snapshot != null) snapshot.close();
			snapshot = device.createTexture(() -> "song island snapshot",
				GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
				TextureFormat.RGBA8, w, h, 1, 1);
			snapshotView = device.createTextureView(snapshot);
		}
		if(snapshotSampler == null){
			snapshotSampler = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
				FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
		}
		encoder.copyTextureToTexture(target.getColorTexture(), snapshot, 0, 0, 0, 0, 0, w, h);
	}

	private static void ensureVertexBuffer(GpuDevice device, int quads) {
		if(vertexBuffer != null && vertexBufferQuads >= quads) return;
		if(vertexBuffer != null) vertexBuffer.close();
		vertexBufferQuads = Math.max(256, quads * 2);
		vertexBuffer = device.createBuffer(() -> "song island vertices",
			GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
			(long) vertexBufferQuads * 4 * VERTEX_SIZE);
	}

	private static void ensureUniformBuffer(GpuDevice device, int slots) {
		if(uniformAlign == 0){
			uniformAlign = Math.max(device.getUniformOffsetAlignment(), PARAM_FLOATS * 4);
		}
		if(uniformBuffer != null && uniformSlots >= slots) return;
		if(uniformBuffer != null) uniformBuffer.close();
		uniformSlots = Math.max(128, slots * 2);
		uniformBuffer = device.createBuffer(() -> "song island params",
			GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
			(long) uniformSlots * uniformAlign);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("song-island", path);
	}

	private IslandRender() {
	}
}
