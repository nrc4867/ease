package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import com.github.stephengold.joltjni.BodyInterface
import com.github.stephengold.joltjni.JobSystem
import com.github.stephengold.joltjni.JobSystemSingleThreaded
import com.github.stephengold.joltjni.JobSystemThreadPool
import com.github.stephengold.joltjni.Jolt
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.RVec3
import com.github.stephengold.joltjni.TempAllocatorImpl
import com.github.stephengold.joltjni.readonly.ConstNarrowPhaseQuery
import dev.chieppa.ease.physics.events.PhysicsEvents
import dev.chieppa.ease.physics.jolt.BodyUserData
import dev.chieppa.ease.physics.jolt.JoltLayers
import dev.chieppa.ease.physics.jolt.JoltLibrary
import dev.chieppa.ease.physics.jolt.LayerFilters
import dev.chieppa.ease.physics.jolt.RayScratch
import dev.chieppa.ease.physics.jolt.TriggerHits
import dev.chieppa.ease.physics.query.RayHit
import dev.chieppa.ease.physics.setup.CollisionLayers
import dev.chieppa.ease.physics.setup.PhysicsSettings
import dev.chieppa.ease.physics.setup.SurfaceTypes
import dev.chieppa.ease.physics.volume.TriggerTracker
import kotlin.math.sqrt

/**
 * One world's physics, a Jolt physics system built from [settings]: the bodies of its colliders,
 * its characters, the queries gameplay asks and the [events] it reports. Systems added by
 * `Physics.addTo` keep it in step with the world's entities; read it from a system as
 * `World.physics`. Queries return what they find in place and allocate nothing. Use it from one
 * thread, the world's.
 */
public class PhysicsWorld(public val settings: PhysicsSettings = PhysicsSettings()) :
  AutoCloseable {
  init {
    JoltLibrary.load()
  }

  /** The game's collision layers. */
  public val layers: CollisionLayers = settings.layers

  /** The game's surface types. */
  public val surfaces: SurfaceTypes = settings.surfaces

  /** Where trigger entries and exits, footsteps and ground changes are reported. */
  public val events: PhysicsEvents = PhysicsEvents()

  internal val joltLayers = JoltLayers(layers)
  internal val system: PhysicsSystem =
    PhysicsSystem()
      .init(
        settings.maxBodies,
        0,
        settings.maxBodyPairs,
        settings.maxContactConstraints,
        joltLayers.broadPhase,
        joltLayers.objectVsBroadPhase,
        joltLayers.pairs,
      )
      .setGravity(0f, settings.gravityY, 0f)

  /** Bodies are reached from the world's thread alone, so without Jolt's body locks. */
  internal val bodyInterface: BodyInterface = system.bodyInterfaceNoLock
  internal val temp = TempAllocatorImpl(TEMP_BYTES)
  internal val filters = LayerFilters(system, joltLayers)
  private val jobs: JobSystem =
    if (settings.workerThreads == 0) JobSystemSingleThreaded(Jolt.cMaxPhysicsJobs)
    else JobSystemThreadPool(Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, settings.workerThreads)

  private val query: ConstNarrowPhaseQuery = system.narrowPhaseQueryNoLock
  private val rays = RayScratch()
  private val below = RayHit()
  private val point = RVec3()
  private val triggerHits = TriggerHits(MAX_TRIGGER_HITS)

  internal val bodies = PhysicsBodies(this)
  internal val characters = Characters(this)
  internal val triggers = TriggerTracker(this)
  private var closed = false

  internal val refused = LinkedHashMap<Int, String>()

  /**
   * Why each collider that gets no body was refused, one line each, such as a dynamic body under a
   * parent. A line goes when its entity is removed or changed so that it builds.
   */
  public val problems: Collection<String>
    get() = refused.values

  /** How many bodies the world holds, characters not included. */
  public val bodyCount: Int
    get() = system.numBodies

  /**
   * Casts a ray from ([originX], [originY], [originZ]) along ([directionX], [directionY],
   * [directionZ]) for up to [maxDistance], against the solid colliders [layer] collides with, or
   * every solid collider when [layer] is [ANY_LAYER]. Fills [hit] with the closest and returns
   * true, or returns false when nothing is hit. Triggers are never hit. A [layer] that is neither
   * [ANY_LAYER] nor one of the game's layers is refused with [IllegalArgumentException].
   */
  @Suppress("LongParameterList") // A ray is six numbers; boxing them would allocate.
  public fun raycast(
    originX: Float,
    originY: Float,
    originZ: Float,
    directionX: Float,
    directionY: Float,
    directionZ: Float,
    maxDistance: Float,
    hit: RayHit,
    layer: Int = ANY_LAYER,
  ): Boolean {
    val length = sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ)
    require(layer == ANY_LAYER || layer in 0 until layers.size) {
      "Layer $layer is not one of the ${layers.size} collision layers, nor ANY_LAYER"
    }
    var found = length > 0f && maxDistance > 0f
    if (found) {
      val reach = maxDistance / length
      rays.aim(
        originX,
        originY,
        originZ,
        directionX * reach,
        directionY * reach,
        directionZ * reach,
      )
      found = cast(layer)
      if (found) {
        val fraction = rays.result.fraction
        val userData = bodyInterface.getUserData(rays.result.bodyId)
        hit.entity = Entity(BodyUserData.entityId(userData))
        hit.surface = BodyUserData.surface(userData)
        hit.distance = fraction * maxDistance
        hit.x = originX + directionX * reach * fraction
        hit.y = originY + directionY * reach * fraction
        hit.z = originZ + directionZ * reach * fraction
      }
    }
    return found
  }

  /** Casts the aimed ray against what [layer] collides with, or every solid when [ANY_LAYER]. */
  private fun cast(layer: Int): Boolean =
    if (layer == ANY_LAYER) {
      query.castRay(
        rays.ray,
        rays.result,
        filters.solidBroadPhase,
        filters.anyObject,
        filters.anyBody,
      )
    } else {
      query.castRay(
        rays.ray,
        rays.result,
        filters.broadPhaseOf[layer],
        filters.objectsOf[layer],
        filters.anyBody,
      )
    }

  /**
   * The surface type, by index, of the solid collider straight below ([x], [y], [z]) within
   * [depth], or -1 when there is none.
   */
  public fun surfaceBelow(x: Float, y: Float, z: Float, depth: Float): Int =
    if (raycast(x, y, z, 0f, -1f, 0f, depth, below)) below.surface else NO_SURFACE

  /**
   * Fills [into] with the bodies of the trigger volumes containing ([x], [y], [z]) and returns how
   * many there are, at most [MAX_TRIGGER_HITS] and at most [into]'s size.
   */
  internal fun triggerBodiesAt(x: Float, y: Float, z: Float, into: IntArray): Int {
    point.set(x.toDouble(), y.toDouble(), z.toDouble())
    triggerHits.clear()
    // The full overload: the shorter ones make a filter per call.
    query.collidePoint(
      point,
      triggerHits,
      filters.triggerBroadPhase,
      filters.triggerObjects,
      filters.anyBody,
      filters.anyShape,
    )
    val count = minOf(triggerHits.count, into.size)
    System.arraycopy(triggerHits.bodies, 0, into, 0, count)
    return count
  }

  /** Advances the simulation by [seconds]. */
  internal fun step(seconds: Float) {
    system.update(seconds, 1, temp, jobs)
  }

  /** Frees the Jolt world and every body and character in it; nothing may use it after. */
  override fun close() {
    if (closed) return
    closed = true
    characters.close()
    bodies.close()
    listOf(triggerHits, rays, filters, jobs, temp, system, joltLayers).forEach { it.close() }
  }

  public companion object {
    /** The layer argument of a query over every solid layer. */
    public const val ANY_LAYER: Int = -1

    /** What surface queries return when nothing is there. */
    public const val NO_SURFACE: Int = -1

    /** The most trigger volumes one point is found inside at once. */
    public const val MAX_TRIGGER_HITS: Int = 16

    private const val TEMP_BYTES = 10 shl 20
  }
}
