# Networking open questions

- [ ] **Signaling for P2P:** deferred. The first release supports client-server and direct connect; a signaling option is chosen when a game needs P2P.
- [ ] **Host migration:** worth building for the first release, or end the session when the host leaves.
- [ ] **Rollback:** whether any game needs input-only rollback, and if so, whether Jolt's cross-platform determinism holds up in practice on the JVM.
- [ ] **Trust in distributed ownership:** which checks the host makes on peer-owned state.
