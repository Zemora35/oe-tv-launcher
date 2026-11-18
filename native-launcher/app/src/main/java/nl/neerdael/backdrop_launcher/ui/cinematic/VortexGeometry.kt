package nl.neerdael.backdrop_launcher.ui.cinematic

import kotlin.math.abs
import androidx.compose.runtime.Immutable

/** Normalised poses. No Context, allocation or trigonometry is needed in animation frames. */
@Immutable
internal data class VortexPose(val x: Float, val y: Float, val scale: Float, val alpha: Float)

internal object VortexGeometry {
    const val RADIUS = 4
    const val TAIL = 8
    private val poses = (-RADIUS..TAIL).associateWith(::calculate)

    fun pose(distance: Int): VortexPose = poses.getValue(distance.coerceIn(-RADIUS, TAIL))

    private fun calculate(distance: Int): VortexPose {
        if (distance == 0) return VortexPose(.5f, .72f, 1.35f, 1f)
        // After the foreground arc, actual apps curl back towards the vanishing point.
        when (distance) {
            5 -> return VortexPose(.90f, .22f, .48f, .38f)
            6 -> return VortexPose(.84f, .11f, .36f, .31f)
            7 -> return VortexPose(.69f, .13f, .26f, .24f)
            8 -> return VortexPose(.64f, .24f, .18f, .17f)
        }
        val d = abs(distance)
        return VortexPose(.5f + distance * .108f,
            .78f - d * (if (distance > 0) .025f else .012f) - d * d * (if (distance > 0) .017f else .003f),
            1f - d * .10f, 1f - d * .12f)
    }

    fun step(index: Int, delta: Int, count: Int): Int =
        if (count <= 0) 0 else (index.toLong() + delta).coerceIn(0, count - 1L).toInt()

    fun visible(index: Int, count: Int): IntRange =
        if (count <= 0) IntRange.EMPTY else maxOf(0, index - RADIUS)..minOf(count - 1, index + TAIL)
}
