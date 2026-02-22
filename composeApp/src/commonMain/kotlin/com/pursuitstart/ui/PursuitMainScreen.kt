package com.pursuitstart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pursuitstart.domain.GpsFix
import com.pursuitstart.domain.PursuitEngine
import com.pursuitstart.domain.PursuitEvent
import com.pursuitstart.platform.location.rememberLocationProvider
import com.pursuitstart.platform.permissions.LocationPermissionGate
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun PursuitMainScreen(
    startTimeMillis: Long,
    onBack: () -> Unit
) {
    val engine = remember { PursuitEngine() }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(startTimeMillis) {
        engine.setStartTimeMillis(startTimeMillis)
    }

    LocationPermissionGate { hasPermission ->
        val locationProvider = rememberLocationProvider()

        val gpsFix by produceState<GpsFix?>(initialValue = null, key1 = locationProvider) {
            locationProvider.gpsFix.collect { newFix -> value = newFix }
        }

        // Start/stop location updates with screen + permission
        DisposableEffect(hasPermission) {
            if (hasPermission) locationProvider.start() else locationProvider.stop()
            onDispose { locationProvider.stop() }
        }

        // Tick loop (time-based; no drift)
        LaunchedEffect(hasPermission) {
            // If permission flips off, we keep ticking with gpsFix=null (engine should handle)
            while (!done) {
                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                val (_, events) = engine.tick(nowMillis = now, gpsFix = gpsFix)

                for (e in events) {
                    when (e) {
                        PursuitEvent.BEEP_60,
                        PursuitEvent.BEEP_30,
                        PursuitEvent.COUNTDOWN_BEEP,
                        PursuitEvent.GO -> {
                            // TODO: platform beep + vibration
                        }
                        PursuitEvent.FINISH -> {
                            done = true
                            // TODO: Android finishAffinity; iOS show Done (cannot force quit)
                        }
                    }
                }

                delay(200)
            }
        }

        val state = engine.state

        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Pursuit Start", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(48.dp))
            }

            ScheduledLabel(startTimeMillis = startTimeMillis)

            if (!hasPermission) {
                Text(
                    text = "Location permission required for GPS features.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    gpsFix?.let { fix ->
                        engine.dropPin(fix)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = gpsFix != null
            ) {
                Text(if (gpsFix == null) "Waiting for GPS..." else "Drop Pin")
            }

            Spacer(Modifier.height(4.dp))

            StatLine("Time to start", state.deltaToStartSeconds?.let { formatSignedSeconds(it) } ?: "—")
            StatLine("Distance to pin", state.distanceMeters?.let { "${it.toInt()} m" } ?: "—")
            StatLine("Speed", state.speedMps?.let { "${(it * 10).toInt() / 10.0} m/s" } ?: "—")
            StatLine("Time-to-pin", state.timeToPinSeconds?.let { formatMmSs(it.toLong()) } ?: "—")
            StatLine("Early/Late", state.guidanceSeconds?.let { formatSignedSeconds(it.toLong()) } ?: "—")

            Spacer(Modifier.height(8.dp))

            if (done) {
                Text("Done.", style = MaterialTheme.typography.headlineSmall)
                Text("Android will close automatically; iOS will stop here.")
            } else {
                Text("Alerts: T-60, T-30, last 10 seconds, GO, then auto-finish at +20s.")
            }
        }
    }
}

@Composable
private fun ScheduledLabel(startTimeMillis: Long) {
    // Keep “now” fresh so the Today/Tomorrow label is always correct
    val nowMillis by androidx.compose.runtime.produceState(initialValue = kotlin.time.Clock.System.now().toEpochMilliseconds()) {
        while (true) {
            value = kotlin.time.Clock.System.now().toEpochMilliseconds()
            delay(500L)
        }
    }

    val dayLabel = if (isTodayVsTomorrow(nowMillis, startTimeMillis) == DayLabel.TODAY) "Today" else "Tomorrow"
    val hms = formatHmsFromMillis(startTimeMillis)

    Text(
        text = "Scheduled: $dayLabel $hms",
        style = MaterialTheme.typography.titleMedium
    )
}

private enum class DayLabel { TODAY, TOMORROW }

private fun isTodayVsTomorrow(nowMillis: Long, startTimeMillis: Long): DayLabel {
    // Your TimeEntry already rolls “past today” to tomorrow.
    // So classification can be simple and reliable:
    // - If startTimeMillis is within the next 24h AND its time-of-day >= now time-of-day => Today
    // - Else => Tomorrow
    val delta = startTimeMillis - nowMillis
    if (delta < 0) return DayLabel.TOMORROW
    if (delta >= 24L * 3600L * 1000L) return DayLabel.TOMORROW

    val dayMs = 24L * 3600L * 1000L
    val nowTod = ((nowMillis % dayMs) + dayMs) % dayMs
    val startTod = ((startTimeMillis % dayMs) + dayMs) % dayMs

    return if (startTod >= nowTod) DayLabel.TODAY else DayLabel.TOMORROW
}

private fun formatHmsFromMillis(millis: Long): String {
    val dayMs = 24L * 3600L * 1000L
    val tod = ((millis % dayMs) + dayMs) % dayMs
    val totalSeconds = tod / 1000L
    val h = totalSeconds / 3600L
    val m = (totalSeconds % 3600L) / 60L
    val s = totalSeconds % 60L
    return two(h) + ":" + two(m) + ":" + two(s)
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value)
    }
    Spacer(Modifier.height(6.dp))
}

private fun formatMmSs(totalSeconds: Long): String {
    val s = if (totalSeconds < 0) 0 else totalSeconds
    val mm = s / 60
    val ss = s % 60
    return two(mm) + ":" + two(ss)
}

private fun formatSignedSeconds(seconds: Long): String {
    val sign = if (seconds >= 0) "+" else "-"
    val a = abs(seconds)
    val mm = a / 60
    val ss = a % 60
    return sign + two(mm) + ":" + two(ss)
}

private fun two(v: Long): String =
    if (v < 10) "0$v" else v.toString()