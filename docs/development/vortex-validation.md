# Vortex TV validation — 18 November 2026

This file preserves earlier prototype test rounds. The latest cinematic home
uses [Quick Launch, with its current validation](quick-launch.md); the featured
carousel is covered in [the rotation report](featured-rotation.md). Referenced
`deliverables/` diagnostics are local, ignored files and are not published.

## Provider playback, Google TV comparison and grid focus revision

Featured artwork, programme metadata, source app and playback URI now stay
together. Real published recommendations show their platform and Watch action;
wallpaper-only titles offer a verified JustWatch search rather than the app
dock. Source catalogue descriptions retain their provider language. Ten new
UI messages have matching EN/TR/ES/VI/AR resources. See
[playback sources and limits](featured-playback.md).

The final cached-paint APK passed 47 focused JVM tests and all 22 TV emulator
tests (71.824 seconds). Release build/signature verification passed; lint has
zero errors and 62 warnings. The historical full host suite was not rerun.
Earlier UI runs caught focus loss when entering an offscreen lazy-grid row.
Logical row/column navigation, stable package/category requesters and minimal
bounded scrolling fixed it; the final remote test checks every row transition,
held OK, return focus and last-row clamping. Paint halos no longer read animated
values, preserving cached tile contents during scale/position animation.

On the physical TCL Android 11 TV (1920 × 1080, density 320, 2 GiB RAM), the
actual provider recommendation and More Info showed matching Crunchyroll
programme/artwork/source data. Provider-account playback itself was not
exercised. The new Google TV header button opened launcherx HomeActivity, and
O.E tv returned successfully. The installed app grid displayed 43 real apps and
reached its second page with the selected card retained. The final APK was
installed and the device was left on O.E tv Home. Google TV's Home role and
recovery applications were not disabled or replaced.

Google TV's Continue Watching UI visibly had three YouTube cards, two with
Resume/progress. The test app's public-provider query returned thirty TCL
browser NEW entries and fourteen YouTube recommendation/trending preview
entries, none with playback positions. The public data does not mirror those
Google TV continuation cards. The O.E tv Google TV/YouTube buttons are shortcuts,
not an automatic import. No private history or pixel-estimated progress was
inserted. Temporary diagnostic packages were removed.

The final short warmed directional capture reported 121 frames with 31 janky
frames (25.62%); UI percentiles were 14/29/32/34 ms at 50/90/95/99%. One snapshot
had PSS 174,757 KiB and RSS 269,477 KiB. This is not evidence of zero-stutter
rendering or a controlled before/after improvement. Earlier mixed/first-scroll
captures differ in workload and memory warm-up; GPU percentile overflow values
from this TV were not interpreted as reliable timings. Some frame delays remain
despite reduced repaint/layout work. Further dedicated hardware profiling is
needed before making stronger performance claims.

Local ignored evidence: `oe-tv-featured-playback-tests.txt`,
`tv-feed-counts.json`, `google-tv-continue.png`,
`oe-tv-on-tcl-verified-info.png`, `oe-tv-on-tcl-final-grid.png`,
`tv-cached-paint-grid-gfxinfo.txt`, `tv-cached-paint-grid-meminfo.txt` and
`oe-tv-on-tcl-final-home.png`, all under deliverables. These contain device data
and must not be published. Final APK SHA-256:
`DDF2029E004999CFF3D387F02875158970708BF3E6E50710DAA2234FC6D6B4D5`.
No GitHub publication was performed.

## Fullscreen application grid and timestamped history revision

All Apps and History now share the cinematic seven-column grid, search,
categories, type filters and sorting. Last-visit timestamps come from the
existing successful-launch database records; package-name-only legacy history
has an explicit unknown-time label. See [data and interaction details](apps-grid.md).

The final build passed 23 focused JVM tests and 19 Android TV emulator tests
(56.744 seconds). The latter include five new grid tests covering physical
directional input/scrolling, launch/hold actions, search and combined filters,
real/unknown date formatting, sorting and mirrored layout. The fourteen
existing content, localization, home layering and input tests passed again.
Release lint reports zero errors and 59 warnings; assembly and APK signature
verification passed. An API-24 comparator call was replaced for API-23
compatibility, and fallback symbols were fitted to history cards before the
final run. The historical full host suite was not rerun.

The final APK was installed on the user's TCL Android 11 TV at 1920 × 1080,
density 320. The actual All Apps screen displayed 43 installed applications
with package icons. History displayed existing recorded visits in Turkish.
Opening Android TV Settings from History and returning updated its visit time,
moved it to the first position under Recent sorting, and retained focus on that
same app. No streaming-account history or sample inventory was inserted.

A short mixed-navigation capture on the physical TV reported 178 frames, 21
janky frames (11.80%), UI frame percentiles of 8/18/19/23 ms at 50/90/95/99%,
and zero slow UI-thread / bitmap-upload counts. It is a limited capture across
navigation/filter states, not a controlled full-inventory grid benchmark or a
zero-stutter result. One memory snapshot reported PSS 108,192 KiB and RSS
194,437 KiB. Further long-duration acceptance remains appropriate.

Local ignored evidence: `deliverables/oe-tv-apps-grid-tests.txt`,
`deliverables/oe-tv-on-tcl-history.png`,
`deliverables/oe-tv-on-tcl-history-return.png`,
`deliverables/tv-apps-grid-gfxinfo.txt` and
`deliverables/tv-apps-grid-meminfo.txt`. Physical screenshots contain private
device inventory/history and must remain outside public repository assets.
Final APK SHA-256:
`D90B0947997F88224B889CEBE00D7522F170E3F72563219957A0374E640FF3D2`.
No GitHub publication was performed.

## Paired film metadata and unfinished playback revision

All ten bundled backgrounds now resolve a matching name and synopsis offline;
TMDB-backed images carry localized title/overview data through the wallpaper
cache. Missing programme art no longer puts another programme's name over an
unrelated background. Compact heroes keep their synopsis visible.

Nineteen focused JVM tests passed, including continuation eligibility, time
calculation, translated resource catalogs and TMDB metadata. Four new Android
content tests plus the ten existing TV/remote/RTL tests passed (14 total,
37.243 seconds). Final release assembly/signature verification passed; lint
reports zero errors and 59 warnings. Spanish many/Vietnamese quantity forms
were corrected after the UI run; the final resource-only build/lint passed.

The updated APK was installed on the user's TCL Android 11 TV. TV-listings
permission and the previously disabled Continue Watching setting were enabled.
The real application initially read recommendation/web-link entries despite an
ADB-shell query returning no rows; the shell result was therefore not treated
as an authoritative inventory. The new filter requires concrete playback
progress and excludes recommendations, completed and hidden items.

Physical-device verification showed both Forrest Gump and then Ucuz Roman with
their matching Turkish synopsis and background. The real refresh action was
activated. After filtering, the TV currently exposes no eligible unfinished
playback entries, so the honest empty state remains. Private account histories
were not accessed, and no sample history was inserted. Real programme-resume
launch could not be exercised on this TV without an eligible shared record.

Local ignored evidence: `deliverables/oe-tv-content-emulator-tests.txt`,
`deliverables/oe-tv-content-validation.json`, `deliverables/tv-content-refreshed.xml`
and `deliverables/oe-tv-on-tcl-content.png`. Current behavior and platform limits:
[content/data notes](cinematic-content.md).

## Hero layering and TV render budget revision

The dock now paints below the hero; rounded hero action surfaces are opaque.
Netflix cannot paint over Browse Apps/More Info, while tile edges and the centre
card stay intact. A pixel regression compares full-height interior strips of
the buttons with/without the overlapping Netflix tile in LTR and RTL. Turkish
and Arabic screenshots were visually inspected. No rectangular mask remains.

Ten focused JVM tests and all ten emulator tests passed; the final instrumented
run took 42.466 seconds. Release build, signing verification and lint passed
(zero errors, 57 warnings). Earlier capture attempts hit a screenshot timeout
and an emulator-host exit; the final run used a fresh, isolated emulator without
concurrent Gradle work. These were not recorded as successful test runs.

On the final release, 80 alternating directional inputs left the same process
alive and the launcher resumed. An eight-second idle window produced zero new
frames. The software SwiftShader navigation capture reported 159 frames and 103
janky frames (64.78%): this is **not** evidence of smooth physical-TV rendering.
The 2-core/1-GiB API 34 x86 emulator has no representative TV GPU. Physical TCL/
other-TV acceptance is still required; no universal zero-stutter claim is made.
One post-navigation snapshot measured PSS 68,208 KiB, RSS 128,232 KiB and swap
PSS 711 KiB; it is not a controlled before/after memory comparison.

Local ignored evidence: `deliverables/oe-tv-optimized-emulator-tests.txt`,
`deliverables/oe-tv-performance-summary.json`, `deliverables/oe-tv-*-gfxinfo.txt`
and `deliverables/oe-tv-navigation-meminfo.txt`. The updated APK is
`deliverables/oe-tv-launcher.apk`; the earlier filename remains an updated alias.
See [rendering-budget details](vortex-performance.md) for the actual optimizations.

## O.E tv branding revision

The visible application label is now `O.E tv` in the Vortex release. The home
header displays that name with a transparent handwritten `Oğuzhan Eklidağ`
signature underneath. Turkish and Arabic/RTL screenshots were inspected after
the change; the signature stays below the brand without displacing navigation.

Release/debug builds and eight focused JVM tests passed. All nine existing TV
emulator tests passed again (30.099 seconds). Release lint reports zero errors
and 57 warnings; four additional `IconDensities` warnings reflect the deliberate
single xxxhdpi signature bitmap, which Android scales for other densities.
APK signing verification passed and the packaged English/Turkish application
labels were checked with `aapt`. Local APK: `deliverables/oe-tv-launcher.apk`;
the earlier `deliverables/vortex-tv-launcher.apk` alias is updated as well.

## Current internationalization revision

The forced Turkish locale has been removed. Vortex follows Android's effective
app/system locale and uses native resource fallback. The cinematic home has 63
messages in English, Turkish, Spanish, Vietnamese and Arabic. Native legacy
resource coverage is EN 230/230, TR 230/230, ES 22/230, VI 40/230, AR 0/230;
the original Spanish/Vietnamese ARB packs supply additional legacy labels.
Missing fields fall back to English. No claim is made that every language or
every legacy settings page is fully translated.

Eight focused JVM tests passed: geometry (4), formatting/locale candidates/user-name preservation (3),
and resource-key/placeholder validation (1). Nine Android TV emulator tests
passed across `InternationalHomeTest`, `VortexRemoteTest` and `AppCardInputTest`.
They cover five locales, regional/unsupported-language fallback, plural rules,
RTL physical D-pad input, the fixed enlarged centre tile, and eight screenshot
scenarios including CJK/emoji, expanded `en-XA` at larger text and RTL `ar-XB`.
The expanded-text check also asserts that hero actions do not overlap the app
lane. Long titles/large text use a compact hero; full descriptions remain in
the information panel.

The final optimized build and `lintVortexRelease` completed successfully:
0 errors, 53 warnings. The complete historical host suite was not rerun;
earlier platform-dependent baseline failures remain documented below.

Local artifacts are ignored by Git: `deliverables/international-tests.txt`,
`deliverables/translation-coverage.json` and `deliverables/i18n/`. English and
Turkish READMEs, a translation contribution guide and a build-only Vortex CI
workflow are included. Upstream publishing workflows are repository-gated.
No GitHub repository, release, push or public deployment was created.

The final Vortex APK was also installed and launched on the API 34 TV emulator.
Android's per-app locale changed the real launcher to Turkish (`Ana Sayfa`,
`TV Uygulamaları`); clearing that override restored English from the device.
This check did not change the system language or touch the connected physical TV.

The sections below preserve earlier prototype validation history. They do not
mean the Turkish locale is still forced or that the complete historical unit
suite was rerun for this revision.

## Türkçe arayüz ve merkez odak güncellemesi

Ana ekran, örnek içerik, menüler, ayar başlıkları/açıklamaları, diyaloglar,
erişilebilirlik açıklamaları ve kumanda ipuçları Türkçeleştirildi. Türkçe tarih
biçimleri Vortex uygulamasına özel uygulanır; TV'nin sistem dili değiştirilmez.
Paket kimlikleri, marka adları ve kullanıcı verileri korunur.

Seçili kart dock'un yatay merkezinde (`x = 0.5`) durur ve normal boyutunun
%135'ine büyür. Komşu kartın ölçeği %90'dır. Sağ/Sol geçişi sonrasında yeni
seçimin aynı merkeze gelmesi, görsel sınırları ölçen emülatör testiyle doğrulandı.
Uzun Türkçe kart ve kısayol adları iki satırda gösterilir.

Bu revizyonda 6 hedefli birim testi (4 geometri, 2 Türkçe metin) ve 6 emülatör
testi başarılıdır. Önceki geniş test taramasının sonuçları aşağıda ayrı olarak
korunmuştur; bu revizyon için tüm paket yeniden çalıştırılmış sayılmaz.
`lintVortexRelease` sonucu: 0 hata, 48 uyarı.

Kanıtlar: [Türkçe ekran](../../deliverables/vortex-preview-tr.png),
[birim testi sonuçları](../../deliverables/focused-tests-tr.json),
[kumanda testi sonuçları](../../deliverables/remote-tests-tr.txt).

## Initial prototype validation

The native Backdrop activity now opens the cinematic screen by default. The
reference composition is implemented as a left hero, a foreground app arc
curling into a distant spiral, and lower Continue Watching / shortcut panels.
The installed package icon is preferred over the television banner. Gloss,
focus scale and halo use drawing primitives and graphics layers.

The original custom-row database, media providers, artwork loader, settings,
screensaver and launcher actions remain the underlying implementation. The
classic home can be selected in Settings → Interface. Search, My List and
History have working local interactions; missing content has explicit empty
states. Weather and unsupported device inputs are not fabricated.

## Checks performed

| Check | Observed result |
| --- | --- |
| Kotlin / Android compilation | Passed for the new home and test components |
| Optimized Home-capable APK | `assembleVortexRelease` completed; R8 and resource shrinking enabled |
| Android lint | `lintVortexRelease`: 0 errors, 47 warnings |
| Unit tests | 149 executed; 135 passed, 14 failed |
| New geometry tests | 4 passed: focus dominance, monotonic depth, bounded residents, logical navigation and inward tail |
| Remote / rendering tests | 5 passed across `VortexRemoteTest` and existing `AppCardInputTest` |
| APK installation | Installed successfully on the isolated Android TV emulator |
| Activity launch | `nl.neerdael.backdrop_launcher.vortex/.MainActivity` opened successfully |
| Home registration | Package query returned one activity handling MAIN + HOME |
| Live remote flow | Down/Right/OK reached All Apps from the real launcher activity; UI hierarchy confirmed the panel and Close action |
| Package integrity | APK signature verified; archive includes 10 offline backdrops, 65 app images and Inter font |
| Visual inspection | Captured and inspected the native Compose sample screen; fixed dark preview text, hero/tile overlap and truncated shortcut labels |
| Whitespace check | `git diff --check` passed |

The remote tests cover repeated right/right/left input, activation of the
incoming selection, stationary dock focus, long OK without accidental launch,
navigation between dock/row picker/Continue Watching and screenshot rendering.
The screenshot fixture uses bundled sample content rather than claiming that
streaming services are installed or supplying playback on the emulator.

## Existing Windows test failures

The complete unit suite is **not green**. Ten `LegacyLibraryTest` cases fail with
`SQLiteCantOpenDatabaseException`; four `NativeMediaTest` cases fail when
asserting persisted selection/cache state. No passing result is claimed for
these tests.

To check attribution, an untouched source archive of upstream commit
`1173459cbcb1727d0beccb39d44ccac923a1c1d9` was built separately under an ASCII
drive path with the same English JVM test locale. The two affected suites ran
44 tests and reproduced **the same 14 failing test names**. Their media and
database implementations were not changed to hide these failures.

The machine's original Turkish JVM locale also caused Conscrypt's native
library lookup to use `wındows`. The test-only locale configuration fixes that
environment issue without changing the app's locale. An ASCII `subst` path
was needed for JVM class loading in this Windows workspace.

Machine-readable suite results and failing test names are in
[`deliverables/test-summary.json`](../../deliverables/test-summary.json).
The lint report is in
[`deliverables/lint-vortex-release.xml`](../../deliverables/lint-vortex-release.xml).

## Limits and performance

Validation device: Android TV API 34 x86 emulator, 1920×1080, density 320,
2 virtual CPU cores, 1 GiB guest RAM, SwiftShader GPU, Windows Hypervisor.
Operations explicitly targeted `emulator-5580`. The real connected television's
installed apps, default Home and settings were not modified.

There are at most 13 resident app tiles, cached pose calculations, 160 ms
interruptible transitions, a lazy programme row and no idle animations. The
original bounded artwork cache and pause cancellation remain active. The
optimized APK is approximately 16 MiB including bundled offline artwork.

These checks establish a working prototype, not a measured TCL frame-rate
guarantee. Physical-TV p95/p99 frame times, jank, cold start and memory-pressure
behaviour still need measurement. Streaming resume intents, Downloads and
provider availability also depend on the apps/firmware installed on the TV.

The Home-capable APK is a separate Vortex package, signed with a development
key for this prototype. It does not import another package's private settings
or replace its installation. The upstream self-updater is disabled for this
fork. Selecting it as default Home remains an explicit Android setting.
