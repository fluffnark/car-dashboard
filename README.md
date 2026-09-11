# Motoring Dashboard

Motoring Dashboard is a low-distraction Android Auto instrument prototype for a Pixel 7/7a projected to a 2021 Mazda CX-5 Grand Touring. Version 0.7.0 removes the live map and presents a calm, mid-century-modern dashboard drawn directly on Android Auto's official car surface.

## Current concept

- Large analog-style speed, compass, and clock instruments
- Recent elevation sparkline, altitude, grade, trip distance, and optional fuel/range
- Instrument Sans typography with separate warm day and graphite night palettes
- One-frame-per-second maximum rendering; no map engine, tile traffic, virtual display, or animation loop
- Host-rendered persistent controls and lists for Mazda Commander-knob focus
- Offline trip checklist edited on the phone and checked on the car display
- Parked-only **Voice** action for the installed official ChatGPT app
- Deterministic US-550 simulated drive in debug builds

The Mazda and Android Auto decide which car-hardware values are exposed. Missing readings stay hidden; the app never invents them. See [Mazda data research](docs/MAZDA_DATA.md).

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew stagePrototypeRelease
```

Outputs:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Signed release APK: `releases/motoring-dashboard-v0.7.0.apk`
- Signed Play bundle: `releases/motoring-dashboard-v0.7.0.aab` (version code 10)

The ignored `signing/` directory contains the existing Play upload key. Keep it backed up; future Play updates must use that key.

## Run on Pixel 7 / 7a and Mazda

1. Upload the 0.7.0 AAB to the existing Play internal-test track, or install the signed APK.
2. Open the phone app once and add any trip-list items.
3. In Android Auto, enable **Motoring Dashboard** under **Customize launcher**, then reconnect USB.
4. Open Motoring Dashboard and grant location and optional vehicle-data permissions.
5. Turn the Mazda Commander knob to move the visible focus ring; press it to activate the checklist or Voice action.

The custom surface currently requires Android Auto's map-template capability and is packaged as a POI prototype. Because 0.7.0 intentionally has no map or POI browser, it is suitable for internal design testing but is not ready for public POI-category review. Android Auto therefore also treats it as a map-like app in dashboard/split layouts and may pair it with media or retain another navigation pane according to host policy. A true media-category build would pair correctly with navigation, but Android Auto would replace this custom artwork with its standard media template.

## Desktop Head Unit

Enable Android Auto developer mode and **Start head unit server** on the phone, then:

```bash
adb forward tcp:5277 tcp:5277
desktop-head-unit
```

Debug builds run a repeatable drive south from Ouray on US-550, updating speed, heading, elevation, and grade once per second. Release builds never run simulated data.

Check these paths in the DHU and the Mazda:

- initial start, app reopen, disconnect/reconnect, and repeated surface recreation
- day/night changes and loss of GPS or vehicle data
- Commander-knob focus and press on both persistent actions and checklist rows
- at least ten minutes of simulated updates while watching `adb logcat` for exceptions or ANRs

## ChatGPT voice

The launcher runtime-checks the official ChatGPT app's exported `com.openai.voice.assistant.AssistantActivity`. If callable, the parked-only action starts it on the phone and returns the phone to its home screen after initialization; otherwise it opens ChatGPT normally. The activity is not a documented OpenAI API and may change. Install and sign in to ChatGPT, then enable **Settings → Voice → Background conversations**.

There is no documented third-party intent for ending a ChatGPT voice conversation. The car button therefore starts Voice but does not pretend to be a reliable on/off toggle; end the session with ChatGPT's own control on the phone.

## Trip list and assistant sync

The checklist is currently local and offline. It now uses a `TripListStore` provider seam so an authenticated Google Tasks adapter can replace local storage without changing the Mazda UI. Google Tasks is the cleanest future shared-list backend because it has a supported read/write API. Google Keep's API is intended for administrator-approved enterprise use, and current ChatGPT Voice integrations do not guarantee write access to the same Keep list. See [Google Tasks hook](docs/GOOGLE_TASKS.md).

## Screenshots

- [Instrument surface — day](docs/screenshots/android-auto-day.png)
- [Instrument surface — night](docs/screenshots/android-auto-night.png)

These images are deterministic surface renders. Android Auto adds its own action strip, compact status pane, focus ring, and system bar on top.

See [Architecture](docs/ARCHITECTURE.md) and [third-party notices](docs/THIRD_PARTY_NOTICES.md).

No analytics, user account, OpenAI API key, unofficial projection protocol, accessibility service, root access, or screen mirroring is used.
