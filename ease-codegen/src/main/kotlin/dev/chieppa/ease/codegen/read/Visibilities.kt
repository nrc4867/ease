package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import dev.chieppa.ease.codegen.model.ClassVisibility

/** Visibilities generated code in the same module can reach. */
private val reachable = setOf(Visibility.PUBLIC, Visibility.INTERNAL)

/** How far [declaration] can be seen, taking every declaration it is nested in into account. */
internal fun classVisibility(declaration: KSDeclaration): ClassVisibility {
  val chain = generateSequence(declaration) { it.parentDeclaration }.map { it.getVisibility() }
  val visibilities = chain.toList()
  return when {
    visibilities.any { it !in reachable } -> ClassVisibility.HIDDEN
    Visibility.INTERNAL in visibilities -> ClassVisibility.INTERNAL
    else -> ClassVisibility.PUBLIC
  }
}

/** Whether generated code in the same module can call [declaration]. */
internal fun isReachable(declaration: KSDeclaration): Boolean =
  declaration.getVisibility() in reachable

/** Whether generated code in the same module can set [property]. */
internal fun isSettable(property: KSPropertyDeclaration): Boolean {
  val setter = property.setter ?: return false
  val hidden = setter.modifiers.any { it == Modifier.PRIVATE || it == Modifier.PROTECTED }
  return !hidden
}
