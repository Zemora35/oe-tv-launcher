package nl.neerdael.backdrop_launcher.ui.cinematic

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.runtime.Immutable
import java.text.Collator
import java.text.DateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.LauncherApp

@Immutable
internal data class AppFacts(val category: Int?, val system: Boolean)

/** Package metadata is read off the UI thread and reused until a package version changes. */
internal class AppCatalogMetadata(context: Context) {
    private val manager = context.applicationContext.packageManager
    private val cache = mutableMapOf<Pair<String, String>, AppFacts>()
    @Synchronized fun read(apps: List<LauncherApp>): Map<String, AppFacts> {
        val keys = apps.map { it.packageName to it.version }.toSet()
        cache.keys.retainAll(keys)
        return apps.associate { app ->
            app.packageName to cache.getOrPut(app.packageName to app.version) {
                runCatching {
                    @Suppress("DEPRECATION") val info = manager.getApplicationInfo(app.packageName, 0)
                    AppFacts(if (Build.VERSION.SDK_INT >= 26) info.category else null,
                        info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                }.getOrDefault(AppFacts(null, false))
            }
        }
    }
}

@Immutable
internal data class AppCatalogEntry(val app: LauncherApp, val category: AppCategory, val visited: Boolean,
    val visitedAtMillis: Long?, val legacyRank: Int, val searchKey: String)

private val combiningMarks = Regex("\\p{M}+")
internal fun appSearchKey(text: String): String = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFKD)
    .replace(combiningMarks, "").replace('ı', 'i')

internal enum class AppCategory(val labelRes: Int) {
    ALL(R.string.vortex_grid_category_all), STREAMING(R.string.vortex_grid_category_streaming),
    MEDIA(R.string.vortex_grid_category_media), TOOLS(R.string.vortex_grid_category_tools), SYSTEM(R.string.vortex_grid_category_system)
}
internal enum class AppTypeFilter(val labelRes: Int) {
    ALL(R.string.vortex_grid_type_all), TV(R.string.vortex_grid_type_tv), ANDROID(R.string.vortex_grid_type_android)
}
internal enum class AppGridSort(val labelRes: Int) {
    NAME_ASC(R.string.vortex_grid_sort_az), NAME_DESC(R.string.vortex_grid_sort_za), RECENT(R.string.vortex_grid_sort_recent)
}
internal fun appCategory(app: LauncherApp, facts: AppFacts? = null): AppCategory {
    val id = (app.packageName + " " + app.label).lowercase(Locale.ROOT)
    if (listOf("settings", "ayarlar", "vending", "permissioncontroller", "systemui").any(id::contains)) return AppCategory.SYSTEM
    if (listOf("spotify", "music", "müzik", "photo", "gallery", "fotoğraf", "soundcloud", "steam").any(id::contains)) return AppCategory.MEDIA
    if (listOf("netflix", "youtube", "amazonvideo", "prime video", "disney", "plex", "kodi", "vlc", "twitch",
        "blutv", "exxen", "puhutv", "mubi", "crunchyroll", "tubi", "pluto", "paramount", "wuplay",
        "appletv", "apple.atve", "f1tv", "digiturk", "bein", "smarttube", "stremio", "nuvio", "com.hbo", "com.wbd").any(id::contains)) return AppCategory.STREAMING
    if (listOf("browser", "chrome", "tarayıcı", "file", "dosya", "vpn", "mirroring", "yansıt").any(id::contains)) return AppCategory.TOOLS
    return when (facts?.category) {
        2 -> AppCategory.STREAMING // ApplicationInfo.CATEGORY_VIDEO
        0, 1, 3 -> AppCategory.MEDIA // GAME, AUDIO, IMAGE
        else -> if (facts?.system == true) AppCategory.SYSTEM else AppCategory.TOOLS
    }
}

internal fun buildAppCatalog(apps: List<LauncherApp>, legacyHistory: List<String>, facts: Map<String, AppFacts>): List<AppCatalogEntry> {
    val legacyOrder = legacyHistory.withIndex().associate { it.value to it.index }
    return apps.asSequence().filterNot { it.hidden }.distinctBy { it.packageName }.map { app ->
        AppCatalogEntry(app, appCategory(app, facts[app.packageName]),
            (app.lastLaunchedAt ?: 0) > 0 || app.packageName in legacyOrder,
            app.lastLaunchedAt?.takeIf { it > 0 }, legacyOrder[app.packageName] ?: Int.MAX_VALUE,
            appSearchKey(app.label + " " + app.packageName))
    }.toList()
}

/** Sort once per dataset/sort change; search and category changes only filter this result. */
internal fun orderAppCatalog(entries: List<AppCatalogEntry>, history: Boolean, sort: AppGridSort, locale: Locale): List<AppCatalogEntry> {
    val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
    val names = Comparator<AppCatalogEntry> { a, b ->
        val labelOrder = collator.compare(a.app.label, b.app.label)
        if (labelOrder != 0) labelOrder else a.app.packageName.compareTo(b.app.packageName)
    }
    val comparator = when (sort) {
        AppGridSort.NAME_ASC -> names
        AppGridSort.NAME_DESC -> Comparator { a, b -> names.compare(b, a) }
        AppGridSort.RECENT -> compareByDescending<AppCatalogEntry> { it.visitedAtMillis ?: Long.MIN_VALUE }
            .thenBy { it.legacyRank }.then(names)
    }
    return entries.filter { !history || it.visited }.sortedWith(comparator)
}

internal fun filterAppCatalog(entries: List<AppCatalogEntry>, query: String, category: AppCategory, type: AppTypeFilter): List<AppCatalogEntry> {
    val needle = appSearchKey(query.trim())
    return entries.filter {
        (category == AppCategory.ALL || it.category == category) &&
            (type == AppTypeFilter.ALL || it.app.androidApp == (type == AppTypeFilter.ANDROID)) &&
            (needle.isEmpty() || it.searchKey.contains(needle))
    }
}

internal fun visitDateFormatter(locale: Locale, zone: TimeZone): DateFormat =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).apply { timeZone = zone }

internal fun formatVisit(timestamp: Long?, formatter: DateFormat): String? =
    timestamp?.takeIf { it > 0 }?.let { formatter.format(Date(it)) }
