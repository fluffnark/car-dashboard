# Motoring Dashboard

A restrained driving-information prototype for Android Auto, with a companion dashboard for the phone. The visual language takes its cues from mid-century instrument panels: warm charcoal, parchment numerals, persimmon accents, simple arcs, and no ornamental chrome.

## What it shows

- Local clock and date
- Display and raw vehicle speed
- Fuel percentage and estimated remaining range
- Odometer
- Vehicle year, manufacturer, and model

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

The current car-screen concept is [`releases/motoring-dashboard-v0.3.0.apk`](releases/motoring-dashboard-v0.3.0.apk). Upload [`releases/motoring-dashboard-v0.3.0.aab`](releases/motoring-dashboard-v0.3.0.aab) to the existing Play Console internal-test track. Both are signed with the project's release upload key, not Android's debug certificate.

Version 0.3 restores the actual dashboard as the Android Auto entry screen. It declares both templated-media and media-browser capabilities, shows time and available Mazda data in a `PaneTemplate`, and places Android Auto's compact media-playback action in the header. Exact typography, spacing, and playback presentation remain controlled by the Android Auto host for driver safety.

The private upload key and its properties live in the ignored `signing/` directory. Preserve both files securely: every future Play update must use the same upload key. They are intentionally never committed.

## Run on Pixel 7 / 7a and Mazda CX-5

1. Upload the v0.3 AAB to the same Play Console internal-test track and publish the release.
2. Open the track's tester opt-in link with the enrolled Google account on the Pixel, then install or update Motoring Dashboard from Google Play.
3. Open **Motoring Dashboard** once on the phone.
4. Connect the Pixel to the Mazda using the infotainment USB port and a data-capable USB cable, then launch Android Auto.
5. In Android Auto settings, open **Customize launcher** and ensure Motoring Dashboard is enabled. Reconnect the phone after changing this setting.
6. Open Motoring Dashboard on the Mazda and choose **Allow vehicle data**. Grant only the vehicle permissions you want to share.

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
- `DashboardScreen`: safe `PaneTemplate` UI, compact media action, and Car Hardware listeners
- `ConceptMediaService`: browsable media session used by Android Auto playback
- `DashboardRepository`: in-process state shared by the projected and companion surfaces

No network access, analytics, account, or background location is used.
