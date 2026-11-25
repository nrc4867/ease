// The distance functions in WGSL. Each mirrors the Kotlin function of the same name in SdfMath
// operation for operation, so the CPU and the GPU agree to within rounding. Parameters come from
// an SdfNode: `values` for primitives and joins, the `inverse` rows for transforms.

fn sdf_length3(p: vec3f) -> f32 {
  return sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
}

fn sdf_length2(x: f32, y: f32) -> f32 {
  return sqrt(x * x + y * y);
}

fn sdf_box_extent(p: vec3f, half: vec3f) -> f32 {
  let qx = abs(p.x) - half.x;
  let qy = abs(p.y) - half.y;
  let qz = abs(p.z) - half.z;
  let outside = sdf_length3(vec3f(max(qx, 0.0), max(qy, 0.0), max(qz, 0.0)));
  let inside = min(max(qx, max(qy, qz)), 0.0);
  return outside + inside;
}

fn sdf_sphere(p: vec3f, v: vec4f) -> f32 {
  return sdf_length3(p) - v.x;
}

fn sdf_box(p: vec3f, v: vec4f) -> f32 {
  return sdf_box_extent(p, v.xyz);
}

fn sdf_rounded_box(p: vec3f, v: vec4f) -> f32 {
  return sdf_box_extent(p, vec3f(v.x - v.w, v.y - v.w, v.z - v.w)) - v.w;
}

fn sdf_capsule(p: vec3f, v: vec4f) -> f32 {
  let along = p.y - clamp(p.y, -v.x, v.x);
  return sdf_length3(vec3f(p.x, along, p.z)) - v.y;
}

fn sdf_cylinder(p: vec3f, v: vec4f) -> f32 {
  let dx = sdf_length2(p.x, p.z) - v.x;
  let dy = abs(p.y) - v.y;
  return min(max(dx, dy), 0.0) + sdf_length2(max(dx, 0.0), max(dy, 0.0));
}

fn sdf_plane(p: vec3f, v: vec4f) -> f32 {
  return p.x * v.x + p.y * v.y + p.z * v.z - v.w;
}

fn sdf_torus(p: vec3f, v: vec4f) -> f32 {
  return sdf_length2(sdf_length2(p.x, p.z) - v.x, p.y) - v.y;
}

fn sdf_transform(p: vec3f, node: SdfNode) -> vec3f {
  let r0 = node.inverse0;
  let r1 = node.inverse1;
  let r2 = node.inverse2;
  return vec3f(
    r0.x * p.x + r0.y * p.y + r0.z * p.z + r0.w,
    r1.x * p.x + r1.y * p.y + r1.z * p.z + r1.w,
    r2.x * p.x + r2.y * p.y + r2.z * p.z + r2.w,
  );
}

fn sdf_union(a: f32, b: f32, blend: f32) -> f32 {
  if (blend <= 0.0) {
    return min(a, b);
  }
  let h = clamp(0.5 + 0.5 * (b - a) / blend, 0.0, 1.0);
  return b + (a - b) * h - blend * h * (1.0 - h);
}

fn sdf_subtract(a: f32, b: f32, blend: f32) -> f32 {
  if (blend <= 0.0) {
    return max(a, -b);
  }
  let h = clamp(0.5 - 0.5 * (a + b) / blend, 0.0, 1.0);
  return a + (-b - a) * h + blend * h * (1.0 - h);
}

fn sdf_intersect(a: f32, b: f32, blend: f32) -> f32 {
  if (blend <= 0.0) {
    return max(a, b);
  }
  let h = clamp(0.5 - 0.5 * (b - a) / blend, 0.0, 1.0);
  return b + (a - b) * h + blend * h * (1.0 - h);
}
