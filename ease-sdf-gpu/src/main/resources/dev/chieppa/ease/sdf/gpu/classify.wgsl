// Pass 2: per corner, whether its cell gets a vertex and how many quads its edges make.

@group(0) @binding(0) var<uniform> grid: SurfaceNetsGrid;
@group(0) @binding(1) var<storage, read> samples: array<f32>;
@group(0) @binding(2) var<storage, read_write> counts: array<vec2u>;

@compute @workgroup_size(SN_WORKGROUP)
fn classify_main(@builtin(global_invocation_id) id: vec3u) {
  let index = id.x;
  if (index >= grid.points) {
    return;
  }
  let c = sn_coords(index);
  var quads = 0u;
  for (var axis = 0u; axis < 3u; axis++) {
    if (sn_has_quad(c, axis)) {
      quads++;
    }
  }
  counts[index] = vec2u(select(0u, 1u, sn_has_vertex(c)), quads);
}
