// Axis-aligned square quads, one instance each, read from a storage buffer by instance index.

struct View {
  scale: vec2f,
  offset: vec2f,
}

struct Quad {
  center: vec2f,
  size: f32,
  padding: f32,
  color: vec4f,
}

@group(0) @binding(0) var<uniform> view: View;
@group(0) @binding(1) var<storage, read> quads: array<Quad>;

struct Varyings {
  @builtin(position) position: vec4f,
  @location(0) color: vec3f,
}

@vertex
fn vertex_main(
  @builtin(vertex_index) vertex: u32,
  @builtin(instance_index) instance: u32,
) -> Varyings {
  var corners = array<vec2f, 6>(
    vec2f(-0.5, -0.5), vec2f(0.5, -0.5), vec2f(0.5, 0.5),
    vec2f(-0.5, -0.5), vec2f(0.5, 0.5), vec2f(-0.5, 0.5),
  );
  let quad = quads[instance];
  let world = quad.center + corners[vertex] * quad.size;
  var out: Varyings;
  out.position = vec4f(world * view.scale + view.offset, 0.0, 1.0);
  out.color = quad.color.rgb;
  return out;
}

@fragment
fn fragment_main(in: Varyings) -> @location(0) vec4f {
  return vec4f(in.color, 1.0);
}
