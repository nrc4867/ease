// Pass 4: one workgroup turns the group totals into each group's exclusive offset and writes the
// grand totals: vertices, then quads.

@group(0) @binding(0) var<uniform> grid: SurfaceNetsGrid;
@group(0) @binding(1) var<storage, read_write> block_sums: array<vec2u>;
@group(0) @binding(2) var<storage, read_write> totals: vec2u;

var<workgroup> scratch: array<vec2u, 256>;

@compute @workgroup_size(256)
fn scan_totals_main(@builtin(local_invocation_id) lane: vec3u) {
  let per = (grid.blocks + SN_WORKGROUP - 1u) / SN_WORKGROUP;
  let start = lane.x * per;
  let end = min(start + per, grid.blocks);
  var own = vec2u(0u);
  for (var group = start; group < end; group++) {
    own = own + block_sums[group];
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
  var running = scratch[lane.x] - own;
  for (var group = start; group < end; group++) {
    let sum = block_sums[group];
    block_sums[group] = running;
    running = running + sum;
  }
  if (lane.x == SN_WORKGROUP - 1u) {
    totals = scratch[lane.x];
  }
}
