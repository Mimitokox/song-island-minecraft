package com.top1.client.island.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class IslandPipelines {
	public static final RenderPipeline SHAPE = build("shape", "island_shape", false);
	public static final RenderPipeline TEXTURE = build("texture", "island_texture", true);
	public static final RenderPipeline BLUR = build("blur", "island_blur", true);
	public static final RenderPipeline MSDF = build("msdf", "island_msdf", true);
	private static RenderPipeline build(String name, String shader, boolean sampler) {
		RenderPipeline.Builder builder = RenderPipeline.builder()
			.withLocation(id(name))
			.withVertexShader(id("core/" + shader))
			.withFragmentShader(id("core/" + shader))
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
			.withUniform("Projection", UniformType.UNIFORM_BUFFER)
			.withUniform("IslandParams", UniformType.UNIFORM_BUFFER)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.withCull(false);
		if(sampler) builder.withSampler("Sampler0");
		return RenderPipelines.register(builder.build());
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("song-island", path);
	}

	private IslandPipelines() {
	}
}
