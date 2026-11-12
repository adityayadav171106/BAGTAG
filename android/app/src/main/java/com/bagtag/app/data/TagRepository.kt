package com.bagtag.app.data

import android.content.Context
import com.bagtag.app.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Small local-only store. It intentionally stores no location history, only the most recent valid location. */
class TagRepository(context: Context) {
    private val prefs = context.getSharedPreferences("bagtag", Context.MODE_PRIVATE)
    fun loadConfig() = TagConfig(
        id = prefs.getString("tagId", "TAG001")!!,
        displayName = prefs.getString("tagName", "BagTag-01")!!,
        missingTimeoutMs = prefs.getLong("timeout", 60_000),
        alertGracePeriodMs = prefs.getLong("grace", 30_000),
        movementThresholdMeters = prefs.getFloat("movement", 75f),
        cooldownMs = prefs.getLong("cooldown", 300_000),
        forgottenAlertsEnabled = prefs.getBoolean("forgotten", true),
        returnAlertsEnabled = prefs.getBoolean("return", false),
    )
    fun saveConfig(value: TagConfig) = prefs.edit().putString("tagId", value.id).putString("tagName", value.displayName)
        .putLong("timeout", value.missingTimeoutMs).putLong("grace", value.alertGracePeriodMs).putFloat("movement", value.movementThresholdMeters).putLong("cooldown", value.cooldownMs)
        .putBoolean("forgotten", value.forgottenAlertsEnabled).putBoolean("return", value.returnAlertsEnabled).apply()

    fun saveLastLocation(value: LastKnownLocation) = prefs.edit().putString("lastLocation", JSONObject().apply {
        put("lat", value.latitude); put("lon", value.longitude); put("accuracy", value.accuracyMeters); put("at", value.recordedAtMs)
    }.toString()).apply()
    fun loadLastLocation(): LastKnownLocation? = prefs.getString("lastLocation", null)?.let { raw -> runCatching {
        JSONObject(raw).let { LastKnownLocation(it.getDouble("lat"), it.getDouble("lon"), it.getDouble("accuracy").toFloat(), it.getLong("at")) }
    }.getOrNull() }

    fun saveLastTagObservation(value: LastTagObservation?) {
        val edit = prefs.edit()
        if (value == null) edit.remove("lastTagObservation") else edit.putString("lastTagObservation", JSONObject().apply {
            put("at", value.detectedAtMs); put("lat", value.latitude); put("lon", value.longitude); put("accuracy", value.accuracyMeters); put("zoneId", value.safeZoneId ?: JSONObject.NULL); put("zoneName", value.safeZoneName ?: JSONObject.NULL)
        }.toString())
        edit.apply()
    }
    fun loadLastTagObservation(): LastTagObservation? = prefs.getString("lastTagObservation", null)?.let { raw -> runCatching {
        JSONObject(raw).let { LastTagObservation(it.getLong("at"), it.getDouble("lat"), it.getDouble("lon"), it.getDouble("accuracy").toFloat(), it.optString("zoneId").takeIf { value -> value.isNotBlank() && value != "null" }, it.optString("zoneName").takeIf { value -> value.isNotBlank() && value != "null" }) }
    }.getOrNull() }

    fun loadZones(): List<SavedZone> = runCatching { JSONArray(prefs.getString("zones", "[]")) }.getOrElse { JSONArray() }.let { array ->
        (0 until array.length()).map { index -> array.getJSONObject(index).let { SavedZone(it.getString("id"), it.getString("name"), it.getDouble("lat"), it.getDouble("lon"), it.getDouble("radius").toFloat(), it.getBoolean("entry"), it.getBoolean("exit"), it.optBoolean("combined")) } }
    }
    fun saveZones(zones: List<SavedZone>) { prefs.edit().putString("zones", JSONArray().apply { zones.forEach { zone -> put(JSONObject().apply {
        put("id", zone.id); put("name", zone.name); put("lat", zone.latitude); put("lon", zone.longitude); put("radius", zone.radiusMeters); put("entry", zone.entryEnabled); put("exit", zone.exitEnabled); put("combined", zone.combinedExitRuleEnabled)
    }) } }.toString()).apply() }

    fun loadEvents(): List<AppEvent> = runCatching { JSONArray(prefs.getString("events", "[]")) }.getOrElse { JSONArray() }.let { array ->
        (0 until array.length()).map { i -> array.getJSONObject(i).let { AppEvent(EventType.valueOf(it.getString("type")), it.getString("message"), it.getLong("at")) } }.sortedByDescending { it.atMs }
    }
    fun addEvent(event: AppEvent) { val events = loadEvents().take(49).toMutableList(); events.add(0, event); prefs.edit().putString("events", JSONArray().apply { events.forEach { put(JSONObject().apply { put("type", it.type.name); put("message", it.message); put("at", it.atMs) }) } }.toString()).apply() }
    fun clearEvents() = prefs.edit().remove("events").apply()
    fun clearZones() = prefs.edit().remove("zones").apply()
}
