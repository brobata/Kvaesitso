package de.mm20.launcher2.ui.launcher.widgets.clock.clocks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.ui.locals.LocalDarkTheme
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Shell-style clock: a `❯ date` prompt, the time in monospace, and the date underneath, as if
 * the home screen had just run the command.
 */
@Composable
fun TerminalClock(
    time: Long,
    compact: Boolean,
    showSeconds: Boolean,
    twentyFourHours: Boolean,
    useThemeColor: Boolean,
    darkColors: Boolean,
) {
    val color = if (useThemeColor) {
        if (!darkColors) {
            if (LocalDarkTheme.current) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.primaryContainer
        } else {
            if (LocalDarkTheme.current) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.primary
        }
    } else {
        LocalContentColor.current
    }

    val timePattern = when {
        twentyFourHours && showSeconds -> "HH:mm:ss"
        twentyFourHours -> "HH:mm"
        showSeconds -> "hh:mm:ss"
        else -> "hh:mm"
    }
    val locale = Locale.getDefault()
    val timeText = SimpleDateFormat(timePattern, locale).format(time)

    if (compact) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                color = color,
            ),
        )
        return
    }

    Column(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "❯ date",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = color.copy(alpha = 0.6f),
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
            ),
            color = color,
        )
        Text(
            text = SimpleDateFormat("EEE MMM d yyyy", locale).format(time),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
            ),
            color = color.copy(alpha = 0.85f),
        )
    }
}
