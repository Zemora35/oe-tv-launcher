# Featured playback and YouTube continuation

The home cycles through actual provider programmes with artwork every 30 seconds.
A stable content key selects the entire hero (image, title, synopsis, platform
and playback target), rather than repeatedly selecting the first recommendation.
The remote-accessible Next button advances immediately. Explicitly focused
programme/Continue Watching items take priority; moving back to a normal app
releases that override. The timer pauses while the activity is inactive, a
settings/browser/grid panel is open, or any hero action has focus. A full
interval starts when it resumes. Feed changes reconcile by identity, not index.
Without provider artwork the current wallpaper and the ten bundled films form
an offline carousel; bundled titles/synopses stay paired to their own image.
FeaturedFeed reads at most twelve video-app channels
and six programmes per channel, retaining at most 36 unique items. Metadata is
cached for five minutes; transient failures retain the last successful result.
Browser recommendations are excluded. Required fields, visibility and publisher
ownership are checked. Artwork, synopsis, package and deep link stay together.
The hero shows the installed app's label as the platform and Watch opens that
programme's link. More Info shows the same metadata. Recommendations receive no
fabricated progress. Preview and Watch Next saved identities are separated;
legacy saved IDs and custom rows remain readable.

Programme names retain provider spelling. Existing native-language synopses
are preserved. A conservative English-sentence detector prevents clearly English
paragraphs from being displayed as localized synopses in non-English UI locales.
An exact-title local summary is currently provided for Frieren. Its original
short synopsis was checked against [the official series overview](https://www.crunchyroll.com/series/GG5H5XQX4/frieren-beyond-journeys-end).
For an unmatched English synopsis the UI explicitly says a synopsis in the UI
language is unavailable (TR/ES/VI/AR), while the real programme/deep link remains
available. This is a bounded local fallback, **not a general translation engine**;
short/ambiguous text and other foreign languages are not reliably detected.
No personal recommendations, titles or history are sent to a translation API.

The carousel uses one sleeping coroutine and no per-frame state updates,
continuous animation, image crossfade, new SDK or translation model. Artwork
keeps the existing asynchronous decoder and 720p cap on constrained TVs. The
old image is cleared when its request changes, so a new title is never painted
over a stale programme image; a slow/unavailable image may show the dark surface
while loading. Existing app-icon memory limits remain unchanged.

For wallpaper-only artwork, no streaming platform is assumed. “Platform
doğrulanmadı” and “Nerede izlenir?” replace the old app-dock action. The latter
opens a URL-encoded JustWatch search in a TV browser (Turkish for Turkish UI,
otherwise US/English). The Turkish endpoint returned HTTP 200 in validation.
This is a viewing-options lookup, not an availability API or a promise of free
access. Matching offline film details remain available. Login and playback
remain provider-owned.

## Physical Google TV comparison

On 18 November 2026, Google TV's Continue Watching UI showed three YouTube cards.
Two had Resume labels and nonempty progress bars; one showed Watch with an
empty bar. An empty third-party feed must therefore not be called an empty
YouTube account history.

A test app with READ_TV_LISTINGS found thirty Watch Next entries, all TCL
browser NEW recommendations without positive playback positions. No YouTube
Watch Next entries were returned. YouTube preview channels contained seven
“Sizin için önerilenler”, seven “Trendler” and zero “Abonelikler” programmes;
none supplied playback positions. Those fourteen recommendations were not
relabeled as unfinished playback.

This proves that the accessible provider result differs from Google TV's UI
on this TV. It does not identify Google's private database/server mechanism.
Package metadata was inspected; no documented continuation interface was found.
No private database, account token or root access was used. The
[YouTube Data API revision history](https://developers.google.com/youtube/data/revision_history)
documents that account watch history is unavailable through that API. A general
API key/OAuth connection alone would not fix this. See also
[Android's public Watch Next attributes](https://developer.android.com/training/tv/discovery/watch-next-programs).

Native unfinished Watch Next entries remain supported. YouTube preview entries
are also accepted when they publish actual unfinished playback, respecting
exclusions, image preferences and item limits. Permission loss hides them. No
pixel estimate of Google TV's progress bar becomes a timestamp.

The earlier Continue Watching header offered YouTube and Google TV shortcuts.
The cinematic home now uses [Quick Launch](quick-launch.md) in that area, retaining
the Google TV button and offering the installed YouTube app among editable favourites. Google TV
is discovered from exported HOME handlers and opened explicitly; it is not
disabled or replaced. These access the provider's real history when unavailable
inside O.E tv. They do **not** mirror the three Google TV cards into this
launcher's feed. Automatic mirroring remains unsupported on the observed TV
through the available public interfaces.

The probe exports package/channel counts, not video titles, account IDs or
playback URLs. Screenshots and diagnostics stay in ignored deliverables and
must not be published with the source repository.
