# Android application

This is the local-only Android implementation for the BagTag v1 prototype.
It scans for the prototype manufacturer's-data identifier (`TAG001`), rather
than trusting the advertised name alone.

## Visual system and required font

The app bundles `res/font/american_captain.ttf`, supplied by the project owner,
as its required application family. Its original source folder is
`C:\Users\aman\Downloads\american_captain`; the supplied files are `American
Captain.ttf` and `American Captain.otf`. `BagTagTheme` applies the bundled TTF
to the Material typography used throughout the app. See `docs/font-assets.md`
for provenance, integrity data, and the maintenance rule.

## Open and run

Open the `android` directory in a current Android Studio installation. Install
Android SDK Platform 35 and use JDK 17. Allow Gradle to create its wrapper or
run the project from the IDE, then use:

```text
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

The device/emulator test only verifies the initial UI. BLE scanning,
notifications, location, and geofence transitions need a physical Android
phone and the ESP32 beacon. Grant Bluetooth, location, and notification access
only when testing the corresponding feature. Background location is required
by Android before geofence monitoring can work reliably.

## What is implemented

- Version-aware runtime permission request path for BLE, location, and alerts.
- Manufacturer-data matching, scan start/stop states, RSSI median filtering,
  last-seen tracking, timeout, grace period, safe-zone classification, distance
  gate, cooldown, and return handling.
- A foreground `MonitoringService` that owns BLE scanning and evaluations after
  the user starts monitoring. The UI no longer owns the scan lifecycle.
- Local SharedPreferences storage for settings, last valid location, zones,
  the last tag observation, and the latest 50 events. No cloud endpoint or
  detailed location history.
- Local notification channel and manual notification test.
- Create/edit/delete zone UI and Android geofence registration/transition receiver.
- Alert settings for forgotten-item and return notifications.

## Forgotten-tag rule

Saved zones are safe locations: the app does not send a forgotten-item alert
when the tag was last detected inside one. Outside saved zones, an alert needs
an accurate last-tag location, the configured BLE timeout and grace period,
the phone to move beyond the configured distance from that observation, and an
open cooldown. This distinction is the central product behavior. See
`../docs/forgotten-tag-behavior.md` for examples and limits.

## Operational limits

RSSI is not distance. A missing tag becomes a possible forgotten item only
after the configured timeout, an available movement comparison, and cooldown
checks. Android can delay BLE and geofence processing while a device is idle;
the app deliberately makes no instant or guaranteed-background claim.

## Implemented package layout

```text
app/src/main/java/com/bagtag/app/
  ble/
    BleScanner.kt
  location/
    LocationRepository.kt
    GeofenceManager.kt
  alerts/
    AlertManager.kt
  data/
    TagRepository.kt
  domain/
    Models.kt
    ProximityMonitor.kt
    Rules.kt
  ui/theme/
    BagTagTheme.kt
  MainActivity.kt
  BagTagViewModel.kt
```

Keep BLE, location, alerting, storage, domain logic, and UI responsibilities
separate.

## Required platform behavior

- Request Bluetooth permissions that match the Android version.
- Request location permission before location-based features.
- Request notification permission where required.
- Keep unrelated UI usable when a permission is denied.
- Handle Bluetooth being disabled and re-enabled.
- Use a supported background strategy and document screen-off limits.
- Use Android geofencing for saved zones where supported.

Do not add a cloud backend to version 1.
