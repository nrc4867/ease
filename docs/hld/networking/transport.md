# Transport and sessions

Replication talks to a `Transport` interface with three channel kinds: unreliable (state), reliable ordered (commands, events) and reliable unordered (bulk data such as a joining peer's world state).

| Transport | Used for |
|---|---|
| Loopback | Server and clients in one process: editor multi-client play, headless tests |
| [GameNetworkingSockets](https://github.com/ValveSoftware/GameNetworkingSockets) | Real games, client-server and P2P. Valve's open-source version of Steam's networking layer, with a plain C interface, so it gets our own FFM bindings like wgpu |
| Steam networking | Optional, for a Steam release: the same API with Steam's relay network and identities |

GameNetworkingSockets brings reliable and unreliable messages over UDP, fragmentation, encryption, prioritized message lanes, and peer-to-peer connections with NAT traversal through WebRTC's ICE. P2P needs a small **signaling service** so peers can find each other; it plugs into the library and is the one piece of server infrastructure P2P still requires.

**Sessions:** host, join by address or invite code, leave, and host migration where the topology allows it. A joining peer gets a full snapshot of relevant state, then deltas. Matchmaking and lobbies are out of scope for the engine; games or platform services provide them.
