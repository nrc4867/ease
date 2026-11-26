// Pass 1: the field at every grid corner.

@group(0) @binding(0) var<uniform> grid: SurfaceNetsGrid;
@group(0) @binding(1) var<storage, read> sdf_nodes: array<SdfNode>;
@group(0) @binding(2) var<storage, read> axes: array<f32>;
@group(0) @binding(3) var<storage, read_write> samples: array<f32>;

@compute @workgroup_size(SN_WORKGROUP)
fn sample_main(@builtin(global_invocation_id) id: vec3u) {
  let index = id.x;
  if (index >= grid.points) {
    return;
  }
  samples[index] = sdf_field(sn_position(sn_coords(index)));
}
