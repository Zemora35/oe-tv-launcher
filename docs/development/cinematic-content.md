# Paired backdrop metadata and real Continue Watching

The cinematic hero resolves the **image, title and synopsis as one content
identity**. Every one of the ten bundled backdrops has explicit resource IDs in
`BackdropMetadata.kt`; no filenames are converted into guessed titles. English,
Turkish, Spanish, Vietnamese and Arabic resource packs include short, locally
written summaries. Turkish uses familiar localized film titles; other packs
retain the original titles. This works offline without API keys.

Remote Trakt/TMDB backdrops now use the selected app locale in the existing
TMDB detail request and retain its title and overview beside the cached image.
No extra detail request is made per focus frame. The last remote synopsis is
persisted with the wallpaper. Older cache entries without a synopsis show an
honest unavailable message until refreshed. A programme without artwork cannot
put its title over an unrelated bundled movie.

The compact hero keeps its synopsis visible; long titles use one line, with the
full title/description available in More Info. Existing dock animation and image
budgets are retained.

## Shared playback progress

`AndroidWatchNextProvider` reads the public Android TV Watch Next provider after
the user grants `READ_TV_LISTINGS`. The Vortex edition enables the section for
new installations without overriding an explicit saved preference. Permission
and an enabled section are independent requirements.

The feed excludes NEXT/NEW/WATCHLIST recommendations, user-hidden entries,
zero-position and completed playback, and items without actual progress. A
CONTINUE type flag alone is insufficient: some publishers use it for generic
links. Legacy percentage-based progress is accepted when strictly between 0
and 100. Up to 150 provider rows are inspected and only up to 50 eligible entries
are retained, so recommendations cannot consume the continuation limit.

Original app deep links are preserved. Remaining minutes are derived from the
published duration/position and rounded up; missing durations never produce
made-up times. Existing provider observers refresh the list, and the empty row
offers a working permission or refresh action. No continuously polling service
or screen scraper was added.

This does not recover Netflix/other apps' private account history. Only playback
those apps share through the public provider can be displayed. An ADB shell
query can see a different set of rows than a launcher with TV-listings permission;
device verification must also check the application itself. No sample viewing
history is inserted on the user's TV.

## Checks

Focused unit checks cover continuation eligibility, recommendation limits,
deep-link/progress preservation, remaining-time boundaries and localized TMDB
metadata. Resource validation checks both native resource catalogs. Device tests
cover all bundled images/locales, image/title consistency, missing-art fallback,
visible compact summaries and real empty-state callbacks, alongside existing
remote/RTL/occlusion tests.

References: [Watch Next attributes](https://developer.android.com/training/tv/discovery/watch-next-programs),
[Watch Next contract](https://developer.android.com/reference/android/media/tv/TvContract.WatchNextPrograms),
[TMDB details](https://developer.themoviedb.org/reference/movie-details).
