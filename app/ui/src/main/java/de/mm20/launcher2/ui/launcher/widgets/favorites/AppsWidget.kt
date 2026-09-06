package de.mm20.launcher2.ui.launcher.widgets.favorites

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import de.mm20.launcher2.ui.launcher.sheets.LocalBottomSheetManager
import de.mm20.launcher2.ui.component.terminal.slugify
import de.mm20.launcher2.ui.component.terminal.rememberHostSlug
import de.mm20.launcher2.ui.component.terminal.TerminalKey
import de.mm20.launcher2.ui.component.terminal.TerminalPrompt
import de.mm20.launcher2.ui.locals.LocalTerminalMode
import de.mm20.launcher2.widgets.FavoritesWidgetLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.search.Tag
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.common.FavoritesTagSelector
import de.mm20.launcher2.ui.component.Banner
import de.mm20.launcher2.ui.launcher.search.common.grid.SearchResultGrid
import de.mm20.launcher2.widgets.AppsWidget

@Composable
fun AppsWidget(widget: AppsWidget) {
    val viewModel: AppsWidgetVM = viewModel(key = "favorites-widget-${widget.id}")
    val favorites by remember { viewModel.favorites }.collectAsState(emptyList())
    val pinnedTags by viewModel.pinnedTags.collectAsState(emptyList())
    val selectedTag by viewModel.selectedTag.collectAsState(null)

    val customTags = widget.config.customTags && widget.config.tagList.isNotEmpty()
    val favoritesEditButton = !customTags && widget.config.editButton

    val compactTags = widget.config.compactTags
    val tagsExpanded = widget.config.tagsMultiline

    LaunchedEffect(widget) {
        viewModel.updateWidget(widget)
    }

    val terminalMode = LocalTerminalMode.current
    val sheetManager = LocalBottomSheetManager.current
    val host = rememberHostSlug()
    val showTagRow = favoritesEditButton || (!customTags && pinnedTags.isNotEmpty()) || (customTags && widget.config.tagList.size > 1)

    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        if (terminalMode) {
            TerminalPrompt(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                host = host,
                dir = selectedTag?.let { "~/apps/" + slugify(it) } ?: "~/apps",
                command = "ls",
            ) {
                if (favoritesEditButton) {
                    TerminalKey("edit") { sheetManager.showEditFavoritesSheet() }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            if (terminalMode || widget.config.layout == FavoritesWidgetLayout.Menu) {
                FavoritesMenu(
                    items = favorites,
                    hotkeys = terminalMode || widget.config.hotkeys,
                    showIcons = !terminalMode && widget.config.menuIcons,
                )
            } else {
                SearchResultGrid(favorites, transitionKey = selectedTag)
            }
        } else {
            Banner(
                modifier = Modifier.padding(16.dp),
                text = stringResource(
                    if (selectedTag == null) R.string.favorites_empty else R.string.favorites_empty_tag
                ),
                icon = if (selectedTag == null) R.drawable.star_24px else R.drawable.tag_24px,
            )
        }
        if (showTagRow && terminalMode) {
            val tags = if (customTags) widget.config.tagList.map { Tag(it) } else pinnedTags
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!customTags) {
                        TerminalKey(stringResource(R.string.favorites).lowercase(), active = selectedTag == null) {
                            viewModel.selectTag(null)
                        }
                    }
                    for (tag in tags) {
                        TerminalKey(tag.tag, active = selectedTag == tag.tag) { viewModel.selectTag(tag.tag) }
                    }
                }
            }
        } else if (showTagRow) {
            FavoritesTagSelector(
                tags = if (customTags) widget.config.tagList.map { Tag(it) } else pinnedTags,
                selectedTag = selectedTag,
                editButton = favoritesEditButton,
                reverse = false,
                onSelectTag = { viewModel.selectTag(it) },
                scrollState = rememberScrollState(),
                expanded = tagsExpanded,
                compact = compactTags,
                onExpand = { viewModel.setTagsExpanded(it) },
                showFavorites = !customTags
            )
        }
    }
}