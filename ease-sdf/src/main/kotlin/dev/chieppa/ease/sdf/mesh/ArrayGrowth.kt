package dev.chieppa.ease.sdf.mesh

/** Room an array starts with when nothing says how much it needs. */
internal const val INITIAL_ROOM = 1024

/** The length an array of [current] length grows to so it holds at least [needed]: doubled. */
internal fun grownLength(current: Int, needed: Int): Int = maxOf(current * 2, needed)
