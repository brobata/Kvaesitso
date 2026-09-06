package de.mm20.launcher2.ui.launcher.scaffold

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import de.mm20.launcher2.ui.base.LocalTime
import java.text.SimpleDateFormat
import java.util.Locale

internal val TerminalStatusLineHeight = 26.dp

/**
 * tmux-style status line: `[host] 0:home* 1:search` on the left, battery, date and time on the
 * right. Pinned to the bottom of the launcher by the scaffold when the setting is on.
 */
@Composable
internal fun TerminalStatusLine(
    modifier: Modifier = Modifier,
    searchActive: Boolean,
) {
    val context = LocalContext.current
    val time = LocalTime.current
    val battery by rememberBatteryStatus(context)
    val host = remember {
        Build.MODEL.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "android" }
    }
    val mono = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TerminalStatusLineHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("[$host]", style = mono, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        StatusWindow("0:home", active = !searchActive, style = mono)
        Spacer(Modifier.width(8.dp))
        StatusWindow("1:search", active = searchActive, style = mono)
        Spacer(Modifier.weight(1f))
        if (battery.first >= 0) {
            Text("${battery.first}%" + if (battery.second) "+" else "", style = mono, color = dim)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            remember(time) { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(time) },
            style = mono,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            remember(time) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(time) },
            style = mono,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatusWindow(label: String, active: Boolean, style: TextStyle) {
    Text(
        text = if (active) "$label*" else label,
        style = if (active) style.copy(fontWeight = FontWeight.Bold) else style,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Battery level (0..100, or -1 when unknown) and whether it is charging. */
@Composable
private fun rememberBatteryStatus(context: Context): State<Pair<Int, Boolean>> {
    return produceState(initialValue = readBattery(context), context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                value = readBattery(context, intent)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitDispose { context.unregisterReceiver(receiver) }
    }
}

private fun readBattery(context: Context, intent: Intent? = null): Pair<Int, Boolean> {
    val batteryManager: BatteryManager? = context.getSystemService()
    val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    val charging = if (intent != null) {
        intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
    } else {
        batteryManager?.isCharging == true
    }
    return level to charging
}
