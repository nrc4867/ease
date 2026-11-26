// Corner positions and cell vertices. The kernel declares `axes`, every corner coordinate along x,
// then y, then z, and, for sn_vertex, `samples`.

// Corner `index`'s coordinate along `axis`, from the table the CPU computed, so both sides place
// corners on the same bits.
fn sn_coordinate(axis: u32, index: u32) -> f32 {
  var start = 0u;
  if (axis > 0u) {
    start += grid.cellsX + 1u;
  }
  if (axis > 1u) {
    start += grid.cellsY + 1u;
  }
  return axes[start + index];
}

fn sn_position(c: vec3u) -> vec3f {
  return vec3f(sn_coordinate(0u, c.x), sn_coordinate(1u, c.y), sn_coordinate(2u, c.z));
}

fn sn_crossing(axis: u32, start: u32, end: u32, t: f32) -> f32 {
  let a = sn_coordinate(axis, start);
  let b = sn_coordinate(axis, end);
  return a + (b - a) * t;
}

// The cell's vertex: the average of where its edges cross the surface.
fn sn_vertex(c: vec3u) -> vec3f {
  var edge_from = array<u32, 12>(0u, 2u, 4u, 6u, 0u, 1u, 4u, 5u, 0u, 1u, 2u, 3u);
  var edge_to = array<u32, 12>(1u, 3u, 5u, 7u, 2u, 3u, 6u, 7u, 4u, 5u, 6u, 7u);
  var sum_x = 0.0;
  var sum_y = 0.0;
  var sum_z = 0.0;
  var crossings = 0u;
  for (var edge = 0u; edge < 12u; edge++) {
    let ca = sn_corner(c, edge_from[edge]);
    let cb = sn_corner(c, edge_to[edge]);
    let a = samples[sn_index(ca)];
    let b = samples[sn_index(cb)];
    if ((a < 0.0) == (b < 0.0)) {
      continue;
    }
    let t = a / (a - b);
    sum_x += sn_crossing(0u, ca.x, cb.x, t);
    sum_y += sn_crossing(1u, ca.y, cb.y, t);
    sum_z += sn_crossing(2u, ca.z, cb.z, t);
    crossings++;
  }
  let count = f32(crossings);
  return vec3f(sum_x / count, sum_y / count, sum_z / count);
}
