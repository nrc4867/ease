package dev.chieppa.ease.net.relevancy

import dev.chieppa.ease.commands.CommandRegistry

/**
 * Registers the components relevancy filters read ([Team], [Area] and [AlwaysRelevant]), so
 * commands and the world's JSON can carry them. Every peer of a session registers the same.
 */
public fun CommandRegistry.registerRelevancyComponents() {
  component(Team::class, Team.serializer())
  component(Area::class, Area.serializer())
  component(AlwaysRelevant::class, AlwaysRelevant.serializer())
}
