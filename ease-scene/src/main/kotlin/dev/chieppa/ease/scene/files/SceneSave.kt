package dev.chieppa.ease.scene.files

/** What saving a scene folder did: the files it [written] and [deleted], as paths inside it. */
public class SceneSave(public val written: List<String>, public val deleted: List<String>) {
  /** Whether the save left every file as it was. */
  public val unchanged: Boolean
    get() = written.isEmpty() && deleted.isEmpty()
}
