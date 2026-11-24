# Quests

A quest is a data asset made of stages, aliases and objectives.

- **Stages** are numbered states (10, 20, 30...). Setting a stage runs its actions and writes its journal entry. Stages can be set out of order, which is how branches work: choosing to help one side sets 40, the other sets 45.
- **Aliases** are named slots filled when the quest starts: a fixed entity, a query ("nearest entity tagged `elder`"), or a value passed in by whatever started the quest. Content refers to the alias, never the entity, so one quest can run against different places and characters.
- **Objectives** are what the player sees in the journal, shown and completed by stage actions.
- **States:** not started, running, completed, failed. Quests can run in parallel.
- **Actions** are registered types: set stage, start or stop a quest, set a global, play a scene, plus whatever the game registers (give an item, add a party member).

The editor shows a quest as a stage graph, with branches drawn from the actions that set each stage.
