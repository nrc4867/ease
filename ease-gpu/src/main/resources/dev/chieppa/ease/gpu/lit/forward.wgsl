// The engine's forward lighting around a material template's surface function. The structs it
// names come before it, written by ease-codegen from their @GpuStruct classes; the template's
// `surface` comes after it. Bind groups: 0 frame, 1 view, 2 material, 3 object.

@group(0) @binding(0) var<uniform> frame: FrameData;
@group(1) @binding(0) var<uniform> view: ViewData;
@group(3) @binding(0) var<storage, read> objects: array<ObjectData>;

const PI: f32 = 3.14159265;

// What a surface function is given about the pixel it shades.
struct SurfaceInput {
  world_position: vec3f,
  normal: vec3f,
  uv: vec2f,
  // The draw's index, which picks its parameters in the template's group 2 array.
  draw: u32,
}

// What a surface function returns: linear albedo, a unit world-space normal, roughness and
// metallic from 0 to 1, and emitted linear light.
struct Surface {
  albedo: vec3f,
  normal: vec3f,
  roughness: f32,
  metallic: f32,
  emissive: vec3f,
}

struct VertexIn {
  @location(0) position: vec3f,
  @location(1) normal: vec3f,
  @location(2) uv: vec2f,
}

struct Varyings {
  @builtin(position) clip: vec4f,
  @location(0) world_position: vec3f,
  @location(1) normal: vec3f,
  @location(2) uv: vec2f,
  @location(3) @interpolate(flat) draw: u32,
}

@vertex
fn vertex_main(in: VertexIn, @builtin(instance_index) draw: u32) -> Varyings {
  let placed = objects[draw];
  let world = placed.model * vec4f(in.position, 1.0);
  var out: Varyings;
  out.clip = view.viewProjection * world;
  out.world_position = world.xyz;
  out.normal = (placed.normalMatrix * vec4f(in.normal, 0.0)).xyz;
  out.uv = in.uv;
  out.draw = draw;
  return out;
}

// GGX normal distribution.
fn distribution(n_dot_h: f32, roughness: f32) -> f32 {
  let a = roughness * roughness;
  let a2 = a * a;
  let d = n_dot_h * n_dot_h * (a2 - 1.0) + 1.0;
  return a2 / (PI * d * d);
}

// Smith's visibility term with Schlick's approximation, folded with the 4 n.l n.v denominator.
fn visibility(n_dot_v: f32, n_dot_l: f32, roughness: f32) -> f32 {
  let k = (roughness + 1.0) * (roughness + 1.0) / 8.0;
  let v = n_dot_v / (n_dot_v * (1.0 - k) + k);
  let l = n_dot_l / (n_dot_l * (1.0 - k) + k);
  return v * l / max(4.0 * n_dot_v * n_dot_l, 0.0001);
}

// Light reflected toward the eye per unit of incoming radiance, metal and roughness workflow.
fn reflectance(surface: Surface, n: vec3f, v: vec3f, l: vec3f) -> vec3f {
  let n_dot_l = max(dot(n, l), 0.0);
  if (n_dot_l <= 0.0) {
    return vec3f(0.0);
  }
  let h = normalize(v + l);
  let n_dot_v = max(dot(n, v), 0.0001);
  let roughness = clamp(surface.roughness, 0.04, 1.0);
  let f0 = mix(vec3f(0.04), surface.albedo, surface.metallic);
  let fresnel = f0 + (1.0 - f0) * pow(1.0 - max(dot(h, v), 0.0), 5.0);
  let specular = fresnel * distribution(max(dot(n, h), 0.0), roughness)
    * visibility(n_dot_v, n_dot_l, roughness);
  let diffuse = (1.0 - fresnel) * (1.0 - surface.metallic) * surface.albedo / PI;
  return (diffuse + specular) * n_dot_l;
}

fn encode_srgb(linear: vec3f) -> vec3f {
  let c = clamp(linear, vec3f(0.0), vec3f(1.0));
  let low = c * 12.92;
  let high = 1.055 * pow(c, vec3f(1.0 / 2.4)) - 0.055;
  return select(high, low, c <= vec3f(0.0031308));
}

@fragment
fn fragment_main(in: Varyings) -> @location(0) vec4f {
  var input: SurfaceInput;
  input.world_position = in.world_position;
  input.normal = normalize(in.normal);
  input.uv = in.uv;
  input.draw = in.draw;
  let shaded = surface(input);
  let n = normalize(shaded.normal);
  let v = normalize(view.cameraPosition - in.world_position);
  let l = normalize(-frame.light.direction);
  let radiance = frame.light.color * frame.light.intensity;
  let lit = reflectance(shaded, n, v, l) * radiance + shaded.albedo * frame.ambient;
  return vec4f(encode_srgb(lit + shaded.emissive), 1.0);
}
