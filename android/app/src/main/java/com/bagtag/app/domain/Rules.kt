package com.bagtag.app.domain

object TagMatcher {
    /**
     * Android removes the two-byte Bluetooth company ID from
     * manufacturerSpecificData. Older BagTag firmware wrote TAG001 without a
     * company ID, so Android exposes only G001 there. Check every parsed
     * payload and the original advertisement bytes before the name fallback.
     */
    fun matches(
        manufacturerPayloads: List<ByteArray>,
        rawAdvertisement: ByteArray?,
        advertisedName: String?,
        configuredId: String,
        configuredName: String,
    ): Boolean = manufacturerPayloads.any { it.containsAscii(configuredId) } ||
        rawAdvertisement.containsAscii(configuredId) ||
        advertisedName == configuredName

    private fun ByteArray?.containsAscii(value: String): Boolean {
        if (this == null) return false
        val needle = value.toByteArray(Charsets.UTF_8)
        return indices.any { start -> start + needle.size <= size && needle.indices.all { offset -> this[start + offset] == needle[offset] } }
    }
}

class AlertThrottle(private val cooldownMs: Long) {
    private var lastAlertAtMs: Long? = null
    fun canAlert(nowMs: Long): Boolean = lastAlertAtMs?.let { nowMs - it >= cooldownMs } ?: true
    fun markAlert(nowMs: Long) { lastAlertAtMs = nowMs }
}

object ZoneValidator {
    fun validate(name: String, latitude: Double, longitude: Double, radiusMeters: Float): String? = when {
        name.isBlank() -> "Zone name is required"
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0 -> "Zone coordinates are invalid"
        radiusMeters !in 25f..10_000f -> "Zone radius must be between 25 m and 10 km"
        else -> null
    }
}
