package dev.chieppa.ease.sample

import dev.chieppa.ease.runtime.loop.FixedStep

private const val NANOS_PER_SECOND = 1_000_000_000f

/** How long one step of [fixedStep] is, in seconds. */
internal fun stepSeconds(fixedStep: FixedStep): Float = fixedStep.stepNanos / NANOS_PER_SECOND
