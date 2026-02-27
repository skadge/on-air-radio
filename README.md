# On Air Radio

A modern Android radio streaming app with full **Android Auto** support. Browse and listen to 89+ curated public radio stations from across Europe and the USA.

## Features

- 🎵 **Live radio streaming** — HLS, AAC, MP3, and Ogg streams
- 🚗 **Android Auto** — full media browsing, playback controls, skip next/previous, and auto-resume of the last played station
- 🎨 **Now playing** — live metadata (artist, title) with album artwork from MusicBrainz/Cover Art Archive
- ⭐ **Favorites & listen tracking** — personalized station ordering based on listening habits
- 🔍 **Filtering** — browse by country, genre, or search
- 📻 **Rich metadata** — dedicated providers for BBC, Radio France, Radio Nova, and ICY stream metadata
- 🖼️ **Station logos** — bundled vector drawables with bitmap rendering for Android Auto
- 🌍 **Localized** — English, French, and German

## Prerequisites

- Android SDK (API 26+, compileSdk 35)
- JDK 11+
- Python 3 (for station data generation from `stations.yaml`)
- Python dependencies: `pip install -r scripts/requirements.txt`

## Build & Deploy

### Build debug APK

```bash
./gradlew assembleDebug
```

There are two product flavors: `playstore` and `fdroid`. To build a specific one:

```bash
./gradlew assemblePlaystoreDebug
./gradlew assembleFdroidDebug
```

### Install on a connected device

```bash
# Build and install the playstore debug variant
./gradlew installPlaystoreDebug

# Or the F-Droid variant
./gradlew installFdroidDebug
```

### One-liner: build, install, and launch

```bash
./gradlew installPlaystoreDebug && adb shell am start -n org.guakamole.onair/.MainActivity
```

For the F-Droid flavor (different application ID):

```bash
./gradlew installFdroidDebug && adb shell am start -n org.guakamole.onair.fdroid/.MainActivity
```

## Station Database

Station data lives in `stations.yaml` and is compiled into `RadioRepository.kt` at build time via `scripts/build_stations.py`. The build task runs automatically as part of `preBuild`.

To regenerate manually:

```bash
python3 scripts/build_stations.py
```

## Architecture

| Component | Description |
|-----------|-------------|
| `RadioPlaybackService` | Media3 `MediaLibraryService` — handles playback and Android Auto browsing |
| `MetadataForwardingPlayer` | Custom `ForwardingPlayer` for out-of-band metadata updates and station skipping |
| `RadioMetadataManager` | Merges raw stream, polled API, and MusicBrainz-refined metadata |
| `MusicBrainzMetadataRefiner` | Fetches canonical artist/title and album artwork from MusicBrainz |
| `ArtworkManager` | Loads and processes artwork bitmaps for media notifications |
| `BitmapContentProvider` | Renders vector drawables as bitmaps for Android Auto |
| `RadioRepository` | Auto-generated station database with favorites, listen counts, and filtering |

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Media:** AndroidX Media3 (ExoPlayer)
- **Images:** Coil
- **Build:** Gradle (Kotlin DSL)

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
