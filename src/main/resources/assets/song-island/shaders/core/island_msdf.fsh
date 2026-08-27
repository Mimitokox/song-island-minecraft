#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <song-island:island.glsl>

uniform sampler2D Sampler0;

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;
in vec2 GlobalPos;

out vec4 fragColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

void main() {
    float range = SizeSmooth.x;
    float thickness = SizeSmooth.y;
    float smoothness = SizeSmooth.z;

    float dist = median(texture(Sampler0, TexCoord).rgb) - 0.5 + thickness;
    vec2 h = vec2(dFdx(TexCoord.x), dFdy(TexCoord.y)) * vec2(textureSize(Sampler0, 0));
    float pixels = range * inversesqrt(h.x * h.x + h.y * h.y);

    float alpha = smoothstep(-smoothness, smoothness, dist * pixels);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (Extra.w > 0.5) {
        float windowStart = Radius.x;
        float windowEnd = Radius.y;
        float fadeRight = Radius.z;
        float fadeLeft = Radius.w;
        float left = fadeLeft > 0.01
            ? smoothstep(windowStart, windowStart + fadeLeft, GlobalPos.x)
            : step(windowStart, GlobalPos.x);
        float right = fadeRight > 0.01
            ? 1.0 - smoothstep(windowEnd - fadeRight, windowEnd, GlobalPos.x)
            : step(GlobalPos.x, windowEnd);
        color.a *= min(left, right);
    }

    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
