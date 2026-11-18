# Featured carousel validation — 18 November 2026

The first recommendation was previously selected on every render. As a result,
the ordinary wallpaper timer could not change the visible provider hero.
`FeaturedRotation.kt` now selects stable content identities and advances every
30 seconds. The arrow beside More Info advances manually. Metadata and action
always resolve from the same selected programme. Custom programme rows and
Continue Watching retain their explicit selection until it is released.

Checks for this change:

- 51 focused JVM tests passed, including four carousel/English-detection tests
  and all five resource catalogues' key/placeholder checks.
- 25 Android UI tests passed on the isolated API 34 TV emulator (83.647 seconds).
  The three new tests check timed/wrapped metadata and playback changes,
  inactive/remote-focused pause and manual Select, plus native/English synopsis
  handling in TR/EN/ES/VI/AR. Existing app-grid/remote/localization tests passed.
- Optimized release build and Android lint passed (zero errors, 62 existing
  warnings); APK signature verification passed.
- A final reconciliation fix preserves the programme selected when restoring or
  changing a custom row. That revision was rebuilt, linted, and the eight focused
  carousel/catalogue/remaining-time unit tests rerun successfully before the
  final APK was installed. Its on-device automatic transition was checked again.
- Physical TCL API 30 TV: the updated home displayed a Turkish Frieren synopsis;
  without further input its title and actual background changed to Golden
  Kamuy. A fresh semantic lookup found Next; its activation changed Fire Force
  to Fate/strange Fake. Screenshots verified distinct matching artwork.
- A subsequent physical focused-pause capture ended in YouTube rather than
  O.E tv. That observation is inconclusive; no physical pause result is claimed.
  Focus pause is covered by the passing automated remote test.

The localized-description fix has deliberate limits: original provider text
already in the UI language stays intact. Recognized English paragraphs use an
exact-title local summary where one exists (currently Frieren), otherwise an
explicit localized missing-synopsis message. This is not comprehensive machine
translation or a language classifier for all languages. Programme names remain
unchanged. See [the data policy and performance notes](featured-playback.md).

Personalized provider screenshots, raw XML, timing logs and APK hashes stay in
ignored `.tv-test/` and `deliverables/`; they are not source assets for publishing.
This change adds no continuous effects or translation SDK, and does not establish
a universal frame-rate guarantee for TV hardware.
