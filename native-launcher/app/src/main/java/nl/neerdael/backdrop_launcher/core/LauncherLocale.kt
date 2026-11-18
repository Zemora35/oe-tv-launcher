package nl.neerdael.backdrop_launcher.core

import android.content.Context
import android.os.Build
import java.util.Locale

/** Read the app's effective locale, including Android 13+ per-app language settings. */
@Suppress("DEPRECATION")
internal fun Context.launcherLocale(): Locale =
    if (Build.VERSION.SDK_INT >= 24) resources.configuration.locales[0] else resources.configuration.locale
