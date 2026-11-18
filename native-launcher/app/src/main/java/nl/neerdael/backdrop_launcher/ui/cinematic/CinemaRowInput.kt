package nl.neerdael.backdrop_launcher.ui.cinematic

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*

internal fun Modifier.cinemaRowInput(focus: CinemaFocus, onRow: (Int) -> Unit, rtl: Boolean = false) =
    focusRequester(focus.row).onPreviewKeyEvent {
        if (it.type != KeyEventType.KeyDown) false else when (it.key) {
            Key.DirectionLeft -> { onRow(if (rtl) 1 else -1); true }
            Key.DirectionRight -> { onRow(if (rtl) -1 else 1); true }
            Key.DirectionDown -> { focus.dock.requestFocus(); true }
            Key.DirectionUp -> { focus.nav.requestFocus(); true }; else -> false
        }
    }
