package dev.chieppa.ease.net.input

import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.PeerId

/** The name a world's [PeerInputs] is injected under. */
public const val PEER_INPUTS_INJECTABLE: String = "ease.peerInputs"

/** The input each peer gave for the tick [this] world is running. */
public val World.peerInputs: PeerInputs
  get() = inject(PEER_INPUTS_INJECTABLE)

/**
 * The input of each peer for the tick a world is running, which its systems read to move what each
 * peer controls. The host sets every joined peer's frame before its tick; a client sets only its
 * own, before each tick it predicts or replays, so on a client other peers have none; a peer whose
 * connection closed has none either. Frames are held by reference, never copied, and nothing here
 * allocates once every peer has been set.
 */
public class PeerInputs {
  private var peers = IntArray(INITIAL_PEERS)
  private var frames = arrayOfNulls<InputFrame>(INITIAL_PEERS)
  private var count = 0

  /** [peer]'s input for the tick being run, or null when this world has none for it. */
  public fun of(peer: PeerId): InputFrame? {
    for (index in 0 until count) if (peers[index] == peer.value) return frames[index]
    return null
  }

  /** Makes [frame] [peer]'s input for the tick about to run. */
  public operator fun set(peer: PeerId, frame: InputFrame) {
    for (index in 0 until count) {
      if (peers[index] == peer.value) {
        frames[index] = frame
        return
      }
    }
    if (count == peers.size) {
      peers = peers.copyOf(count * 2)
      frames = frames.copyOf(count * 2)
    }
    peers[count] = peer.value
    frames[count++] = frame
  }

  /** Takes [peer]'s input away, as when its connection closes, so what it controls stops. */
  public fun remove(peer: PeerId) {
    for (index in 0 until count) {
      if (peers[index] == peer.value) {
        count--
        peers[index] = peers[count]
        frames[index] = frames[count]
        frames[count] = null
        return
      }
    }
  }

  private companion object {
    const val INITIAL_PEERS = 4
  }
}
