// Grid helpers every surface nets kernel shares. The kernel declares `grid`, a SurfaceNetsGrid.

const SN_WORKGROUP: u32 = 256u;

fn sn_cells() -> vec3u {
  return vec3u(grid.cellsX, grid.cellsY, grid.cellsZ);
}

fn sn_coords(index: u32) -> vec3u {
  let px = grid.cellsX + 1u;
  let py = grid.cellsY + 1u;
  return vec3u(index % px, (index / px) % py, index / (px * py));
}

fn sn_index(c: vec3u) -> u32 {
  let px = grid.cellsX + 1u;
  let py = grid.cellsY + 1u;
  return c.x + px * (c.y + py * c.z);
}

fn sn_unit(axis: u32) -> vec3u {
  var unit = vec3u(0u);
  unit[axis] = 1u;
  return unit;
}
