package com.bagtag.app.domain

import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    @Test fun `tag matches full manufacturer value`() { assertTrue(TagMatcher.matches(listOf("TAG001".toByteArray()), null, null, "TAG001", "BagTag-01")); assertFalse(TagMatcher.matches(listOf("TAG002".toByteArray()), null, null, "TAG001", "BagTag-01")) }
    @Test fun `tag matches legacy raw advertisement when Android strips first two bytes`() { assertTrue(TagMatcher.matches(listOf("G001".toByteArray()), byteArrayOf(7, 0xFF.toByte(), 'T'.code.toByte(), 'A'.code.toByte(), 'G'.code.toByte(), '0'.code.toByte(), '0'.code.toByte(), '1'.code.toByte()), null, "TAG001", "BagTag-01")) }
    @Test fun `exact advertised name is a bounded fallback`() { assertTrue(TagMatcher.matches(emptyList(), null, "BagTag-01", "TAG001", "BagTag-01")); assertFalse(TagMatcher.matches(emptyList(), null, "Other tag", "TAG001", "BagTag-01")) }
    @Test fun `cooldown blocks repeated notification`() { val throttle = AlertThrottle(1_000); assertTrue(throttle.canAlert(100)); throttle.markAlert(100); assertFalse(throttle.canAlert(1_099)); assertTrue(throttle.canAlert(1_100)) }
    @Test fun `zones need a valid name coordinate and radius`() { assertNotNull(ZoneValidator.validate("", 0.0, 0.0, 50f)); assertNotNull(ZoneValidator.validate("Home", 91.0, 0.0, 50f)); assertNotNull(ZoneValidator.validate("Home", 0.0, 0.0, 20f)); assertNull(ZoneValidator.validate("Home", 12.0, 77.0, 50f)) }
    @Test fun `safe-zone classifier uses the saved zone radius`() { val home = SavedZone("home", "Home", 12.9716, 77.5946, 100f); assertEquals(home, SafeZoneClassifier.findSafeZone(LastKnownLocation(12.9717, 77.5946, 10f, 0), listOf(home))); assertNull(SafeZoneClassifier.findSafeZone(LastKnownLocation(12.9816, 77.5946, 10f, 0), listOf(home))) }
}
