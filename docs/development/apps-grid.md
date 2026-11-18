# Application grid and visit history

`CinematicAppsScreen` replaces the All Apps / History text dialogs with the
same fullscreen, remote-first presentation. The current cinematic wallpaper
continues behind a dark scrim. O.E tv and the author's signature remain in the
header. Normal TV layouts show seven columns and four rows, with lazy vertical
scrolling for additional apps. Larger text uses three rows; narrow windows use
five columns. Focus uses a 140 ms scale and a painted border/glow, without blur
or continuous animation.

Search matches application labels and package names, including accent-folded
Turkish text. Category buttons combine with the TV/other-app filter. Sorting
supports locale-aware A–Z, Z–A and most recent visit. Categories first recognize
common app identities, then use Android's manifest category/system flags; they
are best-effort classifications, not provider-supplied genres. Installed apps
and user artwork use the existing bounded asynchronous icon loader. No example
apps are inserted into the real device inventory.

## Dates and data ownership

History uses `LauncherApp.lastLaunchedAt` from Backdrop's existing database.
`MainActivity.launch` and successful programme deep links record an opening
only after `startActivity` succeeds. The database stores seconds; `LegacyLibrary`
already normalizes those values to milliseconds. The grid must not multiply
them again. Display uses the effective app locale and TV time zone.

This is **last visit per installed app launched through O.E tv**, not a complete
device-wide usage log, viewing history, or list of visits inside streaming
accounts. It needs no Usage Access or Accessibility permission. Returning from
an app reloads the library so its recorded date updates. Older
`native.vortex.history` package-name-only entries remain visible with “Tarih
kaydı yok” / “Date not recorded”; no timestamp is inferred. Uninstalled and
hidden apps are omitted. No schema migration or history deletion is involved.

## Focus and performance

- Left/right/up/down use Compose's grid spatial navigation, including RTL.
  Up from the first row reaches the selected category; Down returns to the last
  app. Search is above the categories and below the navigation bar.
- OK launches; held OK opens the existing app settings/context actions. The
  grid stays mounted across app launches and restores the selected package on
  return. Closing the screen restores the invoking home shortcut.
- All Apps and History keep separate saved search/filter/sort/scroll state.
  Reordered/filtered data uses package IDs as lazy item keys.
- Package metadata is cached by package/version and read on `Dispatchers.IO`;
  searching, sorting, painting and DPAD handlers make no package-manager calls.
- Search keys and sorting are memoized. The lazy grid composes the viewport
  and a small prefetch window; it does not build a tile for every installed app.
- Icons retain the shared 144 × 144 decode budget, 4 MiB LRU and two-worker
  queue. The grid replaces the home composition, avoiding a second fullscreen
  backdrop underneath it. Backdrops remain capped at 1920 × 1080.
- Gloss/orbit/glow use cached gradients and strokes. Only focus transitions
  animate. These limits reduce rendering work; physical TV measurements are
  still required and no universal zero-stutter claim is made.

`CinematicAppsPreview` is available in Android Studio. `AppsGridTest` exercises
remote input, return focus, scrolling, search, combined filters, history dates,
sorting and RTL using explicit mock data. Mock screenshots stay separate from
private physical-device evidence.
