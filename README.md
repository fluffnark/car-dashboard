# Motoring Dashboard

A restrained driving-information prototype for Android Auto, with a companion dashboard for the phone. The Polestar-inspired visual direction uses neutral charcoal, off-white sans-serif numerals, thin rules, and a single amber telemetry accent. No branding or assets from Polestar are used.

## What it shows

- Local clock and date
- Display and raw vehicle speed
- Fuel percentage and estimated remaining range
- Odometer
- Vehicle year, manufacturer, and model
- Two-minute speed history and a fuel-level graphic, based on actual readings
- Start ChatGPT button (opens the installed phone app; car action is parked-only)

Every vehicle value is optional. Android Auto and the head unit decide which properties an app receives; unavailable data is shown as an em dash rather than guessed. The clock always works.

See [2021 CX-5 data availability](docs/MAZDA_DATA.md) for the model-specific research and confidence matrix.

## Compatibility

- Phone: Android 9 (API 28) or newer; sized responsively for Pixel 7 and Pixel 7a
- Projection: Android Auto with Car App API level 8 or newer
- Target vehicle: 2021 Mazda CX-5 Grand Touring with its factory Android Auto-compatible infotainment system

This is a developer prototype of a templated media app. Android Auto does not provide a general-purpose dashboard category, so the app combines a real media session with a host-rendered information dashboard. The Mazda may withhold some or all vehicle properties; this is normal and cannot be bypassed by the app.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew test assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The current car-screen concept is [`releases/motoring-dashboard-v0.4.0.apk`](releases/motoring-dashboard-v0.4.0.apk). Upload [`releases/motoring-dashboard-v0.4.0.aab`](releases/motoring-dashboard-v0.4.0.aab), version code **6**, to the existing Play Console internal-test track. Both are signed with the project's release upload key, not Android's debug certificate.

Version 0.4.0 uses three large instrument images in a `SectionedItemTemplate`: clock, speed with a history trace, and fuel with remaining range. Tap any instrument for the vehicle details and permissions. The permission action disappears once all requested access is granted. Readings refresh at most once per second; invalid values and gaps longer than five seconds are not joined in the graph. History is limited to two minutes, stored in memory, and cleared on disconnect. Android Auto still controls layout, surrounding typography, and its own media/navigation bar.

**Start ChatGPT** uses the installed app's public Android launcher. It opens ChatGPT on the phone; it does not embed a chat in the car display or automatically start voice mode. Unlock the phone and install/sign in to ChatGPT first. Android Auto restricts this phone handoff to parked use. No telemetry is sent to ChatGPT. The existing media compatibility service remains for the internal-test prototype; this does not establish eligibility as a production standalone dashboard app.

The private upload key and its properties live in the ignored `signing/` directory. Preserve both files securely: every future Play update must use the same upload key. They are intentionally never committed.

## Run on Pixel 7 / 7a and Mazda CX-5

1. Upload the v0.4.0 AAB (code 6) to the same Play Console internal-test track and publish the release. Include only the newest bundle in the draft to avoid shadowed-version errors.
2. Open the track's tester opt-in link with the enrolled Google account on the Pixel, then install or update Motoring Dashboard from Google Play.
3. Open **Motoring Dashboard** once on the phone.
4. Connect the Pixel to the Mazda using the infotainment USB port and a data-capable USB cable, then launch Android Auto.
5. In Android Auto settings, open **Customize launcher** and ensure Motoring Dashboard is enabled. Reconnect the phone after changing this setting.
6. Open Motoring Dashboard on the Mazda, tap an instrument, and choose **Allow vehicle data**. Grant only the vehicle permissions you want to share.

The standalone APK remains useful for phone installation and inspection, but Android Auto only accepts the templated service from a trusted distribution source. Use the Play internal-test build for the Mazda. The media-browser fallback can still appear as a conventional Now Playing screen on hosts that do not support templated media.

Wireless Android Auto availability depends on the Mazda infotainment firmware and regional configuration; USB projection is the baseline supported path for this project.

## Desktop Head Unit testing

Install the Android Auto Desktop Head Unit from Android Studio's SDK Manager, enable the Android Auto developer server on the phone, then run:

```bash
adb forward tcp:5277 tcp:5277
desktop-head-unit
```

Use the DHU sensor controls to simulate speed, fuel, range, mileage, and model values. The app deliberately accepts all hosts only in debug builds; release builds use the Car App Library host allowlist.

## Architecture

- `MainActivity`: responsive Compose companion UI
- `DashboardCarAppService`: Android Auto entry point
- `DashboardScreen`: instrument grid, details pane, and Car Hardware listeners
- `InstrumentArtwork`: native Canvas instrument graphics and speed trace
- `SpeedHistory`: bounded history of actual sensor observations
- `ChatGptLauncher`: public launcher handoff to the installed ChatGPT app
- `ConceptMediaService`: compatibility media session; it has no dashboard controls
- `DashboardRepository`: in-process state shared by the projected and companion surfaces

No network access, analytics, account, or background location is used.
