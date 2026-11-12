# BagTag delivery progress

Last updated: 26 October 2026

## Current state

The ESP32 advertising baseline is reported as detected in nRF Connect. The
Android application source has now been created, including local storage, BLE
matching, proximity rules, notifications, geofence registration, basic zone
management, and automated tests for the pure decision logic. No Android build
or on-device execution has been run from this workspace because it contains no
Android SDK, Gradle installation/wrapper, Kotlin compiler, or connected phone.
The project was subsequently opened and built in Android Studio on a developer
machine. Three Kotlin import errors were reported and corrected on 25 October;
the next Android Studio rebuild must confirm that the corrected source passes.

## Implementation ledger

| Area | Delivered source | Current verification state |
|---|---|---|
| BLE beacon | `firmware/BagTagBeacon/BagTagBeacon.ino` and mirrored `bagtagespcode/bagtagespcode.ino` | nRF Connect discovery reported by the project owner; reflash recommended for corrected manufacturer data. |
| Android foundation | `android/settings.gradle.kts`, `android/build.gradle.kts`, and `android/app/build.gradle.kts` | Android Studio project created; rebuild pending after import correction. |
| Scan and matching | `android/app/src/main/java/com/bagtag/app/ble/BleScanner.kt` and `domain/Rules.kt` | Supports parsed payloads, raw advertisements, and exact configured-name fallback. Owner reported tag detection works after the correction. |
| Proximity rules | `domain/ProximityMonitor.kt`, `BagTagViewModel.kt` | Unit tests authored; run still required. |
| Local data and alerts | `data/TagRepository.kt`, `alerts/AlertManager.kt` | Source implemented; device permission and notification tests required. |
| Location and zones | `location/` package and zones UI | Source implemented; device/geofence tests required. |
| UI system | `MainActivity.kt`, `ui/theme/BagTagTheme.kt` | Redesigned; Android Studio rebuild pending. |
| Forgotten-tag service | `monitoring/MonitoringService.kt`, `domain/ProximityMonitor.kt`, `domain/SafeZoneClassifier.kt` | Implemented in source; unit and physical-device verification required. |

## 26 October 2026 product recalibration

The prototype now treats saved zones as safe places, not as a reason to send
extra geofence alerts. The core alert compares the current phone location with
the last accurate phone location at which the tag was detected. A tag last
seen inside a saved zone never creates the forgotten-item alert. A tag last
seen outside a saved zone can alert only after the configured timeout, grace
period, movement threshold, and cooldown. See `docs/forgotten-tag-behavior.md`.

## 25 October 2026 detection correction

The first Android scanner accepted only an exact `manufacturerSpecificData`
payload. Android removes the mandatory two-byte manufacturer/company prefix
when it exposes that field. The original ESP32 sketch omitted that prefix, so
an Android device can expose `G001` instead of `TAG001` and the app discards a
real scan result. The scanner now checks every parsed manufacturer payload,
the unmodified advertisement bytes, and finally the exact configured name.
The firmware now also emits the testing company ID `0xFFFF` followed by
`TAG001`, which is the normal Android-compatible form. Reflash the ESP32 when
convenient; updating the Android app alone also supports the old packet.

## 25 October 2026 UI redesign

The app now uses a minimal, dark, signal-first dashboard: a live status chip,
one primary monitoring action, compact signal/last-seen cards, and calmer
zones/settings views. The owner-provided American Captain TTF is bundled in
the Android resources and applied by the application theme. No stock imagery
was added because it would not support this monitoring utility's task flow.

## 25 October 2026 build correction

Android Studio reported unresolved `setContent` and `rememberSaveable`
references in `MainActivity.kt`. The missing imports
`androidx.activity.compose.setContent` and
`androidx.compose.runtime.saveable.rememberSaveable` were added. Rebuild from
Android Studio before treating this correction as verified.

## Milestone status

| Milestone | Status | Evidence / remaining work |
|---|---|---|
| M1 BLE beacon baseline | Partially verified | nRF Connect discovery was reported. Serial startup, repeated discovery, and recovery tests still need hardware evidence. |
| M2 Android scanner | Implemented; unverified | `BleScanner` matches `TAG001` in manufacturer data and exposes disabled/denied states. Build and phone scan remain required. |
| M3 Proximity alerting | Logic unit-tested; integration unverified | Timeout, temporary loss, movement gate, cooldown, median RSSI, and return are covered by JVM tests. Notification/device tests remain. |
| M4 Geofencing | Implemented; unverified | Local zone persistence, create/edit/delete UI, registration, receiver, event logging, and zone notifications are present. Test on a real phone. |
| M5 Combined/hardening | In progress | Alert settings, selective event/zone clearing, and conservative limits are implemented. Combined zone-exit/missing-tag rule, foreground monitoring strategy, device battery study, and full screen-off study remain. |

## Automated verification added

| Test source | Scope |
|---|---|
| `android/app/src/test/java/com/bagtag/app/domain/ProximityMonitorTest.kt` | temporary loss, missing state, movement-gated alert, single alert, return, RSSI median |
| `android/app/src/test/java/com/bagtag/app/domain/RulesTest.kt` | strict tag ID matching, cooldown, zone validation |
| `android/app/src/androidTest/java/com/bagtag/app/SmokeTest.kt` | permission-free initial Compose UI smoke test on an emulator/device |

Run `testDebugUnitTest` before an emulator/device run. Then run
`connectedDebugAndroidTest` and complete the hardware matrix in
`docs/test-plan.md`. Do not mark a test as passed merely because the source
exists.

## Next engineering path

1. Install Android SDK 35, JDK 17, and Gradle/Android Studio; run the automated commands in `android/README.md`.
2. Resolve any dependency/API errors found by the first build.
3. Run TEST-001 through TEST-013 with a phone and USB-powered beacon, recording environment and timestamps.
4. Implement the combined zone-exit/missing-tag rule and foreground-service policy after device behavior is measured.
