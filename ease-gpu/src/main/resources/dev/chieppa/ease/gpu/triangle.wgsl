// One triangle, turned by an angle and kept undistorted at any aspect ratio.

struct Params {
  angle: f32,
  aspect: f32,
  padding: vec2f,
}

@group(0) @binding(0) var<uniform> params: Params;

struct Varyings {
  @builtin(position) position: vec4f,
  @location(0) color: vec3f,
}

@vertex
fn vertex_main(@builtin(vertex_index) index: u32) -> Varyings {
  var corners = array<vec2f, 3>(vec2f(0.0, 0.6), vec2f(-0.6, -0.4), vec2f(0.6, -0.4));
  var colors = array<vec3f, 3>(vec3f(0.95, 0.3, 0.2), vec3f(0.2, 0.8, 0.35), vec3f(0.2, 0.4, 0.95));
  let corner = corners[index];
  let c = cos(params.angle);
  let s = sin(params.angle);
  let turned = vec2f(corner.x * c - corner.y * s, corner.x * s + corner.y * c);
  var out: Varyings;
  out.position = vec4f(turned.x / params.aspect, turned.y, 0.0, 1.0);
  out.color = colors[index];
  return out;
}

@fragment
fn fragment_main(in: Varyings) -> @location(0) vec4f {
  return vec4f(in.color, 1.0);
}
