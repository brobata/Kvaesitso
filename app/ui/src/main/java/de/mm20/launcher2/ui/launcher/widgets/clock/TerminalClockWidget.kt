package de.mm20.launcher2.ui.launcher.widgets.clock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.preferences.ui.ClockWidgetSettings
import de.mm20.launcher2.ui.base.LocalTime
import de.mm20.launcher2.ui.component.terminal.TerminalPrompt
import de.mm20.launcher2.ui.component.terminal.rememberHostSlug
import de.mm20.launcher2.ui.locals.LocalTimeFormat
import de.mm20.launcher2.ui.utils.isTwentyFourHours
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The clock in terminal mode: `titan-2:~ ❯ date`, the time and the date. Battery lives in the
 * status line, so the icon-based dynamic parts stay out of the terminal.
 */
@Composable
internal fun TerminalClockWidget(
    modifier: Modifier,
    fillScreenHeight: Boolean,
    viewModel: ClockWidgetVM,
) {
    val context = LocalContext.current
    val time = LocalTime.current
    val clockSettings: ClockWidgetSettings = koinInject()
    val showSeconds by clockSettings.showSeconds.collectAsState(initial = false)
    val twentyFourHours = LocalTimeFormat.current.isTwentyFourHours(context)
    val host = rememberHostSlug()
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()

    val timePattern = when {
        twentyFourHours && showSeconds -> "HH:mm:ss"
        twentyFourHours -> "HH:mm"
        showSeconds -> "hh:mm:ss"
        else -> "hh:mm"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = if (fillScreenHeight) Arrangement.Center else Arrangement.Top,
    ) {
        TerminalPrompt(host = host, dir = "~", command = "date")
        Text(
            text = SimpleDateFormat(timePattern, locale).format(time),
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
            ),
            color = colors.onSurface,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { viewModel.launchClockApp(context) },
        )
        Text(
            text = SimpleDateFormat("EEE MMM d yyyy", locale).format(time),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
            ),
            color = colors.onSurfaceVariant,
        )
    }
}
