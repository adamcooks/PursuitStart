package com.pursuitstart.platform.location

import androidx.compose.runtime.Composable
import com.pursuitstart.domain.GpsFix
import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    val gpsFix: Flow<GpsFix?>
    fun start()
    fun stop()
}

@Composable
expect fun rememberLocationProvider(): LocationProvider