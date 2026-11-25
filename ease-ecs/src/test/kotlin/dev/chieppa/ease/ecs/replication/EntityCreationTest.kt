package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.FamilyOnAdd
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Entities get their network id and authority wherever and however they are created. */
class EntityCreationTest {
  private val settings = ReplicationSettings(serializers = ReplicationWorlds.serializers)

  /** Creates, with Fleks's own `entity`, and writes an entity while the world is being built. */
  private class Founder : IntervalSystem() {
    var founded: Entity = Entity.NONE

    override fun onInit() {
      founded = world.entity { it += Lantern() }
      world.replication.write(founded, Lantern) { fuel = 2f }
    }

    override fun onTick() = Unit
  }

  /** Reads each new lantern's network id and writes it, as soon as its family takes it. */
  private class Greeter : IteratingSystem(family { all(Lantern) }), FamilyOnAdd {
    private val replication = world.replication
    val greeted = mutableListOf<NetworkId>()

    override fun onAddEntity(entity: Entity) {
      greeted += world.networkIdOf(entity)
      replication.write(entity, Lantern) { charges = 1 }
    }

    override fun onTickEntity(entity: Entity) = Unit
  }

  @Test
  fun `a system may create and write an entity while it initializes`() {
    lateinit var founder: Founder

    val world =
      phasedWorld(replication = settings) { add(Phase.SIMULATION, Founder().also { founder = it }) }

    assertEquals(NetworkId(1), world.networkIdOf(founder.founded))
    assertEquals(2f, ReplicationWorlds.lanternOf(world, founder.founded).fuel)
  }

  @Test
  fun `a family hook sees a spawned entity's network id and authority`() {
    lateinit var greeter: Greeter
    val world =
      phasedWorld(replication = settings) { add(Phase.SIMULATION, Greeter().also { greeter = it }) }

    val entity = world.spawnEntity { it += Lantern() }
    world.update(DELTA)

    assertEquals(listOf(world.networkIdOf(entity)), greeter.greeted)
    assertEquals(1, ReplicationWorlds.lanternOf(world, entity).charges)
  }

  @Test
  fun `ids stop below the host's range, and the last one ends the session cleanly`() {
    val world = ReplicationWorlds.world()
    world.entity { it += NetworkIdentity(NetworkId(NetworkId.LOCAL_START - 1)) }

    assertFailsWith<IllegalStateException> { world.entity {} }
  }

  @Test
  fun `an id at or past the limit is refused`() {
    val world = ReplicationWorlds.world()

    assertFailsWith<IllegalArgumentException> {
      world.entity { it += NetworkIdentity(NetworkId(Long.MAX_VALUE)) }
    }
    assertEquals(NetworkId(1), world.networkIdOf(world.entity {}))
  }

  private companion object {
    const val DELTA = 1f / 60f
  }
}
