package nl.neerdael.backdrop_launcher.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit device diagnostic: exports counts only, never titles, account IDs or playback URLs. */
@RunWith(AndroidJUnit4::class)
class TvFeedProbeTest {
    @Test fun reportPublicProviderCounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val watch = AndroidWatchNextProvider(context)
        val channels = AndroidTvChannelProvider(context)
        assertTrue("Grant READ_TV_LISTINGS to the selected test package before running the probe", watch.hasPermission())
        val items = watch.query()
        val rows = channels.channels()
        val report = JSONObject().put("watchNextRows", items.size).put("watchNextByPackage", JSONObject().apply {
            items.groupBy { it.optString("packageName") }.forEach { (name, records) ->
                put(name, JSONObject().put("count", records.size).put("eligible", records.count(WatchNextFeed::isUnfinished))
                    .put("positivePosition", records.count { it.optLong("playbackPosition") > 0 })
                    .put("types", JSONArray(records.map { it.optInt("watchNextType", -1) }.distinct())))
            }
        }).put("channels", JSONArray(rows.take(40).map { row ->
            val programs = channels.programs(row.optLong("id"))
            JSONObject().put("package", row.optString("packageName")).put("programs", programs.size)
                .put("eligible", programs.count(WatchNextFeed::isUnfinished))
                .put("hasPosition", programs.count { !it.isNull("playbackPosition") })
                .put("positivePosition", programs.count { it.optLong("playbackPosition") > 0 })
                .apply { if (youtubePublisher(row.optString("packageName"))) {
                    put("channelName", row.optString("name")); put("channelKind", row.optString("internalId"))
                } }
        }))
        File(context.filesDir, "tv-feed-counts.json").writeText(report.toString(2))
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("stream", "\nOE_FEED_COUNTS " + report.toString() + "\n")
        })
    }
}
