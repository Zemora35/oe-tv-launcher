package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.ui.unit.IntSize
import android.app.ActivityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/** Quality budget independent of a TV's 4K panel; UI/text/icons still render natively. */
internal object CinemaPerformance {
    fun backdropSize(width: Int, height: Int, constrained: Boolean = false): IntSize {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val scale = minOf(1.0, (if (constrained) 1280.0 else 1920.0) / w, (if (constrained) 720.0 else 1080.0) / h)
        return IntSize((w * scale).roundToInt().coerceAtLeast(1),
            (h * scale).roundToInt().coerceAtLeast(1))
    }
    fun minimalScroll(offset: Float, extent: Float, viewport: Float): Float = when {
        offset < 0f -> offset
        offset + extent > viewport -> offset + extent - viewport
        else -> 0f
    }
}

@Composable
internal fun rememberConstrainedTv(): Boolean {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        val manager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? ActivityManager
        val memory = ActivityManager.MemoryInfo()
        manager?.getMemoryInfo(memory)
        manager?.isLowRamDevice == true || memory.totalMem in 1..(2L * 1024 * 1024 * 1024)
    }
}
