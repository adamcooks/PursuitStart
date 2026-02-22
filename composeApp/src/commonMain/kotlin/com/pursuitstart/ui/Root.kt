package com.pursuitstart.ui

import androidx.compose.runtime.*
import com.pursuitstart.platform.KeepAwake
import com.pursuitstart.platform.back.PlatformBackHandler
private enum class Screen { TimeEntry, Main }

@Composable
fun Root() {
    KeepAwake(enabled = true)

    var screen by remember { mutableStateOf(Screen.TimeEntry) }
    var startTimeMillis by remember { mutableStateOf<Long?>(null) }

    // Intercept Android system back ONLY on the main screen to navigate within the app.
    PlatformBackHandler(enabled = screen == Screen.Main) {
        screen = Screen.TimeEntry
    }

    when (screen) {
        Screen.TimeEntry -> TimeEntryScreen(
            onStartTimeConfirmed = { millis ->
                startTimeMillis = millis
                screen = Screen.Main
            }
        )

        Screen.Main -> PursuitMainScreen(
            startTimeMillis = startTimeMillis!!,
            onBack = { screen = Screen.TimeEntry }
        )
    }
}