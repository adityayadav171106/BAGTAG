package com.bagtag.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bagtag.app.domain.SavedZone
import com.bagtag.app.domain.TagStatus
import com.bagtag.app.ui.theme.BagTagTheme
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val viewModel: BagTagViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { App(viewModel) } }
}

private enum class Page(val label: String) { HOME("STATUS"), ZONES("ZONES"), SETTINGS("SETTINGS") }

@Composable private fun App(viewModel: BagTagViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var warning by remember { mutableStateOf<String?>(null) }
    val permissions = buildList { add(Manifest.permission.BLUETOOTH_SCAN); add(Manifest.permission.BLUETOOTH_CONNECT); add(Manifest.permission.ACCESS_FINE_LOCATION); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray()
    val requestPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.BLUETOOTH_SCAN] == true) viewModel.startMonitoring() else warning = "BLUETOOTH PERMISSION IS REQUIRED"
    }
    BagTagTheme {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) { listOf(Page.HOME to Icons.Outlined.Home, Page.ZONES to Icons.Outlined.LocationOn, Page.SETTINGS to Icons.Outlined.Tune).forEach { (item, icon) -> NavigationBarItem(selected = page == item, onClick = { page = item }, icon = { Icon(icon, null) }, label = { Text(item.label) }) } } }) { padding ->
            when (page) {
                Page.HOME -> Home(Modifier.padding(padding), state, warning, { requestPermissions.launch(permissions) }, viewModel::stopMonitoring, viewModel::manualNotificationTest, viewModel::clearEvents)
                Page.ZONES -> Zones(Modifier.padding(padding), state.zones, viewModel::addZone, viewModel::deleteZone, viewModel::clearZones)
                Page.SETTINGS -> Settings(Modifier.padding(padding), state.config, viewModel::updateConfig)
            }
        }
    }
}

@Composable private fun Home(modifier: Modifier, state: BagTagUiState, warning: String?, start: () -> Unit, stop: () -> Unit, test: () -> Unit, clear: () -> Unit) {
    val s = state.snapshot
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("BAGTAG", style = MaterialTheme.typography.headlineLarge); Text("KEEP WHAT MATTERS CLOSE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }; LiveChip(state.monitoring) } }
        item { Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(CircleShape).background(s.status.tint())) { Icon(Icons.Outlined.BluetoothSearching, null, Modifier.padding(11.dp), MaterialTheme.colorScheme.background) }; Spacer(Modifier.width(12.dp)); Column { Text("BAGTAG-01", style = MaterialTheme.typography.titleLarge); Text(s.status.label(), style = MaterialTheme.typography.labelLarge, color = s.status.tint()) } }; Text(s.status.hero(), style = MaterialTheme.typography.headlineMedium); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Stat(Modifier.weight(1f), "SIGNAL", s.latestRssi?.let { "$it dBm" } ?: "—"); Stat(Modifier.weight(1f), "LAST SEEN", s.lastSeenAtMs?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) } ?: "—") }; s.filteredRssi?.let { Text("FILTERED SIGNAL  $it dBm", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        warning?.let { item { Surface(color = MaterialTheme.colorScheme.error.copy(alpha = .14f), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(10.dp)); Text(it, color = MaterialTheme.colorScheme.error) } } } }
        item { Button(onClick = if (state.monitoring) stop else start, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = if (state.monitoring) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary, contentColor = if (state.monitoring) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary)) { Icon(if (state.monitoring) Icons.Outlined.StopCircle else Icons.Outlined.PlayCircle, null); Spacer(Modifier.width(10.dp)); Text(if (state.monitoring) "STOP MONITORING" else "START MONITORING") } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = test, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Outlined.Notifications, null); Spacer(Modifier.width(8.dp)); Text("TEST ALERT") }; Stat(Modifier.weight(1f), "LOCATION", if (s.locationAvailable) "READY" else "OFF") } }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("RECENT ACTIVITY", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); if (state.events.isNotEmpty()) TextButton(onClick = clear) { Text("CLEAR") } } }
        if (state.events.isEmpty()) item { Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Timeline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(12.dp)); Text("NO EVENTS YET", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } else items(state.events.take(5)) { Event(it.message, it.atMs) }
        item { Text("RSSI IS SIGNAL STRENGTH, NOT EXACT DISTANCE. ANDROID MAY DELAY SCANNING WHILE IDLE.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
    }
}

@Composable private fun Stat(modifier: Modifier, label: String, value: String) = Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun LiveChip(active: Boolean) = Row(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)); Spacer(Modifier.width(7.dp)); Text(if (active) "LIVE" else "PAUSED", style = MaterialTheme.typography.labelLarge) }
@Composable private fun Event(message: String, at: Long) = Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Text(message, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis); Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(at)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge) }

@Composable private fun Zones(modifier: Modifier, zones: List<SavedZone>, save: (SavedZone) -> String?, delete: (String) -> Unit, clear: () -> Unit) {
    var id by remember { mutableStateOf<String?>(null) }; var name by remember { mutableStateOf("") }; var lat by remember { mutableStateOf("") }; var lon by remember { mutableStateOf("") }; var radius by remember { mutableStateOf("100") }; var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("ZONES", style = MaterialTheme.typography.headlineLarge); Text("PLACE-BASED REMINDERS", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (id == null) "ADD A ZONE" else "EDIT ZONE", style = MaterialTheme.typography.titleLarge); Field(name, { name = it }, "NAME"); Field(lat, { lat = it }, "LATITUDE"); Field(lon, { lon = it }, "LONGITUDE"); Field(radius, { radius = it }, "RADIUS · METRES"); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Button(onClick = { val z = SavedZone(id ?: System.currentTimeMillis().toString(), name.trim(), lat.toDoubleOrNull() ?: Double.NaN, lon.toDoubleOrNull() ?: Double.NaN, radius.toFloatOrNull() ?: Float.NaN); error = save(z); if (error == null) { id = null; name = ""; lat = ""; lon = ""; radius = "100" } }, Modifier.fillMaxWidth()) { Text(if (id == null) "SAVE ZONE" else "UPDATE ZONE") } } } }; items(zones, key = { it.id }) { z -> Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(z.name, style = MaterialTheme.typography.titleMedium); Text("${z.radiusMeters.toInt()} M RADIUS", color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onClick = { id = z.id; name = z.name; lat = z.latitude.toString(); lon = z.longitude.toString(); radius = z.radiusMeters.toString() }) { Icon(Icons.Outlined.Edit, "EDIT") }; IconButton(onClick = { delete(z.id) }) { Icon(Icons.Outlined.DeleteOutline, "DELETE") } } } }; if (zones.isNotEmpty()) item { TextButton(onClick = clear) { Text("CLEAR ALL ZONES") } } }
}

@Composable private fun Field(value: String, change: (String) -> Unit, label: String) = OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(14.dp))
@Composable private fun Settings(modifier: Modifier, config: com.bagtag.app.domain.TagConfig, save: (com.bagtag.app.domain.TagConfig) -> Unit) {
    var forgotten by remember(config) { mutableStateOf(config.forgottenAlertsEnabled) }; var returned by remember(config) { mutableStateOf(config.returnAlertsEnabled) }
    var timeoutSeconds by remember(config) { mutableStateOf((config.missingTimeoutMs / 1_000).toString()) }; var graceSeconds by remember(config) { mutableStateOf((config.alertGracePeriodMs / 1_000).toString()) }; var movementMeters by remember(config) { mutableStateOf(config.movementThresholdMeters.toInt().toString()) }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("SETTINGS", style = MaterialTheme.typography.headlineLarge); Text("CONTROL YOUR REMINDERS", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Toggle("FORGOTTEN-ITEM ALERTS", forgotten) { forgotten = it }; Toggle("TAG-RETURN ALERTS", returned) { returned = it }; HorizontalDivider(); Field(timeoutSeconds, { timeoutSeconds = it }, "MISSING TIMEOUT · SECONDS"); Field(graceSeconds, { graceSeconds = it }, "ALERT GRACE · SECONDS"); Field(movementMeters, { movementMeters = it }, "MOVE AWAY · METRES"); Button(onClick = { save(config.copy(forgottenAlertsEnabled = forgotten, returnAlertsEnabled = returned, missingTimeoutMs = (timeoutSeconds.toLongOrNull()?.coerceIn(15, 600) ?: 60) * 1_000, alertGracePeriodMs = (graceSeconds.toLongOrNull()?.coerceIn(0, 300) ?: 30) * 1_000, movementThresholdMeters = (movementMeters.toFloatOrNull()?.coerceIn(25f, 1_000f) ?: 75f))) }, Modifier.fillMaxWidth()) { Text("SAVE SETTINGS") } } } }
        item { Text("ALERTS REQUIRE: UNSAVED LAST-TAG LOCATION + TIMEOUT + GRACE + MOVING AWAY.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable private fun Toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, change) }
private fun TagStatus.label() = when (this) { TagStatus.NOT_MONITORED -> "PAUSED"; TagStatus.WAITING_FOR_TAG -> "WAITING"; TagStatus.WITH_TAG -> "WITH TAG"; TagStatus.SIGNAL_TEMPORARILY_LOST -> "SIGNAL LOST"; TagStatus.SAFE_ZONE_LEFT_BEHIND -> "SAFE PLACE"; TagStatus.ALERT_SENT -> "ALERT SENT" }
private fun TagStatus.hero() = when (this) { TagStatus.NOT_MONITORED -> "READY WHEN YOU ARE"; TagStatus.WAITING_FOR_TAG -> "WAITING FOR YOUR TAG"; TagStatus.WITH_TAG -> "YOUR TAG IS CLOSE"; TagStatus.SIGNAL_TEMPORARILY_LOST -> "CHECKING YOUR LOCATION"; TagStatus.SAFE_ZONE_LEFT_BEHIND -> "TAG LEFT AT A SAFE PLACE"; TagStatus.ALERT_SENT -> "YOU MAY HAVE LEFT IT BEHIND" }
@Composable private fun TagStatus.tint() = when (this) { TagStatus.WITH_TAG -> MaterialTheme.colorScheme.primary; TagStatus.ALERT_SENT -> MaterialTheme.colorScheme.error; TagStatus.SAFE_ZONE_LEFT_BEHIND -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurfaceVariant }
