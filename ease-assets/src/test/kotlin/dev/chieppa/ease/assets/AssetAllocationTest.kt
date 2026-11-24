package dev.chieppa.ease.assets

import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.testing.AllocationBudget
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.io.TempDir

class AssetAllocationTest {
  @TempDir lateinit var folder: Path

  private val watcher = ManualFileWatcher()

  private object TextLoader : AssetLoader<String> {
    override val type: KClass<String> = String::class

    override fun load(guid: Guid, bytes: ByteArray): String = bytes.decodeToString()
  }

  @Test
  fun `looking an asset up by GUID or by path allocates nothing`() {
    val project = TestProject(folder)
    val guid = project.asset("a.txt", "one")
    val database = AssetDatabase.load(folder, watcher)
    val path = Path.of("a.txt")

    AllocationBudget.assertNoAllocation {
      assertNotNull(database[guid])
      assertNotNull(database.at(path))
    }
  }

  @Test
  fun `asking the store for a loaded asset allocates nothing`() {
    val project = TestProject(folder)
    val guid = project.asset("a.txt", "one")
    val database = AssetDatabase.load(folder, watcher)
    val associations = FileAssociationRegistry()
    associations.register(AssetLoader.kind, FileMatch.Extension("txt"), 0, TextLoader)
    val store = AssetStore(database, associations)
    val type = String::class
    store.handle(guid, type)

    AllocationBudget.assertNoAllocation { store.handle(guid, type) }
  }

  @Test
  fun `the per-tick change drain allocates nothing when no file changed`() {
    TestProject(folder).asset("a.txt", "one")
    val database = AssetDatabase.load(folder, watcher)

    AllocationBudget.assertNoAllocation { database.sync() }
  }
}
