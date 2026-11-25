package dev.chieppa.ease.cli.template

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc("A line a generator adds to a file the project already has, above a marker comment.")
@Serializable
public class Insertion(
  @Doc("The file to change, relative to the project; it may hold placeholders.")
  public val file: String,
  @Doc("The comment line to insert above, such as `// ease:types`; its text, without spaces.")
  public val marker: String,
  @Doc("The lines to insert, indented like the marker; it may hold placeholders.")
  public val text: String,
  @Doc("Imports the inserted lines need; each goes among the file's imports unless it is there.")
  public val imports: List<String> = emptyList(),
)
