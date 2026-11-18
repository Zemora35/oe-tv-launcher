# Cinematic Vortex prototype

## Internationalization

The current UI uses Android `vortex_strings.xml` resources, not hardcoded
Turkish strings. The English default is complete; five cinematic-home locales
are shipped with per-key fallback and Android 13+ locale configuration. Legacy
settings use native resource aliases plus the original ARB packs. Dialog and
tab routing uses enum identities independent of display language. The dock,
gradient and remote directions mirror for RTL; the centre stays at x=.5 with
scale 1.35. System fallback fonts support non-Latin scripts. Catalog, formatting,
plural and RTL tests are described in [the localization guide](../localization.md).

## Foundation and file plan

The foundation is `johnneerdael/Backdrop-Launcher`, commit
`1173459cbcb1727d0beccb39d44ccac923a1c1d9`. Production is `native-launcher/`, Kotlin 2.3.21,
Compose 1.11.4, TV Material 1.1.0. Historical Flutter and `native-benchmark/`
are not production entry points. Source inspection is sufficient; no binary
analysis or Ghidra session is needed. Graphify detection reports 41 source files
and a corpus small enough for direct inspection.

1. `MainActivity.kt`: route production Home into the cinematic host; continue
   scheduling library, backdrop and provider work on the existing workers.
2. `core/HomeData.kt`: carry the real backdrop title to the presentation layer.
3. `core/ArtworkDecoder.kt`: add an icon-only package scheme so square dock tiles
   use real installed application icons instead of TV banners.
4. `ui/cinematic/`: isolated models, deterministic vortex geometry, remote input,
   presentation components, screen host and preview/sample fixtures.
5. `LauncherSettings.kt`: retain existing row/artwork/Trakt/clock controls and
   expose cinematic/classic and reduced-motion switches.
6. `app/src/test` and `app/src/androidTest`: geometry and remote navigation checks.

## Components and state

`CinematicLauncherScreen` adapts existing `HomeData`, `LibrarySnapshot`,
`HomeSelection`, `PreferenceAccess`, `MediaAccess` and `UiActions`.
`CinematicHomeScreen` is the previewable presentation boundary.
`TopNavBar`, `StatusBar`, `HeroBanner`, `VortexDock`, `VortexDockItem`,
`ContinueWatchingRow` and `QuickShortcutsPanel` each own a small visual concern.
Existing `LauncherSettings` handles configuration and app context actions.
The classic screen stays available for comparison and original layout controls.

## Vortex and focus

One stationary focus target owns the dock. Left/right changes the logical app
index, independent of the spatial order. A bounded window (13 tiles maximum)
is composed around that index. Deterministic poses are cached; graphics-layer
translation, scale and alpha animate for 160 ms without per-frame remeasurement.
The centre is dominant, near neighbours shrink progressively, and distant tiles
fade. Four distant apps curl behind the foreground arc into the spiral's
vanishing point. Input can interrupt motion. Ends clamp; no long backwards wrap animation.
Up exits to the row selector; Down enters Continue Watching/shortcuts. The row
selector uses Left/Right for custom rows, Select for row management. Held Select
opens existing app actions; reorder uses the original library transaction.

## Data and honest empty states

Live content comes only from the public Watch Next/channel providers and current
backdrop. Artwork-only backdrops have no fabricated genre/HDR/runtime/playback.
My List stores user-selected provider identities; History records apps opened
through this screen. Downloads delegates to Android's downloads activity. Search
filters installed apps and available provider programmes. Previews alone use
clearly marked sample content; production never invents installed services or
HDMI/mirroring capabilities. Settings remains available even without app rows.

## Performance budget and validation

Keep the existing two-worker, bounded-queue artwork loader, 4 MiB retained cache,
size-aware decoder, cancellation on pause and package invalidation. No extra
image library, runtime blur, particle system, continuous parallax or idle pulse.
One backdrop plus two cached scrim brushes. Layer alpha uses ModulateAlpha for
non-overlapping tile content. Lazy Continue Watching and app-browser content.
Respect the system animator duration scale and explicit reduced-motion setting.
Release APK/measurements on a target TCL remain required before claiming a
stable device frame rate. Frame deadline: 16.67 ms at 60 Hz; inspect p95/p99 frame
duration, jank and memory with repeated DPAD, rapid reversals, provider updates,
app resume and low-memory/recreation scenarios.

Build: from `native-launcher`, use Java 17+ and Android SDK 36. The project's
`android.overridePathCheck` allows the requested Windows workspace name.
Windows JVM unit-test class loading still requires an ASCII path; a temporary
`subst V: <workspace-root>` mapping works without moving any sources. Run from
`V:/native-launcher` so shared artwork remains available at `V:/backdrops`.
Host test locale is pinned to English because Conscrypt otherwise looks for
`conscrypt_openjdk_jni-wındows-x86_64` under a Turkish default locale.

`assembleVortexRelease` builds the independent `nl.neerdael.backdrop_launcher.vortex`
Home-capable application, labelled Vortex TV. It can coexist with upstream
Backdrop; its official-upstream self-updater is disabled. Storage is isolated
by Android's package sandbox. No installed launcher is replaced automatically.
The local APK uses Gradle's development signing key unless a production key
is explicitly supplied through the original environment-variable interface.
Use `benchmarkDebug`/`benchmarkRelease` for a separate package without a Home
filter or self-updater. Never replace the television's home launcher to test.

References: official [focus navigation](https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-behavior),
[Compose performance](https://developer.android.com/develop/ui/compose/performance),
and [Backdrop source](https://github.com/johnneerdael/Backdrop-Launcher).
