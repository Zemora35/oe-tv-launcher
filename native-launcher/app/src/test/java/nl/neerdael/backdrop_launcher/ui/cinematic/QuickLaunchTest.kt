package nl.neerdael.backdrop_launcher.ui.cinematic

import nl.neerdael.backdrop_launcher.core.LauncherApp
import org.junit.Assert.*
import org.junit.Test

class QuickLaunchTest {
    private fun app(id: String, hidden: Boolean = false) = LauncherApp(id, id, "1", true, hidden, null, "package:$id")

    @Test fun defaultsContainOnlyAvailableAppsAndPreferRecognizableVideoApps() {
        val installed = (0..10).map { app("app.$it") } + app("com.netflix.ninja") + app("com.google.android.youtube.tv") +
            app("hidden", true) + app("self")
        val result = quickLaunchApps(installed, null, "self")
        assertEquals(6, result.size)
        assertEquals(listOf("com.google.android.youtube.tv", "com.netflix.ninja"), result.take(2).map { it.packageName })
        assertFalse(result.any { it.packageName in setOf("hidden", "self") })
        assertTrue(result.all { tile -> installed.any { it.packageName == tile.packageName } })
    }

    @Test fun savedOrderSurvivesInventoryReorderingAndFiltersHiddenMissingDuplicateAndOwnEntries() {
        val installed = listOf(app("b"), app("self"), app("a"), app("hidden", true), app("b"))
        val chosen = listOf("a", "missing", "b", "a", "self", "hidden")
        assertEquals(listOf("a", "b"), quickLaunchApps(installed, chosen, "self").map { it.packageName })
        assertEquals(listOf("a", "b"), quickLaunchApps(installed.reversed(), chosen, "self").map { it.packageName })
        assertTrue(quickLaunchApps(installed, emptyList(), "self").isEmpty())
    }

    @Test fun sixItemLimitAndRemoveThenAddDefineTheOrderWithoutDuplicates() {
        val six = (0..5).map { "app.$it" }
        assertEquals(six, toggleQuickLaunch(six, "extra"))
        val removed = toggleQuickLaunch(six, "app.2")
        assertEquals(5, removed.size)
        val moved = toggleQuickLaunch(removed, "app.2")
        assertEquals("app.2", moved.last())
        assertEquals(6, moved.toSet().size)
        assertEquals(listOf("one"), toggleQuickLaunch(emptyList(), "one"))
        assertTrue(toggleQuickLaunch(listOf("one"), "one").isEmpty())
    }
}
