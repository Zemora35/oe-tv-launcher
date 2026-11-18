package nl.neerdael.backdrop_launcher.ui.cinematic

import java.util.Locale
import java.util.TimeZone
import nl.neerdael.backdrop_launcher.core.LauncherApp
import org.junit.Assert.*
import org.junit.Test

class AppCatalogTest {
    private fun app(id: String, label: String = id, time: Long? = null, android: Boolean = false, hidden: Boolean = false) =
        LauncherApp(id, label, "1", android, hidden, time, null)

    @Test fun historyUsesSuccessfulLaunchTimestampsAndNeverFabricatesLegacyDates() {
        val entries = buildAppCatalog(listOf(app("older", time = 1_700_000_000_000),
            app("newer", time = 1_800_000_000_000), app("legacy"), app("never"), app("hidden", hidden = true)),
            listOf("uninstalled", "legacy", "hidden"), emptyMap())
        val history = orderAppCatalog(entries, true, AppGridSort.RECENT, Locale.US)
        assertEquals(listOf("newer", "older", "legacy"), history.map { it.app.packageName })
        assertEquals(1_800_000_000_000, history.first().visitedAtMillis)
        assertNull(history.last().visitedAtMillis)
        assertNull(formatVisit(history.last().visitedAtMillis, visitDateFormatter(Locale.US, TimeZone.getTimeZone("UTC"))))
        assertEquals(4, entries.size)
    }

    @Test fun searchHandlesTurkishCharactersAccentsAndPackageNamesWithCombinedFilters() {
        val entries = buildAppCatalog(listOf(app("org.photo", "Fotoğraflar"), app("org.music", "Müzik", android = true),
            app("org.browser", "İnternet Tarayıcısı"), app("org.netflix", "Netflix")), emptyList(), emptyMap())
        fun find(query: String, category: AppCategory = AppCategory.ALL, type: AppTypeFilter = AppTypeFilter.ALL) =
            filterAppCatalog(entries, query, category, type).map { it.app.packageName }
        assertEquals(listOf("org.photo"), find(" fotograflar "))
        assertEquals(listOf("org.browser"), find("internet tarayicisi"))
        assertEquals(listOf("org.music"), find("MUZIK", AppCategory.MEDIA, AppTypeFilter.ANDROID))
        assertTrue(find("MUZIK", AppCategory.MEDIA, AppTypeFilter.TV).isEmpty())
        assertEquals(listOf("org.netflix"), find("org.netflix", AppCategory.STREAMING))
    }

    @Test fun recognizablePreinstalledAppsKeepTheirCategoryAndUnknownAppsUseManifest() {
        assertEquals(AppCategory.STREAMING, appCategory(app("com.netflix.ninja"), AppFacts(null, true)))
        assertEquals(AppCategory.MEDIA, appCategory(app("com.spotify.tv.android"), AppFacts(null, true)))
        assertEquals(AppCategory.SYSTEM, appCategory(app("com.android.settings")))
        assertEquals(AppCategory.MEDIA, appCategory(app("game.unknown"), AppFacts(0, false)))
        assertEquals(AppCategory.STREAMING, appCategory(app("video.unknown"), AppFacts(2, false)))
        assertEquals(AppCategory.SYSTEM, appCategory(app("system.unknown"), AppFacts(null, true)))
    }

    @Test fun namesSortWithLocaleAndVisitDatesUseMillisecondsAndLocalTimeZone() {
        val apps = buildAppCatalog(listOf(app("b", "B"), app("z", "Z"), app("a", "A"), app("a", "Duplicate")), emptyList(), emptyMap())
        assertEquals(listOf("a", "b", "z"), orderAppCatalog(apps, false, AppGridSort.NAME_ASC, Locale.US).map { it.app.packageName })
        assertEquals(listOf("z", "b", "a"), orderAppCatalog(apps, false, AppGridSort.NAME_DESC, Locale.US).map { it.app.packageName })
        val formatter = visitDateFormatter(Locale.forLanguageTag("tr-TR"), TimeZone.getTimeZone("GMT+03:00"))
        val text = formatVisit(1_700_000_000_000, formatter)!!
        assertTrue(text, text.contains("2023"))
        assertTrue(text, text.contains("01:13"))
        assertNull(formatVisit(0, formatter))
    }
}
