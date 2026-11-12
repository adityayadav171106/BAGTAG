package com.bagtag.app.monitoring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bagtag.app.alerts.AlertManager
import com.bagtag.app.ble.BleScanner
import com.bagtag.app.ble.ScanState
import com.bagtag.app.data.TagRepository
import com.bagtag.app.domain.*
import com.bagtag.app.location.LocationRepository
import kotlinx.coroutines.*

/** Owns BLE monitoring while the user has explicitly enabled it. */
class MonitoringService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: TagRepository
    private lateinit var locations: LocationRepository
    private lateinit var monitor: ProximityMonitor
    private lateinit var config: TagConfig
    private lateinit var throttle: AlertThrottle
    private var scanner: BleScanner? = null
    private var tickJob: Job? = null
    private var locationAvailable = false
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> scanner?.start()
                BluetoothAdapter.STATE_OFF -> scanner?.stop()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = when (intent?.action) {
        ACTION_STOP -> { stopMonitoring(); START_NOT_STICKY }
        ACTION_RELOAD -> { reloadMonitoring(); START_NOT_STICKY }
        else -> { startMonitoring(); START_NOT_STICKY }
    }

    private fun reloadMonitoring() {
        scanner?.stop()
        config = TagRepository(this).loadConfig(); monitor = ProximityMonitor(config); throttle = AlertThrottle(config.cooldownMs); monitor.start()
        locationAvailable = false
        scanner = BleScanner(this, config.id, config.displayName, ::onTagDetected, ::onScanState).also { it.start() }
        publish(monitor.snapshot(locationAvailable), true)
    }

    private fun startMonitoring() {
        repository = TagRepository(this); locations = LocationRepository(this); config = repository.loadConfig()
        monitor = ProximityMonitor(config); throttle = AlertThrottle(config.cooldownMs); monitor.start()
        locationAvailable = false
        createChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NOTIFICATION_ID, notification("Monitoring active"), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(NOTIFICATION_ID, notification("Monitoring active"))
        scanner = BleScanner(this, config.id, config.displayName, ::onTagDetected, ::onScanState).also { it.start() }
        ContextCompat.registerReceiver(this, bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
        tickJob?.cancel(); tickJob = scope.launch { while (isActive) { delay(10_000); locations.captureIfAccurate { current -> evaluate(current) } } }
        publish(monitor.snapshot(locationAvailable), true)
    }

    private fun onTagDetected(rssi: Int, detectedAtMs: Long) {
        monitor.onDetection(rssi, detectedAtMs, null).forEach(::handle)
        publish(monitor.snapshot(locationAvailable), true)
        locations.captureIfAccurate { location ->
            locationAvailable = location != null
            location?.let { current ->
                val safeZone = SafeZoneClassifier.findSafeZone(current, repository.loadZones())
                LastTagObservation(detectedAtMs, current.latitude, current.longitude, current.accuracyMeters, safeZone?.id, safeZone?.name)
            }?.let { observation ->
                repository.saveLastTagObservation(observation)
                monitor.recordLocation(observation)
            }
            publish(monitor.snapshot(locationAvailable), true)
        }
    }

    private fun evaluate(current: LastKnownLocation?) {
        locationAvailable = current != null
        val observation = repository.loadLastTagObservation()
        val distance = if (current != null && observation != null) SafeZoneClassifier.distanceMeters(current.latitude, current.longitude, observation.latitude, observation.longitude) else null
        val now = System.currentTimeMillis()
        val locationDeadlineExceeded = monitor.snapshot(current != null).lastSeenAtMs?.let {
            now - it >= config.missingTimeoutMs + config.alertGracePeriodMs + LOCATION_DEADLINE_MS
        } == true
        monitor.evaluate(now, distance, throttle.canAlert(now), locationDeadlineExceeded).forEach(::handle)
        publish(monitor.snapshot(locationAvailable), true)
    }

    private fun handle(decision: ProximityDecision) = when (decision) {
        is ProximityDecision.SendForgottenAlert -> {
            throttle.markAlert(System.currentTimeMillis())
            AlertManager(this).sendForgotten(config.displayName)
            repository.addEvent(AppEvent(EventType.TAG_MISSING, "You may have left ${config.displayName} behind", System.currentTimeMillis()))
        }
        is ProximityDecision.SendReturnAlert -> {
            AlertManager(this).sendReturned(config.displayName)
            repository.addEvent(AppEvent(EventType.TAG_RETURNED, "${config.displayName} found again", System.currentTimeMillis()))
        }
        else -> Unit
    }

    private fun onScanState(state: ScanState) {
        if (state is ScanState.BluetoothDisabled || state is ScanState.Error) publish(monitor.snapshot(locationAvailable), true, state.toString())
    }
    private fun publish(snapshot: TagSnapshot, active: Boolean, problem: String? = null) = sendBroadcast(Intent(ACTION_UPDATE).setPackage(packageName).putExtra(EXTRA_ACTIVE, active).putExtra(EXTRA_STATUS, snapshot.status.name).putExtra(EXTRA_RSSI, snapshot.latestRssi ?: Int.MIN_VALUE).putExtra(EXTRA_LAST_SEEN, snapshot.lastSeenAtMs ?: 0L).putExtra(EXTRA_LOCATION_AVAILABLE, snapshot.locationAvailable).putExtra(EXTRA_MESSAGE, problem ?: snapshot.message).putExtra(EXTRA_ZONE, snapshot.lastTagZoneName))
    private fun stopMonitoring() { tickJob?.cancel(); scanner?.stop(); runCatching { unregisterReceiver(bluetoothReceiver) }; sendBroadcast(Intent(ACTION_UPDATE).setPackage(packageName).putExtra(EXTRA_ACTIVE, false)); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { tickJob?.cancel(); scanner?.stop(); runCatching { unregisterReceiver(bluetoothReceiver) }; scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createChannel() { getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "BagTag monitoring", NotificationManager.IMPORTANCE_LOW)) }
    private fun notification(text: String) = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("BagTag").setContentText(text).setOngoing(true).build()
    companion object {
        const val ACTION_START = "com.bagtag.app.monitoring.START"; const val ACTION_STOP = "com.bagtag.app.monitoring.STOP"; const val ACTION_RELOAD = "com.bagtag.app.monitoring.RELOAD"; const val ACTION_UPDATE = "com.bagtag.app.monitoring.UPDATE"
        const val EXTRA_ACTIVE = "active"; const val EXTRA_STATUS = "status"; const val EXTRA_RSSI = "rssi"; const val EXTRA_LAST_SEEN = "lastSeen"; const val EXTRA_LOCATION_AVAILABLE = "locationAvailable"; const val EXTRA_MESSAGE = "message"; const val EXTRA_ZONE = "zone"
        private const val CHANNEL = "bagtag_monitoring"; private const val NOTIFICATION_ID = 1001; private const val LOCATION_DEADLINE_MS = 20_000L
        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, MonitoringService::class.java).setAction(ACTION_START))
        fun stop(context: Context) = context.startService(Intent(context, MonitoringService::class.java).setAction(ACTION_STOP))
        fun reload(context: Context) = context.startService(Intent(context, MonitoringService::class.java).setAction(ACTION_RELOAD))
    }
}
