package com.bagtag.app.domain

enum class TagStatus { NOT_MONITORED, WAITING_FOR_TAG, WITH_TAG, SIGNAL_TEMPORARILY_LOST, SAFE_ZONE_LEFT_BEHIND, ALERT_SENT }
enum class EventType { TAG_MISSING, TAG_RETURNED, ZONE_ENTERED, ZONE_EXITED, MANUAL_TEST, TAG_LEFT_SAFE_ZONE }

data class TagConfig(
    val id: String = "TAG001",
    val displayName: String = "BagTag-01",
    val missingTimeoutMs: Long = 60_000,
    val alertGracePeriodMs: Long = 30_000,
    val movementThresholdMeters: Float = 75f,
    val cooldownMs: Long = 300_000,
    val forgottenAlertsEnabled: Boolean = true,
    val returnAlertsEnabled: Boolean = false,
)

data class TagSnapshot(
    val status: TagStatus = TagStatus.NOT_MONITORED,
    val latestRssi: Int? = null,
    val filteredRssi: Int? = null,
    val lastSeenAtMs: Long? = null,
    val locationAvailable: Boolean = false,
    val lastTagZoneName: String? = null,
    val lastTagLocationKnown: Boolean = false,
    val message: String = "Monitoring is stopped",
)

data class LastKnownLocation(val latitude: Double, val longitude: Double, val accuracyMeters: Float, val recordedAtMs: Long)

data class LastTagObservation(
    val detectedAtMs: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val safeZoneId: String? = null,
    val safeZoneName: String? = null,
)

data class SavedZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val entryEnabled: Boolean = true,
    val exitEnabled: Boolean = true,
    val combinedExitRuleEnabled: Boolean = false,
)

data class AppEvent(val type: EventType, val message: String, val atMs: Long)

sealed interface ProximityDecision {
    data object NoChange : ProximityDecision
    data class StateChanged(val status: TagStatus, val message: String) : ProximityDecision
    data class SendForgottenAlert(val observation: LastTagObservation?) : ProximityDecision
    data object SendReturnAlert : ProximityDecision
}
