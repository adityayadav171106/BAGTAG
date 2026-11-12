# BagTag version-1 test plan

Run hardware tests with the USB-powered ESP32 and a real Android phone.
Record phone model, Android version, app build, firmware revision, location
settings, and power-management settings for each run.

| ID | Action | Expected result | Status |
|---|---|---|---|
| TEST-001 | Connect ESP32 to USB | Firmware starts and advertises | Pending |
| TEST-002 | Scan with nRF Connect | `BagTag-01` appears and repeats in range | Passed by report |
| TEST-002A | Scan with the BagTag Android app after scanner correction | `BagTag-01` is detected and shown by the app | Passed by owner report; capture phone/build/timestamp evidence |
| TEST-003 | Move phone nearer and farther | RSSI changes; app does not present RSSI as exact distance | Pending — implementation ready, device run required |
| TEST-004 | Leave range or power off tag | Missing state follows configured timeout | Pending |
| TEST-005 | Briefly block signal near tag | No false forgotten-item alert from one short loss | Pending |
| TEST-006 | Leave tag and move phone away | One alert occurs after configured conditions | Pending |
| TEST-007 | Return near tag | Missing state clears and return event is recorded | Pending |
| TEST-008 | Enter a test zone | Entry event and configured notification occur | Pending |
| TEST-009 | Exit a test zone | Exit event and configured notification occur | Pending |
| TEST-010 | Deny Bluetooth or location permission | Clear explanation; unsupported feature is not claimed active | Pending |
| TEST-011 | Turn Bluetooth off and on | Scanner recovers or gives actionable restart guidance | Pending |
| TEST-012 | Lock the phone and repeat detection | Delays and limits are recorded; no guarantee is claimed | Pending |
| TEST-013 | Clear selected local data | Selected tag, zone, or event data is deleted | Pending — event clear and zone delete implemented; full data-clear path remains |
| TEST-014 | Detect tag inside a saved Home zone, then leave without the tag | No forgotten-item alert; UI records a safe-place state | Pending — core unit test authored, device run required |
| TEST-015 | Detect tag at an unsaved location, power off tag, then move phone beyond configured threshold | Exactly one forgotten-item notification after timeout and grace period | Pending — core unit test authored, device run required |
| TEST-016 | Lock phone while monitoring, then repeat TEST-015 | Foreground notification remains and limitations/delay are recorded | Pending — foreground service implementation requires device run |
| TEST-017 | Turn Bluetooth off, then back on during active monitoring | UI shows loss/recovery and scanner resumes after Bluetooth returns | Pending — service recovery implementation requires device run |
| TEST-018 | Detect tag, delay GPS resolution beyond 20 seconds, then disconnect tag | Hard deadline fires once and forgotten-item notification is sent without waiting for a later GPS callback | Pending — device run required |

## Test evidence rules

- Record timestamps for state transitions and notifications.
- Record whether location permission and notification permission were granted.
- Record false alerts separately from delayed alerts.
- Do not store a detailed location trail unless explicitly enabled for a test.
