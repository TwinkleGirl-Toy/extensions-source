# EAHentai (filtered) — Keiyoushi / Komikku source

This module adds EAHentai as an English-language source. It uses the site's album API for gallery metadata and reader image URLs.

## Filtering

The source checks gallery titles and API metadata (tags, album type, and character labels) before displaying a result, opening details, and returning reader pages. It blocks common child-coded and underage labels in English, romanized Japanese, and Japanese scripts. Galleries without verifiable tag metadata or reader pages are hidden.

This is a metadata denylist, not image recognition. It cannot guarantee that every untagged or incorrectly tagged gallery is safe or adult-only.

## Build

From this repository root:

```sh
./gradlew :src:en:eahentai:assembleDebug :src:en:eahentai:lintRelease
```

A GitHub Actions workflow is included to build a debug APK and upload it as an artifact.
