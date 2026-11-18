package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import nl.neerdael.backdrop_launcher.core.HomeData
import nl.neerdael.backdrop_launcher.core.ResumeItem

internal const val FEATURED_INTERVAL_MILLIS = 30_000L

internal data class FeaturedChoice(val key: String, val programme: ResumeItem? = null, val backdrop: String? = null)

/** Only genuine provider artwork gets a provider title/action. Offline pictures have their own metadata. */
internal fun featuredChoices(home: HomeData): List<FeaturedChoice> {
    val programmes = (home.scene.watchNext + home.recommendations +
        home.scene.rows.flatMap { row -> row.apps.mapNotNull { it.program } })
        .filter { !it.image.isNullOrBlank() }.distinctBy(::contentKey).take(36)
    if (programmes.isNotEmpty()) return programmes.map { FeaturedChoice(contentKey(it), programme = it) }
    return (listOf(home.scene.backdrop) + bundledBackdropMetadata.keys).distinct()
        .map { FeaturedChoice("backdrop:$it", backdrop = it) }
}

internal fun nextFeaturedKey(keys: List<String>, current: String?): String? =
    if (keys.isEmpty()) null else keys[(keys.indexOf(current).takeIf { it >= 0 }?.plus(1) ?: 0) % keys.size]

internal class FeaturedRotation(val key: String?, val next: () -> Unit)

/** One sleeping coroutine, no frame timer or network polling. Resuming always starts a full interval. */
@Composable
internal fun rememberFeaturedRotation(keys: List<String>, enabled: Boolean,
    intervalMillis: Long = FEATURED_INTERVAL_MILLIS): FeaturedRotation {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val current = selected?.takeIf { it in keys } ?: keys.firstOrNull()
    val advance = { selected = nextFeaturedKey(keys, current) }
    LaunchedEffect(keys, current, enabled) {
        if (enabled && keys.size > 1) {
            delay(intervalMillis)
            advance()
        }
    }
    return FeaturedRotation(current, advance)
}
