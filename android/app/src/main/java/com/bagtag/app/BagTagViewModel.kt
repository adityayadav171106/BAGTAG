package com.bagtag.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.bagtag.app.alerts.AlertManager
import com.bagtag.app.data.TagRepository
import com.bagtag.app.domain.*
import com.bagtag.app.location.GeofenceManager
import com.bagtag.app.monitoring.MonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BagTagUiState(
    val monitoring: Boolean = false,
    val snapshot: TagSnapshot = TagSnapshot(),
    val events: List<AppEvent> = emptyList(),
    val zones: List<SavedZone> = emptyList(),
    val config: TagConfig = TagConfig(),
)

/** UI state only. Background monitoring is owned by MonitoringService. */
class BagTagViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repository = TagRepository(app)
    private val geofences = GeofenceManager(app)
    private val mutableState = MutableStateFlow(BagTagUiState(events = repository.loadEvents(), zones = repository.loadZones(), config = repository.loadConfig()))
    val state = mutableState.asStateFlow()
    private val updates = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val active = intent.getBooleanExtra(MonitoringService.EXTRA_ACTIVE, false)
            val status = intent.getStringExtra(MonitoringService.EXTRA_STATUS)?.let { runCatching { TagStatus.valueOf(it) }.getOrNull() } ?: mutableState.value.snapshot.status
            val rssi = intent.getIntExtra(MonitoringService.EXTRA_RSSI, Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE }
            val lastSeen = intent.getLongExtra(MonitoringService.EXTRA_LAST_SEEN, 0L).takeUnless { it == 0L }
            val locationAvailable = intent.getBooleanExtra(MonitoringService.EXTRA_LOCATION_AVAILABLE, false)
            val message = intent.getStringExtra(MonitoringService.EXTRA_MESSAGE) ?: mutableState.value.snapshot.message
            val zone = intent.getStringExtra(MonitoringService.EXTRA_ZONE)
            mutableState.value = mutableState.value.copy(monitoring = active, snapshot = TagSnapshot(status, rssi, rssi, lastSeen, locationAvailable = locationAvailable, lastTagZoneName = zone, lastTagLocationKnown = zone != null, message = message), events = repository.loadEvents())
        }
    }

    init { ContextCompat.registerReceiver(app, updates, IntentFilter(MonitoringService.ACTION_UPDATE), ContextCompat.RECEIVER_NOT_EXPORTED) }
    fun startMonitoring() = MonitoringService.start(app)
    fun stopMonitoring() = MonitoringService.stop(app)
    fun addZone(zone: SavedZone): String? { ZoneValidator.validate(zone.name, zone.latitude, zone.longitude, zone.radiusMeters)?.let { return it }; val updated = state.value.zones.filterNot { it.id == zone.id } + zone; repository.saveZones(updated); geofences.register(updated); mutableState.value = state.value.copy(zones = updated); return null }
    fun deleteZone(id: String) { val updated = state.value.zones.filterNot { it.id == id }; repository.saveZones(updated); if (updated.isEmpty()) geofences.unregister() else geofences.register(updated); mutableState.value = state.value.copy(zones = updated) }
    fun clearZones() { repository.clearZones(); geofences.unregister(); mutableState.value = state.value.copy(zones = emptyList()) }
    fun updateConfig(updated: TagConfig) { repository.saveConfig(updated); val wasMonitoring = state.value.monitoring; mutableState.value = state.value.copy(config = updated); if (wasMonitoring) MonitoringService.reload(app) }
    fun clearEvents() { repository.clearEvents(); mutableState.value = state.value.copy(events = emptyList()) }
    fun manualNotificationTest() { AlertManager(app).sendManualTest(); repository.addEvent(AppEvent(EventType.MANUAL_TEST, "Manual notification test sent", System.currentTimeMillis())); mutableState.value = state.value.copy(events = repository.loadEvents()) }
    override fun onCleared() { app.unregisterReceiver(updates); super.onCleared() }
}
