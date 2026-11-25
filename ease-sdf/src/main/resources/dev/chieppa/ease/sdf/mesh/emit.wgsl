// Pass 5: each cell's vertex and each edge's quad, written at the offsets the scans found.

@group(0) @binding(0) var<uniform> grid: SurfaceNetsGrid;
@group(0) @binding(1) var<storage, read> sdf_nodes: array<SdfNode>;
@group(0) @binding(2) var<storage, read> samples: array<f32>;
@group(0) @binding(3) var<storage, read> counts: array<vec2u>;
@group(0) @binding(4) var<storage, read> offsets: array<vec2u>;
@group(0) @binding(5) var<storage, read> block_sums: array<vec2u>;
@group(0) @binding(6) var<storage, read> axes: array<f32>;
@group(0) @binding(7) var<storage, read_write> mesh_vertices: array<f32>;
@group(0) @binding(8) var<storage, read_write> mesh_indices: array<u32>;

fn sn_offset(index: u32) -> vec2u {
  return offsets[index] + block_sums[index / SN_WORKGROUP];
}

fn sn_vertex_of(c: vec3u) -> u32 {
  return sn_offset(sn_index(c)).x;
}

fn sn_normal(p: vec3f) -> vec3f {
  let reach = grid.cellSize * SN_NORMAL_STEP;
  let gx = sdf_field(vec3f(p.x + reach, p.y, p.z)) - sdf_field(vec3f(p.x - reach, p.y, p.z));
  let gy = sdf_field(vec3f(p.x, p.y + reach, p.z)) - sdf_field(vec3f(p.x, p.y - reach, p.z));
  let gz = sdf_field(vec3f(p.x, p.y, p.z + reach)) - sdf_field(vec3f(p.x, p.y, p.z - reach));
  let size = sqrt(gx * gx + gy * gy + gz * gz);
  if (size > 0.0) {
    return vec3f(gx / size, gy / size, gz / size);
  }
  return vec3f(0.0, 1.0, 0.0);
}

@compute @workgroup_size(256)
fn emit_main(@builtin(global_invocation_id) id: vec3u) {
  let index = id.x;
  if (index >= grid.points) {
    return;
  }
  let count = counts[index];
  if (count.x == 0u && count.y == 0u) {
    return;
  }
  let c = sn_coords(index);
  let offset = sn_offset(index);
  if (count.x == 1u) {
    let position = sn_vertex(c);
    mesh_write_vertex(offset.x, position, sn_normal(position));
  }
  var at = offset.y * 6u;
  let inward = sn_inside(index);
  for (var axis = 0u; axis < 3u; axis++) {
    if (!sn_has_quad(c, axis)) {
      continue;
    }
    let eu = sn_unit((axis + 1u) % 3u);
    let ev = sn_unit((axis + 2u) % 3u);
    let v00 = sn_vertex_of(c - eu - ev);
    let v10 = sn_vertex_of(c - ev);
    let v11 = offset.x;
    let v01 = sn_vertex_of(c - eu);
    // Inside below the edge faces the quad toward +axis, so (v00, v10, v11) runs counter-clockwise
    // seen from outside; otherwise the turn reverses.
    let second = select(v01, v10, inward);
    let last = select(v10, v01, inward);
    mesh_indices[at] = v00;
    mesh_indices[at + 1u] = second;
    mesh_indices[at + 2u] = v11;
    mesh_indices[at + 3u] = v00;
    mesh_indices[at + 4u] = v11;
    mesh_indices[at + 5u] = last;
    at += 6u;
  }
}
