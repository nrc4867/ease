package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.builtin.Destroy
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.spawnEntity
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Who may send a command follows from its scope and who owns the entities it targets. */
class CommandPermissionTest {
  private val world = phasedWorld {}
  private val guest = PeerId(1)
  private val owned = world.networkIdOf(world.spawnEntity { it += Authority(PeerId.HOST, guest) })
  private val hosts = world.networkIdOf(world.spawnEntity())

  private class Scoped(override val scope: CommandScope, private val ids: List<NetworkId>) :
    Command {
    override fun targets(): List<NetworkId> = ids

    override fun apply(world: World): Command = this
  }

  @Test
  fun `the world's own peer may send any command`() {
    assertNull(permissionRefusal(world, Destroy(hosts), PeerId.HOST))
  }

  @Test
  fun `another peer may not send a command scoped to the world's own peer`() {
    assertNotNull(permissionRefusal(world, Destroy(owned), guest))
  }

  @Test
  fun `a peer may act on entities it owns, and only those`() {
    assertNull(permissionRefusal(world, Scoped(CommandScope.OWNED_ENTITIES, listOf(owned)), guest))
    val mixed = Scoped(CommandScope.OWNED_ENTITIES, listOf(owned, hosts))
    assertNotNull(permissionRefusal(world, mixed, guest))
    assertNotNull(permissionRefusal(world, Scoped(CommandScope.OWNED_ENTITIES, emptyList()), guest))
    val gone = Scoped(CommandScope.OWNED_ENTITIES, listOf(NetworkId(ABSENT)))
    assertNotNull(permissionRefusal(world, gone, guest))
  }

  @Test
  fun `any peer may send a command scoped to every peer`() {
    assertNull(permissionRefusal(world, Scoped(CommandScope.ANY_PEER, emptyList()), guest))
  }

  private companion object {
    const val ABSENT = 999L
  }
}
