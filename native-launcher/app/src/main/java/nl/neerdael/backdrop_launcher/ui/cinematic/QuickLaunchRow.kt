package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.AppTile
import nl.neerdael.backdrop_launcher.ui.ComposeArtwork

/** Six resident icons at most. Existing bounded icon loader and transform-only focus motion. */
@Composable
internal fun QuickLaunchRow(apps: List<AppTile>, focus: FocusRequester, onLaunch: (AppTile) -> Unit,
    onEdit: () -> Unit, onUp: () -> Unit, onRight: () -> Unit, modifier: Modifier = Modifier,
    loader: ComposeArtwork? = null, active: Boolean = true, revision: Int = 0,
    googleTvAvailable: Boolean = false, onGoogleTv: () -> Unit = {}, onFocus: () -> Unit = {}) {
    val forward = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Key.DirectionLeft else Key.DirectionRight
    val editFocus = remember { FocusRequester() }
    var lastPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = apps.take(QUICK_LAUNCH_LIMIT)
    val anchor = visible.firstOrNull { it.packageName == lastPackage } ?: visible.firstOrNull()
    Column(modifier.testTag("quick-launch-row").onFocusChanged { if (it.hasFocus) onFocus() }.focusGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(stringResource(R.string.vortex_quick_launch), fontSize = 18.sp, color = androidx.compose.ui.graphics.Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (googleTvAvailable) CinemaButton(stringResource(R.string.vortex_content_google_tv_open), onGoogleTv,
                Modifier.height(28.dp).testTag("quick-launch-google-tv").onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                        Key.DirectionUp -> { onUp(); true }
                        Key.DirectionDown -> { focus.requestFocus(); true }
                        else -> false
                    }
                }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp), displayLabel = "Google TV")
            CinemaButton(stringResource(R.string.vortex_quick_launch_edit), onEdit,
                Modifier.height(28.dp).testTag("quick-launch-edit").focusRequester(editFocus)
                    .then(if (apps.isEmpty()) Modifier.focusRequester(focus) else Modifier)
                    .onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                            Key.DirectionUp -> { onUp(); true }
                            Key.DirectionDown -> { focus.requestFocus(); true }
                            forward -> { onRight(); true }
                            else -> false
                        }
                    }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp))
        }
        if (apps.isEmpty()) {
            Text(stringResource(R.string.vortex_quick_launch_empty), color = VortexMuted, fontSize = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
        } else BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = ((maxWidth - 8.dp * QUICK_LAUNCH_LIMIT) / QUICK_LAUNCH_LIMIT).coerceAtLeast(64.dp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visible.forEachIndexed { index, app -> key(app.packageName) {
                    CinemaButton(app.label, { onLaunch(app) }, Modifier.width(width).height(86.dp)
                        .testTag("quick-launch:${app.packageName}")
                        .then(if (app.packageName == anchor?.packageName) Modifier.focusRequester(focus) else Modifier)
                        .onFocusChanged { if (it.isFocused) lastPackage = app.packageName }
                        .onPreviewKeyEvent {
                            if (it.type != KeyEventType.KeyDown) false else when {
                                it.key == Key.DirectionUp -> { editFocus.requestFocus(); true }
                                it.key == Key.DirectionDown -> true
                                it.key == forward && index == visible.lastIndex -> { onRight(); true }
                                else -> false
                            }
                        }, onLongClick = onEdit, opaque = true, darkFocus = true,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 5.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AppSymbol(app, loader, active, revision, Modifier.size(38.dp), fallbackSize = 24.sp)
                            Text(app.label, color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, lineHeight = 13.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrLtr))
                        }
                    }
                } }
            }
        }
    }
}
