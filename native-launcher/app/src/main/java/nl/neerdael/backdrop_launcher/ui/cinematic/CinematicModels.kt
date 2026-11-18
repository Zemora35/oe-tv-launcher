package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import android.content.res.Resources
import android.content.Context
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.AppTile
import nl.neerdael.backdrop_launcher.core.ResumeItem

@Immutable
internal data class FeaturedContent(
    val title: String,
    val description: String,
    val artwork: String?,
    val badges: List<String> = emptyList(),
    val playable: ResumeItem? = null,
    val platform: String? = null,
)

internal enum class HomeTab(val labelRes: Int) {
    HOME(R.string.vortex_nav_home), DISCOVER(R.string.vortex_nav_discover),
    MY_LIST(R.string.vortex_nav_my_list), SEARCH(R.string.vortex_nav_search)
}
internal enum class Shortcut(val labelRes: Int) {
    MY_LIST(R.string.vortex_nav_my_list), HISTORY(R.string.vortex_history),
    DOWNLOADS(R.string.vortex_downloads), ALL_APPS(R.string.vortex_all_apps)
}
/** Routing never depends on translated display text. */
internal enum class CinemaPanel(val titleRes: Int) {
    INFO(R.string.vortex_info), DISCOVER(R.string.vortex_nav_discover), MY_LIST(R.string.vortex_nav_my_list),
    SEARCH(R.string.vortex_nav_search), HISTORY(R.string.vortex_history), ALL_APPS(R.string.vortex_all_apps),
    QUICK_LAUNCH(R.string.vortex_quick_launch)
}

internal const val SETTINGS_APP = "vortex:settings"
internal val settingsTile = AppTile(SETTINGS_APP, "Settings", null)

internal fun contentKey(item: ResumeItem): String =
    if (!item.continuation) "preview:${item.packageName}:${item.id}:${item.contentId ?: item.intentUri ?: item.title}"
    else if (item.id != 0L) "id:${item.id}" else "${item.packageName}:${item.contentId ?: item.title}"

internal fun isContentSaved(item: ResumeItem, saved: Set<String>) = contentKey(item) in saved ||
    (!item.continuation && item.id != 0L && "id:${item.id}" in saved)

internal fun featuredContent(item: ResumeItem, resources: Resources) = FeaturedContent(
    item.title, localizedFeaturedSynopsis(item.title, item.description, resources).ifBlank { if (item.continuation) resources.getString(R.string.vortex_resume_description)
        else resources.getString(R.string.vortex_content_recommendation_description, item.sourceName ?: item.packageName.orEmpty()) }, item.image,
    if (item.continuation) listOf(resources.getString(R.string.vortex_continue_watching)) +
        if (item.progress > 0) listOf(resources.getString(R.string.vortex_watched_progress, item.progress.coerceIn(0, 100))) else emptyList()
    else emptyList(), item.takeIf { !it.intentUri.isNullOrBlank() && !it.packageName.isNullOrBlank() }, item.sourceName,
)

/** Sample data is opt-in in previews; never merged into the installed-app inventory. */
internal object CinematicSamples {
    val featured = FeaturedContent("INTERSTELLAR", "A journey beyond the familiar.",
        "backdrops/interstellar-544bb9af46801.jpg", listOf("SAMPLE", "4K", "Science fiction", "2h 49m"))
    val apps = listOf("Netflix", "YouTube", "Prime Video", "Disney+", "Movies", "Live TV", "Games", "Browser", "Screen Mirroring", "HDMI", "Spotify", "Plex", "Kodi", "Photos", "Music")
        .mapIndexed { index, name -> AppTile("sample:$index", name,
            listOf("netflix", "youtube", "amazonprime", "disneyplus", "movies", null, "steam", "chrome", null, null, "spotify", "plex", "kodi", null, null)[index]
                ?.let { "appicons/$it.png" }) } + settingsTile
    val watching = listOf(
        ResumeItem("Interstellar", "Sample · 48 min remaining", featured.artwork, 68, null, null, 1),
        ResumeItem("Inception", "Sample · 1h 12m remaining", "backdrops/inception-52eb41b2ab30f.jpg", 42, null, null, 2),
        ResumeItem("Matrix", "Sample · Chapter 8", "backdrops/the-matrix-553be0c3bdeeb.jpg", 35, null, null, 3),
    )
    val shortcuts = Shortcut.entries
}

internal data class SampleContent(val featured: FeaturedContent, val apps: List<AppTile>, val watching: List<ResumeItem>)

@Composable
internal fun localizedSamples(): SampleContent {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    return remember(resources, configuration) {
        val labels = mapOf(4 to R.string.vortex_movies, 5 to R.string.vortex_live_tv, 6 to R.string.vortex_games,
            7 to R.string.vortex_browser, 8 to R.string.vortex_mirroring, 13 to R.string.vortex_photos,
            14 to R.string.vortex_music, 15 to R.string.vortex_settings)
        SampleContent(CinematicSamples.featured.copy(title = resources.getString(R.string.vortex_sample_title),
            description = resources.getString(R.string.vortex_sample_description),
            badges = listOf(resources.getString(R.string.vortex_sample_badge), "4K",
                resources.getString(R.string.vortex_sample_genre), resources.getString(R.string.vortex_sample_duration))),
            CinematicSamples.apps.mapIndexed { index, app -> labels[index]?.let { app.copy(label = resources.getString(it)) } ?: app },
            CinematicSamples.watching.mapIndexed { index, item -> item.copy(
                title = when (index) { 0 -> resources.getString(R.string.vortex_sample_title); 1 -> resources.getString(R.string.vortex_sample_inception); else -> item.title },
                description = resources.getString(listOf(R.string.vortex_sample_resume_first, R.string.vortex_sample_resume_second, R.string.vortex_sample_resume_third)[index])) })
    }
}
