package nl.neerdael.backdrop_launcher.ui

import android.content.Context
import android.content.res.Resources
import java.util.Locale
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.launcherLocale

/** Android selects regional resources and per-key English fallback; legacy settings retain their storage keys. */
internal class ResourceUiLanguage(private val labels: Map<String, String>, val resources: Resources) : Map<String, String> by labels

internal fun nativeUiLanguage(context: Context): ResourceUiLanguage {
    val sources = context.resources.getStringArray(R.array.vortex_legacy_sources)
    val ids = context.resources.obtainTypedArray(R.array.vortex_legacy_ids)
    return try {
        ResourceUiLanguage(buildMap {
            sources.forEachIndexed { index, source -> put(source, context.getString(ids.getResourceId(index, 0))) }
            put("__locale", context.launcherLocale().toLanguageTag())
        }, context.resources)
    } finally { ids.recycle() }
}

private val dynamicUiMessages = listOf(
    Regex("Reorder in (.+)") to "Reorder in %1\$s",
    Regex("Remove from (.+)") to "Remove from %1\$s",
    Regex("Rows for (.+)") to "Rows for %1\$s",
    Regex("Delete (.+)\\?") to "Delete %1\$s?",
    Regex("Remove (.+)\\?") to "Remove %1\$s?",
    Regex("(\\d+) minutes") to "%1\$d minutes",
    Regex("(\\d+) min") to "%1\$d min",
    Regex("(\\d+) items") to "%1\$d items",
    Regex("Install update (.+)") to "Install update %1\$s",
    Regex("Download (.+)") to "Download %1\$s",
    Regex("Latest release: (.+)") to "Latest release: %1\$s",
)
private val appCount = Regex("(\\d+) apps")

internal fun uiText(source: String, labels: Map<String, String>): String {
    labels[source]?.let { return it }
    if (labels.isEmpty()) return source
    val locale = labels["__locale"]?.let(Locale::forLanguageTag) ?: Locale.ENGLISH
    for ((pattern, template) in dynamicUiMessages) pattern.matchEntire(source)?.let { match ->
        val argument: Any = if (template.contains("\$d")) match.groupValues[1].toIntOrNull() ?: return source else match.groupValues[1]
        return String.format(locale, labels[template] ?: template, argument)
    }
    return source.split(" · ").joinToString(" · ") { fragment ->
        val count = appCount.matchEntire(fragment)?.groupValues?.get(1)?.toIntOrNull()
        when {
            count != null && labels is ResourceUiLanguage -> labels.resources.getQuantityString(R.plurals.vortex_app_count, count, count)
            labels.containsKey(fragment) -> labels.getValue(fragment)
            else -> fragment
        }
    }
}

/** Source 1/2 identify generated rows. A custom row with the same text is still user content. */
internal fun defaultRowLabelRes(name: String, source: Int): Int? = when {
    source == 1 && name == "TV Apps" -> R.string.vortex_tv_apps
    source == 2 && name == "Android Apps" -> R.string.vortex_android_apps
    else -> null
}
