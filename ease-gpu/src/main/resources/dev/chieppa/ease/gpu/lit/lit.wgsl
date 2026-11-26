// The lit material template: metal and roughness, one LitMaterial per draw, with glTF's texture set.
// Each texture (group 1, declared by the engine) multiplies the material's own value.

@group(2) @binding(0) var<storage, read> materials: array<LitMaterial>;

// The normal map's x and y (z rebuilt, so two-channel maps work) turned to world space by the
// vertex tangent frame, as glTF defines it: the tangent made square to the normal, and the
// bitangent cross(normal, tangent) times the tangent's w. A tangent along the normal leaves the
// normal as it is.
fn mapped_normal(n: vec3f, tangent: vec4f, stored: vec2f) -> vec3f {
  let xy = stored * 2.0 - 1.0;
  let z = sqrt(max(1.0 - dot(xy, xy), 0.0));
  let along = tangent.xyz - n * dot(n, tangent.xyz);
  let length2 = dot(along, along);
  let t = along * select(0.0, inverseSqrt(length2), length2 > 1e-20);
  let b = cross(n, t) * select(1.0, -1.0, tangent.w < 0.0);
  let bent = t * xy.x + b * xy.y + n * z;
  return select(n, normalize(bent), length2 > 1e-20);
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
  out.normal = mapped_normal(input.normal, input.tangent, normal);
  out.roughness = material.roughness * metal_rough.g;
  out.metallic = material.metallic * metal_rough.b;
  out.emissive = material.emissive * emitted;
  out.occlusion = occlusion;
  return out;
}
