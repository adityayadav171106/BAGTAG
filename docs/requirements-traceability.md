# Requirements traceability

This map converts the version-1 requirements into implementation and
verification targets. IDs are from `BAGTAG-SYS-001`.

| Requirements | Planned component | Verification |
|---|---|---|
| FR-001 to FR-004, FW-001 to FW-007 | `firmware/BagTagBeacon/BagTagBeacon.ino` | TEST-001, TEST-002 |
| FR-005 to FR-009 | `android/app/.../ble/BleScanner.kt`, `domain/Models.kt`, home UI | Unit tests for identifier matching; TEST-003, TEST-011 pending device run |
| FR-010 to FR-014, FR-019, FR-023 | `android/app/.../domain/ProximityMonitor.kt`, `alerts/AlertManager.kt` | Unit tests pass when run; TEST-004 to TEST-007 pending device run |
| FR-015 to FR-018 | `android/app/.../location/GeofenceManager.kt`, `domain/SavedZone.kt`, zones UI | TEST-008, TEST-009 pending device run |
| FR-020, FR-025 | `data/TagRepository.kt`, event history UI | TEST-013 |
| FR-021, FR-022 | permission model, scanner recovery, settings UI | TEST-010, TEST-011 |
| FR-024 | `alerts/AlertManager.kt`, settings UI | Manual notification test |
| NFR-001 to NFR-010 | whole project and documentation | TEST-003, TEST-005, TEST-010, TEST-012, TEST-013 |

## Acceptance checklist

- [ ] AC-001: ESP32 advertises after power-on.
- [x] AC-002: Android detects the configured tag. The project owner reported successful tag detection after the scanner correction; formal device evidence should still be recorded.
- [x] AC-003: State, RSSI, and last-seen time are visible. The owner reported that the app shows the tag; record RSSI and timestamp evidence in the hardware test log.
- [ ] AC-004: Missing state follows the configured timeout. Unit tests authored; needs execution and device evidence.
- [ ] AC-005: Forgotten-item alert requires configured conditions. Unit tests authored; needs execution and device evidence.
- [ ] AC-006: Tag return clears the missing state. Unit tests authored; needs execution and device evidence.
- [ ] AC-007: Zones can be created, edited, and deleted. Implemented; needs device evidence.
- [ ] AC-008: Zone entry and exit events are recorded and notified. Implemented; needs device evidence.
- [ ] AC-009: Denied permissions and disabled services have clear messages. BLE path implemented; needs device evidence.
- [ ] AC-010: Known limits are visible and claims remain conservative. Implemented in the UI and Android README; needs review.
