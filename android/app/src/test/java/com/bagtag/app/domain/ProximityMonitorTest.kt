package com.bagtag.app.domain

import org.junit.Assert.*
import org.junit.Test

class ProximityMonitorTest {
    private val config = TagConfig(missingTimeoutMs = 60_000, alertGracePeriodMs = 30_000, movementThresholdMeters = 75f, cooldownMs = 300_000, returnAlertsEnabled = true)
    private val unsafe = LastTagObservation(1_000, 12.0, 77.0, 10f)
    private val home = unsafe.copy(safeZoneId = "home", safeZoneName = "Home")

    @Test fun `brief BLE loss does not change with-tag state`() {
        val monitor = ProximityMonitor(config); monitor.start(); monitor.onDetection(-62, 1_000, unsafe)
        assertTrue(monitor.evaluate(60_999, 100f, true).isEmpty())
        assertEquals(TagStatus.WITH_TAG, monitor.snapshot(true).status)
    }
    @Test fun `unknown location alerts only after timeout grace and movement`() {
        val monitor = ProximityMonitor(config); monitor.start(); monitor.onDetection(-62, 1_000, unsafe)
        assertEquals(TagStatus.SIGNAL_TEMPORARILY_LOST, (monitor.evaluate(61_000, 100f, true).first() as ProximityDecision.StateChanged).status)
        val alert = monitor.evaluate(91_000, 76f, true)
        assertTrue(alert.any { it is ProximityDecision.SendForgottenAlert })
        assertEquals(TagStatus.ALERT_SENT, monitor.snapshot(true).status)
    }
    @Test fun `missing tag at saved home never sends forgotten alert`() {
        val monitor = ProximityMonitor(config); monitor.start(); monitor.onDetection(-62, 1_000, home)
        val decisions = monitor.evaluate(91_000, 500f, true)
        assertFalse(decisions.any { it is ProximityDecision.SendForgottenAlert })
        assertEquals(TagStatus.SAFE_ZONE_LEFT_BEHIND, monitor.snapshot(true).status)
    }
    @Test fun `missing tag outside safe zone while still nearby does not alert`() {
        val monitor = ProximityMonitor(config); monitor.start(); monitor.onDetection(-62, 1_000, unsafe)
        assertFalse(monitor.evaluate(91_000, 74f, true).any { it is ProximityDecision.SendForgottenAlert })
    }
    @Test fun `no accurate last-tag location never alerts`() {
        val monitor = ProximityMonitor(config); monitor.start(); monitor.onDetection(-62, 1_000, null)
        assertFalse(monitor.evaluate(91_000, 500f, true).any { it is ProximityDecision.SendForgottenAlert })
    }
    @Test fun `location timeout allows alert after missing and grace periods`() {
        val monitor = ProximityMonitor(config); monitor.start()
        monitor.onDetection(-62, 1_000, null)
        monitor.markLocationTimedOut()
        assertTrue(monitor.evaluate(111_000, null, true, locationDeadlineExceeded = true).any { it is ProximityDecision.SendForgottenAlert })
        assertEquals(TagStatus.ALERT_SENT, monitor.snapshot(false).status)
    }
    @Test fun `deadline alerts even when a GPS fix exists but decision remains stuck`() {
        val monitor = ProximityMonitor(config); monitor.start()
        monitor.onDetection(-62, 1_000, unsafe)
        assertTrue(monitor.evaluate(111_000, 10f, true, locationDeadlineExceeded = true).any { it is ProximityDecision.SendForgottenAlert })
    }
    @Test fun `alert is sent once then tag return clears state`() {
        val monitor = ProximityMonitor(config); monitor.start(); monitor.onDetection(-62, 1_000, unsafe)
        assertEquals(1, monitor.evaluate(91_000, 76f, true).count { it is ProximityDecision.SendForgottenAlert })
        assertTrue(monitor.evaluate(100_000, 76f, true).isEmpty())
        assertTrue(monitor.onDetection(-60, 101_000, unsafe).any { it is ProximityDecision.SendReturnAlert })
        assertEquals(TagStatus.WITH_TAG, monitor.snapshot(true).status)
    }
    @Test fun `median RSSI resists one outlier`() {
        val monitor = ProximityMonitor(config); monitor.start(); listOf(-60, -61, -59, -100, -60).forEachIndexed { index, rssi -> monitor.onDetection(rssi, index.toLong(), unsafe) }
        assertEquals(-60, monitor.snapshot(true).filteredRssi)
    }
}
