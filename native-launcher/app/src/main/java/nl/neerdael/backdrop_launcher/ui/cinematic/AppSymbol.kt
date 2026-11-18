package nl.neerdael.backdrop_launcher.ui.cinematic

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import nl.neerdael.backdrop_launcher.core.AppTile
import nl.neerdael.backdrop_launcher.ui.ComposeArtwork

/** Shared real package icon policy and fixed decode budget for both home and app grids. */
@Composable
internal fun AppSymbol(app: AppTile, loader: ComposeArtwork?, active: Boolean, revision: Int, modifier: Modifier,
    fallbackSize: TextUnit = 28.sp) {
    val path = if (app.program != null || app.packageName.startsWith("sample:")) app.artwork
        else if (app.packageName == SETTINGS_APP) null
        else app.artwork?.takeUnless { it.startsWith("package:") } ?: "package-icon:${app.packageName}"
    val preview = LocalInspectionMode.current
    val icon by produceState<Bitmap?>(null, path, loader, active, revision) {
        value = null
        if (!preview && path != null && loader != null && active) {
            val unsubscribe = loader.observe(path, 144, 144, true) { value = it }
            awaitDispose { unsubscribe() }
        }
    }
    if (preview && path != null) CinematicArt(path, loader, active, revision, modifier, ContentScale.Fit, size = IntSize(144, 144))
    else icon?.let { Image(remember(it) { it.asImageBitmap() }, null, modifier, contentScale = ContentScale.Fit) }
        ?: Box(modifier, contentAlignment = Alignment.Center) {
            Text(fallbackSymbol(app), color = Color.White, fontSize = fallbackSize, lineHeight = fallbackSize,
                maxLines = 1, fontWeight = FontWeight.Medium)
        }
}
