package com.pursuitstart.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * TimeEntryScreen
 *
 * Purpose:
 * - Collect a start time in HH:MM:SS (24h)
 * - Provide a live "Scheduled: Today/Tomorrow HH:MM:SS" preview
 * - Confirm => returns resolved start time in epoch millis
 *
 * Race-day philosophy:
 * - Minimal moving parts
 * - Predictable typing behavior (no cursor jump)
 * - Clear validation + rollover rule
 */
@Composable
fun TimeEntryScreen(
    onStartTimeConfirmed: (Long) -> Unit
) {
    /* =========================
       STATE
       ========================= */

    // Store only digits the user typed: "HHMMSS" (0..6 digits).
    // We format these digits for display, which avoids cursor placement bugs.
    var digits by remember { mutableStateOf("") }

    // We control the cursor using TextFieldValue (force cursor to end).
    var field by remember { mutableStateOf(TextFieldValue("")) }

    // UI messages
    var error by remember { mutableStateOf<String?>(null) }
    var resolvedLabel by remember { mutableStateOf<String?>(null) }

    /* =========================
       UI
       ========================= */

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Text("Pursuit Start", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        // Instruction
        Text("Enter start time (24h): HH:MM:SS")
        Spacer(Modifier.height(8.dp))

        // Input
        OutlinedTextField(
            value = field,
            onValueChange = { incoming ->
                // 1) Keep digits only; limit to 6 (HHMMSS)
                val newDigits = incoming.text.filter { it.isDigit() }.take(6)
                digits = newDigits

                // 2) Format for display as HH:MM:SS (progressively)
                val formatted = formatAsHhMmSsFromDigits(newDigits)

                // 3) Force cursor to end for stable typing (no weird jumps)
                field = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                )

                // 4) Clear error while typing and update preview label
                error = null
                resolvedLabel = previewResolvedDayLabel(formatted)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("08:32:56") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // Live preview (Today/Tomorrow) once input is complete
        resolvedLabel?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        // Validation error
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        // Continue
        Button(
            onClick = {
                val formatted = field.text
                val result = parseStartTimeMillisWithRollover(formatted)
                if (result == null) {
                    error = "Invalid format. Enter HH:MM:SS (24h)."
                    return@Button
                }
                onStartTimeConfirmed(result)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = field.text.length == 8
        ) {
            Text("Continue")
        }
    }
}

/* =========================
   INPUT FORMATTING
   ========================= */

/**
 * Formats digits (HHMMSS) as HH:MM:SS progressively.
 *  ""        -> ""
 *  "0"       -> "0"
 *  "01"      -> "01"
 *  "010"     -> "01:0"
 *  "0101"    -> "01:01"
 *  "01010"   -> "01:01:0"
 *  "010101"  -> "01:01:01"
 */
private fun formatAsHhMmSsFromDigits(digits: String): String {
    val d = digits.take(6)
    return when (d.length) {
        0 -> ""
        1, 2 -> d
        3, 4 -> d.substring(0, 2) + ":" + d.substring(2)
        else -> d.substring(0, 2) + ":" + d.substring(2, 4) + ":" + d.substring(4)
    }
}

/* =========================
   PARSING + ROLLOVER LOGIC
   ========================= */

/**
 * Parses HH:MM:SS and returns epoch millis.
 *
 * Rollover rule:
 * - If the entered time is already in the past "today",
 *   interpret it as "tomorrow" at that time.
 *
 * Notes:
 * - We use kotlinx-datetime to create a LocalDateTime for today's date in the current timezone.
 * - For rollover we add 24h in millis, which is "good enough" for this use case.
 *   (If you need DST-perfect rollover, we can do LocalDate + 1 day in the timezone.)
 */
private fun parseStartTimeMillisWithRollover(hhmmss: String): Long? {
    val parts = hhmmss.trim().split(":")
    if (parts.size != 3) return null

    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val s = parts[2].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59 || s !in 0..59) return null

    val tz = TimeZone.currentSystemDefault()

    val nowInstant = Clock.System.now()
    val nowMillis = nowInstant.toEpochMilliseconds()

    val today = kotlinx.datetime.Clock.System.todayIn(tz)
    val todayLdt = LocalDateTime(
        year = today.year,
        monthNumber = today.monthNumber,
        dayOfMonth = today.dayOfMonth,
        hour = h,
        minute = m,
        second = s
    )
    var startMillis = todayLdt.toInstant(tz).toEpochMilliseconds()

    // If the time has already passed today, treat it as tomorrow.
    if (startMillis <= nowMillis) {
        startMillis += 24L * 60L * 60L * 1000L
    }

    return startMillis
}

/* =========================
   PREVIEW LABEL
   ========================= */

/**
 * Shows "Scheduled: Today HH:MM:SS" vs "Scheduled: Tomorrow HH:MM:SS"
 * once the input is complete.
 *
 * We compute the resolved start millis and compare to "now".
 * If it lands more than ~20 hours away, it's effectively tomorrow.
 */
private fun previewResolvedDayLabel(formatted: String): String? {
    if (formatted.length != 8) return null

    val millis = parseStartTimeMillisWithRollover(formatted) ?: return null
    val nowMillis = Clock.System.now().toEpochMilliseconds()

    val deltaHours = (millis - nowMillis).toDouble() / (1000.0 * 60.0 * 60.0)

    return if (deltaHours > 20.0) "Scheduled: Tomorrow $formatted"
    else "Scheduled: Today $formatted"
}