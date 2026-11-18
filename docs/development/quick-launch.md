# Quick Launch

The cinematic home's bottom-left area now contains **Quick Launch** instead
of Continue Watching. This choice does not add playback tracking, notification
access, account access or a continuously running collector. Existing Watch Next
data/provider support remains available to the classic launcher and featured
content pipeline.

## Usage

- Left/Right moves between at most six favourite applications; OK launches the
  focused application through the existing launch/history boundary.
- Up opens the Edit header; another Up returns to the app dock. Down from Edit
  returns to the remembered favourite. The last tile leads to Quick Shortcuts.
- Long OK on a favourite opens Edit. The editor reuses the searchable, filtered,
  remote-controlled All Apps grid, with numbered selected tiles.
- OK selects/removes an app. The selection order defines the row order; remove
  then select an app again to move it to the end. The limit is six.
- Done or Back saves the ordered selection and returns Home. An explicit empty
  list stays empty after restart, with a reachable Edit button.
- The Google TV button remains available in the row header when the system
  exposes its HOME activity.

Only installed, visible apps appear. A removed/hidden app disappears from the
row; the launcher itself is excluded. Unconfigured installations seed a bounded
selection from available familiar apps, then other available apps. Samples never
enter the installed-app inventory.

## Implementation

`QuickLaunch.kt` handles default/saved selection, filtering, order and capacity.
`QuickLaunchRow.kt` renders real package icons using the existing shared icon
loader. `CinematicLauncherScreen` loads/saves the ordered package list under
`native.vortex.quick_launch.apps` and opens the picker; preference writes run
off the main thread. `CinematicAppsScreen` adds an optional selection mode while
its ordinary All Apps/History launch behaviour stays intact.

The row has six icons at most, each decoded to the existing 144px budget within
the shared 4MiB cache. Focus uses the existing 160ms transform animation; there
is no idle animation, screen capture, polling, extra SDK or new permission.
EN/TR/ES/VI/AR strings and Compose previews are included. These bounded choices
are not a substitute for measuring frame times on a physical television.

Validation results and the installed APK hash are recorded in the ignored
`deliverables/oe-tv-quick-launch-validation.json`; screenshots and personalized
app inventories must not be published as source assets.

## Validation — 18 November 2026

- 54 focused JVM tests passed, including default/explicit-empty selection,
  hidden/uninstalled/self filtering, saved order, capacity, and locale catalogues.
- 28 Android TV UI tests passed together (99.862s), including three new picker,
  persistence and RTL remote tests. An additional narrow-row scroll/anchor test
  passed separately (4.517s), for 29 successful UI checks on the same UI revision.
- Optimized release and lint passed: zero errors, 66 warnings. APK signature
  verification passed. This is not a claim that all pre-existing host tests or
  all lint warnings have been resolved.
- Physical TCL API 30: six real installed icons appeared under the Turkish
  heading. Removing the last favourite survived a forced launcher restart;
  re-adding it restored all six in their original order after another restart.
- DPAD navigation and Select opened the installed YouTube app. Returning to
  O.E tv restored focus to that same favourite. The launcher was left on Home
  with the original six favourites, and no playback collector was enabled.

The first physical remote attempt followed touch-driven setup and found focus
on the top navigation instead of the dock. A fresh semantic focus check then
guided the verified navigation/launch test; no unverified Select was dispatched.
Frame-rate improvement and exact TV RAM overhead were not benchmarked here.
