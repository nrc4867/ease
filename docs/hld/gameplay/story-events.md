# Story events

The story manager starts quests in response to things that happen in the world, so content can react without hand-wired triggers.

1. Gameplay code raises a **story event**: entered a location, entity defeated, item acquired, time passed, plus any event type a game registers.
2. The event carries data (who, where, what), which can fill a quest's aliases.
3. **Event nodes** are data: an event type, conditions, and the quests to try. The first quest whose aliases can be filled starts.
4. The same quest template can run many times with different fills, which is how repeatable or generated jobs work.
