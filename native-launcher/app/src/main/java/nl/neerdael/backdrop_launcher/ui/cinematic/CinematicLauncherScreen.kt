package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import nl.neerdael.backdrop_launcher.R

import android.app.DownloadManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.neerdael.backdrop_launcher.core.*
import nl.neerdael.backdrop_launcher.ui.*

/** Adapter over Backdrop's existing workers, legacy storage, provider feeds and action boundary. */
@Composable
internal fun CinematicLauncherScreen(home: HomeData, library: LibrarySnapshot, prefs: PreferenceAccess,
    media: MediaAccess, updates: UpdateAccess, actions: UiActions, selection: HomeSelection,
    artwork: ComposeArtwork, active: Boolean, artworkRevision: Int, preferenceVersion: Int) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val focus = remember { CinemaFocus() }
    val style = remember(preferenceVersion) { HomeAppearance.read(prefs) }
    val motion = remember(active, preferenceVersion) { !prefs.bool("native.vortex.reduce_motion") &&
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
    // System fallback fonts include Arabic, CJK, Indic scripts and combining marks.
    val font = FontFamily.SansSerif
    val configuration = LocalConfiguration.current
    val scene = home.scene
    var rowId by remember { mutableStateOf(selection.rowId ?: scene.rows.firstOrNull()?.id) }
    val row = scene.rows.firstOrNull { it.id == rowId } ?: scene.rows.firstOrNull()
    val rowLabelRes = row?.let { defaultRowLabelRes(it.name, library.rows.firstOrNull { stored -> stored.id == it.id }?.source ?: 0) }
    val rowLabel = if (row == null) stringResource(R.string.vortex_applications)
        else rowLabelRes?.let { stringResource(it) } ?: row.name
    val apps = remember(row, resources, configuration) {
        row?.apps.orEmpty() + settingsTile.copy(label = resources.getString(R.string.vortex_settings))
    }
    var appKey by remember { mutableStateOf(selection.app?.key ?: apps.first().key) }
    val index = apps.indexOfFirst { it.key == appKey }.coerceAtLeast(0)
    var resumeKey by remember { mutableStateOf(apps[index].program?.let(::contentKey)) }
    var settings by remember { mutableStateOf<SettingsRoute?>(null) }
    var panel by remember { mutableStateOf<CinemaPanel?>(null) }
    var heroFocused by remember { mutableStateOf(false) }
    var lastZone by remember { mutableStateOf("dock") }
    val available = remember(scene.rows, scene.watchNext, home.recommendations) {
        (scene.watchNext + home.recommendations + scene.rows.flatMap { it.apps.mapNotNull(AppTile::program) }).distinctBy(::contentKey)
    }
    val choices = remember(scene.rows, scene.watchNext, home.recommendations, scene.backdrop) { featuredChoices(home) }
    val keys = remember(choices) { choices.map { it.key } }
    val explicitProgramme = available.firstOrNull { contentKey(it) == resumeKey }
    val rotation = rememberFeaturedRotation(keys, active && settings == null && panel == null &&
        !heroFocused && explicitProgramme == null)
    val choice = choices.firstOrNull { it.key == rotation.key }
    val chosenProgramme = explicitProgramme ?: choice?.programme
    val programme = remember(chosenProgramme, library.apps) { chosenProgramme?.let { item ->
        item.copy(sourceName = library.apps.firstOrNull { it.packageName == item.packageName }?.label ?: item.sourceName ?: item.packageName)
    } }
    val featured = remember(programme, choice?.backdrop, resources, configuration, home.backdropTitle, home.backdropDescription, scene.backdrop) {
        resolveFeaturedContent(home, programme, resources, choice?.backdrop ?: scene.backdrop)
    }
    var moving by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var moveBusy by remember { mutableStateOf(false) }
    var saved by remember(preferenceVersion) { mutableStateOf(prefs.strings("native.vortex.my_list").toSet()) }
    var lastShortcut by remember { mutableStateOf(Shortcut.MY_LIST) }
    val gridStates = rememberSaveableStateHolder()
    val appGrid = panel == CinemaPanel.ALL_APPS || panel == CinemaPanel.HISTORY || panel == CinemaPanel.QUICK_LAUNCH
    var quickSelection by remember(preferenceVersion) { mutableStateOf(readQuickLaunch(prefs)) }
    val quickApps = remember(library.apps, quickSelection) { quickLaunchApps(library.apps, quickSelection, context.packageName) }
    var quickDraft by remember { mutableStateOf(emptyList<String>()) }
    var quickSaving by remember { mutableStateOf(false) }
    val metadata = remember(context) { AppCatalogMetadata(context) }
    val facts by produceState<Map<String, AppFacts>>(emptyMap(), library.apps, appGrid) {
        if (appGrid) value = withContext(Dispatchers.IO) { metadata.read(library.apps) }
    }
    val legacyHistory = remember(preferenceVersion) { prefs.strings("native.vortex.history") }
    val catalog = remember(library.apps, legacyHistory, facts) { buildAppCatalog(library.apps, legacyHistory, facts) }
    val googleTv by produceState<android.content.ComponentName?>(null, context) {
        value = withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .firstOrNull { it.activityInfo.exported && it.activityInfo.packageName in
                    setOf("com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher") }
                ?.activityInfo?.let { android.content.ComponentName(it.packageName, it.name) }
        }
    }
    val identities = remember(scene.rows) { scene.rows.map { it.id to it.apps.map(AppTile::key) } }

    fun editQuickLaunch() {
        quickDraft = quickApps.map { it.packageName }
        lastZone = "quick"
        panel = CinemaPanel.QUICK_LAUNCH
    }
    fun toggleQuickApp(name: String) {
        if (quickSaving) return
        val next = toggleQuickLaunch(quickDraft, name)
        if (next == quickDraft) actions.message(resources.getString(R.string.vortex_quick_launch_limit, QUICK_LAUNCH_LIMIT))
        else quickDraft = next
    }
    fun finishQuickLaunch(nextPanel: CinemaPanel? = null) {
        if (quickSaving) return
        quickSaving = true
        val snapshot = quickDraft.toList()
        scope.launch {
            try {
                withContext(Dispatchers.IO) { prefs.putStrings(QUICK_LAUNCH_KEY, snapshot) }
                quickSelection = snapshot
                panel = nextPanel
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { actions.message(resources.getString(R.string.vortex_quick_launch_save_error)) }
            finally { quickSaving = false }
        }
    }

    fun openApp(app: AppTile) {
        if (moving != null) { moving = null; return }
        when {
            app.packageName == SETTINGS_APP -> settings = SettingsRoute("settings")
            app.program != null -> actions.play(app.program)
            else -> actions.launch(app.packageName) // MainActivity records only successful launches, with a timestamp.
        }
    }
    fun chooseApp(next: Int) {
        appKey = apps[next].key
        resumeKey = apps[next].program?.let(::contentKey)
        val r = scene.rows.indexOfFirst { it.id == row?.id }
        if (r >= 0 && next < row!!.apps.size) selection.remember(scene, r, next)
    }
    fun watchOptions() {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(
            watchOptionsUrl(featured.title, resources.configuration.let {
                @Suppress("DEPRECATION") if (android.os.Build.VERSION.SDK_INT >= 24) it.locales[0].language else it.locale.language
            })))) }
        catch (_: android.content.ActivityNotFoundException) { actions.message(resources.getString(R.string.vortex_content_no_browser)) }
    }
    fun step(delta: Int) {
        val reorder = moving
        if (reorder == null) chooseApp(VortexGeometry.step(index, delta, apps.size))
        else if (!moveBusy) scope.launch {
            moveBusy = true
            try {
                withContext(Dispatchers.IO) { actions.library.moveApp(reorder.first, reorder.second, delta) }
                actions.refresh()
            } catch (_: Exception) { actions.message(resources.getString(R.string.vortex_error_move_app)) }
            finally { moveBusy = false }
        }
    }
    LaunchedEffect(active, settings != null, panel != null) {
        if (active && settings == null && panel == null) {
            withFrameNanos { }
            when (lastZone) {
                "quick" -> focus.quickLaunch.requestFocus()
                "nav" -> focus.nav.requestFocus()
                "shortcuts" -> focus.shortcuts.requestFocus()
                else -> focus.dock.requestFocus()
            }
        }
    }
    // Updates reconcile stable identities without stealing focus from nav, hero or a dialog.
    LaunchedEffect(identities) {
        if (apps.none { it.key == appKey }) {
            appKey = apps.first().key
            resumeKey = apps.first().program?.let(::contentKey)
        }
        if (moving != null && row?.apps?.none { it.packageName == moving?.second } != false) moving = null
    }
    CompositionLocalProvider(LocalVortexMotion provides motion,
        LocalTextStyle provides TextStyle(color = Color.White, fontFamily = font)) {
        Box(Modifier.fillMaxSize().onPreviewKeyEvent {
            if (settings == null && panel == null && it.type == KeyEventType.KeyDown && it.key == Key.Menu) {
                settings = SettingsRoute("settings"); true
            } else false
        }) {
            if (!appGrid) CinematicHomeScreen(featured, scene.backdrop, apps, index, quickApps,
                "‹  $rowLabel  ›", focus, ::step, ::openApp,
                onHold = { app ->
                    if (moving != null) moving = null
                    else settings = if (app.packageName == SETTINGS_APP) SettingsRoute("settings")
                        else if (app.program != null) SettingsRoute("row", rowId = row?.id)
                        else SettingsRoute("app", rowId = row?.id, packageName = app.packageName)
                }, onRow = { delta ->
                    if (scene.rows.isNotEmpty()) {
                        val current = scene.rows.indexOfFirst { it.id == row?.id }.coerceAtLeast(0)
                        val next = VortexGeometry.step(current, delta, scene.rows.size)
                        rowId = scene.rows[next].id
                        val remembered = selection.focusForRow(scene, next)?.key
                        val selected = scene.rows[next].apps.firstOrNull { it.key == remembered } ?: scene.rows[next].apps.first()
                        appKey = selected.key
                        resumeKey = selected.program?.let(::contentKey)
                        moving = null
                    }
                }, onRows = { settings = SettingsRoute("rows") },
                onWatch = { featured.playable?.let(actions::play) ?: watchOptions() },
                onInfo = { panel = CinemaPanel.INFO },
                onTab = { tab ->
                    lastZone = "nav"
                    when (tab) {
                        HomeTab.HOME -> focus.dock.requestFocus()
                        HomeTab.DISCOVER -> panel = CinemaPanel.DISCOVER
                        HomeTab.MY_LIST -> panel = CinemaPanel.MY_LIST
                        HomeTab.SEARCH -> panel = CinemaPanel.SEARCH
                    }
                },
                onShortcut = { shortcut ->
                    lastZone = "shortcuts"
                    lastShortcut = shortcut
                    if (shortcut == Shortcut.DOWNLOADS) {
                        try { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                        catch (_: android.content.ActivityNotFoundException) { actions.message(resources.getString(R.string.vortex_error_no_downloads)) }
                        catch (_: SecurityException) { actions.message(resources.getString(R.string.vortex_error_downloads_unavailable)) }
                    } else panel = when (shortcut) {
                        Shortcut.MY_LIST -> CinemaPanel.MY_LIST
                        Shortcut.HISTORY -> CinemaPanel.HISTORY
                        else -> CinemaPanel.ALL_APPS
                    }
                }, onQuickLaunch = { app -> lastZone = "quick"; actions.launch(app.packageName) },
                onQuickEdit = ::editQuickLaunch, onQuickFocus = { lastZone = "quick" }, onDockFocus = {
                    lastZone = "dock"; resumeKey = apps[index].program?.let(::contentKey)
                },
                onNextFeatured = if (choices.size > 1) ({ resumeKey = null; rotation.next() }) else null,
                onHeroFocus = { heroFocused = it },
                loader = artwork, active = active, revision = artworkRevision, reordering = moving != null,
                restoreShortcut = lastShortcut, googleTvAvailable = googleTv != null,
                onGoogleTv = { googleTv?.let { component ->
                    lastZone = "quick"
                    try { context.startActivity(Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    catch (_: android.content.ActivityNotFoundException) { actions.message(resources.getString(R.string.vortex_content_google_tv_error)) }
                    catch (_: SecurityException) { actions.message(resources.getString(R.string.vortex_content_google_tv_error)) }
                } }, status = { StatusBar(style, active, actions) })
            settings?.let { route ->
                LauncherSettings(route, library, prefs, media, updates, actions, artwork, active, artworkRevision,
                    close = { settings = null }, reorder = { id, name -> moving = id to name; appKey = name; settings = null })
            }
            panel?.let { title ->
                if (appGrid) gridStates.SaveableStateProvider(title.name) {
                    val picking = title == CinemaPanel.QUICK_LAUNCH
                    CinematicAppsScreen(if (picking) catalog.filter { it.app.packageName != context.packageName } else catalog,
                        title == CinemaPanel.HISTORY, featured.artwork ?: scene.backdrop,
                        onClose = { if (picking) finishQuickLaunch() else panel = null },
                        onLaunch = { if (picking) toggleQuickApp(it) else actions.launch(it) },
                        onHold = { if (picking) toggleQuickApp(it) else settings = SettingsRoute("app", packageName = it) },
                        onTab = { tab ->
                            val nextPanel = when (tab) {
                                HomeTab.HOME -> null
                                HomeTab.DISCOVER -> CinemaPanel.DISCOVER
                                HomeTab.MY_LIST -> CinemaPanel.MY_LIST
                                HomeTab.SEARCH -> CinemaPanel.SEARCH
                            }
                            if (picking) finishQuickLaunch(nextPanel) else panel = nextPanel
                        }, loader = artwork, active = active, canRequestFocus = settings == null && !quickSaving,
                        revision = artworkRevision, quickSelection = if (picking) quickDraft else null,
                        status = { StatusClock(style, active, compact = true) })
                } else CinemaBrowserPanel(title, library.apps.filter { !it.hidden }, available, featured, saved,
                    close = { panel = null }, watchOptions = ::watchOptions, launch = { name ->
                        panel = null; openApp(AppTile(name, library.apps.firstOrNull { it.packageName == name }?.label ?: name, null))
                    }, play = { panel = null; actions.play(it) }, toggleSaved = { item ->
                        val id = contentKey(item)
                        saved = if (isContentSaved(item, saved)) saved - id - "id:${item.id}" else saved + id
                        val snapshot = saved.toList().take(200)
                        scope.launch(Dispatchers.IO) { prefs.putStrings("native.vortex.my_list", snapshot) }
                    })
            }
        }
        BackHandler(settings == null && panel == null) {
            if (moving != null) moving = null
            else if (lastZone != "dock") { lastZone = "dock"; focus.dock.requestFocus() }
            else actions.homeBack(style.backAction)
        }
    }
}

@Composable
private fun StatusBar(style: HomeAppearance, active: Boolean, actions: UiActions) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (style.showNetwork) NetworkStatus(active, actions::openNetworkSettings)
        StatusClock(style, active, compact = true)
    }
}

@Composable
private fun CinemaBrowserPanel(panel: CinemaPanel, apps: List<LauncherApp>, programmes: List<ResumeItem>,
    featured: FeaturedContent, saved: Set<String>, close: () -> Unit, watchOptions: () -> Unit,
    launch: (String) -> Unit, play: (ResumeItem) -> Unit, toggleSaved: (ResumeItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    val first = remember { androidx.compose.ui.focus.FocusRequester() }
    Dialog(close, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.width(700.dp).heightIn(max = 460.dp).background(VortexInk, RoundedCornerShape(24.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(panel.titleRes), Modifier.weight(1f), fontSize = 26.sp, maxLines = 2)
                CinemaButton(stringResource(R.string.vortex_close), close, Modifier.focusRequester(first))
            }
            if (panel == CinemaPanel.INFO) {
                Text(featured.title, fontSize = 22.sp)
                Text(featured.platform?.let { stringResource(R.string.vortex_content_platform, it) }
                    ?: stringResource(R.string.vortex_content_platform_unknown), color = VortexAccent, fontSize = 13.sp)
                Text(featured.description, color = VortexMuted, fontSize = 16.sp)
                featured.playable?.let { item ->
                    CinemaButton(stringResource(R.string.vortex_watch_now), { play(item) }, primary = true)
                    CinemaButton(stringResource(if (isContentSaved(item, saved)) R.string.vortex_remove_from_list else R.string.vortex_add_to_list), { toggleSaved(item) })
                } ?: run {
                    CinemaButton(stringResource(R.string.vortex_content_watch_options), watchOptions, primary = true)
                    Text(stringResource(R.string.vortex_content_artwork_note), color = VortexMuted, fontSize = 14.sp)
                }
            } else {
                if (panel == CinemaPanel.SEARCH) BasicTextField(query, { query = it }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                    modifier = Modifier.fillMaxWidth().background(Color(0xff20283b), RoundedCornerShape(10.dp)).padding(16.dp),
                    decorationBox = { inner -> if (query.isEmpty()) Text(stringResource(R.string.vortex_search_hint), color = VortexMuted, fontSize = 16.sp); inner() })
                val filteredApps = when (panel) {
                    CinemaPanel.MY_LIST, CinemaPanel.DISCOVER -> emptyList()
                    else -> apps.filter { it.label.contains(query, true) }
                }
                val filteredProgrammes = when (panel) {
                    CinemaPanel.MY_LIST -> programmes.filter { isContentSaved(it, saved) }
                    CinemaPanel.SEARCH -> programmes.filter { it.title.contains(query, true) }
                    CinemaPanel.DISCOVER -> programmes
                    else -> emptyList()
                }
                if (filteredApps.isEmpty() && filteredProgrammes.isEmpty()) Text(stringResource(when (panel) {
                    CinemaPanel.MY_LIST -> R.string.vortex_list_empty
                    CinemaPanel.HISTORY -> R.string.vortex_history_empty
                    CinemaPanel.DISCOVER -> R.string.vortex_discover_empty
                    else -> R.string.vortex_search_empty
                }), color = VortexMuted, fontSize = 16.sp)
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(filteredProgrammes, key = { i, item -> "programme:${contentKey(item)}:$i" }) { _, item ->
                        CinemaButton("▶  ${item.title}", { play(item) }, Modifier.fillMaxWidth(),
                            onLongClick = { toggleSaved(item) })
                    }
                    itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { _, app ->
                        CinemaButton(app.label, { launch(app.packageName) }, Modifier.fillMaxWidth())
                    }
                }
                if (panel == CinemaPanel.DISCOVER || panel == CinemaPanel.MY_LIST) Text(stringResource(R.string.vortex_save_programme_hint), color = VortexMuted, fontSize = 12.sp)
            }
        }
        LaunchedEffect(Unit) { first.requestFocus() }
    }
}
