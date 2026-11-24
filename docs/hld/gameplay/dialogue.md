# Dialogue

Dialogue is a set of topics, each holding lines guarded by conditions. When a conversation opens, the engine picks the highest-priority line whose conditions pass.

- A **topic** belongs to a quest (active only while it runs) or is global (greetings, barks).
- A **line** has a speaker alias, text as a localization key, optional voice clip, an animation or camera cue, and actions to run when spoken (often a quest stage).
- **Player responses** lead to further topics, so branching conversations are the same structure as everything else.
- Lines can be marked say-once; what has been said is saved.

Because quest stages and dialogue share conditions and actions, a choice in conversation and a branch in a quest are one mechanism.
