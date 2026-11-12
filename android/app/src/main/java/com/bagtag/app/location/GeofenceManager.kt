package com.bagtag.app.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.bagtag.app.domain.SavedZone
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {
    private val client = LocationServices.getGeofencingClient(context)
    private val pendingIntent = PendingIntent.getBroadcast(context, 1, Intent(context, GeofenceReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    @SuppressLint("MissingPermission") fun register(zones: List<SavedZone>, onFailure: (String) -> Unit = {}) {
        val fences = zones.filter { it.entryEnabled || it.exitEnabled }.map { zone -> Geofence.Builder().setRequestId(zone.id)
            .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters).setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes((if (zone.entryEnabled) Geofence.GEOFENCE_TRANSITION_ENTER else 0) or (if (zone.exitEnabled) Geofence.GEOFENCE_TRANSITION_EXIT else 0)).build() }
        client.removeGeofences(pendingIntent).continueWithTask { client.addGeofences(GeofencingRequest.Builder().setInitialTrigger(0).addGeofences(fences).build(), pendingIntent) }
            .addOnFailureListener { onFailure(it.message ?: "Could not register zones") }
    }
    fun unregister() { client.removeGeofences(pendingIntent) }
}
