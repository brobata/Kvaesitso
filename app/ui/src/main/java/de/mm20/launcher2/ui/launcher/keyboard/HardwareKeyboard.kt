package de.mm20.launcher2.ui.launcher.keyboard

import android.view.KeyEvent
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Bridges hardware key events from the activity's `dispatchKeyEvent` into Compose.
 *
 * The activity owns one instance and offers every [KeyEvent] to it before the view hierarchy
 * sees the event. The launcher scaffold installs a handler while it is composed and returns
 * `true` for events it consumed (for example, the first key press that opens search).
 */
class HardwareKeyDispatcher {
    @Volatile
    var handler: ((KeyEvent) -> Boolean)? = null

    fun dispatch(event: KeyEvent): Boolean = handler?.invoke(event) ?: false
}

val LocalHardwareKeyDispatcher = staticCompositionLocalOf<HardwareKeyDispatcher?> { null }

/**
 * Single-character shortcuts that fire while the home screen is idle (no search open, nothing
 * focused). Components register an action for a character while they are on the home page and
 * remove it when they leave composition. Registration is keyed by the typed character rather than
 * the key code so that keyboards which produce digits through a modifier layer (Alt+letter on
 * BlackBerry-style QWERTY boards) still match.
 */
class HomeHotkeyRegistry {
    private val actions = HashMap<Char, () -> Unit>()

    @Synchronized
    fun register(char: Char, action: () -> Unit) {
        actions[char] = action
    }

    /** Removes [action] for [char] only if it is still the registered one. */
    @Synchronized
    fun unregister(char: Char, action: () -> Unit) {
        if (actions[char] === action) actions.remove(char)
    }

    fun dispatch(char: Char): Boolean {
        val action = synchronized(this) { actions[char] } ?: return false
        action()
        return true
    }
}

val LocalHomeHotkeys = staticCompositionLocalOf<HomeHotkeyRegistry?> { null }

/** Hotkey character for the n-th menu row: rows 0..8 map to 1..9, row 9 maps to 0. */
fun menuHotkeyForIndex(index: Int): Char? = when (index) {
    in 0..8 -> '1' + index
    9 -> '0'
    else -> null
}
