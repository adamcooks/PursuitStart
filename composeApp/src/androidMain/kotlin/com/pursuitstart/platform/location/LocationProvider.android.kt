package com.pursuitstart.platform.location

import android.annotation.SuppressLint
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.*
import com.pursuitstart.domain.GpsFix
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private class AndroidFusedLocationProvider(
    private val client: FusedLocationProviderClient
) : LocationProvider {

    // Latest-only stream (race-day friendly)
    private val _gpsFix = MutableSharedFlow<GpsFix?>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val gpsFix: Flow<GpsFix?> = _gpsFix.asSharedFlow()

    private var started = false
    private var callback: LocationCallback? = null

    // Tuneables (SF Bay / open water)
    private val goodAccuracyM = 6f          // “locked”
    private val acceptableAccuracyM = 12f   // show updates, but don’t let user drop pin
    private var goodStreak = 0              // count consecutive good fixes

    private val request: LocationRequest =
        LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            500L // 2Hz
        )
            .setMinUpdateIntervalMillis(250L)
            .setMaxUpdateDelayMillis(500L)
            .setWaitForAccurateLocation(true)
            .build()

    init {
        _gpsFix.tryEmit(null)
    }

    @SuppressLint("MissingPermission") // permission is handled by LocationPermissionGate
    override fun start() {
        if (started) return
        started = true

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                // Reject truly bogus / uninitialized
                if (loc.elapsedRealtimeNanos == 0L) return

                val acc = if (loc.hasAccuracy()) loc.accuracy else null
                val provider = loc.provider

                // Track "good" streak (for UI gating)
                goodStreak = if (acc != null && acc <= goodAccuracyM) goodStreak + 1 else 0

                // Optionally: if accuracy is *really* bad, ignore it
                // (keeps stream from flipping around due to coarse/network)
                if (acc != null && acc > acceptableAccuracyM) {
                    // Still emit, but you can choose to not emit if you prefer.
                    // Emitting lets UI show "Acquiring... 25m" instead of looking dead.
                }

                val speed = if (loc.hasSpeed() && loc.speed >= 0f) loc.speed.toDouble() else null

                _gpsFix.tryEmit(
                    GpsFix(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedMps = speed,
                        accuracyMeters = acc,
                        provider = provider
                    )
                )
            }
        }

        callback = cb
        client.requestLocationUpdates(request, cb, Looper.getMainLooper())
    }

    override fun stop() {
        if (!started) return
        started = false
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
        goodStreak = 0
    }
}

@Composable
actual fun rememberLocationProvider(): LocationProvider {
    val context = LocalContext.current
    val client = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    return remember(client) { AndroidFusedLocationProvider(client) }
}