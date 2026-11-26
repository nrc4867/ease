package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.memory.GpuLayout
import dev.chieppa.ease.memory.wgslDeclarations

/**
 * A material template: WGSL that implements the surface function, `surface`, which takes a
 * `SurfaceInput` (a pixel's world position, normal, texture coordinates and draw index) and returns
 * a `Surface` (albedo, normal, roughness, metallic, emissive and occlusion; see `forward.wgsl`). It
 * binds its per-draw parameters as one read-only storage array at group 2, binding 0, and may
 * sample the draw's [TextureSlot] textures the engine declares in group 1. It starts from
 * `default_surface(input)` and changes what it shades, so fields it leaves alone keep their
 * defaults. The engine wraps it in the lighting, so a template never lights. [layouts] are the
 * structs its WGSL uses.
 */
public class MaterialTemplate(
  public val name: String,
  public val wgsl: String,
  public val layouts: List<GpuLayout>,
) {
  public companion object {
    /**
     * The lit template: metal and roughness from [LitMaterial], one per draw, each value multiplied
     * by its slot's texture; the normal map bends the normal in a frame built from screen-space
     * derivatives, with its y flipped for texture coordinates that run down the image.
     */
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
