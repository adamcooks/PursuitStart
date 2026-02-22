package com.pursuitstart.domain

data class PinLocation(
    val latitude: Double,
    val longitude: Double
)

data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Double? = null,
    val accuracyMeters: Float? = null,
    val provider: String? = null
)

data class PursuitUiState(
    val startTimeMillis: Long? = null,
    val nowMillis: Long = 0L,
    val pin: PinLocation? = null,
    val distanceMeters: Double? = null,
    val speedMps: Double? = null,
    val timeToPinSeconds: Double? = null,
    val deltaToStartSeconds: Long? = null,
    val guidanceSeconds: Double? = null,
    val isFinished: Boolean = false,
    val message: String? = null
)

enum class PursuitEvent {
    BEEP_60,
    BEEP_30,
    COUNTDOWN_BEEP,
    GO,
    FINISH
}