package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.*
import org.junit.Test

class CinemaPerformanceTest {
    @Test fun constrainedTvArtworkCutsTexturePixelsAndVisibleTilesDoNotScroll() {
        assertEquals(IntSize(1280, 720), CinemaPerformance.backdropSize(1920, 1080, constrained = true))
        assertEquals(0f, CinemaPerformance.minimalScroll(20f, 60f, 200f), 0f)
        assertEquals(-10f, CinemaPerformance.minimalScroll(-10f, 60f, 200f), 0f)
        assertEquals(30f, CinemaPerformance.minimalScroll(170f, 60f, 200f), 0f)
    }
    @Test fun caps4kArtworkToAQuarterOfItsNativePixelCount() {
        val size = CinemaPerformance.backdropSize(3840, 2160)
        assertEquals(IntSize(1920, 1080), size)
        assertEquals(8_294_400L, size.width.toLong() * size.height * 4)
    }

    @Test fun smallerAndUnusualDisplaysKeepTheirAspectWithoutUpscaling() {
        assertEquals(IntSize(1280, 720), CinemaPerformance.backdropSize(1280, 720))
        assertEquals(IntSize(1920, 804), CinemaPerformance.backdropSize(3440, 1440))
        assertEquals(IntSize(1080, 1080), CinemaPerformance.backdropSize(2160, 2160))
        assertEquals(IntSize(1, 1), CinemaPerformance.backdropSize(0, -1))
    }
}
