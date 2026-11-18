package nl.neerdael.backdrop_launcher.ui.cinematic

/** Logical columns/rows, independent of animated paint bounds and clipped lazy items. */
internal fun gridNeighbour(index: Int, count: Int, columns: Int, dx: Int, dy: Int): Int {
    if (count <= 0 || columns <= 0) return 0
    val current = index.coerceIn(0, count - 1)
    val column = current % columns + dx
    val target = current + dy * columns + dx
    return if (column in 0 until columns && target in 0 until count) target else current
}
