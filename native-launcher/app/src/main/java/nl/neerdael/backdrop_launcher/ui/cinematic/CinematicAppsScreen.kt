package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import nl.neerdael.backdrop_launcher.core.ResumeItem
import nl.neerdael.backdrop_launcher.ui.ComposeArtwork

private enum class GridChoice { FILTER, SORT }

@OptIn(ExperimentalFoundationApi::class)
private object GridViewport : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) =
        CinemaPerformance.minimalScroll(offset, size, containerSize)
}

/** Fullscreen presentation; no inventory queries, disk writes or image work in key handlers. */
@Composable
internal fun CinematicAppsScreen(entries: List<AppCatalogEntry>, history: Boolean, backdrop: String,
    onClose: () -> Unit, onLaunch: (String) -> Unit, onHold: (String) -> Unit, onTab: (HomeTab) -> Unit,
    loader: ComposeArtwork? = null, active: Boolean = true, canRequestFocus: Boolean = true,
    revision: Int = 0, quickSelection: List<String>? = null, status: @Composable () -> Unit = {}) {
    CinematicCatalogScreen(entries, history, backdrop, onClose, onLaunch, onHold, onTab,
        loader, active, canRequestFocus, revision, quickSelection, null, false, status)
}

@Composable
internal fun CinematicDiscoverScreen(programmes: List<ResumeItem>, saved: Set<String>, backdrop: String,
    onClose: () -> Unit, onOpen: (ResumeItem) -> Unit, onSave: (ResumeItem) -> Unit, onTab: (HomeTab) -> Unit,
    loader: ComposeArtwork? = null, active: Boolean = true, revision: Int = 0, status: @Composable () -> Unit = {}) {
    CinematicCatalogScreen(emptyList(), false, backdrop, onClose, {}, {}, onTab, loader, active, true,
        revision, null, DiscoverGrid(programmes, saved, onOpen, onSave), false, status)
}

@Composable
internal fun CinematicSearchScreen(entries: List<AppCatalogEntry>, programmes: List<ResumeItem>, saved: Set<String>, backdrop: String,
    onClose: () -> Unit, onLaunch: (String) -> Unit, onHold: (String) -> Unit,
    onOpen: (ResumeItem) -> Unit, onSave: (ResumeItem) -> Unit, onTab: (HomeTab) -> Unit,
    loader: ComposeArtwork? = null, active: Boolean = true, canRequestFocus: Boolean = true,
    revision: Int = 0, status: @Composable () -> Unit = {}) {
    CinematicCatalogScreen(entries, false, backdrop, onClose, onLaunch, onHold, onTab, loader, active,
        canRequestFocus, revision, null, DiscoverGrid(programmes, saved, onOpen, onSave), true, status)
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CinematicCatalogScreen(entries: List<AppCatalogEntry>, history: Boolean, backdrop: String,
    onClose: () -> Unit, onLaunch: (String) -> Unit, onHold: (String) -> Unit, onTab: (HomeTab) -> Unit,
    loader: ComposeArtwork?, active: Boolean, canRequestFocus: Boolean, revision: Int,
    quickSelection: List<String>?, discover: DiscoverGrid?, searchMode: Boolean, status: @Composable () -> Unit) {
    var searchContent by rememberSaveable { mutableStateOf(false) }
    val discovering = discover != null && (!searchMode || searchContent)
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
    var contentKind by rememberSaveable { mutableStateOf(DiscoverKind.ALL) }
    var contentSort by rememberSaveable { mutableStateOf(DiscoverSort.RECOMMENDED) }
    var platform by rememberSaveable { mutableStateOf<String?>(null) }
    val contentOrdered = remember(discover?.programmes, contentSort, locale) { orderDiscover(discover?.programmes.orEmpty(), contentSort, locale) }
    val sources = remember(contentOrdered) { discoverSources(contentOrdered) }
    val currentPlatform = platform?.takeIf { key -> sources.any { it.key == key } }
    val contentVisible = remember(contentOrdered, query, currentPlatform, contentKind, discover?.saved) {
        filterDiscover(contentOrdered, query, currentPlatform, contentKind, discover?.saved.orEmpty())
    }
    // Read this state only in callbacks / the small footer. Moving focus must not
    // recompose the screen, recreate every tile modifier, or rebuild their text.
    val lastPackage = rememberSaveable { mutableStateOf<String?>(null) }
    var focusZone by remember { mutableStateOf(if (searchMode) "search" else "grid") }
    var choice by remember { mutableStateOf<GridChoice?>(null) }
    val ordered = remember(entries, history, sort, locale) { orderAppCatalog(entries, history, sort, locale) }
    val visible = remember(ordered, query, category, type) { filterAppCatalog(ordered, query, category, type) }
    val keys = remember(visible, contentVisible, discovering) {
        if (discovering) contentVisible.map(::contentKey) else visible.map { it.app.packageName }
    }
    val requesters = remember(keys) { keys.associateWith { FocusRequester() } }
    val navFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val categoryRequesters = remember { AppCategory.entries.associateWith { FocusRequester() } }
    val contentRequesters = remember { DiscoverKind.entries.associateWith { FocusRequester() } }
    val modeRequesters = remember { listOf(FocusRequester(), FocusRequester()) }
    val categoryFocus = if (searchMode) modeRequesters[if (searchContent) 1 else 0]
        else if (discovering) contentRequesters.getValue(contentKind) else categoryRequesters.getValue(category)
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
    val criteria = listOf(query, category.name, type.name, sort.name, contentKind.name, contentSort.name, currentPlatform.orEmpty(), searchContent.toString())
    var lastCriteria by rememberSaveable { mutableStateOf(criteria) }
    LaunchedEffect(criteria) {
        if (criteria != lastCriteria) {
            lastCriteria = criteria
            lastPackage.value = null
            navigationTask?.cancel()
            grid.scrollToItem(0)
        }
    }
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
        val requester = requesters.getValue(keys[target])
        if (item != null && distance == 0f) requester.requestFocus()
        else navigationTask = scope.launch {
            grid.scrollBy(distance)
            withFrameNanos { }
            requester.requestFocus()
        }
    }
    fun focusGrid() {
        scope.launch {
            if (keys.isEmpty()) { if (searchMode) searchFocus.requestFocus() else backFocus.requestFocus(); return@launch }
            val anchorIndex = keys.indexOf(lastPackage.value).coerceAtLeast(0)
            if (grid.layoutInfo.visibleItemsInfo.none { it.index == anchorIndex }) grid.scrollToItem(anchorIndex)
            withFrameNanos { }
            requesters.getValue(keys[anchorIndex]).requestFocus()
        }
    }
    LaunchedEffect(canRequestFocus, active, choice == null, keys) {
        if (active && canRequestFocus && choice == null && focusZone == "grid") {
            withFrameNanos { }
            focusGrid()
        }
    }
    LaunchedEffect(canRequestFocus, active, choice == null) {
        if (searchMode && active && canRequestFocus && choice == null && focusZone == "search") {
            withFrameNanos { }
            searchFocus.requestFocus()
        }
    }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler {
        // Some TV IMEs resize the window without exposing IME insets. Leaving
        // the editor must still dismiss typing before closing the catalogue.
        if (imeVisible || focusZone == "search") {
            keyboard?.hide()
            focusZone = "category"
            categoryFocus.requestFocus()
        }
        else onClose()
    }
    val window = LocalWindowInfo.current.containerSize
    val constrained = rememberConstrainedTv()
    val artSize = remember(window, constrained) { CinemaPerformance.backdropSize(window.width, window.height, constrained) }
    val wide = window.width / density.density >= 744
    val columns = if (discovering) (if (wide) 5 else 4) else if (wide) 7 else 5
    val rows = if (discovering) 2 else if (density.fontScale > 1.1f) 3 else 4
    Box(Modifier.fillMaxSize().background(VortexInk).testTag(if (searchMode) "search-screen" else if (discovering) "discover-screen" else if (picking) "quick-launch-editor" else if (history) "history-screen" else "all-apps-screen")) {
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
            TopNavBar(if (searchMode) HomeTab.SEARCH else if (discovering) HomeTab.DISCOVER else HomeTab.HOME,
                navFocus, onTab, { searchFocus.requestFocus() }, status, compact = true)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val interactions = remember { MutableInteractionSource() }
                val searchFocused by interactions.collectIsFocusedAsState()
                val shape = RoundedCornerShape(20.dp)
                BasicTextField(query, { query = it; focusZone = "search" }, Modifier.weight(1f).fillMaxHeight()
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
                                if (query.isEmpty()) Text(stringResource(if (searchMode) R.string.vortex_search_grid_hint else if (discovering) R.string.vortex_discover_search else if (history) R.string.vortex_grid_history_search else R.string.vortex_grid_search),
                                    fontSize = 14.sp, color = VortexMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                field()
                            }
                            if (query.isNotEmpty()) CinemaButton(stringResource(R.string.vortex_grid_clear_search), { query = "" },
                                Modifier.size(22.dp).testTag("app-grid-clear"), contentPadding = PaddingValues(0.dp)) {
                                Text("×", fontSize = 17.sp, color = if (searchFocused) Color.White else VortexMuted)
                            }
                        }
                    })
                val sourceLabel = sources.firstOrNull { it.key == currentPlatform }?.let { it.label ?: stringResource(R.string.vortex_content_platform_unknown) }
                    ?: stringResource(R.string.vortex_grid_category_all)
                CinemaButton((if (discovering) stringResource(R.string.vortex_content_platform, sourceLabel)
                    else stringResource(R.string.vortex_grid_filter, stringResource(type.labelRes))) + " ▾", { focusZone = "filter"; choice = GridChoice.FILTER },
                    Modifier.width(if (discovering || searchMode) 170.dp else 136.dp).fillMaxHeight().testTag("app-grid-filter").focusRequester(filterFocus)
                        .onFocusChanged { if (it.isFocused) focusZone = "filter" },
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp))
                CinemaButton(stringResource(R.string.vortex_grid_sort, stringResource(if (discovering) contentSort.labelRes else sort.labelRes)) + " ▾", { focusZone = "sort"; choice = GridChoice.SORT },
                    Modifier.width(if (discovering || searchMode) 180.dp else 156.dp).fillMaxHeight().testTag("app-grid-sort").focusRequester(sortFocus)
                        .onFocusChanged { if (it.isFocused) focusZone = "sort" },
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp))
                CinemaButton(stringResource(if (picking) R.string.vortex_quick_launch_done else R.string.vortex_grid_close), onClose,
                    Modifier.width(if (picking) 78.dp else 58.dp).fillMaxHeight().focusRequester(backFocus).testTag("app-grid-back"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(if (searchMode) R.string.vortex_search_grid_title else if (discovering) R.string.vortex_nav_discover else if (picking) R.string.vortex_quick_launch else if (history) R.string.vortex_history else R.string.vortex_all_apps), color = Color.White,
                fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
            Text(if (picking) stringResource(R.string.vortex_quick_launch_choose, quickSelection.orEmpty().size, QUICK_LAUNCH_LIMIT)
                else stringResource(if (searchMode) R.string.vortex_search_grid_subtitle else if (discovering) R.string.vortex_discover_subtitle else if (history) R.string.vortex_grid_history_subtitle else R.string.vortex_grid_all_subtitle),
                fontSize = 11.sp, lineHeight = 15.sp, color = VortexMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            val categoryRow = Modifier.fillMaxWidth().height(if (searchMode) 40.dp else 32.dp)
            Row(if (searchMode) categoryRow.horizontalScroll(rememberScrollState()).padding(3.dp) else categoryRow,
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searchMode) {
                    listOf(R.string.vortex_applications, R.string.vortex_search_grid_content).forEachIndexed { index, labelRes ->
                        val selectedMode = searchContent == (index == 1)
                        CinemaButton(stringResource(R.string.vortex_search_grid_tab_count, stringResource(labelRes),
                            if (index == 0) visible.size else contentVisible.size), {
                            focusZone = "category"; searchContent = index == 1
                        }, Modifier.fillMaxHeight().testTag(if (index == 0) "search-mode:apps" else "search-mode:content")
                            .focusRequester(modeRequesters[index]).semantics { selected = selectedMode }
                            .onPreviewKeyEvent {
                                if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                                    Key.DirectionUp -> { searchFocus.requestFocus(); true }
                                    Key.DirectionDown -> { focusZone = "grid"; focusGrid(); true }
                                    else -> false
                                }
                            }, primary = selectedMode, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp))
                    }
                    Box(Modifier.width(1.dp).height(23.dp).background(Color(0x667a86a8)))
                }
                val categories = if (discovering) DiscoverKind.entries.map { it.name to it.labelRes }
                    else AppCategory.entries.map { it.name to it.labelRes }
                categories.forEachIndexed { index, (name, label) ->
                    CinemaButton(stringResource(label), {
                        focusZone = "category"
                        if (discovering) contentKind = DiscoverKind.entries[index] else category = AppCategory.entries[index]
                    },
                        Modifier.fillMaxHeight().testTag("app-grid-category:$name")
                            .focusRequester(if (discovering) contentRequesters.getValue(DiscoverKind.entries[index])
                                else categoryRequesters.getValue(AppCategory.entries[index]))
                            .onFocusChanged { if (it.isFocused) focusZone = "category" }
                            .onPreviewKeyEvent {
                                if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                                    Key.DirectionUp -> { searchFocus.requestFocus(); true }
                                    Key.DirectionDown -> { focusZone = "grid"; focusGrid(); true }
                                    else -> false
                                }
                            }, primary = if (discovering) contentKind.name == name else category.name == name,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val tileHeight = ((maxHeight - 16.dp - 10.dp * (rows - 1)) / rows).coerceAtLeast(60.dp)
                if (keys.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(if (discovering) {
                            if (contentOrdered.isEmpty()) R.string.vortex_discover_grid_empty
                            else if (contentKind == DiscoverKind.SAVED) R.string.vortex_list_empty else R.string.vortex_discover_no_results
                        } else if (history && ordered.isEmpty()) R.string.vortex_grid_history_empty else R.string.vortex_grid_no_results),
                            color = VortexMuted, fontSize = 16.sp, modifier = Modifier.testTag("app-grid-empty"))
                    }
                } else CompositionLocalProvider(LocalBringIntoViewSpec provides GridViewport) { LazyVerticalGrid(GridCells.Fixed(columns), Modifier.fillMaxSize().testTag("app-grid")
                    .focusRestorer(tileFocus).focusGroup(), state = grid, contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(keys, key = { _, key -> key }, contentType = { _, _ -> if (discovering) "programme" else "app" }) { index, key ->
                        val input = Modifier.height(tileHeight)
                                .focusRequester(requesters.getValue(key))
                                .onFocusChanged { if (it.isFocused) { lastPackage.value = key; focusZone = "grid" } }
                                .onPreviewKeyEvent {
                                    if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                                        Key.DirectionUp -> {
                                            if (index < columns) categoryFocus.requestFocus()
                                            else moveGrid(gridNeighbour(index, keys.size, columns, 0, -1), columns)
                                            true
                                        }
                                        Key.DirectionDown -> { moveGrid(gridNeighbour(index, keys.size, columns, 0, 1), columns); true }
                                        Key.DirectionLeft -> { moveGrid(gridNeighbour(index, keys.size, columns, if (rtl) 1 else -1, 0), columns); true }
                                        Key.DirectionRight -> { moveGrid(gridNeighbour(index, keys.size, columns, if (rtl) -1 else 1, 0), columns); true }
                                        else -> false
                                    }
                                }
                        if (discovering && discover != null) {
                            val item = contentVisible[index]
                            DiscoverCard(item, isContentSaved(item, discover.saved), loader, active, revision,
                                { discover.onOpen(item) }, { discover.onSave(item) }, input)
                        } else {
                            val entry = visible[index]
                            val visit = if (history) formatVisit(entry.visitedAtMillis, dateFormatter)
                                ?: stringResource(R.string.vortex_grid_time_unknown) else null
                            AppGridTile(entry, visit, loader, active, revision, { onLaunch(entry.app.packageName) },
                                { onHold(entry.app.packageName) }, input, selectionMode = picking, selectionOrder = selectionOrder[entry.app.packageName])
                        }
                    }
                } }
            }
            GridFooter(keys, lastPackage, columns, rows, discovering)
        }
    }
    choice?.let { selectedChoice ->
        val labels = if (discovering) {
            if (selectedChoice == GridChoice.FILTER) listOf(stringResource(R.string.vortex_grid_category_all)) +
                sources.map { it.label ?: stringResource(R.string.vortex_content_platform_unknown) }
            else DiscoverSort.entries.map { stringResource(it.labelRes) }
        } else if (selectedChoice == GridChoice.FILTER) AppTypeFilter.entries.map { stringResource(it.labelRes) }
            else AppGridSort.entries.map { stringResource(it.labelRes) }
        val current = if (discovering) {
            if (selectedChoice == GridChoice.FILTER) sources.indexOfFirst { it.key == currentPlatform } + 1 else contentSort.ordinal
        } else if (selectedChoice == GridChoice.FILTER) type.ordinal else sort.ordinal
        fun dismiss() {
            choice = null
            scope.launch { withFrameNanos { }; if (selectedChoice == GridChoice.FILTER) filterFocus.requestFocus() else sortFocus.requestFocus() }
        }
        GridChoiceDialog(stringResource(if (discovering) {
            if (selectedChoice == GridChoice.FILTER) R.string.vortex_discover_choose_platform else R.string.vortex_discover_choose_sort
        } else if (selectedChoice == GridChoice.FILTER) R.string.vortex_grid_choose_filter else R.string.vortex_grid_choose_sort),
            labels, current, ::dismiss) { index ->
            if (discovering) {
                if (selectedChoice == GridChoice.FILTER) platform = sources.getOrNull(index - 1)?.key else contentSort = DiscoverSort.entries[index]
            } else if (selectedChoice == GridChoice.FILTER) type = AppTypeFilter.entries[index] else sort = AppGridSort.entries[index]
            dismiss()
        }
    }
}

@Composable
private fun GridFooter(keys: List<String>, selected: State<String?>, columns: Int, rows: Int, discovering: Boolean) {
    val pages = ((keys.size + columns * rows - 1) / (columns * rows)).coerceAtLeast(1)
    val page by remember(keys, columns, rows) { derivedStateOf {
        (keys.indexOf(selected.value).coerceAtLeast(0) / (columns * rows)).coerceAtMost(pages - 1)
    } }
    Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (discovering) stringResource(R.string.vortex_discover_count, keys.size)
            else pluralStringResource(R.plurals.vortex_app_count, keys.size, keys.size), fontSize = 10.sp, color = VortexMuted)
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
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 330.dp).testTag("grid-choice-list"),
                state = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(labels.indices)),
                verticalArrangement = Arrangement.spacedBy(10.dp)) { itemsIndexed(labels) { index, label ->
                CinemaButton(label, { pick(index) }, Modifier.fillMaxWidth().testTag("app-grid-choice:$index")
                    .then(if (index == selected) Modifier.focusRequester(focus) else Modifier), selected = index == selected)
            } }
        }
        LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
    }
}

@Preview(name = "O.E tv · Applications", locale = "tr", device = "spec:width=1920px,height=1080px,dpi=320")
@Composable
internal fun CinematicAppsPreview() {
    val apps = CinematicSamples.apps.map { nl.neerdael.backdrop_launcher.core.LauncherApp(it.packageName, it.label, "1", false, false, null, it.artwork) }
    MaterialTheme { CinematicAppsScreen(buildAppCatalog(apps, emptyList(), emptyMap()), false, CinematicSamples.featured.artwork!!, {}, {}, {}, {}) }
}

@Preview(name = "O.E tv · Discover", locale = "tr", device = "spec:width=1920px,height=1080px,dpi=320")
@Preview(name = "O.E tv · Discover RTL", locale = "ar", device = "spec:width=1920px,height=1080px,dpi=320")
@Composable
internal fun CinematicDiscoverPreview() {
    val sample = localizedSamples()
    val items = sample.watching.mapIndexed { index, item -> item.copy(id = index.toLong(),
        packageName = "sample:video", intentUri = "sample://$index", sourceName = stringResource(R.string.vortex_sample_badge), continuation = false) }
    MaterialTheme { CinematicDiscoverScreen(items, emptySet(), sample.featured.artwork!!, {}, {}, {}, {}) }
}

@Preview(name = "O.E tv · Search", locale = "tr", device = "spec:width=1920px,height=1080px,dpi=320")
@Preview(name = "O.E tv · Search RTL", locale = "ar", device = "spec:width=1920px,height=1080px,dpi=320")
@Composable
internal fun CinematicSearchPreview() {
    val sample = localizedSamples()
    val apps = sample.apps.map { nl.neerdael.backdrop_launcher.core.LauncherApp(it.packageName, it.label, "1", true, false, null, it.artwork) }
    MaterialTheme { CinematicSearchScreen(buildAppCatalog(apps, emptyList(), emptyMap()), sample.watching,
        emptySet(), sample.featured.artwork!!, {}, {}, {}, {}, {}, {}) }
}
