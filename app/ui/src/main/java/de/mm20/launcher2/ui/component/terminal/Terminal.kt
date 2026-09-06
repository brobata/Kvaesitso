package de.mm20.launcher2.ui.component.terminal

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Building blocks for terminal mode. Every widget is rendered as the output of a command:
 * a prompt line `host:dir ❯ command`, then plain monospace lines. No cards, no icons.
 */

@Composable
fun terminalTextStyle(): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

/** `titan-2` for a Titan 2; the device model as a shell-friendly slug. */
@Composable
fun rememberHostSlug(): String = remember { slugify(Build.MODEL).ifEmpty { "android" } }

fun slugify(text: String): String =
    text.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')

/**
 * A prompt line. [host] is yellow (secondary), [dir] is blue (tertiary, like `ls` colors
 * directories), the chevron is orange (primary), the command is plain. [trailing] can hold
 * bracketed [TerminalKey] actions, right-aligned.
 */
@Composable
fun TerminalPrompt(
    host: String,
    dir: String,
    command: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val style = terminalTextStyle()
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(host, style = style, color = colors.secondary, maxLines = 1)
            Text(":", style = style, color = colors.onSurfaceVariant)
            Text(dir, style = style, color = colors.tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(" ❯ ", style = style.copy(fontWeight = FontWeight.Bold), color = colors.primary)
            Text(command, style = style, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = trailing,
        )
    }
}

/** A bracketed, tappable keyword: `[edit]`. */
@Composable
fun TerminalKey(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = "[$label]",
        modifier = modifier
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        style = terminalTextStyle().copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Normal),
        color = when {
            !enabled -> colors.onSurfaceVariant.copy(alpha = 0.5f)
            active -> colors.primary
            else -> colors.onSurfaceVariant
        },
        maxLines = 1,
    )
}

/** One output line. Optional leading column keeps rows aligned like `ls -l`. */
@Composable
fun TerminalLine(
    text: String,
    modifier: Modifier = Modifier,
    leading: String? = null,
    leadingColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val style = terminalTextStyle()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick ?: {},
                        onLongClick = onLongClick,
                    )
                } else Modifier
            )
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Text(leading, style = style, color = leadingColor, maxLines = 1)
            Text("  ", style = style)
        }
        Text(
            text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** stderr-style notice: `! message` with optional actions underneath. */
@Composable
fun TerminalNotice(
    text: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val style = terminalTextStyle()
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text("! ", style = style.copy(fontWeight = FontWeight.Bold), color = colors.error)
            Text(text, style = style, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
        if (actions != null) {
            // Callers pass Material buttons; render them as bare orange words instead of pills.
            MaterialTheme(
                colorScheme = colors.copy(
                    primary = androidx.compose.ui.graphics.Color.Transparent,
                    onPrimary = colors.primary,
                    outline = androidx.compose.ui.graphics.Color.Transparent,
                    surfaceContainer = androidx.compose.ui.graphics.Color.Transparent,
                    surfaceContainerHigh = androidx.compose.ui.graphics.Color.Transparent,
                ),
                shapes = MaterialTheme.shapes,
                typography = MaterialTheme.typography,
            ) {
                Row(
                    modifier = Modifier.padding(top = 2.dp, start = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
