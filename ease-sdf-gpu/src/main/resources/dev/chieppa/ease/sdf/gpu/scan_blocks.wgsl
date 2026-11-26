// Pass 3: each workgroup's exclusive prefix sums of the counts, and its group's totals.

@group(0) @binding(0) var<uniform> grid: SurfaceNetsGrid;
@group(0) @binding(1) var<storage, read> counts: array<vec2u>;
@group(0) @binding(2) var<storage, read_write> offsets: array<vec2u>;
@group(0) @binding(3) var<storage, read_write> block_sums: array<vec2u>;

var<workgroup> scratch: array<vec2u, SN_WORKGROUP>;

@compute @workgroup_size(SN_WORKGROUP)
fn scan_blocks_main(
  @builtin(global_invocation_id) id: vec3u,
  @builtin(local_invocation_id) lane: vec3u,
  @builtin(workgroup_id) group: vec3u,
) {
  let index = id.x;
  var own = vec2u(0u);
  if (index < grid.points) {
    own = counts[index];
  }
  scratch[lane.x] = own;
  for (var reach = 1u; reach < SN_WORKGROUP; reach = reach * 2u) {
    workgroupBarrier();
    var add = vec2u(0u);
    if (lane.x >= reach) {
      add = scratch[lane.x - reach];
    }
    workgroupBarrier();
    scratch[lane.x] = scratch[lane.x] + add;
  }
  workgroupBarrier();
  if (index < grid.points) {
    offsets[index] = scratch[lane.x] - own;
  }
  if (lane.x == SN_WORKGROUP - 1u) {
    block_sums[group.x] = scratch[lane.x];
  }
}
