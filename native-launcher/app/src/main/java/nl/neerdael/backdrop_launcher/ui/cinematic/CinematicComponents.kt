package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import nl.neerdael.backdrop_launcher.R

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import nl.neerdael.backdrop_launcher.core.*
import nl.neerdael.backdrop_launcher.ui.Artwork
import nl.neerdael.backdrop_launcher.ui.ComposeArtwork

internal val VortexInk = Color(0xff080d1b)
internal val VortexAccent = Color(0xffbca8ff)
internal val VortexMuted = Color(0xffb4bdcf)
internal val LocalVortexMotion = compositionLocalOf { true }

/** Focus motion is transform-only. Foundation owns short/held Select and cancellation. */
@Composable
internal fun CinemaButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    selected: Boolean = false, primary: Boolean = false, onLongClick: (() -> Unit)? = null,
    opaque: Boolean = false, darkFocus: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
    displayLabel: String = label,
    content: (@Composable () -> Unit)? = null) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val duration = if (LocalVortexMotion.current) 160 else 0
    val scale = animateFloatAsState(if (focused) 1.06f else 1f, tween(duration), label = "control-focus")
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .background(if ((focused || primary) && darkFocus) Color(0xff363052)
            else if (focused || primary) Color(0xffd5ceff) else if (selected) Color(0x664c5b80)
            else if (opaque) Color(0xff1b2232) else Color(0x59262c3d), shape)
        .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0x336e7e9a), shape)
        .combinedClickable(interactionSource = interactions, indication = null, onClick = onClick, onLongClick = onLongClick)
        .semantics { contentDescription = label; role = Role.Button }
        .padding(contentPadding), contentAlignment = Alignment.Center) {
        if (content != null) content() else Text(displayLabel, color = if ((focused || primary) && !darkFocus) VortexInk else Color.White,
            fontSize = 14.sp, fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2, lineHeight = 17.sp, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun CinematicArt(path: String?, loader: ComposeArtwork?, active: Boolean, revision: Int,
    modifier: Modifier, scale: ContentScale = ContentScale.Crop, retain: Boolean = true, size: IntSize? = null) {
    if (LocalInspectionMode.current && path != null && ':' !in path) {
        val context = LocalContext.current
        val bitmap = remember(path) { runCatching { context.assets.open(path).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })
        } }.getOrNull() }
        bitmap?.let { Image(it.asImageBitmap(), null, modifier, contentScale = scale) }
    } else if (loader != null) Artwork(path, loader, active, revision, modifier, scale, retain, decodeSize = size)
}

@Composable
internal fun TopNavBar(tab: HomeTab, focus: FocusRequester, onTab: (HomeTab) -> Unit,
    onDown: () -> Unit, status: @Composable () -> Unit, compact: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(144.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("◉", color = VortexAccent, fontSize = 24.sp, lineHeight = 26.sp)
                Text(stringResource(R.string.oe_tv_app_name), color = Color.White, fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp, lineHeight = 26.sp, maxLines = 1,
                    style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr))
            }
            // Static, density-scaled artwork: no font/network dependency or animation cost.
            Image(painterResource(R.drawable.oguzhan_eklidag_signature),
                contentDescription = stringResource(R.string.oe_tv_author),
                modifier = Modifier.width(144.dp).height(if (compact) 16.dp else 26.dp).testTag("brand-signature"),
                contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.width(12.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) { HomeTab.entries.forEach { item ->
            CinemaButton(stringResource(item.labelRes), { onTab(item) }, Modifier.weight(1f)
                .then(if (item == HomeTab.HOME) Modifier.focusRequester(focus) else Modifier)
                .onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) { onDown(); true } else false },
                selected = tab == item,
                contentPadding = if (compact) PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    else PaddingValues(horizontal = 15.dp, vertical = 11.dp))
        } }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(150.dp)) { status() }
    }
}

@Composable
internal fun HeroBanner(featured: FeaturedContent, focus: FocusRequester, onWatch: () -> Unit,
    onInfo: () -> Unit, onDown: () -> Unit, onUp: () -> Unit, modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null, onHeroFocus: (Boolean) -> Unit = {}) {
    // Reserve the foreground dock lane when translations or accessibility text expand.
    // Full descriptions remain available in More Info.
    val compact = LocalDensity.current.fontScale > 1.05f || featured.title.length > 26
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.vortex_featured), fontSize = 10.sp, letterSpacing = 2.sp, color = VortexMuted)
            Text(featured.platform ?: stringResource(R.string.vortex_content_platform_unknown),
                fontSize = 10.sp, lineHeight = 12.sp, color = VortexAccent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("hero-platform").weight(1f))
        }
        Text(featured.title, color = Color.White, fontSize = if (compact) 26.sp else 36.sp, lineHeight = if (compact) 29.sp else 39.sp, fontWeight = FontWeight.Light,
            maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
        Text(featured.description, color = Color(0xffd0d6e2), fontSize = if (compact) 12.sp else 14.sp,
            lineHeight = if (compact) 15.sp else 19.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!compact && featured.badges.isNotEmpty()) Text(featured.badges.joinToString("   ·   "), fontSize = 11.sp, color = VortexMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.testTag("hero-actions").onFocusChanged { onHeroFocus(it.hasFocus) }.focusGroup()
            .padding(top = 4.dp).onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                Key.DirectionDown -> { onDown(); true }; Key.DirectionUp -> { onUp(); true }; else -> false
            }
        }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CinemaButton(stringResource(if (featured.playable != null) R.string.vortex_watch_now else R.string.vortex_content_watch_options), onWatch,
                Modifier.focusRequester(focus), primary = true)
            CinemaButton(stringResource(R.string.vortex_more_info), onInfo, opaque = true)
            if (onNext != null) CinemaButton(stringResource(R.string.vortex_featured_next), onNext,
                Modifier.testTag("hero-next").width(38.dp), opaque = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
                displayLabel = if (LocalLayoutDirection.current == LayoutDirection.Rtl) "‹" else "›")
        }
    }
}

@Composable
internal fun ContinueWatchingRow(items: List<ResumeItem>, loader: ComposeArtwork?, active: Boolean,
    revision: Int, focus: FocusRequester, onSelect: (Int) -> Unit, onPlay: (ResumeItem) -> Unit,
    onUp: () -> Unit, onRight: () -> Unit, modifier: Modifier = Modifier, restoreIndex: Int = 0,
    permissionGranted: Boolean = true, onEmptyAction: () -> Unit = onUp,
    youtubeAvailable: Boolean = false, onYouTube: () -> Unit = {},
    googleTvAvailable: Boolean = false, onGoogleTv: () -> Unit = {}) {
    val forward = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Key.DirectionLeft else Key.DirectionRight
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.vortex_continue_watching), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (googleTvAvailable) CinemaButton(stringResource(R.string.vortex_content_google_tv_open), onGoogleTv,
                    Modifier.testTag("watching-google-tv").height(28.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("Google TV", fontSize = 12.sp, color = Color.White)
                }
                if (youtubeAvailable) CinemaButton(stringResource(R.string.vortex_content_youtube_open), onYouTube,
                    Modifier.testTag("watching-youtube").height(28.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("YouTube", fontSize = 12.sp, color = Color.White)
                }
            }
        }
        if (items.isEmpty()) {
            CinemaButton(stringResource(if (permissionGranted) R.string.vortex_watching_empty_title else R.string.vortex_watch_next_permission),
                onEmptyAction, Modifier.focusRequester(focus).fillMaxWidth().onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                    Key.DirectionUp -> { onUp(); true }
                    forward, Key.DirectionDown -> { onRight(); true }; else -> false
                }
            })
            Text(stringResource(if (!permissionGranted) R.string.vortex_watch_next_permission_hint
                else if (youtubeAvailable) R.string.vortex_content_youtube_hint else R.string.vortex_watching_empty_message),
                color = VortexMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else LazyRow(state = rememberLazyListState(initialFirstVisibleItemIndex = restoreIndex.coerceIn(items.indices)),
            horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
            itemsIndexed(items, key = { index, item -> "${contentKey(item)}:$index" }) { index, item ->
                CinemaButton(item.title, { onPlay(item) }, Modifier.width(184.dp).height(98.dp)
                    .then(if (index == restoreIndex.coerceIn(items.indices)) Modifier.focusRequester(focus) else Modifier)
                    .onFocusChanged { if (it.isFocused) onSelect(index) }
                    .onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) false else when {
                            it.key == Key.DirectionUp -> { onUp(); true }
                            it.key == forward && index == items.lastIndex -> { onRight(); true }
                            it.key == Key.DirectionDown -> { onRight(); true }; else -> false
                        }
                    }, contentPadding = PaddingValues(2.dp), content = {
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
                            CinematicArt(item.image, loader, active, revision, Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xf0080d1b)))))
                            Column(Modifier.align(Alignment.BottomStart).padding(6.dp)) {
                                Text("▶  ${item.title}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                                val minutes = remainingMinutes(item)
                                val subtitle = if (minutes != null) pluralStringResource(R.plurals.vortex_resume_minutes_left, minutes, minutes) else item.description
                                if (subtitle.isNotBlank()) Text(subtitle, fontSize = 10.sp, color = VortexMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0x66596371))) {
                                    Box(Modifier.fillMaxWidth(item.progress.coerceIn(0, 100) / 100f).fillMaxHeight().background(VortexAccent))
                                }
                            }
                        }
                    })
            }
        }
    }
}

@Composable
internal fun QuickShortcutsPanel(focus: FocusRequester, onShortcut: (Shortcut) -> Unit,
    onUp: () -> Unit, onLeft: () -> Unit, modifier: Modifier = Modifier, restoreShortcut: Shortcut = Shortcut.MY_LIST) {
    val backward = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Key.DirectionRight else Key.DirectionLeft
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.vortex_quick_shortcuts), fontSize = 16.sp, color = Color(0xffdce0ec))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shortcut.entries.forEachIndexed { index, shortcut ->
                val label = stringResource(shortcut.labelRes)
                CinemaButton(label, { onShortcut(shortcut) }, Modifier.width(64.dp).height(78.dp)
                    .then(if (shortcut == restoreShortcut) Modifier.focusRequester(focus) else Modifier)
                    .onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) false else when {
                            it.key == Key.DirectionUp -> { onUp(); true }
                            it.key == backward && index == 0 -> { onLeft(); true }; else -> false
                        }
                    }, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp), content = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShortcutIcon(index)
                            Text(label, color = Color.White, fontSize = 9.sp, lineHeight = 11.sp, maxLines = 2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    })
            }
        }
    }
}

@Composable
private fun ShortcutIcon(index: Int) {
    Canvas(Modifier.size(24.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        when (index) {
            0 -> drawPath(Path().apply {
                moveTo(w * .3f, h * .15f); lineTo(w * .7f, h * .15f)
                lineTo(w * .7f, h * .85f); lineTo(w * .5f, h * .65f)
                lineTo(w * .3f, h * .85f); close()
            }, Color.White, style = Stroke(stroke))
            1 -> {
                drawCircle(Color.White, w * .34f, style = Stroke(stroke))
                drawLine(Color.White, center, Offset(w * .5f, h * .28f), stroke)
                drawLine(Color.White, center, Offset(w * .68f, h * .5f), stroke)
            }
            2 -> {
                drawLine(Color.White, Offset(w * .5f, h * .1f), Offset(w * .5f, h * .68f), stroke)
                drawLine(Color.White, Offset(w * .3f, h * .48f), Offset(w * .5f, h * .68f), stroke)
                drawLine(Color.White, Offset(w * .7f, h * .48f), Offset(w * .5f, h * .68f), stroke)
                drawLine(Color.White, Offset(w * .2f, h * .85f), Offset(w * .8f, h * .85f), stroke)
            }
            else -> for (x in 0..2) for (y in 0..2)
                drawCircle(Color.White, w * .045f, Offset(w * (.25f + x * .25f), h * (.25f + y * .25f)))
        }
    }
}
