package {{package}}

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("A {{nameWords}} component.")
@Serializable
@SerialName("{{serialPrefix}}.{{Name}}")
public class {{Name}} : Component<{{Name}}> {
  override fun type(): ComponentType<{{Name}}> = {{Name}}

  /** The component type. */
  public companion object : ComponentType<{{Name}}>()
}
