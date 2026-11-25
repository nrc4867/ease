package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.memory.GpuLayout
import dev.chieppa.ease.memory.wgslDeclarations

/**
 * A material template: WGSL that implements the surface function, `surface`, which takes a
 * `SurfaceInput` (a pixel's world position, normal, texture coordinates and draw index) and returns
 * a `Surface` (albedo, normal, roughness, metallic and emissive; see `forward.wgsl`), and binds its
 * parameters in group 2. The engine wraps it in the lighting, so a template never lights. [layouts]
 * are the structs its WGSL uses.
 */
public class MaterialTemplate(
  public val name: String,
  public val wgsl: String,
  public val layouts: List<GpuLayout>,
) {
  public companion object {
    /** The lit template: metal and roughness from [LitMaterial], one per draw. */
    public val LIT: MaterialTemplate =
      MaterialTemplate("lit", resource("lit.wgsl"), listOf(LitMaterialWriter))

    internal fun resource(name: String): String =
      checkNotNull(MaterialTemplate::class.java.getResource(name)) { name }.readText()
  }
}

/**
 * The shader a forward pass draws [template] with: the declarations of every struct either side
 * uses, then the engine's forward lighting, then the template's surface function.
 */
internal fun forwardShader(template: MaterialTemplate): String {
  val engine = listOf(FrameDataWriter, ViewDataWriter, ObjectDataWriter)
  val structs = wgslDeclarations(engine + template.layouts)
  return structs + "\n" + MaterialTemplate.resource("forward.wgsl") + "\n" + template.wgsl
}
