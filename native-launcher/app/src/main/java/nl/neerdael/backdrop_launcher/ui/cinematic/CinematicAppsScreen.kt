package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import nl.neerdael.backdrop_launcher.R
import nl.neerdael.backdrop_launcher.core.AppTile
import nl.neerdael.backdrop_launcher.ui.ComposeArtwork

private enum class GridChoice { FILTER, SORT }

@OptIn(ExperimentalFoundationApi::class)
private object GridViewport : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) =
        CinemaPerformance.minimalScroll(offset, size, containerSize)
}

/** Fullscreen presentation; no inventory queries, disk writes or image work in key handlers. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun CinematicAppsScreen(entries: List<AppCatalogEntry>, history: Boolean, backdrop: String,
    onClose: () -> Unit, onLaunch: (String) -> Unit, onHold: (String) -> Unit, onTab: (HomeTab) -> Unit,
    loader: ComposeArtwork? = null, active: Boolean = true, canRequestFocus: Boolean = true,
    revision: Int = 0, quickSelection: List<String>? = null, status: @Composable () -> Unit = {}) {
    val picking = quickSelection != null
    val selectionOrder = remember(quickSelection) { quickSelection.orEmpty().withIndex().associate { it.value to it.index + 1 } }
    val configuration = LocalConfiguration.current
    @Suppress("DEPRECATION")
    val locale = if (android.os.Build.VERSION.SDK_INT >= 24) configuration.locales[0] else configuration.locale
    val zone = TimeZone.getDefault()
    val dateFormatter = remember(locale, zone) { visitDateFormatter(locale, zone) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(AppCategory.ALL) }
    var type by rememberSaveable { mutableStateOf(AppTypeFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(if (history) AppGridSort.RECENT else AppGridSort.NAME_ASC) }
    // Read this state only in callbacks / the small footer. Moving focus must not
    // recompose the screen, recreate every tile modifier, or rebuild their text.
    val lastPackage = rememberSaveable { mutableStateOf<String?>(null) }
    var focusZone by remember { mutableStateOf("grid") }
    var choice by remember { mutableStateOf<GridChoice?>(null) }
    val ordered = remember(entries, history, sort, locale) { orderAppCatalog(entries, history, sort, locale) }
    val visible = remember(ordered, query, category, type) { filterAppCatalog(ordered, query, category, type) }
    val keys = remember(visible) { visible.map { it.app.packageName } }
    val requesters = remember(keys) { keys.associateWith { FocusRequester() } }
    val navFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val categoryRequesters = remember { AppCategory.entries.associateWith { FocusRequester() } }
    val categoryFocus = categoryRequesters.getValue(category)
    val emptyFocus = remember { FocusRequester() }
    val tileFocus = requesters[keys.firstOrNull()] ?: emptyFocus
    val filterFocus = remember { FocusRequester() }
    val sortFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var navigationTask by remember { mutableStateOf<Job?>(null) }
    fun moveGrid(target: Int, columns: Int) {
        navigationTask?.cancel()
        val info = grid.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == target }
        val reference = item ?: info.visibleItemsInfo.firstOrNull() ?: return
        val top = item?.offset?.y ?: (reference.offset.y + (target / columns - reference.index / columns) *
            (reference.size.height + with(density) { 10.dp.roundToPx() }))
        val start = info.viewportStartOffset + info.beforeContentPadding
        val end = info.viewportEndOffset - info.afterContentPadding
        val distance = CinemaPerformance.minimalScroll((top - start).toFloat(), reference.size.height.toFloat(), (end - start).toFloat())
        val requester = requesters.getValue(visible[target].app.packageName)
        if (item != null && distance == 0f) requester.requestFocus()
        else navigationTask = scope.launch {
            grid.scrollBy(distance)
            withFrameNanos { }
            requester.requestFocus()
        }
    }
    fun focusGrid() {
        scope.launch {
            if (visible.isEmpty()) { backFocus.requestFocus(); return@launch }
            val anchorIndex = visible.indexOfFirst { it.app.packageName == lastPackage.value }.coerceAtLeast(0)
            if (grid.layoutInfo.visibleItemsInfo.none { it.index == anchorIndex }) grid.scrollToItem(anchorIndex)
            withFrameNanos { }
            requesters.getValue(visible[anchorIndex].app.packageName).requestFocus()
        }
    }
    LaunchedEffect(canRequestFocus, active, choice == null, keys) {
        if (active && canRequestFocus && choice == null && focusZone == "grid") {
            withFrameNanos { }
            focusGrid()
        }
    }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler {
        if (imeVisible) { keyboard?.hide(); categoryFocus.requestFocus() }
        else onClose()
    }
    val window = LocalWindowInfo.current.containerSize
    val constrained = rememberConstrainedTv()
    val artSize = remember(window, constrained) { CinemaPerformance.backdropSize(window.width, window.height, constrained) }
    Box(Modifier.fillMaxSize().background(VortexInk).testTag(if (picking) "quick-launch-editor" else if (history) "history-screen" else "all-apps-screen")) {
        CinematicArt(backdrop, loader, active, revision, Modifier.fillMaxSize(), retain = false, size = artSize)
        Box(Modifier.fillMaxSize().drawWithCache {
            val shade = Brush.verticalGradient(listOf(Color(0xa8080d1b), Color(0xdc080d1b), Color(0xf5080d1b)))
            val orbit = Path().apply {
                moveTo(-size.width * .05f, size.height * .91f)
                cubicTo(size.width * .12f, size.height * .63f, size.width * .88f, size.height * 1.08f, size.width * 1.05f, size.height * .76f)
            }
            onDrawBehind {
                drawRect(shade)
                drawPath(orbit, VortexAccent, style = Stroke(5.dp.toPx()), alpha = .08f)
                drawPath(orbit, VortexAccent, style = Stroke(.7.dp.toPx()), alpha = .45f)
            }
        })
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
            TopNavBar(HomeTab.HOME, navFocus, onTab, { searchFocus.requestFocus() }, status, compact = true)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val interactions = remember { MutableInteractionSource() }
                val searchFocused by interactions.collectIsFocusedAsState()
                val shape = RoundedCornerShape(20.dp)
                BasicTextField(query, { query = it }, Modifier.weight(1f).fillMaxHeight()
                    .testTag("app-grid-search").focusRequester(searchFocus)
                    .onFocusChanged { if (it.isFocused) focusZone = "search" }
                    .onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                            Key.DirectionUp -> { keyboard?.hide(); navFocus.requestFocus(); true }
                            Key.DirectionDown -> { keyboard?.hide(); categoryFocus.requestFocus(); true }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { keyboard?.show(); false }
                            else -> false
                        }
                    }.background(Color(0x9b141c30), shape)
                    .border(if (searchFocused) 2.dp else 1.dp, if (searchFocused) VortexAccent else Color(0x667a86a8), shape)
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp), singleLine = true,
                    cursorBrush = SolidColor(VortexAccent), interactionSource = interactions,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, showKeyboardOnFocus = false),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focusZone = "grid"; focusGrid() }),
                    decorationBox = { field ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SearchSymbol()
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text(stringResource(if (history) R.string.vortex_grid_history_search else R.string.vortex_grid_search),
                                    fontSize = 14.sp, color = VortexMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                field()
                            }
                            if (query.isNotEmpty()) CinemaButton(stringResource(R.string.vortex_grid_clear_search), { query = "" },
                                Modifier.size(22.dp).testTag("app-grid-clear"), contentPadding = PaddingValues(0.dp)) {
                                Text("×", fontSize = 17.sp, color = if (searchFocused) Color.White else VortexMuted)
                            }
                        }
                    })
                CinemaButton(stringResource(R.string.vortex_grid_filter, stringResource(type.labelRes)) + " ▾", { choice = GridChoice.FILTER },
                    Modifier.width(136.dp).fillMaxHeight().testTag("app-grid-filter").focusRequester(filterFocus)
                        .onFocusChanged { if (it.isFocused) focusZone = "filter" },
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp))
                CinemaButton(stringResource(R.string.vortex_grid_sort, stringResource(sort.labelRes)) + " ▾", { choice = GridChoice.SORT },
                    Modifier.width(156.dp).fillMaxHeight().testTag("app-grid-sort").focusRequester(sortFocus)
                        .onFocusChanged { if (it.isFocused) focusZone = "sort" },
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp))
                CinemaButton(stringResource(if (picking) R.string.vortex_quick_launch_done else R.string.vortex_grid_close), onClose,
                    Modifier.width(if (picking) 78.dp else 58.dp).fillMaxHeight().focusRequester(backFocus).testTag("app-grid-back"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(if (picking) R.string.vortex_quick_launch else if (history) R.string.vortex_history else R.string.vortex_all_apps), color = Color.White,
                fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
            Text(if (picking) stringResource(R.string.vortex_quick_launch_choose, quickSelection.orEmpty().size, QUICK_LAUNCH_LIMIT)
                else stringResource(if (history) R.string.vortex_grid_history_subtitle else R.string.vortex_grid_all_subtitle),
                fontSize = 11.sp, lineHeight = 15.sp, color = VortexMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppCategory.entries.forEach { item ->
                    CinemaButton(stringResource(item.labelRes), { category = item },
                        Modifier.fillMaxHeight().testTag("app-grid-category:${item.name}")
                            .focusRequester(categoryRequesters.getValue(item))
                            .onFocusChanged { if (it.isFocused) focusZone = "category" }
                            .onPreviewKeyEvent {
                                if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                                    Key.DirectionUp -> { searchFocus.requestFocus(); true }
                                    Key.DirectionDown -> { focusZone = "grid"; focusGrid(); true }
                                    else -> false
                                }
                            }, primary = category == item, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val columns = if (maxWidth >= 680.dp) 7 else 5
                val rows = if (LocalDensity.current.fontScale > 1.1f) 3 else 4
                val tileHeight = ((maxHeight - 16.dp - 10.dp * (rows - 1)) / rows).coerceAtLeast(60.dp)
                if (visible.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(if (history && ordered.isEmpty()) R.string.vortex_grid_history_empty else R.string.vortex_grid_no_results),
                            color = VortexMuted, fontSize = 16.sp, modifier = Modifier.testTag("app-grid-empty"))
                    }
                } else CompositionLocalProvider(LocalBringIntoViewSpec provides GridViewport) { LazyVerticalGrid(GridCells.Fixed(columns), Modifier.fillMaxSize().testTag("app-grid")
                    .focusRestorer(tileFocus).focusGroup(), state = grid, contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(visible, key = { _, entry -> entry.app.packageName }, contentType = { _, _ -> "app" }) { index, entry ->
                        val visit = if (history) formatVisit(entry.visitedAtMillis, dateFormatter)
                            ?: stringResource(R.string.vortex_grid_time_unknown) else null
                        AppGridTile(entry, visit, loader, active, revision, { onLaunch(entry.app.packageName) },
                            { onHold(entry.app.packageName) }, Modifier.height(tileHeight)
                                .focusRequester(requesters.getValue(entry.app.packageName))
                                .onFocusChanged { if (it.isFocused) { lastPackage.value = entry.app.packageName; focusZone = "grid" } }
                                .onPreviewKeyEvent {
                                    if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                                        Key.DirectionUp -> {
                                            if (index < columns) categoryFocus.requestFocus()
                                            else moveGrid(gridNeighbour(index, visible.size, columns, 0, -1), columns)
                                            true
                                        }
                                        Key.DirectionDown -> { moveGrid(gridNeighbour(index, visible.size, columns, 0, 1), columns); true }
                                        Key.DirectionLeft -> { moveGrid(gridNeighbour(index, visible.size, columns, if (rtl) 1 else -1, 0), columns); true }
                                        Key.DirectionRight -> { moveGrid(gridNeighbour(index, visible.size, columns, if (rtl) -1 else 1, 0), columns); true }
                                        else -> false
                                    }
                                }, selectionMode = picking, selectionOrder = selectionOrder[entry.app.packageName])
                    }
                } }
            }
            GridFooter(visible, lastPackage)
        }
    }
    choice?.let { selectedChoice ->
        val labels = if (selectedChoice == GridChoice.FILTER) AppTypeFilter.entries.map { stringResource(it.labelRes) }
            else AppGridSort.entries.map { stringResource(it.labelRes) }
        val current = if (selectedChoice == GridChoice.FILTER) type.ordinal else sort.ordinal
        fun dismiss() {
            choice = null
            scope.launch { withFrameNanos { }; if (selectedChoice == GridChoice.FILTER) filterFocus.requestFocus() else sortFocus.requestFocus() }
        }
        GridChoiceDialog(stringResource(if (selectedChoice == GridChoice.FILTER) R.string.vortex_grid_choose_filter else R.string.vortex_grid_choose_sort),
            labels, current, ::dismiss) { index ->
            if (selectedChoice == GridChoice.FILTER) type = AppTypeFilter.entries[index] else sort = AppGridSort.entries[index]
            dismiss()
        }
    }
}

@Composable
private fun GridFooter(visible: List<AppCatalogEntry>, selected: State<String?>) {
    val columns = if (LocalWindowInfo.current.containerSize.width / LocalDensity.current.density >= 744) 7 else 5
    val rows = if (LocalDensity.current.fontScale > 1.1f) 3 else 4
    val pages = ((visible.size + columns * rows - 1) / (columns * rows)).coerceAtLeast(1)
    val page by remember(visible, columns, rows) { derivedStateOf {
        (visible.indexOfFirst { it.app.packageName == selected.value }.coerceAtLeast(0) / (columns * rows)).coerceAtMost(pages - 1)
    } }
    Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(pluralStringResource(R.plurals.vortex_app_count, visible.size, visible.size), fontSize = 10.sp, color = VortexMuted)
        Spacer(Modifier.weight(1f))
        PositionDots(page, pages)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.vortex_grid_position, page + 1, pages), color = VortexMuted, fontSize = 10.sp)
    }
}

@Composable
private fun AppGridTile(entry: AppCatalogEntry, visit: String?, loader: ComposeArtwork?, active: Boolean,
    revision: Int, onOpen: () -> Unit, onHold: () -> Unit, modifier: Modifier,
    selectionMode: Boolean = false, selectionOrder: Int? = null) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val duration = if (LocalVortexMotion.current && active) 140 else 0
    val emphasis = animateFloatAsState(if (focused) 1f else 0f, tween(duration), label = "grid-focus")
    val app = entry.app
    val tint = remember(app.packageName, app.label, entry.category) { appTint(app.packageName + " " + app.label, entry.category) }
    val description = if (visit != null) app.label + ", " + stringResource(R.string.vortex_grid_last_visit, visit) else app.label
    val selectionState = if (selectionMode) stringResource(if (selectionOrder != null) R.string.vortex_quick_launch_selected else R.string.vortex_quick_launch_unselected) else ""
    val selectionAction = if (selectionMode) stringResource(if (selectionOrder != null) R.string.vortex_quick_launch_remove else R.string.vortex_quick_launch_add) else ""
    val openLabel = if (selectionMode) selectionAction else stringResource(R.string.vortex_grid_open_app, app.label)
    val holdLabel = if (selectionMode) selectionAction else stringResource(R.string.vortex_grid_app_actions)
    Column(modifier.testTag("app-grid:${app.packageName}").zIndex(if (focused) 1f else 0f)
        .graphicsLayer { val scale = 1f + emphasis.value * .055f; scaleX = scale; scaleY = scale }
        .drawWithCache {
            val radius = CornerRadius(17.dp.toPx())
            val gloss = Brush.verticalGradient(listOf(Color(0x707e9bdd), Color(0x063d4b68), Color(0x50020412)))
            onDrawBehind {
                // Keep tile paint cached while only the RenderNode scale animates.
                val glow = if (focused) 1f else 0f
                if (glow > 0f) {
                    drawRoundRect(VortexAccent, Offset(-3.dp.toPx(), -3.dp.toPx()), Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                        CornerRadius(radius.x + 3.dp.toPx()), style = Stroke(6.dp.toPx()), alpha = .16f * glow)
                }
                drawRoundRect(tint, cornerRadius = radius)
                drawRoundRect(gloss, cornerRadius = radius)
                drawRoundRect(if (focused) Color(0xffeadcff) else if (selectionOrder != null) VortexAccent else Color(0x887e9bc8), cornerRadius = radius,
                    style = Stroke(if (focused || selectionOrder != null) 1.8.dp.toPx() else .7.dp.toPx()))
            }
        }.combinedClickable(interactionSource = interactions, indication = null, onClick = onOpen, onLongClick = onHold,
            onClickLabel = openLabel, onLongClickLabel = holdLabel)
        .semantics(mergeDescendants = true) {
            contentDescription = description; role = Role.Button
            if (selectionMode) { selected = selectionOrder != null; stateDescription = selectionState }
        }
        .padding(horizontal = 6.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val tile = remember(app.packageName, app.label, app.artwork) { AppTile(app.packageName, app.label, app.artwork) }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AppSymbol(tile, loader, active, revision, Modifier.fillMaxSize().padding(2.dp), fallbackSize = 20.sp)
            if (selectionOrder != null) Text(selectionOrder.toString(), color = VortexInk, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).background(VortexAccent, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Text(app.label, fontSize = 11.sp, lineHeight = 13.sp, maxLines = if (visit == null) 2 else 1,
            overflow = TextOverflow.Ellipsis, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = TextStyle(textDirection = TextDirection.ContentOrLtr), fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal)
        if (visit != null) Text(visit, fontSize = 9.sp, lineHeight = 11.sp, color = Color(0xffc5cce0), maxLines = 1,
            overflow = TextOverflow.Ellipsis, style = TextStyle(textDirection = TextDirection.ContentOrLtr),
            modifier = Modifier.testTag("app-visit:${app.packageName}"))
    }
}

private fun appTint(identity: String, category: AppCategory): Color {
    val name = identity.lowercase(Locale.ROOT)
    return when {
        "netflix" in name -> Color(0xff490d20)
        "youtube" in name -> Color(0xff8b1834)
        "spotify" in name -> Color(0xff075b43)
        "prime" in name || "amazonvideo" in name -> Color(0xff084f8e)
        "disney" in name -> Color(0xff153b7d)
        "photo" in name || "fotoğraf" in name || "vlc" in name -> Color(0xff985719)
        "music" in name || "müzik" in name -> Color(0xff732760)
        category == AppCategory.SYSTEM -> Color(0xff3d3d51)
        category == AppCategory.MEDIA -> Color(0xff28597a)
        else -> Color(0xff263552)
    }
}

@Composable
private fun SearchSymbol() {
    Canvas(Modifier.size(17.dp)) {
        val line = Stroke(1.2.dp.toPx())
        drawCircle(Color(0xffd5d9e8), size.minDimension * .31f, Offset(size.width * .42f, size.height * .42f), style = line)
        drawLine(Color(0xffd5d9e8), Offset(size.width * .64f, size.height * .64f), Offset(size.width * .94f, size.height * .94f), 1.2.dp.toPx())
    }
}

@Composable
private fun PositionDots(page: Int, count: Int) {
    val first = (page - 2).coerceAtLeast(0).coerceAtMost((count - 5).coerceAtLeast(0))
    val shown = count.coerceAtMost(5)
    Canvas(Modifier.width((shown * 12).dp).height(12.dp)) {
        repeat(shown) { index ->
            drawCircle(if (first + index == page) VortexAccent else Color(0xff606a83),
                if (first + index == page) 3.dp.toPx() else 2.dp.toPx(), Offset((index * 12 + 6).dp.toPx(), size.height / 2))
        }
    }
}

@Composable
private fun GridChoiceDialog(title: String, labels: List<String>, selected: Int, close: () -> Unit, pick: (Int) -> Unit) {
    val focus = remember { FocusRequester() }
    Dialog(close, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.width(310.dp).background(VortexInk, RoundedCornerShape(22.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 20.sp, color = Color.White)
            labels.forEachIndexed { index, label ->
                CinemaButton(label, { pick(index) }, Modifier.fillMaxWidth().testTag("app-grid-choice:$index")
                    .then(if (index == selected) Modifier.focusRequester(focus) else Modifier), selected = index == selected)
            }
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}

@Preview(name = "O.E tv · Applications", locale = "tr", device = "spec:width=1920px,height=1080px,dpi=320")
@Composable
internal fun CinematicAppsPreview() {
    val apps = CinematicSamples.apps.map { nl.neerdael.backdrop_launcher.core.LauncherApp(it.packageName, it.label, "1", false, false, null, it.artwork) }
    MaterialTheme { CinematicAppsScreen(buildAppCatalog(apps, emptyList(), emptyMap()), false, CinematicSamples.featured.artwork!!, {}, {}, {}, {}) }
}
