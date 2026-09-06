package de.mm20.launcher2.ui.launcher.widgets.calendar

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import de.mm20.launcher2.search.CalendarEvent
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.MissingPermissionBanner
import de.mm20.launcher2.ui.component.terminal.TerminalKey
import de.mm20.launcher2.ui.component.terminal.TerminalLine
import de.mm20.launcher2.ui.component.terminal.TerminalPrompt
import de.mm20.launcher2.ui.component.terminal.terminalTextStyle
import de.mm20.launcher2.ui.launcher.search.common.grid.ItemPopup
import de.mm20.launcher2.ui.locals.LocalTimeFormat
import de.mm20.launcher2.ui.utils.isTwentyFourHours
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Calendar as a directory listing. The selected day is the "machine", `~/calendar` the
 * directory, and each event a file: `HH:MM  title`. Tapping a line opens the event in the
 * calendar app; long-press shows the launcher's details popup.
 */
@Composable
internal fun TerminalCalendar(viewModel: CalendarWidgetVM) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate
    val events by viewModel.calendarEvents
    val nextEvents by viewModel.nextEvents
    val runningEvents by viewModel.hiddenPastEvents
    val runningTasks by viewModel.hiddenRunningTasks
    val hasPermission by viewModel.hasPermission.collectAsState()
    val pinnedEvents by viewModel.pinnedCalendarEvents.collectAsState()
    val dates = viewModel.availableDates
    val dateIndex = dates.indexOf(selectedDate)

    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TerminalPrompt(
            host = daySlug(selectedDate),
            dir = "~/calendar",
            command = "ls",
        ) {
            TerminalKey("‹", enabled = dateIndex > 0) { viewModel.previousDay() }
            TerminalKey("›", enabled = dateIndex in 0 until dates.lastIndex) { viewModel.nextDay() }
            TerminalKey("+") { viewModel.createEvent(context) }
            TerminalKey("cal") { viewModel.openCalendarApp(context) }
        }

        if (hasPermission == false) {
            MissingPermissionBanner(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.missing_permission_calendar_widget),
                onClick = { viewModel.requestCalendarPermission(context as AppCompatActivity) },
            )
        }
        if (events.isEmpty() && hasPermission == true) {
            TerminalLine(stringResource(R.string.calendar_widget_no_events), color = dim)
        }
        for (event in events) {
            key(event.key) { EventLine(event) }
        }
        if (runningEvents > 0) {
            TerminalLine(
                pluralStringResource(R.plurals.calendar_widget_running_events, runningEvents, runningEvents),
                leading = "+",
                color = dim,
                onClick = { viewModel.showAllEvents() },
            )
        }
        if (runningTasks > 0) {
            TerminalLine(
                pluralStringResource(R.plurals.calendar_widget_running_tasks, runningTasks, runningTasks),
                leading = "+",
                color = dim,
                onClick = { viewModel.showAllTasks() },
            )
        }
        if (nextEvents.isNotEmpty()) {
            TerminalLine("# " + stringResource(R.string.terminal_calendar_upcoming), color = MaterialTheme.colorScheme.tertiary)
            for (event in nextEvents) {
                key(event.key) { EventLine(event, showDate = true) }
            }
        }
        val pinned = pinnedEvents.filterIsInstance<CalendarEvent>()
        if (pinned.isNotEmpty()) {
            TerminalLine("# " + stringResource(R.string.terminal_calendar_pinned), color = MaterialTheme.colorScheme.tertiary)
            for (event in pinned) {
                key(event.key) { EventLine(event, showDate = true) }
            }
        }
    }
}

@Composable
private fun EventLine(event: CalendarEvent, showDate: Boolean = false) {
    val context = LocalContext.current
    val timeFormat = LocalTimeFormat.current
    val twentyFourHours = timeFormat.isTwentyFourHours(context)
    val style = terminalTextStyle()
    val colors = MaterialTheme.colorScheme

    val leading = remember(event, showDate, twentyFourHours) {
        val stamp = event.startTime ?: event.endTime
        val datePart = if (showDate) SimpleDateFormat("EEE ", Locale.getDefault()).format(stamp) else ""
        val timePart = if (event.allDay) {
            context.getString(R.string.terminal_calendar_all_day)
        } else {
            SimpleDateFormat(if (twentyFourHours) "HH:mm" else "h:mma", Locale.getDefault())
                .format(stamp).lowercase(Locale.getDefault())
        }
        datePart + timePart
    }
    val marker = when {
        event.isTask -> if (event.isCompleted == true) "[x]" else "[ ]"
        else -> "●"
    }
    val markerColor = event.color?.let { Color(it) } ?: colors.primary

    var showPopup by remember(event.key) { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(IntRect.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (!event.launch(context, null)) showPopup = true },
                onLongClick = { showPopup = true },
            )
            .padding(vertical = 5.dp)
            .onGloballyPositioned { bounds = it.boundsInWindow().roundToIntRect() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            leading,
            style = style,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(if (showDate) 104.dp else 72.dp),
        )
        Text(marker, style = style, color = markerColor, maxLines = 1)
        Text(
            " " + (event.labelOverride ?: event.label),
            style = style,
            color = if (event.isCompleted == true) colors.onSurfaceVariant else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }

    if (showPopup) {
        ItemPopup(origin = bounds, searchable = event, onDismissRequest = { showPopup = false })
    }
}

/** `sun-sep-06`: the day as a hostname. */
private fun daySlug(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("EEE-MMM-dd", Locale.getDefault()))
        .lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9-]"), "")
}
