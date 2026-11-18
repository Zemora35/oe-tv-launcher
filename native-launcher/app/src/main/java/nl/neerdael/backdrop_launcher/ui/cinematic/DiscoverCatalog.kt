package nl.neerdael.backdrop_launcher.ui.cinematic

import java.text.Collator
import java.util.Locale
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.ResumeItem

internal enum class DiscoverKind(val labelRes: Int) {
    ALL(R.string.vortex_grid_category_all), RECOMMENDED(R.string.vortex_discover_recommended), SAVED(R.string.vortex_nav_my_list)
}
internal enum class DiscoverSort(val labelRes: Int) {
    RECOMMENDED(R.string.vortex_discover_sort_default), NAME_ASC(R.string.vortex_grid_sort_az), NAME_DESC(R.string.vortex_grid_sort_za)
}
internal data class DiscoverSource(val key: String, val label: String?)
private const val UNKNOWN_SOURCE = "vortex:unknown-source"
internal fun discoverSourceKey(item: ResumeItem) = item.packageName?.takeIf { it.isNotBlank() } ?: UNKNOWN_SOURCE
internal fun discoverSources(items: List<ResumeItem>): List<DiscoverSource> = items.distinctBy(::discoverSourceKey).map {
    DiscoverSource(discoverSourceKey(it), it.sourceName?.takeIf { label -> label.isNotBlank() && label != it.packageName })
}

/** Identity and provider order are retained. Titles are never used as playback identifiers. */
internal fun orderDiscover(items: List<ResumeItem>, sort: DiscoverSort, locale: Locale): List<ResumeItem> {
    val unique = items.filter { it.title.isNotBlank() }.distinctBy(::contentKey)
    if (sort == DiscoverSort.RECOMMENDED) return unique
    val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
    val names = Comparator<ResumeItem> { a, b ->
        val order = collator.compare(a.title, b.title)
        if (order != 0) order else contentKey(a).compareTo(contentKey(b))
    }
    return unique.sortedWith(if (sort == DiscoverSort.NAME_DESC) Comparator { a, b -> names.compare(b, a) } else names)
}
internal fun filterDiscover(items: List<ResumeItem>, query: String, source: String?, kind: DiscoverKind,
    saved: Set<String>): List<ResumeItem> {
    val needle = appSearchKey(query.trim())
    return items.filter {
        (source == null || discoverSourceKey(it) == source) &&
            (kind != DiscoverKind.RECOMMENDED || !it.continuation) &&
            (kind != DiscoverKind.SAVED || isContentSaved(it, saved)) &&
            (needle.isEmpty() || appSearchKey(it.title + " " + it.sourceName.orEmpty()).contains(needle))
    }
}

internal class DiscoverGrid(val programmes: List<ResumeItem>, val saved: Set<String>,
    val onOpen: (ResumeItem) -> Unit, val onSave: (ResumeItem) -> Unit)
