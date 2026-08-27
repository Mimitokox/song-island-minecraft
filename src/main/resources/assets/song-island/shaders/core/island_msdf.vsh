#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 FragCoord;
out vec2 TexCoord;
out vec4 FragColor;
out vec2 GlobalPos;

const vec2[4] RECT_COORDS = vec2[](
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

void main() {
    FragCoord = RECT_COORDS[gl_VertexID % 4];
    TexCoord = UV0;
    FragColor = Color;
    GlobalPos = Position.xy;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
