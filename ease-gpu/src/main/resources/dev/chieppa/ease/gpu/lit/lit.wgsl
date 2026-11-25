// The lit material template: metal and roughness, one LitMaterial per draw.

@group(2) @binding(0) var<storage, read> materials: array<LitMaterial>;

fn surface(input: SurfaceInput) -> Surface {
  let material = materials[input.draw];
  var out: Surface;
  out.albedo = material.baseColor;
  out.normal = input.normal;
  out.roughness = material.roughness;
  out.metallic = material.metallic;
  out.emissive = material.emissive;
  return out;
}
