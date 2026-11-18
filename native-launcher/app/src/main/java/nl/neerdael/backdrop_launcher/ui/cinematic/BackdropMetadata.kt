package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.res.Resources
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.HomeData
import nl.neerdael.backdrop_launcher.core.ResumeItem
import java.net.URLEncoder

/** Bundled image identity is explicit: no filename guessing, screen scraping or network lookup. */
internal val bundledBackdropMetadata = mapOf(
    "backdrops/fight-club-55e2393686745.jpg" to (R.string.vortex_film_fight_club_title to R.string.vortex_film_fight_club_description),
    "backdrops/forrest-gump-55e4e70827c69.jpg" to (R.string.vortex_film_forrest_gump_title to R.string.vortex_film_forrest_gump_description),
    "backdrops/inception-52eb41b2ab30f.jpg" to (R.string.vortex_film_inception_title to R.string.vortex_film_inception_description),
    "backdrops/interstellar-544bb9af46801.jpg" to (R.string.vortex_film_interstellar_title to R.string.vortex_film_interstellar_description),
    "backdrops/pulp-fiction-526d50098865c.jpg" to (R.string.vortex_film_pulp_fiction_title to R.string.vortex_film_pulp_fiction_description),
    "backdrops/the-dark-knight-58fa275637ae0.jpg" to (R.string.vortex_film_dark_knight_title to R.string.vortex_film_dark_knight_description),
    "backdrops/the-godfather-505714161422a.jpg" to (R.string.vortex_film_godfather_title to R.string.vortex_film_godfather_description),
    "backdrops/the-lord-of-the-rings-the-return-of-the-king-51121045c2707.jpg" to (R.string.vortex_film_lotr_title to R.string.vortex_film_lotr_description),
    "backdrops/the-matrix-553be0c3bdeeb.jpg" to (R.string.vortex_film_matrix_title to R.string.vortex_film_matrix_description),
    "backdrops/the-shawshank-redemption-50584de200d3c.jpg" to (R.string.vortex_film_shawshank_title to R.string.vortex_film_shawshank_description)
)

internal fun resolveFeaturedContent(home: HomeData, programme: ResumeItem?, resources: Resources,
    backdrop: String = home.scene.backdrop): FeaturedContent {
    // Never put one programme's name over an unrelated wallpaper when its artwork is missing.
    programme?.takeIf { !it.image.isNullOrBlank() }?.let { return featuredContent(it, resources) }
    val image = backdrop
    val local = bundledBackdropMetadata[image]
    val title = local?.let { resources.getString(it.first) }
        ?: home.backdropTitle?.takeIf { image == home.scene.backdrop && it.isNotBlank() }
        ?: resources.getString(R.string.vortex_backdrop_unknown_title)
    val description = local?.let { resources.getString(it.second) }
        ?: home.backdropDescription?.takeIf { image == home.scene.backdrop && it.isNotBlank() }
        ?: resources.getString(R.string.vortex_backdrop_unknown_description)
    return FeaturedContent(title, localizedFeaturedSynopsis(title, description, resources), image)
}

/** Rounded up, without overflowing on malformed provider durations. */
internal fun remainingMinutes(item: ResumeItem): Int? {
    val duration = item.durationMillis ?: return null
    val position = item.playbackPositionMillis ?: return null
    if (position < 0 || duration <= position) return null
    val left = duration - position
    return (left / 60_000 + if (left % 60_000 > 0) 1 else 0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

/** A lookup, never a claim that a bundled poster is available from a particular provider. */
internal fun watchOptionsUrl(title: String, language: String): String {
    val path = if (language == "tr") "tr/arama" else "us/search"
    return "https://www.justwatch.com/$path?q=${URLEncoder.encode(title, "UTF-8")}"
}
