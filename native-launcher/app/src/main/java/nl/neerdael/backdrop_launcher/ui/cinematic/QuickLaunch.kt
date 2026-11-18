package nl.neerdael.backdrop_launcher.ui.cinematic

import nl.neerdael.backdrop_launcher.core.AppTile
import nl.neerdael.backdrop_launcher.core.LauncherApp
import nl.neerdael.backdrop_launcher.core.PreferenceAccess

internal const val QUICK_LAUNCH_LIMIT = 6
internal const val QUICK_LAUNCH_KEY = "native.vortex.quick_launch.apps"
private const val UNCONFIGURED = "vortex:unconfigured"
private val preferredPackages = listOf("com.google.android.youtube.tv", "com.netflix.ninja",
    "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus", "com.spotify.tv.android", "com.plexapp.android")

/** An explicitly saved empty selection stays empty; it must not re-seed on restart. */
internal fun readQuickLaunch(prefs: PreferenceAccess): List<String>? =
    prefs.strings(QUICK_LAUNCH_KEY, listOf(UNCONFIGURED)).takeUnless { it == listOf(UNCONFIGURED) }

internal fun quickLaunchApps(installed: List<LauncherApp>, selection: List<String>?, selfPackage: String): List<AppTile> {
    val available = installed.filter { !it.hidden && it.packageName != selfPackage }.distinctBy { it.packageName }
    val byPackage = available.associateBy { it.packageName }
    val packages = selection ?: (preferredPackages.filter { it in byPackage } +
        available.sortedBy { appSearchKey(it.label) }.map { it.packageName })
    return packages.distinct().mapNotNull(byPackage::get).take(QUICK_LAUNCH_LIMIT)
        .map { AppTile(it.packageName, it.label, it.artwork) }
}

/** Selection order is display order. Remove then select again to move an app to the end. */
internal fun toggleQuickLaunch(selection: List<String>, packageName: String): List<String> = when {
    packageName in selection -> selection - packageName
    selection.size < QUICK_LAUNCH_LIMIT -> selection + packageName
    else -> selection
}
