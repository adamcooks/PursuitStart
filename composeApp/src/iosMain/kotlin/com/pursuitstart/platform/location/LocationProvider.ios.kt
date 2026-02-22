package com.pursuitstart.platform.location

import androidx.compose.runtime.Composable
import com.pursuitstart.domain.GpsFix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private class IosStubLocationProvider : LocationProvider {
    override val gpsFix: Flow<GpsFix?> = flowOf(null)
    override fun start() {}
    override fun stop() {}
}

@Composable
actual fun rememberLocationProvider(): LocationProvider = IosStubLocationProvider()