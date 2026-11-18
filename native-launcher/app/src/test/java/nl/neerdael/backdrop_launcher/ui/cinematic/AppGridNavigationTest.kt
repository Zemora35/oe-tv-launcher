package nl.neerdael.backdrop_launcher.ui.cinematic

import org.junit.Assert.assertEquals
import org.junit.Test

class AppGridNavigationTest {
    @Test fun rowsPreserveColumnAndEdgesNeverWrapToAnUnrelatedControl() {
        assertEquals(29, gridNeighbour(22, 42, 7, 0, 1))
        assertEquals(36, gridNeighbour(29, 42, 7, 0, 1))
        assertEquals(36, gridNeighbour(36, 42, 7, 0, 1))
        assertEquals(7, gridNeighbour(7, 42, 7, -1, 0))
        assertEquals(6, gridNeighbour(6, 42, 7, 1, 0))
        assertEquals(41, gridNeighbour(41, 42, 7, 0, 1))
        assertEquals(0, gridNeighbour(0, 42, 7, 0, -1))
        assertEquals(11, gridNeighbour(6, 12, 5, 0, 1))
    }
}
