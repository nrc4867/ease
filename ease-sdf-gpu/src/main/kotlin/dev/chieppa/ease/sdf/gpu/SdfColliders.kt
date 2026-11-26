package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.physics.body.MeshCollider
import dev.chieppa.ease.physics.body.TriangleMesh
import dev.chieppa.ease.sdf.mesh.SdfMesh

/** This mesh's triangles for a collider, copied. */
public fun SdfMesh.triangleMesh(): TriangleMesh = TriangleMesh(positions, indices)

/**
 * A static collider of this mesh's triangles on [layer] with [surface], both by name. Build it from
 * [dev.chieppa.ease.sdf.mesh.SurfaceNets]' mesh, which is the same on every machine, so host and
 * clients collide with the same triangles.
 */
public fun SdfMesh.collider(layer: String = "", surface: String = ""): MeshCollider =
  MeshCollider(triangleMesh(), layer, surface)
