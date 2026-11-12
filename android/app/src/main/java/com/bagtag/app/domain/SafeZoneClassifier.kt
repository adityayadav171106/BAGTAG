package com.bagtag.app.domain

import kotlin.math.*

object SafeZoneClassifier {
    fun findSafeZone(location: LastKnownLocation, zones: List<SavedZone>): SavedZone? = zones.firstOrNull { zone ->
        distanceMeters(location.latitude, location.longitude, zone.latitude, zone.longitude) <= zone.radiusMeters
    }

    fun distanceMeters(aLatitude: Double, aLongitude: Double, bLatitude: Double, bLongitude: Double): Float {
        val earthRadius = 6_371_000.0
        val latitudeDelta = Math.toRadians(bLatitude - aLatitude)
        val longitudeDelta = Math.toRadians(bLongitude - aLongitude)
        val h = sin(latitudeDelta / 2).pow(2) + cos(Math.toRadians(aLatitude)) * cos(Math.toRadians(bLatitude)) * sin(longitudeDelta / 2).pow(2)
        return (2 * earthRadius * asin(sqrt(h))).toFloat()
    }
}
