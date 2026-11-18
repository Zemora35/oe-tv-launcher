package nl.neerdael.backdrop_launcher.ui.cinematic

import org.junit.Assert.*
import org.junit.Test

class VortexGeometryTest {
    @Test fun focusDominatesAndNeighboursShrinkMonotonically() {
        val centre = VortexGeometry.pose(0)
        assertEquals(.5f, centre.x, .001f)
        assertEquals(1.35f, centre.scale, .001f)
        assertTrue(centre.scale / VortexGeometry.pose(1).scale >= 1.5f)
        for (direction in listOf(-1, 1)) {
            var previous = centre
            for (distance in 1..VortexGeometry.RADIUS) {
                val pose = VortexGeometry.pose(distance * direction)
                assertTrue(pose.scale < previous.scale)
                assertTrue(pose.alpha < previous.alpha)
                assertTrue(pose.x in 0f..1f && pose.y in 0f..1f)
                previous = pose
            }
        }
    }

    @Test fun remoteOrderIsIndependentOfCurveAndClampsAtEnds() {
        assertEquals(0, VortexGeometry.step(0, -1, 12))
        assertEquals(11, VortexGeometry.step(11, 1, 12))
        assertEquals(6, VortexGeometry.step(5, 1, 12))
        assertEquals(4, VortexGeometry.step(5, -1, 12))
        assertEquals(0, VortexGeometry.step(0, 1, 0))
        assertEquals(11, VortexGeometry.step(2, Int.MAX_VALUE, 12))
    }

    @Test fun residentWindowStaysBoundedEvenForHugeInventories() {
        for (count in listOf(0, 1, 4, 10, 10000)) {
            for (index in listOf(0, count / 2, maxOf(0, count - 1))) {
                val visible = VortexGeometry.visible(index, count)
                assertTrue(visible.count() <= 13)
                assertTrue(visible.all { it in 0 until count })
                if (count > 0) assertTrue(index in visible)
            }
        }
    }

    @Test fun distantAppsCurlBackIntoTheSpiralAndKeepShrinking() {
        var previous = VortexGeometry.pose(4)
        for (distance in 5..VortexGeometry.TAIL) {
            val pose = VortexGeometry.pose(distance)
            assertTrue(pose.x < previous.x)
            assertTrue(pose.scale < previous.scale && pose.alpha < previous.alpha)
            assertTrue(pose.x in 0f..1f && pose.y in 0f..1f)
            previous = pose
        }
    }
}
