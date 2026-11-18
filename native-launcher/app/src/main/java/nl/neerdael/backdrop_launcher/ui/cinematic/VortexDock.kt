package nl.neerdael.backdrop_launcher.ui.cinematic

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import nl.neerdael.backdrop_launcher.R
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import nl.neerdael.backdrop_launcher.core.AppTile
import nl.neerdael.backdrop_launcher.ui.ComposeArtwork
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val VortexPoseConverter = TwoWayConverter<VortexPose, AnimationVector4D>(
    { AnimationVector4D(it.x, it.y, it.scale, it.alpha) },
    { VortexPose(it.v1, it.v2, it.v3, it.v4) })

/** Spatial decoration is independent of a single stationary remote/accessibility focus target. */
@Composable
internal fun VortexDock(apps: List<AppTile>, selectedIndex: Int, focus: FocusRequester,
    loader: ComposeArtwork?, active: Boolean, revision: Int, onStep: (Int) -> Unit,
    onOpen: (AppTile) -> Unit, onHold: (AppTile) -> Unit, onUp: () -> Unit, onDown: () -> Unit,
    onFocused: () -> Unit, modifier: Modifier = Modifier, reordering: Boolean = false) {
    if (apps.isEmpty()) return
    val index = selectedIndex.coerceIn(apps.indices)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val state = stringResource(R.string.vortex_dock_state, index + 1, apps.size,
        stringResource(if (reordering) R.string.vortex_dock_reordering else R.string.vortex_dock_navigation))
    val previousLabel = stringResource(R.string.vortex_dock_previous)
    val nextLabel = stringResource(R.string.vortex_dock_next)
    var focused by remember { mutableStateOf(false) }
    val motion = LocalVortexMotion.current && active
    val interactions = remember { MutableInteractionSource() }
    BoxWithConstraints(modifier.testTag("vortex-dock").focusRequester(focus)
        .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
        .onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                Key.DirectionLeft -> { onStep(if (rtl) 1 else -1); true }
                Key.DirectionRight -> { onStep(if (rtl) -1 else 1); true }
                Key.DirectionUp -> { onUp(); true }
                Key.DirectionDown -> { onDown(); true }; else -> false
            }
        }
        .combinedClickable(interactionSource = interactions, indication = null,
            onClick = { onOpen(apps[index]) }, onLongClick = { onHold(apps[index]) })
        .semantics(mergeDescendants = true) {
            contentDescription = apps[index].label
            stateDescription = state
            role = Role.Button
            customActions = listOf(
                CustomAccessibilityAction(previousLabel) { onStep(-1); true },
                CustomAccessibilityAction(nextLabel) { onStep(1); true })
        }
        .drawWithCache {
            // A single cached path supplies the depth cue. There are no animated particles or blur buffers.
            val path = Path()
            for (step in 0..160) {
                val t = step / 160f
                val angle = t * 9.8f
                val radius = .025f + t * .49f
                val poseX = .65f + cos(angle) * radius * .79f
                val x = (if (rtl) 1f - poseX else poseX) * size.width
                val y = (.29f + sin(angle) * radius * .59f) * size.height
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val light = Brush.horizontalGradient(listOf(Color(0x226dcaff), Color(0x887b69ed), Color(0x997da6ff)))
            onDrawBehind {
                drawPath(path, light, style = Stroke(8.dp.toPx()), alpha = .07f)
                drawPath(path, light, style = Stroke(3.dp.toPx()), alpha = .22f)
                drawPath(path, light, style = Stroke(.8.dp.toPx()), alpha = .72f)
            }
        }) {
        val width = maxWidth
        val height = maxHeight
        // Constant decode size across all poses avoids re-decoding as items move or gain focus.
        val visible = remember(index, apps.size) { VortexGeometry.visible(index, apps.size) }
        for (appIndex in visible) {
            key(apps[appIndex].key) {
                val distance = appIndex - index
                val pose = VortexGeometry.pose(distance)
                val duration = if (motion) 160 else 0
                // One interruptible animation per tile instead of four independent clocks.
                val animated = animateValueAsState(pose, VortexPoseConverter, tween(duration), label = "vortex-pose")
                VortexDockItem(apps[appIndex], appIndex == index, focused, loader, active, revision,
                    Modifier.align(Alignment.TopCenter).size(82.dp).zIndex((10 - abs(distance)).toFloat())
                        .graphicsLayer {
                            val value = animated.value
                            translationX = width.toPx() * ((if (rtl) 1f - value.x else value.x) - .5f)
                            translationY = height.toPx() * value.y - 41.dp.toPx()
                            scaleX = value.scale; scaleY = value.scale
                            alpha = value.alpha
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            rotationZ = distance.coerceIn(-4, 4) * if (rtl) 2f else -2f
                        }.clearAndSetSemantics { testTag = "vortex-tile:${apps[appIndex].key}" })
            }
        }
        Text(if (reordering) stringResource(R.string.vortex_dock_reorder_hint) else stringResource(R.string.vortex_dock_hint, index + 1, apps.size),
            Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp), fontSize = 10.sp, color = VortexMuted)
    }
}

@Composable
internal fun VortexDockItem(app: AppTile, selected: Boolean, focused: Boolean, loader: ComposeArtwork?,
    active: Boolean, revision: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    val tint = remember(app.packageName) { when {
        app.packageName.contains("netflix", true) -> Color(0xff661421)
        app.packageName.contains("youtube", true) -> Color(0xff8c252c)
        app.packageName.contains("spotify", true) -> Color(0xff174d3c)
        app.packageName == SETTINGS_APP -> Color(0xff414453)
        else -> Color(0xff263d70)
    } }
    val halo = if (selected && focused) 1f else if (selected) .4f else 0f
    Column(modifier.drawWithCache {
        val radius = CornerRadius(20.dp.toPx())
        val gloss = Brush.linearGradient(listOf(Color(0x667e9bd2), Color.Transparent, Color(0x40121b47)))
        onDrawBehind {
            // Paint on focus changes, not on every transform animation frame.
            val glow = halo
            if (glow > 0f) {
                for (spread in 3 downTo 1) {
                    val inset = spread * 2.dp.toPx()
                    drawRoundRect(VortexAccent, Offset(-inset, -inset), Size(size.width + inset * 2, size.height + inset * 2),
                        CornerRadius(radius.x + inset), style = Stroke((spread * 2).dp.toPx()), alpha = glow * .06f)
                }
            }
            drawRoundRect(if (selected) Color(0xff50328c) else tint, cornerRadius = radius)
            drawRoundRect(gloss, cornerRadius = radius)
            drawRoundRect(if (selected) Color(0xffe3d4ff) else Color(0x667ea5d8), cornerRadius = radius,
                style = Stroke(if (selected) 2.dp.toPx() else .8.dp.toPx()), alpha = if (selected) .65f + glow * .35f else 1f)
        }
    }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AppSymbol(app, loader, active, revision, Modifier.fillMaxSize().padding(3.dp))
        }
        Text(app.label, color = Color.White, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrLtr),
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

internal fun fallbackSymbol(app: AppTile): String = when {
    app.packageName == SETTINGS_APP || app.label.contains("settings", true) || app.label.contains("ayar", true) -> "⚙"
    app.label.contains("music", true) || app.label.contains("müzik", true) -> "♫"
    app.label.contains("photo", true) || app.label.contains("fotoğraf", true) -> "▧"
    app.label.contains("live", true) || app.label.contains("series", true) || app.label.contains("canlı", true) -> "▣"
    app.label.contains("movie", true) || app.label.contains("film", true) -> "▶"
    app.label.contains("game", true) || app.label.contains("oyun", true) -> "✚"
    app.label.contains("browser", true) || app.label.contains("tarayıcı", true) -> "◎"
    app.label.contains("mirror", true) || app.label.contains("yansıt", true) -> "▱"
    app.label.contains("hdmi", true) -> "▤"
    else -> if (app.label.isBlank()) "•" else app.label.substring(0, app.label.offsetByCodePoints(0, 1)).uppercase(java.util.Locale.ROOT)
}
