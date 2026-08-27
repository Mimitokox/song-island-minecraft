#version 330

layout(std140) uniform IslandParams {
    vec4 SizeSmooth;
    vec4 Radius;
    vec4 Extra;
};

float islandSdf(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float islandSdfSquircle(vec2 p, vec2 b, vec4 r, float n) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 qc = max(q, 0.0);
    float len = pow(pow(qc.x, n) + pow(qc.y, n), 1.0 / n);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}
