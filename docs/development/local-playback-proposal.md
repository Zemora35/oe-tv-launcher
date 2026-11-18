# Continue Watching: local playback capture proposal

Status: feasibility investigation, 18 November 2026. No collector service,
new permission grant or launcher APK change was made during this investigation.
The user subsequently chose Quick Launch for the cinematic bottom section.
This proposal is deferred; see [the implemented replacement](quick-launch.md).

## Evidence

- Existing Watch Next and preview-channel checks did not expose YouTube's
  unfinished entries, although Google TV displayed them. See
  [the provider comparison](featured-playback.md).
- A read-only `dumpsys media_session` check found an active
  `com.google.android.youtube.tv/starboard` session, a programme title and
  creator, and `PlaybackState` with state PLAYING and a position value.
  This is evidence of a potential live capture source, not historical access.
- That session reported `launchIntent=null`. The diagnostic summary did not
  establish a stable media ID, content URI, image URI or total duration. A
  content-specific durable resume action therefore remains unproven.
- The standard `android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`
  activity query returned no activities on this TV. An alternative vendor
  settings screen or an explicitly authorized one-time ADB setup may be needed.
  No notification access was enabled.

## Proposed implementation, conditional on the next probe

1. Build an opt-in diagnostic listener to inspect metadata **keys and field
   availability**, using an enabled NotificationListenerService and
   MediaSessionManager. Ask the user to enable the access only once the concrete
   diagnostic build and data scope are reviewable. Inspect no notification body;
   limit media sessions to user-selected video applications. Do not log account
   tokens or export personalized titles/links to the public repository.
2. Verify stable media identity/URI, duration and playback updates while watching,
   pausing, switching videos and returning Home. Explicitly test session death
   and launcher/TV restart. A title plus position alone is insufficient for a
   persistent resumable card. A transient PendingIntent is also not a durable
   video link.
3. For sources passing that probe, store a bounded local history of stable
   content identity, title, available artwork, verified link, observed duration
   and position, and update time. Use event callbacks and throttled background
   writes; detach callbacks when access is revoked. No permanent frame timer,
   screenshot scraping or background ADB dependency.
4. Merge genuine incomplete records with the existing Watch Next feed, dedupe
   by source/content identity, and prefer provider playback data. Preserve the
   distinction between an opened app, a recommendation and observed playback.
   Unknown durations/identities cannot become invented progress bars. Completed
   content drops out; users can clear/disable their locally captured history.
5. Verify a content-specific launch and position restoration with YouTube. If
   the app ignores a timestamp, expose that limitation instead of promising
   exact resume. If a stable identity/link is unavailable, retain the Google TV
   shortcut; do not label an app-home launch as resuming a specific video.

This would capture **future viewing after opt-in**. It would not import the
existing Google TV/YouTube history or synchronize viewing on other devices.

## Official references

- [MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager):
  getActiveSessions requires privileged MEDIA_CONTENT_CONTROL or an enabled
  notification listener.
- [MediaController](https://developer.android.com/reference/android/media/session/MediaController):
  metadata/state callbacks and nullable session activity.
- [MediaMetadata](https://developer.android.com/reference/android/media/MediaMetadata):
  optional media ID/URI, duration and artwork fields must be checked at runtime.
- [YouTube API revision history](https://developers.google.com/youtube/data/revision_history):
  account Watch History is not accessible through the public Data API.
