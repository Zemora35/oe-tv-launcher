package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.res.Resources
import android.os.Build
import nl.neerdael.backdrop_launcher.R
import java.util.Locale

// Conservative detection of English sentences, not names. No title or user history leaves the TV.
private val englishWords = setOf("the", "a", "an", "and", "of", "to", "is", "are", "in", "with", "for",
    "his", "her", "their", "he", "she", "they", "who", "when", "from", "after", "has", "have", "that", "as")
private val turkishWords = setOf("ve", "bir", "bu", "için", "ile", "olan", "daha", "sonra", "gibi", "en", "ama")
internal fun isEnglishSynopsis(text: String): Boolean {
    val words = Regex("[\\p{L}]+").findAll(text.lowercase(Locale.ROOT)).map { it.value }.toList()
    if (words.isEmpty() || words.count { it in turkishWords } >= 2) return false
    val english = words.count { it in englishWords }
    return english >= 4 && words.filter { it in englishWords }.distinct().size >= 3 && english * 7 >= words.size
}

internal fun localizedFeaturedSynopsis(title: String, text: String, resources: Resources): String {
    @Suppress("DEPRECATION")
    val language = if (Build.VERSION.SDK_INT >= 24) resources.configuration.locales[0].language else resources.configuration.locale.language
    if (language == "en" || text.isBlank() || !isEnglishSynopsis(text)) return text
    // Exact series titles only; these are original short summaries, not automatic translations.
    val normalized = title.lowercase(Locale.ROOT).replace('’', '\'').trim()
    if (normalized in setOf("frieren", "frieren: beyond journey's end"))
        return resources.getString(R.string.vortex_synopsis_frieren)
    return resources.getString(R.string.vortex_synopsis_unavailable)
}
