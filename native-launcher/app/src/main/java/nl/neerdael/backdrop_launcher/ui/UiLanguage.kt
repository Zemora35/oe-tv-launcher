package nl.neerdael.backdrop_launcher.ui

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import org.json.JSONObject
import java.util.Locale
import nl.neerdael.backdrop_launcher.core.launcherLocale

/** BCP-47 region/script first, base language next, then English; future ARB packs need no whitelist change. */
internal fun languageCandidates(tag: String): List<String> {
    val locale = Locale.forLanguageTag(tag.replace('_', '-'))
    return listOf(locale.toLanguageTag().replace('-', '_'), locale.language, "en")
        .filter { it.matches(Regex("[A-Za-z]{2,3}(?:_[A-Za-z0-9]{2,8})*")) }.distinct()
}

internal val LocalUiLanguage = staticCompositionLocalOf<Map<String, String>> { emptyMap() }

/** Reuse the shipped language packs; never translate application/row/catalogue names. */
internal fun loadUiLanguage(context: Context, language: String): Map<String, String> {
    val requested = Locale.forLanguageTag(language.replace('_', '-'))
    val localizedContext = if (context.launcherLocale().language == requested.language) context else
        context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply { setLocale(requested) })
    val native = nativeUiLanguage(localizedContext)
    val legacy = runCatching {
        val english = context.assets.open("l10n/app_en.arb").bufferedReader().use { JSONObject(it.readText()) }
        val available = context.assets.list("l10n").orEmpty().toSet()
        val file = languageCandidates(language).map { "app_$it.arb" }.firstOrNull { it in available } ?: "app_en.arb"
        val target = context.assets.open("l10n/$file").bufferedReader().use { JSONObject(it.readText()) }
        buildMap {
            english.keys().forEach { key ->
                if (!key.startsWith("@") && english.opt(key) is String && target.opt(key) is String) {
                    val source = english.getString(key)
                    if ('{' !in source) put(source, target.getString(key))
                }
            }
            val aliases = mapOf("Continue Watching" to "watchNextSectionTitle", "Show Continue Watching" to "showWatchNextSection",
                "Big picture / hero mode" to "watchNextBigPictureMode", "Hide items without images" to "watchNextHideWithoutImages",
                "Maximum items" to "watchNextMaxItems", "Maximum Continue Watching items" to "watchNextMaxItems",
                "Grant TV listings permission" to "watchNextGrantPermission", "Refresh Continue Watching" to "watchNextRefresh",
                "App info" to "appInfo", "App key click sound" to "appKeyClick", "Animated focus border" to "appCardHighlightAnimation",
                "Show app names below icons" to "showAppNamesBelowIcons", "Auto-hide status bar" to "autoHideAppBar",
                "Show date" to "date", "Show time" to "time", "Show network indicator" to "networkIndicator",
                "Date and time formats" to "dateAndTimeFormat", "Screensaver clock style" to "screensaverClockStyle",
                "Android screensaver settings" to "screensaverSettings", "Start screensaver" to "dialogOptionBackButtonActionShowScreensaver",
                "Choose packaged icon / banner" to "setCustomBanner", "Restore application artwork" to "clearCustomBanner")
            aliases.forEach { (label, key) -> if (target.opt(key) is String) put(label, target.getString(key)) }
        }
    }.getOrDefault(emptyMap())
    return ResourceUiLanguage(buildMap {
        putAll(legacy)
        native.forEach { (source, target) -> if (source != target || source !in legacy) put(source, target) }
    }, localizedContext.resources)
}
