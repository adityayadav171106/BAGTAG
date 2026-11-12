# BagTag project plan

## Objective

Deliver a testable version-1 prototype that detects one ESP32-WROOM BLE tag
with one Android phone and gives conservative forgotten-item alerts when the
phone moves away from a tag left outside a saved safe zone. Saved zones are
places where leaving a tag is intentional, not generic location-alert targets.

## Constraints

- USB-powered ESP32 during development.
- Arduino firmware with NimBLE-Arduino.
- Kotlin and Jetpack Compose for Android.
- Local storage only. No cloud service.
- No GPS receiver in the tag.
- No exact distance or guaranteed real-time background behavior.

## Milestones

| ID | Milestone | Exit condition | Dependencies |
|---|---|---|---|
| M1 | BLE beacon baseline | `BagTag-01` advertises `TAG001` after power-on and is repeatedly visible in nRF Connect | None |
| M2 | Android scanner | The app starts and stops a scan, identifies the configured tag, shows RSSI and last-seen time, and handles disabled Bluetooth or denied permissions | M1 |
| M3 | Forgotten-item alerting | Last-tag observation, safe-zone classification, timeout, grace, phone-to-observation distance, cooldown, and local notifications pass tests | M2 |
| M4 | Safe zones | Users can create, edit, delete, and monitor named safe zones used to suppress intentional left-behind alerts | M2 |
| M5 | Combined prototype | Zone exit plus missing-tag rule, recovery behavior, settings, data clearing, and screen-off limits are documented and tested | M3, M4 |

## Work packages

### WP-01: Firmware baseline

- Move the confirmed sketch into the repository firmware path.
- Verify serial startup at 115200 baud.
- Verify the advertised name and identifier.
- Record RSSI observations at controlled distances.
- Add advertising recovery only if testing exposes a recoverable failure.

### WP-02: Android project foundation

- Create the Kotlin/Compose Android Studio project.
- Add version-appropriate Bluetooth, location, and notification permissions.
- Add a permission/status model and user-facing explanations.
- Add local settings storage.

### WP-03: BLE scanning

- Implement `BleScanner` and `TagDevice`.
- Filter by the configured tag identifier, not name alone.
- Expose scan start/stop and Bluetooth-disabled states.
- Persist last-seen time and the most recent RSSI samples.

### WP-04: Forgotten-item alerts

- Implement a moving average or median over recent RSSI values.
- Use a timeout before changing from Nearby to Checking or Missing.
- Record the last accurate phone location at the moment of a valid tag detection.
- Classify that observation against saved safe zones immediately.
- Compare current phone location against the last-tag location, not only against a previous phone location.
- Add a grace period after the missing timeout.
- Use a cooldown so one event creates at most one alert.
- Show lower-confidence missing status when location is unavailable.
- Add a manual notification test.

Initial test values are 45 seconds for missing timeout and 50 metres for
movement. Treat them as configurable test values, not proven limits.

### WP-05: Safe zones and location

- Capture the phone location near valid detections when permitted.
- Validate accuracy before storing location data.
- Implement saved zones with name, center, radius, and enabled transitions.
- Use zones as safe places that suppress forgotten-item alerts.
- Retain optional geofence events only if they demonstrably help the core alert behavior.

### WP-06: Hardening and documentation

- Test Bluetooth restart, permission denial, screen-off behavior, and data
  clearing.
- Measure battery impact during representative monitoring.
- Confirm no detailed location history is stored by default.
- Write the user guide and test report.
- State clearly that the static prototype identifier is not secure.

## Definition of done

Version 1 is done when all acceptance criteria in the concept document are
verified, or a known limitation is recorded with evidence. The app must not
claim exact range, global tracking, or certainty when it only has an estimate.

## Recommended implementation order

1. Complete M1 verification using the existing firmware.
2. Create the Android project and permission model.
3. Implement scanning and the home screen.
4. Implement proximity state transitions and notifications.
5. Implement location capture and saved zones.
6. Add combined rules, recovery, settings, and data clearing.
7. Run the complete test plan on screen-on and screen-off paths.
