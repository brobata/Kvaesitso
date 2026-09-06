package de.mm20.launcher2.ui.settings.appearance

import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import de.mm20.launcher2.widgets.FavoritesWidgetLayout
import de.mm20.launcher2.widgets.FavoritesWidgetConfig
import de.mm20.launcher2.widgets.AppsWidget
import de.mm20.launcher2.themes.TerminalThemeId
import de.mm20.launcher2.themes.RectShapesId
import de.mm20.launcher2.themes.OpaqueTransparenciesId
import de.mm20.launcher2.themes.MonospaceId
import de.mm20.launcher2.themes.DefaultThemeId
import de.mm20.launcher2.preferences.WidgetScreenTarget
import de.mm20.launcher2.widgets.WidgetRepository
import de.mm20.launcher2.preferences.ui.ClockWidgetSettings
import de.mm20.launcher2.preferences.SearchBarStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.preferences.ColorScheme
import de.mm20.launcher2.preferences.ui.UiSettings
import de.mm20.launcher2.themes.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppearanceSettingsScreenVM : ViewModel(), KoinComponent {
    private val uiSettings: UiSettings by inject()

    private val themeRepository: ThemeRepository by inject()
    private val clockWidgetSettings: ClockWidgetSettings by inject()
    private val widgetRepository: WidgetRepository by inject()

    val colorScheme = uiSettings.colorScheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setColorScheme(colorScheme: ColorScheme) {
        uiSettings.setColorScheme(colorScheme)
    }

    val colorThemeName = uiSettings.colorsId.flatMapLatest {
        themeRepository.colors.getOrDefault(it)
    }.map {
        it.name
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val shapeThemeName = uiSettings.shapesId.flatMapLatest {
        themeRepository.shapes.getOrDefault(it)
    }.map {
        it.name
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val typographyThemeName = uiSettings.typographyId.flatMapLatest {
        themeRepository.typographies.getOrDefault(it)
    }.map {
        it.name
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val transparencyThemeName = uiSettings.transparenciesId.flatMapLatest {
        themeRepository.transparencies.getOrDefault(it)
    }.map {
        it.name
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val compatModeColors = uiSettings.compatModeColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun setCompatModeColors(enabled: Boolean) {
        uiSettings.setCompatModeColors(enabled)
    }

    /**
     * One-tap terminal look: monospace type, green-on-black static colors, square corners,
     * opaque surfaces, `$` prompt search bar, list-style results, and every favorites widget
     * switched to the numbered menu layout (one is created if none exists).
     */
    fun applyTerminalPreset() {
        uiSettings.setColorsId(TerminalThemeId)
        uiSettings.setTypographyId(MonospaceId)
        uiSettings.setShapesId(RectShapesId)
        uiSettings.setTransparenciesId(OpaqueTransparenciesId)
        uiSettings.setColorScheme(ColorScheme.Dark)
        uiSettings.setSearchBarStyle(SearchBarStyle.Terminal)
        uiSettings.setGridShowList(true)
        uiSettings.setDimWallpaper(true)
        uiSettings.setHomeScreenWidgets(true)
        clockWidgetSettings.setMonospaced(true)
        viewModelScope.launch {
            // Home-screen widgets live under the default widget screen target, not at the root.
            val homeScreen = WidgetScreenTarget.Default.id
            val homeWidgets = widgetRepository.get(parent = homeScreen).first()
            val favoritesWidgets = homeWidgets.filterIsInstance<AppsWidget>()
            if (favoritesWidgets.isEmpty()) {
                // set() renumbers positions, so the menu lands above the stock widgets.
                val menu = AppsWidget(
                    id = UUID.randomUUID(),
                    config = FavoritesWidgetConfig(layout = FavoritesWidgetLayout.Menu),
                )
                widgetRepository.set(listOf(menu) + homeWidgets, homeScreen)
            } else {
                for (widget in favoritesWidgets) {
                    if (widget.config.layout == FavoritesWidgetLayout.Menu) continue
                    widgetRepository.update(
                        widget.copy(config = widget.config.copy(layout = FavoritesWidgetLayout.Menu))
                    )
                }
            }
        }
    }

    /** Reverts everything [applyTerminalPreset] touches, except widgets. */
    fun applyDefaultPreset() {
        uiSettings.setColorsId(DefaultThemeId)
        uiSettings.setTypographyId(DefaultThemeId)
        uiSettings.setShapesId(DefaultThemeId)
        uiSettings.setTransparenciesId(DefaultThemeId)
        uiSettings.setColorScheme(ColorScheme.System)
        uiSettings.setSearchBarStyle(SearchBarStyle.Transparent)
        uiSettings.setGridShowList(false)
        uiSettings.setDimWallpaper(false)
        clockWidgetSettings.setMonospaced(false)
    }
}
