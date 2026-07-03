# Music — local music player for Android

A single-module Kotlin / Jetpack Compose music player for locally stored audio.
Built with Media3 (ExoPlayer + MediaSession), Room, and Material 3.

## Features

- Plays local audio indexed by MediaStore (Music, Downloads, any folder on shared storage)
- Formats: MP3, AAC, FLAC, WAV, OGG, OPUS, M4A (Media3's bundled decoders; WMA is not
  supported by Media3 and will be skipped automatically if encountered)
- Background playback via `MediaSessionService` + foreground media notification with
  album art, play/pause/next/prev, and seekbar
- Lock screen and Bluetooth/headset controls through MediaSession
- Audio focus handling (pauses on calls, ducks on notifications) and pause on headphone unplug
- Library tabs: Songs, Albums, Artists, Playlists — with search, sort, shuffle,
  repeat (off/one/all), and queue management
- Playlists persisted locally with Room
- Material 3, dynamic color on Android 12+, dark/light theme follows the system
- minSdk 26 (Android 8.0), targetSdk 35, R8 shrinking enabled for release builds

## Building

Requirements: JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).
`local.properties` must point at the SDK, e.g. `sdk.dir=C:/Users/you/android-dev/sdk`.

```
gradlew.bat assembleRelease
```

The signed APK lands at `app/build/outputs/apk/release/app-release.apk`.

Release signing reads `keystore.properties` (storeFile/storePassword/keyAlias/keyPassword)
in the project root. Keep `release.jks` and `keystore.properties` safe — installing future
updates over the top requires signing with the same keystore. Both files are gitignored.
If `keystore.properties` is missing, the release build falls back to the debug keystore.

## Installing

Copy `app-release.apk` to the phone and open it (allow "install from unknown sources"),
or with USB debugging enabled:

```
adb install app/build/outputs/apk/release/app-release.apk
```

On first launch, grant the audio-files permission (and notification permission on
Android 13+ so playback controls show in the notification shade).
