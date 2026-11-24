# Networking overview

Any entity can be networked, and a game picks client-server or peer-to-peer per session without changing gameplay code. The split that makes this work: topology decides who has authority over each entity; replication and transport are the same in every case.

**Network-ready from day one.** Single-player is not a special case: it runs as a host world plus a local client over the loopback transport, like an integrated server. Every game therefore exercises authority, replication and prediction from its first playable build, and adding real multiplayer later changes the transport, not the game.

Structure: three authority policies (dedicated server, host, distributed ownership) sit over one shared replication layer, which sits over one transport interface.

See `authority.md`, `replication.md`, `prediction.md`, `integration.md`, `transport.md`, `tooling.md` and `open-questions.md`.
