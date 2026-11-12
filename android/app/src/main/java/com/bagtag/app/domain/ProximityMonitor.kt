package com.bagtag.app.domain

/** Pure forgotten-tag state machine. BLE loss alone never proves an item was forgotten. */
class ProximityMonitor(private val config: TagConfig) {
    private val rssiSamples = ArrayDeque<Int>()
    private var lastSeenAtMs: Long? = null
    private var lastObservation: LastTagObservation? = null
    private var locationTimedOut = false
    private var status = TagStatus.NOT_MONITORED
    private var alertRaised = false

    fun start() { status = TagStatus.WAITING_FOR_TAG; alertRaised = false }
    fun stop() { status = TagStatus.NOT_MONITORED; rssiSamples.clear(); lastSeenAtMs = null; lastObservation = null; locationTimedOut = false; alertRaised = false }

    fun onDetection(rssi: Int, nowMs: Long, observation: LastTagObservation?, locationTimedOut: Boolean = false): List<ProximityDecision> {
        rssiSamples.addLast(rssi); if (rssiSamples.size > 8) rssiSamples.removeFirst()
        val wasAlerted = status == TagStatus.ALERT_SENT
        lastSeenAtMs = nowMs
        // A later detection without valid GPS must not inherit an earlier location.
        lastObservation = observation
        this.locationTimedOut = locationTimedOut
        alertRaised = false; status = TagStatus.WITH_TAG
        return buildList {
            add(ProximityDecision.StateChanged(TagStatus.WITH_TAG, "Tag detected"))
            if (wasAlerted && config.returnAlertsEnabled) add(ProximityDecision.SendReturnAlert)
        }
    }

    fun recordLocation(observation: LastTagObservation) {
        if (status != TagStatus.ALERT_SENT) {
            lastObservation = observation
            locationTimedOut = false
        }
    }

    fun markLocationTimedOut() {
        if (lastObservation == null) locationTimedOut = true
    }

    fun evaluate(nowMs: Long, distanceFromLastTagMeters: Float?, cooldownOpen: Boolean, locationDeadlineExceeded: Boolean = false): List<ProximityDecision> {
        if (status == TagStatus.NOT_MONITORED) return emptyList()
        val lastSeen = lastSeenAtMs ?: return change(TagStatus.WAITING_FOR_TAG, "Waiting for the first tag detection")
        val missingFor = nowMs - lastSeen
        if (missingFor < config.missingTimeoutMs) return emptyList()
        if (missingFor < config.missingTimeoutMs + config.alertGracePeriodMs) return change(TagStatus.SIGNAL_TEMPORARILY_LOST, "Tag signal temporarily lost")
        val observation = lastObservation
        if (observation == null && !locationTimedOut) return change(TagStatus.SIGNAL_TEMPORARILY_LOST, "Tag location is unavailable; no forgotten-item alert")
        if (locationDeadlineExceeded && !alertRaised && config.forgottenAlertsEnabled && cooldownOpen) {
            alertRaised = true; status = TagStatus.ALERT_SENT
            return listOf(ProximityDecision.StateChanged(TagStatus.ALERT_SENT, "You may have left your tag behind; location check timed out"), ProximityDecision.SendForgottenAlert(observation))
        }
        if (observation == null) return change(TagStatus.ALERT_SENT, "Forgotten-item alert already sent")
        if (observation.safeZoneId != null) return change(TagStatus.SAFE_ZONE_LEFT_BEHIND, "Tag was last seen at ${observation.safeZoneName ?: "a saved safe place"}")
        if (distanceFromLastTagMeters == null) return change(TagStatus.SIGNAL_TEMPORARILY_LOST, "Tag is missing; current phone location is unavailable")
        if (distanceFromLastTagMeters < config.movementThresholdMeters) return change(TagStatus.SIGNAL_TEMPORARILY_LOST, "Tag is missing, but you are still nearby")
        if (!alertRaised && config.forgottenAlertsEnabled && cooldownOpen) {
            alertRaised = true; status = TagStatus.ALERT_SENT
            return listOf(ProximityDecision.StateChanged(TagStatus.ALERT_SENT, "You may have left your tag behind"), ProximityDecision.SendForgottenAlert(observation))
        }
        return change(TagStatus.ALERT_SENT, "Forgotten-item alert already sent")
    }

    fun snapshot(locationAvailable: Boolean) = TagSnapshot(
        status = status, latestRssi = rssiSamples.lastOrNull(),
        filteredRssi = rssiSamples.sorted().let { if (it.isEmpty()) null else it[it.size / 2] },
        lastSeenAtMs = lastSeenAtMs, locationAvailable = locationAvailable,
        lastTagZoneName = lastObservation?.safeZoneName, lastTagLocationKnown = lastObservation != null,
        message = when (status) {
            TagStatus.NOT_MONITORED -> "Monitoring is stopped"
            TagStatus.WAITING_FOR_TAG -> "Waiting for the first tag detection"
            TagStatus.WITH_TAG -> "Tag detected"
            TagStatus.SIGNAL_TEMPORARILY_LOST -> "Tag signal temporarily lost"
            TagStatus.SAFE_ZONE_LEFT_BEHIND -> "Tag is at a saved safe place"
            TagStatus.ALERT_SENT -> "You may have left your tag behind"
        },
    )

    private fun change(next: TagStatus, message: String): List<ProximityDecision> {
        if (status == next) return emptyList()
        status = next
        return listOf(ProximityDecision.StateChanged(next, message))
    }
}
