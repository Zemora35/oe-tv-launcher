package nl.neerdael.backdrop_launcher.media

import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FeaturedFeedTest {
    @Test fun recommendationsKeepPlatformDescriptionArtworkAndDeepLinkTogetherWithoutInventingPlayback() {
        val root = Files.createTempDirectory("oe-featured-").toFile()
        try {
            val provider = FakeFeaturedProvider()
            val feed = FeaturedFeed(provider, MediaDisk(root))
            val items = feed.refresh()
            assertEquals(2, items.size)
            val recommended = items.first { it.title == "Recommended" }
            assertEquals("YouTube", recommended.sourceName)
            assertEquals("https://img.example/recommended.jpg", recommended.image)
            assertEquals("youtube://watch/recommended", recommended.intentUri)
            assertEquals("Provider synopsis", recommended.description)
            assertFalse(recommended.continuation)
            val resume = feed.youtubeResume().single()
            assertEquals("Paused", resume.title)
            assertEquals(30, resume.progress)
            assertTrue(resume.continuation)
            feed.refresh()
            assertEquals(1, provider.calls) // Metadata refresh is bounded and cached for five minutes.
        } finally { root.listFiles().orEmpty().forEach { child -> child.listFiles().orEmpty().forEach { it.delete() }; child.delete() }; root.delete() }
    }

    private class FakeFeaturedProvider : TvChannelProvider {
        var calls = 0
        override fun hasPermission() = true
        override fun channels(): List<JSONObject> { calls++; return listOf(
            JSONObject().put("id", 1).put("packageName", "com.google.android.youtube.tv").put("appName", "YouTube"),
            JSONObject().put("id", 2).put("packageName", "browser.app").put("appName", "Browser")) }
        override fun programs(channelId: Long) = listOf(
            row("Recommended"), row("Paused").put("duration", 1000).put("playbackPosition", 300),
            row("Foreign").put("packageName", "other.app"), row("Hidden").put("browsable", 0))
        private fun row(name: String) = JSONObject().put("id", if (name == "Paused") 2 else 1)
            .put("title", name).put("description", "Provider synopsis")
            .put("thumbnailUri", "https://img.example/${name.lowercase()}.jpg")
            .put("intentUri", "youtube://watch/${name.lowercase()}")
        override fun contentImage(uri: String, maxBytes: Int) = byteArrayOf()
    }
}
