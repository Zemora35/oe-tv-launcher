package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.AppTile
import nl.neerdael.backdrop_launcher.core.ResumeItem
import nl.neerdael.backdrop_launcher.ui.ComposeArtwork

@Composable
internal fun DiscoverCard(item: ResumeItem, saved: Boolean, loader: ComposeArtwork?, active: Boolean,
    revision: Int, onOpen: () -> Unit, onSave: () -> Unit, modifier: Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val scale = animateFloatAsState(if (focused) 1.045f else 1f,
        tween(if (LocalVortexMotion.current && active) 140 else 0), label = "discover-focus")
    val source = item.sourceName?.takeIf { it.isNotBlank() && it != item.packageName }
        ?: stringResource(R.string.vortex_content_platform_unknown)
    val saveLabel = stringResource(if (saved) R.string.vortex_remove_from_list else R.string.vortex_add_to_list)
    val openLabel = stringResource(if (!item.intentUri.isNullOrBlank() && !item.packageName.isNullOrBlank())
        R.string.vortex_watch_now else R.string.vortex_more_info)
    Column(modifier.testTag("discover:${contentKey(item)}").zIndex(if (focused) 1f else 0f)
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .drawWithCache {
            val radius = CornerRadius(17.dp.toPx())
            val gloss = Brush.verticalGradient(listOf(Color(0x506b80b7), Color.Transparent, Color(0x33020412)))
            onDrawBehind {
                if (focused) drawRoundRect(VortexAccent, Offset(-3.dp.toPx(), -3.dp.toPx()),
                    Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()), CornerRadius(radius.x + 3.dp.toPx()),
                    style = Stroke(6.dp.toPx()), alpha = .16f)
                drawRoundRect(Color(0xff172136), cornerRadius = radius)
                drawRoundRect(gloss, cornerRadius = radius)
                drawRoundRect(if (focused) Color(0xffeadcff) else Color(0x887e9bc8), cornerRadius = radius,
                    style = Stroke(if (focused) 1.8.dp.toPx() else .7.dp.toPx()))
            }
        }.combinedClickable(interactionSource = interactions, indication = null,
            onClick = onOpen, onLongClick = onSave, onClickLabel = openLabel, onLongClickLabel = saveLabel)
        .semantics(mergeDescendants = true) { contentDescription = "${item.title}, $source"; role = Role.Button; selected = saved }
        .padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(VortexInk)) {
            // Programme art does not evict the shared app-icon cache. Only visible cards subscribe.
            if (!item.image.isNullOrBlank()) CinematicArt(item.image, loader, active, revision,
                Modifier.fillMaxSize(), retain = false, size = IntSize(384, 216))
            else Text(item.title.take(1), color = VortexMuted, fontSize = 28.sp, modifier = Modifier.align(Alignment.Center))
            if (saved) Text("★", color = VortexAccent, fontSize = 17.sp, modifier = Modifier.align(Alignment.TopEnd)
                .padding(4.dp).background(VortexInk, RoundedCornerShape(8.dp)).padding(horizontal = 4.dp))
        }
        Text(item.title, color = Color.White, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 2,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal, overflow = TextOverflow.Ellipsis,
            style = TextStyle(textDirection = TextDirection.ContentOrLtr))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            item.packageName?.takeIf { it.isNotBlank() }?.let { name ->
                val tile = remember(name, source) { AppTile(name, source, null) }
                AppSymbol(tile, loader, active, revision, Modifier.size(14.dp), fallbackSize = 10.sp)
            }
            Text(source, color = VortexMuted, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                style = TextStyle(textDirection = TextDirection.ContentOrLtr))
        }
    }
}
