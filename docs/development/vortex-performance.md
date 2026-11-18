# O.E tv rendering budget and hero occlusion

## App-grid focus and constrained-TV revision

Each grid package and category owns a stable FocusRequester. Last selection is
read in callbacks and the small page footer, rather than in the screen's
composition. Directional movement no longer rebuilds every tile or changes
focus-requester attachment for each step. The footer derives the page number;
focus restoration scrolls only if the saved app is outside the viewport.

Directional targets use logical row/column indices rather than a spatial
search against animated or clipped rectangles. When a target row is outside
the viewport, a bounded minimal scroll places it before requesting focus.
Opposite/repeated inputs cancel pending focus work; grid edges keep their
current card. This avoids the offscreen-row focus loss found by the remote test.

The final paint pass makes grid and dock halos static for each focus/selection
state. Scale/position still animate, but their animation values are no longer
read inside card paint callbacks. This keeps the tile display list cached while
RenderNode transforms change, avoiding per-frame repaint of gloss, text and
icons just to fade a halo. The selected centre tile and visible focus ring stay.

The grid scrolls only the minimum distance needed to reveal a card, avoiding a
pivot move for a visible card. DPAD focus alone no longer requests the search
keyboard; OK requests it. Back hides a visible keyboard before closing the
grid. Short transform feedback, bounded icon decoding and lazy composition
remain in place.

Low-RAM devices and TVs with at most 2 GiB physical RAM use a 1280 × 720 maximum
backdrop decode; others retain 1920 × 1080. Capability is read once per screen entry, without
polling. The constrained target has 55.6% fewer pixels (3,686,400 vs 8,294,400
raw RGBA bytes). UI, text and icons remain at native display resolution. This
reduces texture/decode work; it does not halve total application memory or
guarantee zero frame misses. Physical results belong in the validation log.

The spiral is drawn before the hero, whose higher z-index keeps its controls
above all dock tiles in both layout directions. Both hero action buttons use
opaque rounded surfaces, so icons cannot bleed through them. Tile outlines and
the central selected tile stay intact: no rectangular clipping, alpha mask,
extra offscreen texture or layout-feedback state is used for this occlusion.

The performance budget includes:

- One vector pose animation per dock item, replacing four independent animated
  states/coroutines. Position, scale and opacity still change together over
  160 ms and can reverse mid-flight. Animated values are read only inside the
  graphics-layer lambda, avoiding per-frame composition and measurement.
- A maximum of 13 attached tiles regardless of installed-app count. Geometry
  remains precomputed, and the visible range is remembered between changes.
- Fullscreen artwork decodes capped to the smaller of the actual viewport and
  1920×1080, preserving aspect ratio. On a 3840×2160 viewport the maximum decoded
  RGBA pixel budget falls from about 31.6 MiB to 7.9 MiB. This is an image pixel
  budget, not a claim about total process memory. Text, tiles and focus render
  at native output resolution. The fullscreen bitmap is not retained in the
  thumbnail cache.
- Existing fixed 144×144 icon decode requests and 4 MiB thumbnail LRU cache;
  two background image workers, a bounded queue and cancellation on detach or
  app pause. Cache size is not increased to hide expensive work.
- Featured metadata and bitmap wrappers are remembered. No perpetual glow,
  parallax, particles or polling was added. The existing reduced-motion option
  and Android animator-scale setting are respected.

Unit checks cover 4K, 720p and unusual aspect ratios. The hero regression check
renders the screen with and without dock tiles and compares full-height interior strips of each rounded button in
both layout directions; its fixture verifies a real Netflix/hero geometric
overlap. Existing remote tests cover focus, short/long OK and rapid reversals.

Software-rendered emulator frame timing is a diagnostic, not a prediction for
TCL/other physical TVs. Validate release APK navigation, launch/return, 720p,
1080p and 4K output on the target hardware before publishing. No universal
zero-stutter guarantee is made.

Android API references:
[graphicsLayer](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/package-summary),
[animateValueAsState](https://developer.android.com/reference/kotlin/androidx/compose/animation/core/package-summary).
