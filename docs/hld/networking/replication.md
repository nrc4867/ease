# Replication

Replication is declared on components, not written by hand per game.

- **Replicated components** carry an annotation. Field options set quantization (how many bits a position or angle needs), whether replicas interpolate, and who receives it (everyone, owner only, everyone but the owner). KSP generates the serializers at build time from annotated Kotlin components; components described at runtime get schema-driven serializers. Nothing reflects at runtime.
- **Network ids:** each replicated entity gets a session-wide id that is never reused, unlike Fleks entity ids; commands and the command log name entities by network id. Spawns send the prefab's asset GUID plus initial state, so replicas build from local content.
- **Snapshots:** each tick the authority writes the state of relevant entities, delta-compressed against the last snapshot that peer acknowledged. State goes on an unreliable channel; a lost packet is replaced by the next one.
- **Relevancy and priority:** peers receive only entities that matter to them (distance, same area, team, always-relevant). A priority score per entity decides what fits in each packet when bandwidth is short, so nearby fast things update more often than distant still ones.
- **Commands and events:** a client sends commands to the authority ("use move 2 on target 41"), which validates them before acting. The authority sends events to replicas ("play this effect", "show this line of dialogue"). Both are reliable and ordered.
- **Tick numbers** from the fixed-step loop stamp every snapshot, command and event, so peers agree on when things happened.
