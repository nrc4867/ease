# Networking tooling and testing

- **Multi-client play in the editor:** Play can start a server plus any number of clients in one process over loopback, each in its own viewport, or as separate player processes.
- **Network simulation:** latency, jitter, packet loss and bandwidth caps per connection, adjustable live, so prediction is tested under bad conditions from day one.
- **Debug views:** per-entity authority and owner, bandwidth by component, prediction corrections drawn in the viewport, and a timeline of snapshots, commands and events.
- **Headless bots:** simulation-mode players driven by scripted input join real sessions in CI, so a replication regression fails a build.
- **Replays:** a recorded stream of snapshots and events plays back in the editor for debugging desyncs.
