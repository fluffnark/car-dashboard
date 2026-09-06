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
- Projection: Android Auto with Car App API level 3 or newer
- Target vehicle: 2021 Mazda CX-5 Grand Touring with its factory Android Auto-compatible infotainment system

This is a developer prototype, not a Google Play-ready product. Google does not provide a general dashboard app category. The projected experience uses the closest templated category for development and must be sideloaded with Android Auto developer mode enabled. A production release would need a supported core purpose and Google Play car-app review. The Mazda may withhold some or all vehicle properties; this is normal and cannot be bypassed by the app.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew test assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The current car-screen concept is [`releases/motoring-dashboard-v0.2.0.apk`](releases/motoring-dashboard-v0.2.0.apk). It declares a standard Android Auto media browser and presents three visual-design programs through Android Auto's native library and Now Playing surfaces. Upload [`releases/motoring-dashboard-v0.2.0.aab`](releases/motoring-dashboard-v0.2.0.aab) to Play Console. Both are signed with the project's release upload key, not Android's debug certificate.

The private upload key and its properties live in the ignored `signing/` directory. Preserve both files securely: every future Play update must use the same upload key. They are intentionally never committed.

## Run on Pixel 7 / 7a and Mazda CX-5

1. Enable Android Auto developer mode on the phone: Android Auto settings → tap **Version** repeatedly → menu → **Developer settings**.
2. In Developer settings, enable **Unknown sources**.
3. Enable USB debugging in the phone's Android developer options and install the debug build:

   ```bash
   adb install -r releases/motoring-dashboard-v0.2.0.apk
   ```

4. Open **Motoring Dashboard** once on the phone.
5. Connect the Pixel to the Mazda using the infotainment USB port and a data-capable USB cable, then launch Android Auto.
6. Open Motoring Dashboard in the Android Auto launcher and choose **Allow vehicle data**. Grant only the vehicle permissions you want to share.

Because v0.2 is a standard media app, Android Auto's **Unknown sources** developer option can discover a directly installed APK. In Android Auto settings, open **Customize launcher** and ensure Motoring Dashboard is enabled before reconnecting the phone.

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
- `DashboardScreen`: safe `PaneTemplate` UI and Car Hardware listeners
- `DashboardRepository`: in-process state shared by the projected and companion surfaces

No network access, analytics, account, or background location is used.
