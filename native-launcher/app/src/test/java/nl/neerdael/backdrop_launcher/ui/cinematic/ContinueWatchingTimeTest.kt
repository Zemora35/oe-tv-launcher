package nl.neerdael.backdrop_launcher.ui.cinematic

import nl.neerdael.backdrop_launcher.core.ResumeItem
import org.junit.Assert.*
import org.junit.Test

class ContinueWatchingTimeTest {
    @org.junit.Test fun recommendationIdsCannotCollideWithContinueWatchingAndSearchEncodesUnicode() {
        val watched = nl.neerdael.backdrop_launcher.core.ResumeItem("Title", "", null, 30, "video://1", "video.app", 7)
        val recommendation = watched.copy(continuation = false)
        org.junit.Assert.assertNotEquals(contentKey(watched), contentKey(recommendation))
        org.junit.Assert.assertTrue(isContentSaved(recommendation, setOf("id:7")))
        org.junit.Assert.assertTrue(watchOptionsUrl("Özel & Film", "tr").contains("%26"))
        org.junit.Assert.assertTrue(watchOptionsUrl("Matrix", "tr").startsWith("https://www.justwatch.com/tr/arama?q="))
    }
    private fun item(duration: Long?, position: Long?) = ResumeItem("Episode", "", null, 50, null, null,
        durationMillis = duration, playbackPositionMillis = position)

    @Test fun remainingTimeComesFromPlaybackDataAndRoundsUp() {
        assertEquals(45, remainingMinutes(item(5_400_000, 2_700_000)))
        assertEquals(1, remainingMinutes(item(60_000, 59_999)))
        assertEquals(2, remainingMinutes(item(120_000, 59_999)))
    }

    @Test fun unknownCompletedAndMalformedDurationsNeverMakeUpARemainingTime() {
        assertNull(remainingMinutes(item(null, 10)))
        assertNull(remainingMinutes(item(1000, null)))
        assertNull(remainingMinutes(item(1000, -1)))
        assertNull(remainingMinutes(item(1000, 1000)))
        assertNull(remainingMinutes(item(1000, 2000)))
        assertEquals(Int.MAX_VALUE, remainingMinutes(item(Long.MAX_VALUE, 1)))
    }
}
