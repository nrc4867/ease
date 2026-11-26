// The cell and edge rules of surface nets, as CellEdges applies them on the CPU. The kernel
// declares `samples`, the field at every grid corner.

fn sn_inside(index: u32) -> bool {
  return samples[index] < 0.0;
}

fn sn_bit(corner: u32, axis: u32) -> u32 {
  return (corner >> axis) & 1u;
}

fn sn_corner(cell: vec3u, corner: u32) -> vec3u {
  return cell + vec3u(sn_bit(corner, 0u), sn_bit(corner, 1u), sn_bit(corner, 2u));
}

// Whether the edge from corner `c` along `axis` crosses the surface with four cells around it.
fn sn_has_quad(c: vec3u, axis: u32) -> bool {
  let n = sn_cells();
  let u = (axis + 1u) % 3u;
  let v = (axis + 2u) % 3u;
  if (c[axis] >= n[axis] || c[u] < 1u || c[u] >= n[u] || c[v] < 1u || c[v] >= n[v]) {
    return false;
  }
  return sn_inside(sn_index(c)) != sn_inside(sn_index(c + sn_unit(axis)));
}

// Whether the cell at `c` crosses the surface: corners on both sides.
fn sn_has_vertex(c: vec3u) -> bool {
  let n = sn_cells();
  if (c.x >= n.x || c.y >= n.y || c.z >= n.z) {
    return false;
  }
  var inside = 0u;
  for (var corner = 0u; corner < 8u; corner++) {
    if (sn_inside(sn_index(sn_corner(c, corner)))) {
      inside++;
    }
  }
  return inside != 0u && inside != 8u;
}
