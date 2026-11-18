# Localization and international layouts

Vortex follows the effective Android app locale. Android 13+ also exposes the
languages listed in `native-launcher/app/src/main/res/xml/vortex_locales.xml`
through system per-app language settings. No activity forces Turkish and no
code changes the television's system locale.

## Current translation scope

The 63 cinematic-home messages have English, Turkish, Spanish, Vietnamese and
Arabic translations. Native settings have an English default and a complete
Turkish supplement. Spanish/Vietnamese reuse the upstream ARB translations;
untranslated native settings and Arabic settings fall back to English per key.
These are initial translations, open to native-speaker review. This does not
mean every language has already been translated.

An additional 26 film/continuation strings and the remaining-time plural live
in `vortex_content_strings.xml` in the same five locale directories. These
pair bundled images with titles/summaries; other locales use Android's English
fallback. Translate this catalog too when contributing a new language.

The application grid adds 28 strings in `vortex_grid_strings.xml`, complete in
the same five languages. Translate all three cinematic catalogs when adding a
language. Last-visit dates use the app locale and television time zone.

`vortex_playback_strings.xml` adds ten platform/playback/shortcut messages in
the same five languages. These use the same key/placeholder validation.

An unsupported device language still works: Android resolves missing strings
to `res/values/vortex_strings.xml`. Installed application names, user row names,
package IDs, intents and programme metadata keep their own identity/content.
Launcher-owned default row names and preview data use localized resources.

## Add a language without changing Kotlin

1. Copy the cinematic strings from `res/values/vortex_strings.xml` to a new
   language resource directory, for example `res/values-fr/vortex_strings.xml`.
   Add legacy settings translations too when available.
2. Register `fr` in `res/xml/vortex_locales.xml` for Android's app-language list.
   Regional resources such as `values-pt-rBR` follow normal Android fallback.
3. Keep resource names and positional placeholders (`%1$s`, `%2$d`) unchanged.
   Reordering placeholders is allowed; their argument numbers/types must match.
   Escape XML characters and Android string apostrophes. Do not concatenate
   translated fragments to build new sentences.
4. Use Android `<plurals>` for quantities. Preserve all applicable plural forms,
   including Arabic zero/two/few/many. Numbers and dates use the effective locale.
5. Run the catalog checks, Android lint and emulator layout tests below.

The English-to-resource arrays in `values/vortex_legacy_index.xml` bridge the
existing Backdrop settings implementation. They are internal identifiers and
must not be translated. Legacy `lib/l10n/app_*.arb` loading resolves full BCP-47
tags, base language and English, without a hardcoded supported-language list.
New UI messages should use `stringResource`/`pluralStringResource` directly.

## RTL, scripts and remote navigation

The application declares RTL support. Start/end layout, hero scrims, the spiral
curve and rotations mirror in RTL. The selected tile remains at horizontal
centre with scale 1.35. Physical Left advances in RTL; Right moves back. In LTR,
Right advances and Left moves back. Accessibility next/previous actions keep
their logical meanings. Dialog routing uses enums, never translated titles.

System sans-serif fallback supports Arabic, CJK and other scripts instead of
forcing a Latin-only font. Labels can wrap; auxiliary status text can ellipsize.
No translations are downloaded and no API key is needed to switch languages.

## Verify a contribution

From `native-launcher`:

```sh
./gradlew :app:testBenchmarkDebugUnitTest \
  --tests '*UiTextTest' --tests '*LocaleCatalogTest' --tests '*VortexGeometryTest'
./gradlew :app:lintVortexRelease :app:assembleVortexRelease
```

`LocaleCatalogTest` checks missing home strings, unknown/duplicate keys and
placeholder signatures. Deliberately partial legacy translations use Android
fallback; they are not presented as complete translations.

Run `InternationalHomeTest` on an explicitly selected Android TV emulator. It
checks English, Turkish, Spanish, Vietnamese, Arabic, regional fallback, unknown
languages, plural forms and RTL D-pad input. Its rendering matrix also includes
Japanese/CJK/emoji content, expanded `en-XA` with larger text and mirrored
`ar-XB`. Pseudolocales are generated only for debug builds. Screenshots are local
test artifacts under the test package's external files directory.

Compose previews include English, Turkish and Arabic layouts in
`CinematicHomeScreen.kt`. Physical-TV acceptance and native-speaker review are
still required before a public release.
