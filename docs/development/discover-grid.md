# Fullscreen Discover

Discover now uses the same fullscreen catalogue frame as All Apps, History and
the Quick Launch picker, replacing its earlier list dialog. The shared engine
provides top navigation, search, platform/filter and sort menus, category tabs,
lazy grid navigation, focus restoration and the page footer. Content cards use
five columns and two rows on the standard 960dp TV window (four columns on
narrow windows), leaving room for artwork, two title lines and the source app.
All Apps retains its seven-column application layout.

## Behaviour

- Search matches programme titles and platform names using the existing
  Unicode/Turkish-aware search normalization.
- Filter by publishing package, independently of its translated/display name.
  The platform menu scrolls when there are many sources.
- Keep the feed's recommended order or sort by title A–Z/Z–A using the UI locale.
- Tabs show All, Recommended and known feed items saved in My List.
- OK opens the programme's original provider link; a missing link opens matching
  metadata instead. Hold OK toggles My List using the existing saved identities.
- Changing search/filter/sort resets the grid to the first result. Returning
  from playback restores the selected content key and scroll position when the
  item is still present. Directional keys move logical rows/columns; RTL mirrors
  physical Left/Right.

The data is the existing published recommendations, real Watch Next items and
custom programme rows. No new remote API, account-history access, fabricated
programme availability or inferred watch progress is introduced. An empty feed
and unmatched search have separate localized messages. Saved IDs refer to known
feed content; this does not import a streaming account's private watchlist.

## Structure and budget

`DiscoverCatalog.kt` owns filtering/sorting and package-based facets.
`DiscoverCard.kt` renders the content cards. `CinematicAppsScreen.kt` exposes
separate Apps/Discover entry points over one private catalogue layout engine.
`CinematicLauncherScreen` adapts provider app names, deep links and saved actions.

Only visible lazy-grid artwork subscribes to the existing bounded decoder.
Thumbnails request 384×216 decoding and do not retain entries in the shared app
icon cache. Platform icons reuse the 144px app-icon policy. Focus scales for
140ms with cached tile paint; there is no blur, continuous animation or added
background collector. EN/TR/ES/VI/AR resources and TR/AR Compose previews are
included. Preview/test records are never merged into the installed launcher.

Validation covers Turkish search, distinct source identities, default/name
ordering, legacy saved keys, offscreen remote navigation, Select/hold behaviour,
scroll restoration, long platform menus, RTL and localized empty states.
Physical-device and final build results are recorded in the ignored
`deliverables/oe-tv-discover-validation.json`; this is not a frame-rate guarantee.

On the physical TCL / API 30 device, the optimized build displayed 30 shared
programmes with covers and source labels. D-pad Down reached the first card and
continued to an offscreen row while retaining focus. These counts describe the
test device's current feed, not a bundled catalogue.
