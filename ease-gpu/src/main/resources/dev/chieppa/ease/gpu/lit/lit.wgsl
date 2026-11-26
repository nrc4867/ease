// The lit material template: metal and roughness, one LitMaterial per draw, with glTF's texture set.
// Each texture (group 1, declared by the engine) multiplies the material's own value.

@group(2) @binding(0) var<storage, read> materials: array<LitMaterial>;

// The normal map's x and y (z rebuilt, so two-channel maps work) turned to world space by a frame
// built from screen-space derivatives of position and texture coordinates. Texture coordinates run
// down the image, as glTF's do, so the map's y is flipped to point up it.
fn mapped_normal(n: vec3f, position: vec3f, uv: vec2f, stored: vec2f) -> vec3f {
  let xy = stored * 2.0 - 1.0;
  let z = sqrt(max(1.0 - dot(xy, xy), 0.0));
  let dp1 = dpdx(position);
  let dp2 = dpdy(position);
  let duv1 = dpdx(uv);
  let duv2 = dpdy(uv);
  let dp2_perp = cross(dp2, n);
  let dp1_perp = cross(n, dp1);
  let t = dp2_perp * duv1.x + dp1_perp * duv2.x;
  let b = dp2_perp * duv1.y + dp1_perp * duv2.y;
  let scale = max(dot(t, t), dot(b, b));
  let frame_scale = select(0.0, inverseSqrt(scale), scale > 1e-20);
  let bent = t * frame_scale * xy.x - b * frame_scale * xy.y + n * z;
  return select(n, normalize(bent), frame_scale > 0.0);
}

fn surface(input: SurfaceInput) -> Surface {
  let material = materials[input.draw];
  let base = textureSample(base_color_texture, base_color_sampler, input.uv);
  let normal = textureSample(normal_texture, normal_sampler, input.uv).xy;
  let metal_rough = textureSample(metallic_roughness_texture, metallic_roughness_sampler, input.uv);
  let emitted = textureSample(emissive_texture, emissive_sampler, input.uv).rgb;
  let occlusion = textureSample(occlusion_texture, occlusion_sampler, input.uv).r;
  var out = default_surface(input);
  out.albedo = material.baseColor * base.rgb;
  out.normal = mapped_normal(input.normal, input.world_position, input.uv, normal);
  out.roughness = material.roughness * metal_rough.g;
  out.metallic = material.metallic * metal_rough.b;
  out.emissive = material.emissive * emitted;
  out.occlusion = occlusion;
  return out;
}
