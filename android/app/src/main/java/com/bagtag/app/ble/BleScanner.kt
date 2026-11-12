package com.bagtag.app.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.bagtag.app.domain.TagMatcher

sealed interface ScanState { data object Idle : ScanState; data object Scanning : ScanState; data object BluetoothDisabled : ScanState; data object PermissionDenied : ScanState; data class Error(val message: String) : ScanState }

class BleScanner(private val context: Context, private val configuredId: String, private val configuredName: String, private val onDetection: (Int, Long) -> Unit, private val onState: (ScanState) -> Unit) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var scanner: BluetoothLeScanner? = null
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord
            val manufacturerPayloads = record?.manufacturerSpecificData?.let { data -> (0 until data.size()).map { data.valueAt(it) } }.orEmpty()
            if (TagMatcher.matches(manufacturerPayloads, record?.bytes, record?.deviceName, configuredId, configuredName)) onDetection(result.rssi, System.currentTimeMillis())
        }
        override fun onScanFailed(errorCode: Int) { onState(ScanState.Error("BLE scan failed ($errorCode)")) }
    }

    @SuppressLint("MissingPermission") fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) { onState(ScanState.PermissionDenied); return }
        if (adapter?.isEnabled != true) { onState(ScanState.BluetoothDisabled); return }
        scanner = adapter.bluetoothLeScanner
        if (scanner == null) { onState(ScanState.Error("BLE scanning is unavailable on this phone")); return }
        // Android stops unfiltered scans when the screen turns off. An exact
        // configured-name filter keeps this companion scan eligible to run.
        val filter = ScanFilter.Builder().setDeviceName(configuredName).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(listOf(filter), settings, callback); onState(ScanState.Scanning)
    }
    @SuppressLint("MissingPermission") fun stop() { scanner?.stopScan(callback); onState(ScanState.Idle) }
}
