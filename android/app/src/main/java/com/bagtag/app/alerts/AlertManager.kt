package com.bagtag.app.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.bagtag.app.R

class AlertManager(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init { manager.createNotificationChannel(NotificationChannel(CHANNEL, "BagTag alerts", NotificationManager.IMPORTANCE_DEFAULT)) }
    fun sendForgotten(name: String) = send("Possible forgotten item", "$name is not nearby. Check that you have it.")
    fun sendReturned(name: String) = send("Tag returned", "$name is nearby again.")
    fun sendZone(message: String) = send("Zone update", message)
    fun sendManualTest() = send("BagTag test", "Notifications are working on this phone.")
    private fun send(title: String, text: String) { manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), NotificationCompat.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle(title).setContentText(text).setAutoCancel(true).build()) }
    companion object { private const val CHANNEL = "bagtag_alerts" }
}
