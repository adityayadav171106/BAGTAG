# Forgotten-tag behavior

## Product promise

BagTag is a local, phone-assisted forgotten-item reminder. It should warn when
the phone moves away from a tag left at an **unsaved** location. A saved zone is
a safe place where leaving the tag is intentional, such as Home.

It does not claim GPS-level tag tracking. The location saved is the phone's
location at the most recent valid BLE detection of the tag.

## Core rule

The default forgotten-item alert requires all of these conditions:

1. The tag was previously detected with an accurate phone location.
2. That last-tag location was outside all saved safe zones.
3. The tag has not been detected for 60 seconds.
4. A further 30-second grace period has passed.
5. The current phone location is at least 75 metres from the last-tag location.
6. The five-minute alert cooldown is open.

The timeout, grace period, and movement distance are editable in Settings.

## Outcomes

| Situation | Result |
|---|---|
| Phone and tag leave Home together | Tag continues to be detected; no alert. |
| Tag is left at Home | Tag last observation is inside a saved safe zone; no forgotten-item alert. |
| Tag is left at an unsaved shop and phone moves away | One forgotten-item alert after timeout, grace, and distance checks. |
| One or a few BLE packets are missed | `SIGNAL_TEMPORARILY_LOST`; no immediate alert. |
| Location is unavailable or inaccurate at last detection | No forgotten-item alert; the app cannot safely classify the left-behind location. |
| Tag is detected again | The active forgotten state clears; an optional return alert can be sent. |

## Runtime ownership

`MonitoringService` owns BLE scanning, timeout evaluation, GPS comparison, and
notifications while monitoring is enabled. `BagTagViewModel` only exposes the
service state to the visible UI. This prevents monitoring from stopping merely
because `MainActivity` is no longer visible.

The service is started only from the user pressing Start Monitoring. It uses a
foreground notification and declares `connectedDevice|location` service types.
Android requires foreground-service types and the matching permissions on
recent versions; see the [Android foreground-service documentation](https://developer.android.com/develop/background-work/services/fgs/service-types)
and [location permission guidance](https://developer.android.com/develop/sensors-and-location/location/permissions).

## Explicit limits

- Screen-off behavior still needs physical-device verification. The scanner
  uses an exact tag-name `ScanFilter`, because Android stops unfiltered scans
  when the screen turns off; see the [BLE scanner reference](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner).
- A foreground service improves continuity but cannot promise uninterrupted
  execution under every vendor battery-management policy.
- The current prototype is not paired or authenticated. `TAG001` remains a
  public prototype identifier.
- Phone GPS has accuracy limits; the app rejects locations worse than 100 m.
- Location checks request a fresh high-accuracy fix (up to 20 seconds) rather
  than depending only on Android's cached `lastLocation`. The status screen
  reports whether the monitoring service currently has a usable fix.
- Once the tag has passed the missing timeout and grace period, the location
  decision has a hard 20-second deadline. If the decision is still unresolved
  at that point, the app sends the forgotten-item alert without GPS distance
  verification. This fallback avoids leaving monitoring stuck while checking
  in weak-signal locations.
