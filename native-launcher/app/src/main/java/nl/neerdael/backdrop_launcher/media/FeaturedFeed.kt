package nl.neerdael.backdrop_launcher.media

import nl.neerdael.backdrop_launcher.core.ResumeItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Provider-owned recommendations, kept separate from evidence of unfinished playback. */
internal class FeaturedFeed(private val provider: TvChannelProvider, private val disk: MediaDisk,
    private val now: () -> Long = System::currentTimeMillis) {
    private var raw = emptyList<JSONObject>()
    private var loadedAt = 0L

    @Synchronized fun refresh(): List<ResumeItem> {
        if (!provider.hasPermission()) { raw = emptyList(); return emptyList() }
        if (raw.isEmpty()) disk.readJson("featured", "feed")?.optJSONArray("items")?.let { rows ->
            raw = (0 until minOf(rows.length(), 36)).mapNotNull(rows::optJSONObject)
        }
        if (loadedAt == 0L || now() - loadedAt > 5 * 60_000) try {
            val channels = provider.channels().filter { videoPublisher(it.optString("packageName")) }
                .sortedBy { if (youtubePublisher(it.optString("packageName"))) 0 else 1 }.take(12)
            val queried = channels.flatMap { channel ->
                ensureMediaActive()
                val owner = channel.optString("packageName")
                provider.programs(channel.optLong("id")).filter {
                    it.optInt("browsable", 1) != 0 && !it.textOrNull("title").isNullOrBlank() &&
                        !it.textOrNull("intentUri").isNullOrBlank() &&
                        (it.textOrNull("packageName") ?: owner) == owner
                }.sortedBy { if (WatchNextFeed.isUnfinished(it)) 0 else 1 }.take(6).map { row -> JSONObject(row.toString()).put("packageName", owner)
                    .put("sourceName", channel.textOrNull("appName") ?: owner) }.toList()
            }.distinctBy { it.optString("packageName") + "|" + it.optString("intentUri") }
                .sortedBy { if (youtubePublisher(it.optString("packageName"))) 1 else 0 }.take(36)
            raw = queried
            loadedAt = now()
            disk.writeJson("featured", "feed", JSONObject().put("items", JSONArray(raw)))
        } catch (_: SecurityException) { raw = emptyList() }
        catch (_: Exception) { ensureMediaActive() /* Retain the last successful public feed. */ }
        return raw.filter { artwork(it) != null }.map { item(it, false) }
    }

    /** Some TV apps publish real playback positions in preview channels, not Watch Next. */
    @Synchronized fun youtubeResume(): List<ResumeItem> = if (!provider.hasPermission()) emptyList() else raw.filter {
        youtubePublisher(it.optString("packageName")) && WatchNextFeed.isUnfinished(it)
    }.map { item(it, true) }

    private fun item(row: JSONObject, continuation: Boolean) = ResumeItem(row.optString("title"),
        row.textOrNull("description").orEmpty(), artwork(row), WatchNextFeed.progress(row),
        row.textOrNull("intentUri"), row.textOrNull("packageName"), row.optLong("id"), row.textOrNull("contentId"),
        row.optLong("duration").takeIf { it > 0 }, row.optLong("playbackPosition").takeIf { it > 0 },
        row.textOrNull("sourceName"), continuation)

    private fun artwork(row: JSONObject) = row.textOrNull("thumbnailUri") ?: row.textOrNull("posterUri")
}

internal fun youtubePublisher(name: String) = name == "com.google.android.youtube.tv" || name == "com.google.android.youtube"
internal fun videoPublisher(name: String) = youtubePublisher(name) || listOf("netflix", "amazonvideo", "disney", "hbo",
    "wbd", "plex", "kodi", "mubi", "blutv", "exxen", "puhu", "digiturk", "bein", "crunchyroll", "apple.atve",
    "tubitv", "pluto", "paramount", "google.android.videos", "stremio", "nuvio").any(name.lowercase(Locale.ROOT)::contains)
