package de.mm20.launcher2.ui.component

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.preferences.SearchBarStyle
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.layout.BottomReversed
import de.mm20.launcher2.ui.theme.transparency.transparency

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    style: SearchBarStyle,
    level: SearchBarLevel,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocus: () -> Unit = {},
    onUnfocus: () -> Unit = {},
    reverse: Boolean = false,
    darkColors: Boolean = false,
    readOnly: Boolean = false,
    menu: @Composable RowScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {},
    onKeyboardActionGo: (KeyboardActionScope.() -> Unit)? = null
) {

    val transition = updateTransition(level, label = "Searchbar")
    val context = LocalContext.current

    // Terminal shares Transparent's resting behaviour (no card over the wallpaper).
    val transparentLike = style == SearchBarStyle.Transparent || style == SearchBarStyle.Terminal
    val inputTextStyle = MaterialTheme.typography.titleMedium.let {
        if (style == SearchBarStyle.Terminal) it.copy(fontFamily = FontFamily.Monospace) else it
    }

    val elevation by transition.animateDp(
        label = "elevation",
        transitionSpec = {
            when {
                initialState == SearchBarLevel.Resting -> tween(
                    durationMillis = 200,
                    delayMillis = 200
                )

                targetState == SearchBarLevel.Resting -> tween(durationMillis = 200)
                else -> tween(durationMillis = 500)
            }
        }
    ) {
        when {
            style == SearchBarStyle.Terminal -> 0.dp
            it == SearchBarLevel.Resting && style != SearchBarStyle.Solid -> 0.dp
            it == SearchBarLevel.Raised -> 8.dp
            else -> 0.dp
        }
    }

    val backgroundOpacity by transition.animateFloat(label = "backgroundOpacity",
        transitionSpec = {
            when {
                initialState == SearchBarLevel.Resting -> tween(durationMillis = 200)
                targetState == SearchBarLevel.Resting -> tween(
                    durationMillis = 200,
                    delayMillis = 200
                )

                else -> tween(durationMillis = 200)
            }
        }) {
        when {
            it == SearchBarLevel.Active -> MaterialTheme.transparency.surface
            !transparentLike -> 1f
            it == SearchBarLevel.Resting -> 0f
            else -> 1f
        }
    }

    val contentColor by transition.animateColor(label = "textColor",
        transitionSpec = {
            when {
                initialState == SearchBarLevel.Resting -> tween(durationMillis = 200)
                targetState == SearchBarLevel.Resting -> tween(
                    durationMillis = 200,
                    delayMillis = 200
                )

                else -> tween(durationMillis = 500)
            }
        }) {
        when {
            !transparentLike -> MaterialTheme.colorScheme.onSurface
            it == SearchBarLevel.Resting -> if (darkColors) Color(0, 0, 0, 180) else Color.White
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    val opacity by transition.animateFloat(label = "opacity") {
        if (style == SearchBarStyle.Hidden && it == SearchBarLevel.Resting) 0f
        else 1f
    }

    LauncherCard(
        modifier = modifier
            .alpha(opacity),
        backgroundOpacity = backgroundOpacity,
        elevation = elevation
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            Column(
                verticalArrangement = if (reverse) Arrangement.BottomReversed else Arrangement.Top
            ) {
                Row(
                    modifier = Modifier.height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (style == SearchBarStyle.Terminal) {
                        Text(
                            modifier = Modifier.padding(start = 16.dp, end = 10.dp),
                            text = "$",
                            style = inputTextStyle.copy(fontWeight = FontWeight.Bold),
                            color = contentColor,
                        )
                    } else {
                        Icon(
                            modifier = Modifier.padding(12.dp),
                            painter = painterResource(R.drawable.search_24px),
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_bar_placeholder),
                                style = inputTextStyle,
                                color = contentColor.copy(alpha = if (style == SearchBarStyle.Terminal) 0.6f else contentColor.alpha)
                            )
                        }
                        // Keep a TextFieldValue so text injected from outside (hardware
                        // type-to-search) lands with the cursor at the end, not at index 0.
                        var textFieldValue by remember {
                            mutableStateOf(TextFieldValue(value, TextRange(value.length)))
                        }
                        if (textFieldValue.text != value) {
                            textFieldValue = TextFieldValue(value, TextRange(value.length))
                        }
                        BasicTextField(
                            modifier = Modifier
                                .onFocusChanged {
                                    if (it.hasFocus) onFocus()
                                }
                                .focusRequester(focusRequester)
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = context.getString(R.string.search_bar_placeholder)
                                },
                            textStyle = inputTextStyle.copy(
                                color = contentColor
                            ),
                            singleLine = true,
                            value = textFieldValue,
                            onValueChange = {
                                textFieldValue = it
                                if (it.text != value) onValueChange(it.text)
                            },
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go,
                                autoCorrectEnabled = false,
                                capitalization = KeyboardCapitalization.None,
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = onKeyboardActionGo,
                            ),
                            readOnly = readOnly,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        menu()
                    }
                }
                this.actions()
            }
        }
    }
}

enum class SearchBarLevel: Comparable<SearchBarLevel> {
    /**
     * The default, "hidden" state, when the launcher is in its initial state (scroll position is 0
     * and search is closed)
     */
    Resting,

    /**
     * When the search is open but there is no content behind the search bar (scroll position is 0)
     */
    Active,

    /**
     * When there is content below the search bar which requires the search bar to be raised above
     * this content (scroll position is not 0)
     */
    Raised
}