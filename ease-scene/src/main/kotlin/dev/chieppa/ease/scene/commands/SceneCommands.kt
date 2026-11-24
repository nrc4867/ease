package dev.chieppa.ease.scene.commands

import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.node.Parent

/**
 * Registers the scene commands and the node components, so a world's codec can log and send them. A
 * game registers its own scene component types with the registry as well.
 */
public fun CommandRegistry.registerSceneCommands() {
  command(SpawnNode::class, SpawnNode.serializer())
  command(DestroyNode::class, DestroyNode.serializer())
  command(UpdateNode::class, UpdateNode.serializer())
  component(NodeKey::class, NodeKey.serializer())
  component(Parent::class, Parent.serializer())
  component(NodeName::class, NodeName.serializer())
}
