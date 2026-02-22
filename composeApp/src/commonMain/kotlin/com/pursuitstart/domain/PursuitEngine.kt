package com.pursuitstart.domain

import com.pursuitstart.domain.DistanceMath
import kotlin.math.floor
import kotlin.math.max

class PursuitEngine {

    private var lastSecondEmitted: Long? = null
    private var beep60Fired = false
    private var beep30Fired = false
    private var finishedFired = false

    var state = PursuitUiState()
        private set

    fun setStartTimeMillis(startTimeMillis: Long) {
        state = state.copy(
            startTimeMillis = startTimeMillis,
            message = null,
            isFinished = false
        )
    }

    fun dropPin(fix: GpsFix) {
        state = state.copy(
            pin = PinLocation(fix.latitude, fix.longitude)
        )
    }

    fun tick(nowMillis: Long, gpsFix: GpsFix?): Pair<PursuitUiState, List<PursuitEvent>> {

        var events = mutableListOf<PursuitEvent>()

        val pin = state.pin
        val startTime = state.startTimeMillis

        var distance: Double? = null
        var timeToPin: Double? = null
        var guidance: Double? = null
        var deltaToStart: Long? = null

        if (pin != null && gpsFix != null) {
            distance = DistanceMath.distanceMeters(
                gpsFix.latitude,
                gpsFix.longitude,
                pin.latitude,
                pin.longitude
            )

            val speed = gpsFix.speedMps ?: 0.0
            if (speed > 0.3) {
                timeToPin = distance / speed
            }
        }

        if (startTime != null) {
            deltaToStart = (startTime - nowMillis) / 1000

            if (timeToPin != null) {
                guidance = deltaToStart - timeToPin
            }

            // Alert logic
            if (deltaToStart <= 60 && !beep60Fired && deltaToStart > 30) {
                events += PursuitEvent.BEEP_60
                beep60Fired = true
            }

            if (deltaToStart <= 30 && !beep30Fired && deltaToStart > 10) {
                events += PursuitEvent.BEEP_30
                beep30Fired = true
            }

            if (deltaToStart in 1..10) {
                val currentSecond = floor(deltaToStart.toDouble()).toLong()
                if (lastSecondEmitted != currentSecond) {
                    events += PursuitEvent.COUNTDOWN_BEEP
                    lastSecondEmitted = currentSecond
                }
            }

            if (deltaToStart == 0L) {
                events += PursuitEvent.GO
            }

            if (deltaToStart <= -20 && !finishedFired) {
                events += PursuitEvent.FINISH
                finishedFired = true
            }
        }

        state = state.copy(
            nowMillis = nowMillis,
            distanceMeters = distance,
            speedMps = gpsFix?.speedMps,
            timeToPinSeconds = timeToPin,
            deltaToStartSeconds = deltaToStart,
            guidanceSeconds = guidance
        )

        return state to events
    }
}