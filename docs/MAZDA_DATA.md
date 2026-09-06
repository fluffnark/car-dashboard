# 2021 Mazda CX-5 Grand Touring data availability

Research date: September 6, 2026. Scope: U.S.-market 2021 CX-5 Grand Touring with the factory Mazda Connect system, projected Android Auto, and a Pixel 7 or Pixel 7a.

## Bottom line

The factory system supports the app's projection path. Mazda says the 2021 CX-5 sends **location, speed, and other vehicle data** to the connected phone while Android Auto is in use. Speed is therefore the strongest live vehicle signal for this dashboard. Mazda does not publish a field-by-field third-party interface for fuel, range, odometer, or model identity, so those values must be tested in the actual vehicle and remain explicitly optional in the app.

The 2021 CX-5 has a 10.25-inch Mazda Connect display and standard Android Auto across the U.S. range, including Grand Touring. The documented baseline connection is USB. Pixel 7 and Pixel 7a meet Android Auto's Android 9+ phone requirement.

## Confidence matrix

| Reading | In the 2021 CX-5 | Sent toward Android Auto | Public app API | Expected in this app |
|---|---|---|---|---|
| Clock/date | Phone-provided | Not needed | Standard Android | **Yes** |
| Vehicle speed | Speedometer and multi-information display | Mazda explicitly confirms speed transfer | `CarInfo.addSpeedListener` | **Likely**, subject to permission/host |
| Location | Navigation/compass functions | Mazda explicitly confirms location transfer | `CarSensors.addCarHardwareLocationListener` | Available to add; not displayed in v0.1 |
| Heading/compass | Native compass display | Not specified field-by-field | `CarSensors.addCompassListener` | Possible, but unconfirmed on this head unit |
| Fuel level | Native fuel gauge | Only covered by “other vehicle data” | `CarInfo.addEnergyLevelListener` | Possible; unconfirmed |
| Distance to empty | Native multi-information display | Only covered by “other vehicle data” | `CarInfo.addEnergyLevelListener` | Possible; unconfirmed |
| Odometer | Native multi-information display | Not specifically confirmed | `CarInfo.addMileageListener` | Possible; unconfirmed |
| Make/model/year | Known by vehicle/host | Not specifically confirmed | `CarInfo.fetchModel` | Possible; unconfirmed |
| Outside temperature | Native multi-information display | Not specifically confirmed | No projected Car App field | **No** through the public API |
| Current/average fuel economy | Native Mazda displays | Not specifically confirmed | No projected Car App field | **No** through the public API |
| RPM, gear, coolant temperature | Vehicle internally knows these | Not documented for Android Auto | No projected Car App field | **No** without external hardware |
| Tire pressure, doors, ADAS state | Some data exists internally | Not documented for Android Auto | No projected Car App field | **No** without private/OBD access |

“Sent toward Android Auto” is not the same as “available to every third-party app.” Android Auto's host consumes vehicle signals for platform features, and the Car App Library exposes only a permission-gated subset. Google explicitly says availability can depend on the OEM. A successful real-car test is the only conclusive answer for fuel, range, mileage, model, and compass on a particular Mazda firmware version.

## Sources

- [Mazda 2021 CX-5 manual — Android Auto appendix](https://www.mazdausa.com/static/manuals/2021/cx-5/contents/06100202.html): confirms location, speed, and other vehicle data transfer to the phone.
- [Mazda 2021 CX-5 instrument cluster manual](https://www.mazdausa.com/static/manuals/2021/cx-5/contents/05020808.html): lists speed, odometer, outside temperature, distance-to-empty, fuel economy, compass, and related native displays.
- [Mazda 2021 CX-5 announcement](https://news.mazdausa.com/vehicles-2021-cx-5): confirms the 10.25-inch Mazda Connect display and standard Android Auto.
- [Android Car Hardware APIs](https://developer.android.com/training/cars/apps/library/car-hardware-api): documents public speed, fuel/range, mileage, model, location, compass, accelerometer, and gyroscope access and OEM-dependent availability.
- [Android for Cars overview](https://developer.android.com/training/cars): documents Android Auto's Android 9+ phone requirement and supported application categories.

## Practical verification in the Mazda

After installing the APK and granting vehicle permissions, connect by USB and inspect each row:

1. Drive only in a safe, legal setting and compare app speed with the Mazda speedometer.
2. Compare fuel percentage and range with the multi-information display.
3. Compare odometer with the cluster.
4. Any em dash means Android Auto returned unavailable, unimplemented, or permission denied—it does not mean the Mazda lacks the underlying sensor.

If fuel or mileage remains unavailable, the supported next option is an external Bluetooth Low Energy OBD-II adapter with a separate, carefully permissioned phone-side data source. OBD integration is not included in v0.1.
