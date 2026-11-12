package com.bagtag.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bagtag.app.alerts.AlertManager
import com.bagtag.app.data.TagRepository
import com.bagtag.app.domain.*
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val type = when (event.geofenceTransition) { 1 -> EventType.ZONE_ENTERED; 2 -> EventType.ZONE_EXITED; else -> return }
        val zoneId = event.triggeringGeofences?.firstOrNull()?.requestId ?: return
        val zone = TagRepository(context).loadZones().firstOrNull { it.id == zoneId } ?: return
        val message = "${if (type == EventType.ZONE_ENTERED) "Entered" else "Left"} ${zone.name}"
        TagRepository(context).addEvent(AppEvent(type, message, System.currentTimeMillis()))
        AlertManager(context).sendZone(message)
    }
}
