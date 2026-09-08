# Motoring Dashboard

Motoring Dashboard is an Android Auto map and point-of-interest prototype for calm, glanceable driving information. Its custom MapLibre cartography mixes warm mid-century color with the restraint of a modern instrument cluster. It targets a Pixel 7/7a projected to a 2021 Mazda CX-5 Grand Touring, while adapting to other Android Auto displays.

## Current 0.5.1 concept

- Custom vector map rendered directly into Android Auto's official map `Surface`
- Warm, Scandinavian, and technical cartography; dedicated day and night palettes
- GPS speed, heading, elevation, grade, trip distance, and a prominent surface-rendered elevation profile
- Instrument Sans telemetry artwork layered over the map rather than constrained to stock host cards
- Optional Android Auto car-hardware speed, fuel, range, mileage, and model readings
- Curated San Juan Mountains POIs with distance sorting and navigation handoff
- Pan, zoom, recenter, rotary-compatible host controls, and automatic day/night response
- Parked-only **Voice** action for the installed official ChatGPT app
- Deterministic US-550 simulated drive in debug builds

The Mazda and Android Auto decide which car-hardware values are exposed. Missing readings remain unavailable; the app does not estimate or invent them. See [Mazda data research](docs/MAZDA_DATA.md).

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew stagePrototypeRelease
```

Outputs:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Signed release APK: `releases/motoring-dashboard-v0.5.1.apk`
- Signed Play bundle: `releases/motoring-dashboard-v0.5.1.aab` (version code 8)

The ignored `signing/` directory contains the existing Play upload key. Keep it backed up; future Play updates must use that same upload key.

## Run on Pixel 7 / 7a and Mazda

1. Upload the 0.5.1 AAB to the existing Play internal-test track. Do not include an older, shadowed bundle in the same release.
2. Install the update from the track's tester link using the enrolled Google account.
3. Open the phone app once and choose a map palette.
4. In Android Auto, enable **Motoring Dashboard** under **Customize launcher** and reconnect USB.
5. Open Motoring Dashboard on the Mazda and grant location and optional vehicle-data permissions.

The release is a genuine `androidx.car.app.category.POI` app using map templates, not media-category scaffolding or screen mirroring. A Play-reviewed production release still depends on Google's category and car-quality review.

## Desktop Head Unit

Enable Android Auto developer mode and **Start head unit server** on the phone, then:

```bash
adb forward tcp:5277 tcp:5277
desktop-head-unit
```

Debug builds automatically run a repeatable drive south from Ouray on US-550, changing position, speed, heading, elevation, and grade every three seconds. Release builds never draw or run the simulated route. Use DHU controls to switch day/night mode and test pan, zoom, recenter, touch, and rotary focus.

Suggested DHU checks:

- start, disconnect/reconnect, and reopen the app
- switch all three palettes in both day and night modes
- pan away and verify recenter resumes following
- open Places, select a POI, and verify navigation handoff
- deny location and vehicle permissions, then grant them
- test offline/no-tile behavior and GPS loss
- inspect `adb logcat` for MapLibre or car-app exceptions

## ChatGPT voice

The launcher first checks at runtime for the official ChatGPT app's exported `com.openai.voice.assistant.AssistantActivity`. If callable, the parked-only car action launches it on the phone's default display and returns to Android Auto after initialization; otherwise the app falls back to ChatGPT's public launcher. This component is not a documented OpenAI API and may change. Install and sign in to ChatGPT first. Enable **Settings → Voice → Background conversations** in ChatGPT so the conversation continues after returning to the map.

## Screenshots

- [Android Auto day mode](docs/screenshots/android-auto-day.png)
- [Android Auto night mode](docs/screenshots/android-auto-night.png)

Android may reject background activity launches in some host/OS states. This feature is deliberately secondary and parked-only; it may need to be omitted from a Play-reviewed POI release if car-quality review considers it unrelated to the app category.

## Architecture

See [Architecture](docs/ARCHITECTURE.md) and [third-party notices](docs/THIRD_PARTY_NOTICES.md).

No analytics, user account, OpenAI API key, unofficial projection protocol, accessibility service, root access, or screen mirroring is used.
