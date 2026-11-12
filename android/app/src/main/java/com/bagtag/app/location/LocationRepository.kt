package com.bagtag.app.location

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.bagtag.app.domain.LastKnownLocation
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationRepository(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    @SuppressLint("MissingPermission") fun captureIfAccurate(onLocation: (LastKnownLocation?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { onLocation(null); return }
        val cancellation = CancellationTokenSource()
        var completed = false
        fun complete(location: LastKnownLocation?) {
            if (completed) return
            completed = true
            cancellation.cancel()
            onLocation(location)
        }
        Handler(Looper.getMainLooper()).postDelayed({ complete(null) }, LOCATION_TIMEOUT_MS)
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(5_000)
            .setDurationMillis(LOCATION_TIMEOUT_MS)
            .build()
        client.getCurrentLocation(request, cancellation.token)
            .addOnSuccessListener { location ->
                complete(location?.takeIf { it.accuracy <= 100f }?.let {
                    LastKnownLocation(it.latitude, it.longitude, it.accuracy, System.currentTimeMillis())
                })
            }
            .addOnFailureListener { complete(null) }
    }

    private companion object { const val LOCATION_TIMEOUT_MS = 20_000L }
}
