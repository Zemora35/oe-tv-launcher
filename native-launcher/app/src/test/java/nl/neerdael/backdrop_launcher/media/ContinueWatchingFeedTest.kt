package nl.neerdael.backdrop_launcher.media

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class ContinueWatchingFeedTest {
    private fun entry(type: Int = 0, position: Long = 300, duration: Long = 1000) = JSONObject()
        .put("watchNextType", type).put("playbackPosition", position).put("duration", duration)

    @Test fun acceptsOnlyPartiallyWatchedContinueEntries() {
        assertTrue(WatchNextFeed.isUnfinished(entry()))
        for (type in listOf(1, 2, 3, -1)) assertFalse(WatchNextFeed.isUnfinished(entry(type)))
        for (position in listOf(-1L, 0L, 1000L, 1200L)) assertFalse(WatchNextFeed.isUnfinished(entry(position = position)))
        assertFalse(WatchNextFeed.isUnfinished(entry().put("browsable", 0)))
        assertFalse(WatchNextFeed.isUnfinished(entry().put("progressPercent", 100)))
    }

    @Test fun legacyEntriesRequireConcretePlaybackProgress() {
        assertFalse(WatchNextFeed.isUnfinished(JSONObject()))
        assertTrue(WatchNextFeed.isUnfinished(JSONObject().put("progressPercent", 35)))
        assertFalse(WatchNextFeed.isUnfinished(JSONObject().put("watchNextType", 0)))
        assertFalse(WatchNextFeed.isUnfinished(JSONObject().put("progressPercent", 100)))
    }

    @Test fun recommendationsDoNotCrowdResumableItemsOutOfTheLimit() {
        val root = Files.createTempDirectory("oe-tv-resume").toFile()
        try {
            val prefs = MediaPrefs().apply { putBool("show_watch_next_section", true); putInt("watch_next_max_items", 2) }
            val recommendations = (1..70).map { entry(2).put("id", it).put("title", "Recommendation") }
            val resume = entry().put("id", 99).put("title", "Paused episode").put("packageName", "video.app")
                .put("intentUri", "video://resume/99").put("contentId", "episode-99")
            val provider = FeedProvider(recommendations + resume)
            val feed = WatchNextFeed(prefs, provider, MediaDisk(root) { it.isNotEmpty() })
            val item = feed.refresh().single()
            assertEquals("Paused episode", item.title)
            assertEquals(30, item.progress)
            assertEquals(1000L, item.durationMillis)
            assertEquals(300L, item.playbackPositionMillis)
            assertEquals("video://resume/99", item.intentUri)
            assertEquals("episode-99", item.contentId)
        } finally {
            check(root.canonicalFile.parentFile == java.io.File(System.getProperty("java.io.tmpdir")).canonicalFile && root.name.startsWith("oe-tv-"))
            root.deleteRecursively()
        }
    }

    @Test fun anEmptyProviderDoesNotInventWatchingHistory() {
        val root = Files.createTempDirectory("oe-tv-empty").toFile()
        try {
            val prefs = MediaPrefs().apply { putBool("show_watch_next_section", true) }
            val provider = FeedProvider(emptyList())
            assertTrue(WatchNextFeed(prefs, provider, MediaDisk(root)).refresh().isEmpty())
            assertEquals(1, provider.queries)
        } finally {
            check(root.canonicalFile.parentFile == java.io.File(System.getProperty("java.io.tmpdir")).canonicalFile && root.name.startsWith("oe-tv-"))
            root.deleteRecursively()
        }
    }

    @Test fun tmdbReturnsTheTitleAndOverviewForTheSameImageAndRequestedLanguage() {
        var requested = ""
        val client = TraktClient(MediaPrefs(), "client", "key", "https://example.com", MediaTransport {
            requested = it.url
            MediaResponse(200, emptyMap(), """{"title":"Başlangıç","overview":"Bir rüya görevi.","backdrop_path":"/dream.jpg"}""".toByteArray())
        })
        val details = requireNotNull(client.backdropDetails(Candidate("Inception", 42, "movie"), "tr-TR"))
        assertTrue(requested.contains("language=tr-TR"))
        assertEquals("Başlangıç", details.title)
        assertEquals("Bir rüya görevi.", details.description)
        assertEquals("https://image.tmdb.org/t/p/w1280/dream.jpg", details.imageUrl)
    }
}
