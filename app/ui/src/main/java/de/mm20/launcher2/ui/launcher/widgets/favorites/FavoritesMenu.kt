package de.mm20.launcher2.ui.launcher.widgets.favorites

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.ui.component.ShapedLauncherIcon
import de.mm20.launcher2.ui.ktx.toPixels
import de.mm20.launcher2.ui.launcher.keyboard.LocalHomeHotkeys
import de.mm20.launcher2.ui.launcher.keyboard.menuHotkeyForIndex
import de.mm20.launcher2.ui.launcher.scaffold.LocalScaffoldPage
import de.mm20.launcher2.ui.launcher.scaffold.ScaffoldPage
import de.mm20.launcher2.ui.launcher.search.common.SearchableItemVM
import de.mm20.launcher2.ui.launcher.search.common.grid.ItemPopup
import de.mm20.launcher2.ui.launcher.search.listItemViewModel
import de.mm20.launcher2.ui.locals.LocalGridSettings

/**
 * Favorites rendered as a launcher menu: one row per item, prefixed with its hotkey.
 * While the menu is on the idle home screen, pressing a row's hotkey on a hardware keyboard
 * launches it; tapping does the same and long-press opens the item popup.
 */
@Composable
fun FavoritesMenu(
    items: List<SavableSearchable>,
    hotkeys: Boolean,
    showIcons: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        items.forEachIndexed { index, item ->
            key(item.key) {
                FavoritesMenuRow(
                    item = item,
                    hotkey = if (hotkeys) menuHotkeyForIndex(index) else null,
                    showIcon = showIcons,
                )
            }
        }
    }
}

@Composable
private fun FavoritesMenuRow(
    item: SavableSearchable,
    hotkey: Char?,
    showIcon: Boolean,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val hotkeyRegistry = LocalHomeHotkeys.current
    val onHomePage = LocalScaffoldPage.current == ScaffoldPage.Home

    // Same view model key as the grid so icon and badge loads are shared.
    val viewModel: SearchableItemVM = listItemViewModel(key = "search-${item.key}")
    val iconSize = LocalGridSettings.current.iconSize.dp.toPixels()

    LaunchedEffect(item, iconSize) {
        viewModel.init(item, iconSize.toInt())
    }

    var showPopup by remember(item.key) { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(IntRect.Zero) }

    LaunchedEffect(showPopup) {
        if (showPopup) viewModel.requestUpdatedSearchable(context)
    }

    val launchOnPress = !item.preferDetailsOverLaunch
    val activate = {
        if (!launchOnPress || !viewModel.launch(context, bounds)) {
            showPopup = true
        }
    }

    DisposableEffect(hotkeyRegistry, onHomePage, hotkey, item.key) {
        if (hotkeyRegistry == null || !onHomePage || hotkey == null) {
            return@DisposableEffect onDispose {}
        }
        val action: () -> Unit = { activate() }
        hotkeyRegistry.register(hotkey, action)
        onDispose { hotkeyRegistry.unregister(hotkey, action) }
    }

    val badge by viewModel.badge.collectAsStateWithLifecycle()
    val icon by viewModel.icon.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = activate,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showPopup = true
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .onGloballyPositioned {
                bounds = it.boundsInWindow().roundToIntRect()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hotkey?.let { "[$it]" } ?: "",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        if (showIcon) {
            ShapedLauncherIcon(
                modifier = Modifier.padding(end = 12.dp),
                size = 24.dp,
                icon = { icon },
                badge = { badge },
            )
        }
        Text(
            text = item.labelOverride ?: item.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (showPopup) {
        ItemPopup(origin = bounds, searchable = item, onDismissRequest = { showPopup = false })
    }
}
